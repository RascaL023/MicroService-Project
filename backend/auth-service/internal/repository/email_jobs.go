package repository

import (
	"context"
	"log"

	"github.com/redis/go-redis/v9"
)

type EmailJobPublisher struct {
	client *redis.Client
	stream string
}

func NewEmailJobPublisher(client *redis.Client, stream string) *EmailJobPublisher {
	return &EmailJobPublisher{client: client, stream: stream}
}

func (p *EmailJobPublisher) PublishActivation(ctx context.Context, to, activationURL string) error {
	id, err := p.client.XAdd(ctx, &redis.XAddArgs{
		Stream: p.stream,
		Values: map[string]any{
			"type":          "ACTIVATION_EMAIL",
			"to":            to,
			"activationUrl": activationURL,
		},
	}).Result()
	if err != nil {
		return err
	}

	log.Printf("activation email job queued stream=%s id=%s to=%s", p.stream, id, to)
	return nil
}
