package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"auth-service/internal/config"
	"auth-service/internal/db"
	apphttp "auth-service/internal/http"
	"auth-service/internal/repository"
	"auth-service/internal/service"
)

func main() {
	cfg := config.Load()
	ctx := context.Background()

	db_pool, err := db.Connect(ctx, cfg.DatabaseURL)
	if err != nil {
		log.Fatalf("Connect database: %v", err)
	}
	defer db_pool.Close()

	// if err := db.Migrate(ctx, db_pool); err != nil {
	// 	log.Fatalf("migrate database: %v", err)
	// }

	repos := repository.New(db_pool)
	authSvc := service.NewAuthService(
		cfg,
		repos.Users,
		// repos.Sessions,
	)
	userSvc := service.NewUserService(repos.Users, repos.Roles)
	roleSvc := service.NewRoleService(repos.Roles)

	if err := service.SeedAdmin(ctx, cfg, repos.Users, repos.Roles); err != nil {
		log.Fatalf("seed admin: %v", err)
	}

	router := apphttp.NewRouter(cfg, authSvc, userSvc, roleSvc)
	server := &http.Server{
		Addr:              ":" + cfg.Port,
		Handler:           router,
		ReadHeaderTimeout: 5 * time.Second,
	}

	go func() {
		log.Printf("auth-service listening on :%s", cfg.Port)
		if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("listen: %v", err)
		}
	}()

	stop := make(chan os.Signal, 1)
	signal.Notify(stop, syscall.SIGINT, syscall.SIGTERM)
	<-stop

	shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()
	if err := server.Shutdown(shutdownCtx); err != nil {
		log.Printf("shutdown: %v", err)
	}
}
