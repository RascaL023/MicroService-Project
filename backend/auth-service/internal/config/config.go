package config

import (
	"fmt"
	"net"
	"net/url"
	"os"
	"path/filepath"
	"strconv"
	"time"

	"github.com/spf13/viper"
)

type Config struct {
	Port        string
	DatabaseURL string
	JWTSecret   string
	AuthMode    string
	SessionTTL  time.Duration
	JWTTTL      time.Duration
	RedisAddr   string
	RedisPass   string
	RedisDB     int
	RedisPrefix string

	AdminUsername string
	AdminPassword string
}

type globalConfig struct {
	Services servicesConfig `mapstructure:"services"`
	Database databaseConfig `mapstructure:"database"`
	Redis    redisConfig    `mapstructure:"redis"`
	Auth     authConfig     `mapstructure:"auth"`
	Admin    adminConfig    `mapstructure:"admin"`
	Security securityConfig `mapstructure:"security"`
}

type servicesConfig struct {
	Auth serviceConfig `mapstructure:"auth"`
}

type serviceConfig struct {
	Port int `mapstructure:"port"`
}

type databaseConfig struct {
	Provider string              `mapstructure:"provider"`
	Host     string              `mapstructure:"host"`
	Port     int                 `mapstructure:"port"`
	Username string              `mapstructure:"username"`
	Password string              `mapstructure:"password"`
	SSLMode  string              `mapstructure:"sslmode"`
	Names    databaseNamesConfig `mapstructure:"names"`
}

type databaseNamesConfig struct {
	AuthService string `mapstructure:"auth_service"`
}

type redisConfig struct {
	Host     string              `mapstructure:"host"`
	Port     int                 `mapstructure:"port"`
	Password string              `mapstructure:"password"`
	DB       int                 `mapstructure:"db"`
	Prefixes redisPrefixesConfig `mapstructure:"prefixes"`
}

type redisPrefixesConfig struct {
	Session string `mapstructure:"session"`
}

type authConfig struct {
	Mode            string `mapstructure:"mode"`
	SessionTTLHours int    `mapstructure:"session_ttl_hours"`
	JWTTTLMinutes   int    `mapstructure:"jwt_ttl_minutes"`
}

type adminConfig struct {
	Username string `mapstructure:"username"`
	Password string `mapstructure:"password"`
}

type securityConfig struct {
	SecretKey string `mapstructure:"secret_key"`
}

func Load() Config {
	var raw globalConfig
	if err := loadGlobalConfig(&raw); err != nil {
		panic(fmt.Sprintf("load config: %v", err))
	}

	return Config{
		Port:          strconv.Itoa(raw.Services.Auth.Port),
		DatabaseURL:   databaseURL(raw.Database),
		JWTSecret:     raw.Security.SecretKey,
		AuthMode:      raw.Auth.Mode,
		SessionTTL:    time.Duration(raw.Auth.SessionTTLHours) * time.Hour,
		JWTTTL:        time.Duration(raw.Auth.JWTTTLMinutes) * time.Minute,
		RedisAddr:     net.JoinHostPort(raw.Redis.Host, strconv.Itoa(raw.Redis.Port)),
		RedisPass:     raw.Redis.Password,
		RedisDB:       raw.Redis.DB,
		RedisPrefix:   raw.Redis.Prefixes.Session,
		AdminUsername: raw.Admin.Username,
		AdminPassword: raw.Admin.Password,
	}
}

func loadGlobalConfig(target any) error {
	dir, err := globalConfigDir()
	if err != nil { return err }

	v := viper.New()
	v.SetConfigType("yaml")

	v.SetConfigFile(filepath.Join(dir, "public-config.yml"))
	if err := v.ReadInConfig(); err != nil {
		return fmt.Errorf("read public-config.yml: %w", err)
	}

	v.SetConfigFile(filepath.Join(dir, "private-config.yml"))
	if err := v.MergeInConfig(); err != nil {
		return fmt.Errorf("read private-config.yml: %w", err)
	}

	if err := v.Unmarshal(target); err != nil {
		return fmt.Errorf("parse global config: %w", err)
	}

	return nil
}

func globalConfigDir() (string, error) {
	wd, err := os.Getwd()
	if err != nil { return "", err }

	for dir := wd; ; dir = filepath.Dir(dir) {
		candidate := filepath.Join(dir, "global")
		if fileExists(filepath.Join(candidate, "public-config.yml")) &&
			fileExists(filepath.Join(candidate, "private-config.yml")) {
			return candidate, nil
		}

		parent := filepath.Dir(dir)
		if parent == dir { break }
	}

	return "", fmt.Errorf("global config directory not found from %s", wd)
}

func databaseURL(cfg databaseConfig) string {
	dsn := url.URL{
		Scheme: cfg.Provider,
		Host:   net.JoinHostPort(cfg.Host, strconv.Itoa(cfg.Port)),
		Path:   "/" + cfg.Names.AuthService,
	}

	if cfg.Password != "" {
		dsn.User = url.UserPassword(cfg.Username, cfg.Password)
	} else { dsn.User = url.User(cfg.Username) }

	query := dsn.Query()
	query.Set("sslmode", cfg.SSLMode)
	dsn.RawQuery = query.Encode()

	return dsn.String()
}

func fileExists(path string) bool {
	info, err := os.Stat(path)
	return err == nil && !info.IsDir()
}
