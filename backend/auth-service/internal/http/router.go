package http

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"strconv"

	"auth-service/internal/config"
	"auth-service/internal/dto/request"
	"auth-service/internal/dto/response"
	"auth-service/internal/repository"
	"auth-service/internal/service"

	"github.com/go-chi/chi/v5"
	"github.com/go-chi/chi/v5/middleware"
)

type contextKey string

const authContextKey contextKey = "auth"

type authContext struct {
	User        response.UserResponse
	Roles       map[string]bool
	Authorities map[string]bool
}

type Server struct {
	cfg     config.Config
	authSvc *service.AuthService
	userSvc *service.UserService
	roleSvc *service.RoleService
}

func NewRouter(cfg config.Config, authSvc *service.AuthService, userSvc *service.UserService, roleSvc *service.RoleService) http.Handler {
	server := &Server{cfg: cfg, authSvc: authSvc, userSvc: userSvc, roleSvc: roleSvc}
	router := chi.NewRouter()
	router.Use(middleware.RequestID)
	router.Use(middleware.RealIP)
	router.Use(middleware.Recoverer)
	router.Use(middleware.SetHeader("Content-Type", "application/json"))

	router.Route("/api/auths", func(r chi.Router) {
		r.Post("/login", server.login)
		r.Post("/register", server.register)
		r.With(server.requireRole("ADMIN")).Get("/test", server.test)
	})

	router.Route("/api/users", func(r chi.Router) {
		r.With(server.requireAuthority("user.create")).Post("/", server.createUser)
		r.With(server.requireAuthority("user.readAll")).Get("/", server.listUsers)
		r.With(server.requireAuthenticated).Get("/{id}", server.getUserByID)
	})

	router.Route("/api/roles", func(r chi.Router) {
		r.Get("/", server.listRoles)
		r.Get("/{id}", server.getRoleByID)
	})

	router.Get("/health", func(w http.ResponseWriter, r *http.Request) {
		writeJSON(w, http.StatusOK, map[string]string{"status": "ok"})
	})
	return router
}

func (s *Server) login(w http.ResponseWriter, r *http.Request) {
	var req request.LoginRequest
	if !decode(w, r, &req) {
		return
	}
	loginResponse, err := s.authSvc.Login(r.Context(), req)
	respond(w, http.StatusOK, loginResponse, err)
}

func (s *Server) register(w http.ResponseWriter, r *http.Request) {
	var req request.RegisterRequest
	if !decode(w, r, &req) {
		return
	}
	registerResponse, err := s.userSvc.Register(r.Context(), req)
	respond(w, http.StatusCreated, registerResponse, err)
}

func (s *Server) test(w http.ResponseWriter, r *http.Request) {
	writeJSON(w, http.StatusOK, "Test Successful!")
}

func (s *Server) createUser(w http.ResponseWriter, r *http.Request) {
	var req request.UserRequest
	if !decode(w, r, &req) {
		return
	}
	userResponse, err := s.userSvc.Create(r.Context(), req)
	respond(w, http.StatusCreated, userResponse, err)
}

func (s *Server) listUsers(w http.ResponseWriter, r *http.Request) {
	page, size := pagination(r)
	users, total, err := s.userSvc.List(r.Context(), page, size)
	if err != nil {
		respond(w, http.StatusOK, nil, err)
		return
	}
	writePage(w, http.StatusOK, users, page, size, total)
}

func (s *Server) getUserByID(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r)
	if !ok {
		return
	}
	auth, _ := r.Context().Value(authContextKey).(authContext)
	if auth.User.ID != id && !auth.Authorities["user.readAll"] {
		writeError(w, http.StatusForbidden, "forbidden")
		return
	}
	userResponse, err := s.userSvc.GetByID(r.Context(), id)
	respond(w, http.StatusOK, userResponse, err)
}

func (s *Server) listRoles(w http.ResponseWriter, r *http.Request) {
	page, size := pagination(r)
	roles, total, err := s.roleSvc.List(r.Context(), page, size)
	if err != nil {
		respond(w, http.StatusOK, nil, err)
		return
	}
	writePage(w, http.StatusOK, roles, page, size, total)
}

func (s *Server) getRoleByID(w http.ResponseWriter, r *http.Request) {
	id, ok := pathID(w, r)
	if !ok {
		return
	}
	roleResponse, err := s.roleSvc.GetByID(r.Context(), id)
	respond(w, http.StatusOK, roleResponse, err)
}

func (s *Server) requireAuthenticated(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		user, roles, authorities, err := s.authSvc.Authenticate(r.Context(), r.Header.Get("Authorization"))
		if err != nil {
			writeError(w, http.StatusUnauthorized, "unauthorized")
			return
		}
		auth := authContext{
			User:        response.UserResponse{ID: user.ID, Username: user.Username},
			Roles:       set(roles),
			Authorities: set(authorities),
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), authContextKey, auth)))
	})
}

func (s *Server) requireRole(role string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return s.requireAuthenticated(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			auth := r.Context().Value(authContextKey).(authContext)
			if !auth.Roles[role] {
				writeError(w, http.StatusForbidden, "forbidden")
				return
			}
			next.ServeHTTP(w, r)
		}))
	}
}

func (s *Server) requireAuthority(authority string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return s.requireAuthenticated(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			auth := r.Context().Value(authContextKey).(authContext)
			if !auth.Roles["ADMIN"] && !auth.Authorities[authority] {
				writeError(w, http.StatusForbidden, "forbidden")
				return
			}
			next.ServeHTTP(w, r)
		}))
	}
}

func decode(w http.ResponseWriter, r *http.Request, target any) bool {
	defer r.Body.Close()
	if err := json.NewDecoder(r.Body).Decode(target); err != nil {
		writeError(w, http.StatusBadRequest, "invalid request body")
		return false
	}
	return true
}

func respond(w http.ResponseWriter, status int, data any, err error) {
	if err == nil {
		writeJSON(w, status, data)
		return
	}
	switch {
	case errors.Is(err, service.ErrValidation):
		writeError(w, http.StatusBadRequest, "request tidak valid")
	case errors.Is(err, service.ErrUnauthorized):
		writeError(w, http.StatusUnauthorized, "username atau password salah")
	case errors.Is(err, service.ErrForbidden):
		writeError(w, http.StatusForbidden, "forbidden")
	case errors.Is(err, repository.ErrNotFound):
		writeError(w, http.StatusNotFound, "data tidak ditemukkan")
	case errors.Is(err, repository.ErrDuplicate):
		writeError(w, http.StatusConflict, "data telah terdaftar")
	default:
		writeError(w, http.StatusInternalServerError, "internal server error")
	}
}

func pagination(r *http.Request) (int, int) {
	page, _ := strconv.Atoi(r.URL.Query().Get("page"))
	size, _ := strconv.Atoi(r.URL.Query().Get("size"))
	if page < 0 {
		page = 0
	}
	if size <= 0 {
		size = 10
	}
	if size > 100 {
		size = 100
	}
	return page, size
}

func pathID(w http.ResponseWriter, r *http.Request) (int64, bool) {
	id, err := strconv.ParseInt(chi.URLParam(r, "id"), 10, 64)
	if err != nil || id <= 0 {
		writeError(w, http.StatusBadRequest, "id tidak valid")
		return 0, false
	}
	return id, true
}

func set(values []string) map[string]bool {
	result := make(map[string]bool, len(values))
	for _, value := range values {
		result[value] = true
	}
	return result
}
