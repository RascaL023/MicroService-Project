package http

import (
	"auth-service/internal/dto/response"
	"auth-service/internal/service"
	"context"
	"errors"
	"net/http"
)

func (s *Server) requireAuthenticated(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		user, roles, authorities, err := s.authSvc.Authenticate(r.Context(), r.Header.Get("Authorization"))
		if err != nil {
			if errors.Is(err, service.ErrForbidden) {
				writeError(w, http.StatusForbidden, "You don't have access to this resource", "FORBIDDEN")
			} else {
				writeError(w, http.StatusUnauthorized, "You are not authenticated", "UNAUTHORIZED")
			}
			return
		}
		auth := authContext{
			User:        response.UserResponse{ID: user.ID, Email: user.Email, Status: user.Status, IsBanned: user.IsBanned},
			Roles:       set(roles),
			Authorities: set(authorities),
		}
		next.ServeHTTP(w, r.WithContext(context.WithValue(r.Context(), authContextKey, auth)))
	})
}

func (s *Server) requireAuthority(authority string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return s.requireAuthenticated(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			auth := r.Context().Value(authContextKey).(authContext)

			if !auth.Authorities[authority] {
				writeError(w, http.StatusForbidden, "You don't have access to this resource", "FORBIDDEN")
				return
			}

			next.ServeHTTP(w, r)
		}))
	}
}

func (s *Server) requireAnyAuthority(authorities ...string) func(http.Handler) http.Handler  {
	return func(h http.Handler) http.Handler {
		return s.requireAuthenticated(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			auth := r.Context().Value(authContextKey).(authContext)

			for _, authority := range authorities {
				if auth.Authorities[authority] {
					h.ServeHTTP(w, r)
					return 
				}
			}

			writeError(w, http.StatusForbidden, "You don't have access to this resource", "FORBIDDEN")
		}))
	}
}

func set(values []string) map[string]bool {
	result := make(map[string]bool, len(values))
	for _, value := range values {
		result[value] = true
	}
	return result
}
