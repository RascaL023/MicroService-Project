package service

import (
	"context"
	"errors"

	"auth-service/internal/config"
	"auth-service/internal/entity"
	"auth-service/internal/repository"

	"golang.org/x/crypto/bcrypt"
)

func SeedAdmin(ctx context.Context, cfg config.Config, users *repository.UserRepository, roles *repository.RoleRepository) error {
	adminRole, err := ensureRole(ctx, roles, "ADMIN")
	if err != nil {
		return err
	}

	if _, err := ensureRole(ctx, roles, "USER"); err != nil {
		return err
	}

	hash, err := bcrypt.GenerateFromPassword([]byte(cfg.AdminPassword), bcrypt.DefaultCost)
	if err != nil {
		return err
	}

	_, err = users.CreateBootstrapAdmin(ctx, -1, cfg.AdminEmail, string(hash), []int64{adminRole.ID})
	if errors.Is(err, repository.ErrDuplicate) {
		return nil
	}

	return err
}

func ensureRole(ctx context.Context, roles *repository.RoleRepository, name string) (entity.Role, error) {
	role, err := roles.FindByName(ctx, name)
	if errors.Is(err, repository.ErrNotFound) {
		role, err = roles.Create(ctx, name, nil)
	}
	if err != nil {
		return entity.Role{}, err
	}

	return role, nil
}
