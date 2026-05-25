package db

import (
	"context"

	"github.com/redis/go-redis/v9"
)

func ConnectRedis(ctx context.Context, addr, password string, database int) (*redis.Client, error) {
	client := redis.NewClient(&redis.Options{
		Addr:     addr,
		Password: password,
		DB:       database,
	})

	if err := client.Ping(ctx).Err(); err != nil {
		_ = client.Close()
		return nil, err
	}

	return client, nil
}
