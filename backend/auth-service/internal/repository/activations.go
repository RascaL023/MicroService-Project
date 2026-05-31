package repository

import (
	"context"
	"errors"
	"strconv"
	"time"

	"github.com/redis/go-redis/v9"
)

type ActivationRepository struct {
	client *redis.Client
	prefix string
}

func NewActivationRepository(client *redis.Client) *ActivationRepository {
	return &ActivationRepository{client: client, prefix: "activation:"}
}

func (r *ActivationRepository) Create(ctx context.Context, tokenHash string, userID int64, ttl time.Duration) error {
	return r.client.Set(ctx, r.key(tokenHash), strconv.FormatInt(userID, 10), ttl).Err()
}

func (r *ActivationRepository) Consume(ctx context.Context, tokenHash string) (int64, error) {
	raw, err := r.client.GetDel(ctx, r.key(tokenHash)).Result()
	if errors.Is(err, redis.Nil) {
		return 0, ErrNotFound
	}
	if err != nil {
		return 0, err
	}

	return strconv.ParseInt(raw, 10, 64)
}

func (r *ActivationRepository) key(tokenHash string) string {
	return r.prefix + tokenHash
}
