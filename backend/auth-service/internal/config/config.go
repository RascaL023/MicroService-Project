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
	Port                    string
	DatabaseURL             string
	SessionTTL              time.Duration
	ActivationTTL           time.Duration
	ActivationURL           string
	RedisAddr               string
	RedisPass               string
	RedisDB                 int
	RedisPrefix             string
	RedisBanPrefix          string
	UserEventsStream        string
	NotificationEmailStream string

	AdminEmail    string
	AdminPassword string
}

type globalConfig struct {
	Services servicesConfig `mapstructure:"services"`
	Database databaseConfig `mapstructure:"database"`
	Redis    redisConfig    `mapstructure:"redis"`
	Auth     authConfig     `mapstructure:"auth"`
	Admin    adminConfig    `mapstructure:"admin"`
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
	Streams  redisStreamsConfig  `mapstructure:"streams"`
}

type redisPrefixesConfig struct {
	Session string `mapstructure:"session"`
	Banned  string `mapstructure:"ban"`
}

type redisStreamsConfig struct {
	UserEvents        string `mapstructure:"user_events"`
	NotificationEmail string `mapstructure:"notification_emails"`
}

type authConfig struct {
	SessionTTLHours      int    `mapstructure:"session_ttl_hours"`
	ActivationTTLMinutes int    `mapstructure:"activation_ttl_minutes"`
	ActivationURL        string `mapstructure:"activation_url"`
}

type adminConfig struct {
	Email    string `mapstructure:"email"`
	Username string `mapstructure:"username"`
	Password string `mapstructure:"password"`
}

func Load() Config {
	var raw globalConfig
	if err := loadGlobalConfig(&raw); err != nil {
		panic(fmt.Sprintf("load config: %v", err))
	}

	sessionTTLHours := raw.Auth.SessionTTLHours
	if sessionTTLHours == 0 {
		sessionTTLHours = 24
	}
	activationTTLMinutes := raw.Auth.ActivationTTLMinutes
	if activationTTLMinutes == 0 {
		activationTTLMinutes = 30
	}
	adminEmail := raw.Admin.Email
	if adminEmail == "" {
		adminEmail = raw.Admin.Username
	}

	return Config{
		Port:                    strconv.Itoa(raw.Services.Auth.Port),
		DatabaseURL:             databaseURL(raw.Database),
		SessionTTL:              time.Duration(sessionTTLHours) * time.Hour,
		ActivationTTL:           time.Duration(activationTTLMinutes) * time.Minute,
		ActivationURL:           defaultString(raw.Auth.ActivationURL, "http://localhost:5173/activate"),
		RedisAddr:               net.JoinHostPort(raw.Redis.Host, strconv.Itoa(raw.Redis.Port)),
		RedisPass:               raw.Redis.Password,
		RedisDB:                 raw.Redis.DB,
		RedisPrefix:             raw.Redis.Prefixes.Session,
		RedisBanPrefix:          raw.Redis.Prefixes.Banned,
		UserEventsStream:        defaultString(raw.Redis.Streams.UserEvents, "user:events"),
		NotificationEmailStream: defaultString(raw.Redis.Streams.NotificationEmail, "notification:emails"),
		AdminEmail:              adminEmail,
		AdminPassword:           raw.Admin.Password,
	}
}

func loadGlobalConfig(target any) error {
	dir, err := globalConfigDir()
	if err != nil {
		return err
	}

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
	if err != nil {
		return "", err
	}

	for dir := wd; ; dir = filepath.Dir(dir) {
		candidate := filepath.Join(dir, "global")
		if fileExists(filepath.Join(candidate, "public-config.yml")) &&
			fileExists(filepath.Join(candidate, "private-config.yml")) {
			return candidate, nil
		}

		parent := filepath.Dir(dir)
		if parent == dir {
			break
		}
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
	} else {
		dsn.User = url.User(cfg.Username)
	}

	query := dsn.Query()
	query.Set("sslmode", cfg.SSLMode)
	dsn.RawQuery = query.Encode()

	return dsn.String()
}

func fileExists(path string) bool {
	info, err := os.Stat(path)
	return err == nil && !info.IsDir()
}

func defaultString(value, fallback string) string {
	if value == "" {
		return fallback
	}
	return value
}
