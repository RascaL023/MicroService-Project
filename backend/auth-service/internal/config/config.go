package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	Port        string
	DatabaseURL string
	JWTSecret   string
	AuthMode    string
	SessionTTL  time.Duration

	AdminUsername string
	AdminPassword string
}

func Load() Config {
	ttlHours, err := strconv.Atoi(env("AUTH_SESSION_TTL_HOURS", "24"))
	if err != nil || ttlHours <= 0 {
		ttlHours = 24
	}

	return Config{
		// Port:          env("PORT", "8081"),
		// DatabaseURL:   env("DATABASE_URL", "postgres://rascal@localhost:5555/auth_service?sslmode=disable"),
		// JWTSecret:     env("JWT_SECRET", "change-me"),
		// AuthMode:      env("AUTH_MODE", "stateful"),
		// SessionTTL:    time.Duration(ttlHours) * time.Hour,
		// AdminUsername: env("ADMIN_USERNAME", "rascal"),
		// AdminPassword: env("ADMIN_PASSWORD", "atmin123"),
		Port:          "8081",
		DatabaseURL:   "postgres://rascal@localhost:5555/auth_test?sslmode=disable",
		JWTSecret:     "change-me",
		AuthMode:      "stateful",
		SessionTTL:    time.Duration(ttlHours) * time.Hour,
		AdminUsername: "rascal",
		AdminPassword: "atmin123",
	}
}

func env(key, fallback string) string {
	if value := os.Getenv(key); value != "" {
		return value
	}
	return fallback
}
