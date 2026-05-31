package service

import (
	"context"
	"errors"
	"fmt"
	"log"
	"strconv"
	"strings"
	"time"

	"auth-service/internal/repository"

	"github.com/redis/go-redis/v9"
)

type UserEventConsumer struct {
	client        *redis.Client
	stream        string
	group         string
	consumer      string
	checkpointKey string
	users         *repository.UserRepository
}

func NewUserEventConsumer(client *redis.Client, stream, group, consumer string, users *repository.UserRepository) *UserEventConsumer {
	return &UserEventConsumer{
		client:        client,
		stream:        stream,
		group:         group,
		consumer:      consumer,
		checkpointKey: "auth-service:" + stream + ":checkpoint",
		users:         users,
	}
}

func (c *UserEventConsumer) Run(ctx context.Context) {
	c.ensureGroup(ctx)
	c.replayExisting(ctx)
	for {
		select {
		case <-ctx.Done():
			return
		default:
		}

		streams, err := c.client.XReadGroup(ctx, &redis.XReadGroupArgs{
			Group:    c.group,
			Consumer: c.consumer,
			Streams:  []string{c.stream, ">"},
			Count:    10,
			Block:    5 * time.Second,
		}).Result()
		if errors.Is(err, redis.Nil) {
			continue
		}
		if err != nil {
			log.Printf("read user events: %v", err)
			c.ensureGroup(ctx)
			time.Sleep(time.Second)
			continue
		}

		for _, stream := range streams {
			for _, message := range stream.Messages {
				if err := c.handle(ctx, message.Values); err != nil {
					if errors.Is(err, repository.ErrDuplicate) {
						log.Printf("skip duplicate user event %s: %v", message.ID, err)
					} else {
						log.Printf("handle user event %s: %v", message.ID, err)
						continue
					}
				}
				if err := c.client.XAck(ctx, c.stream, c.group, message.ID).Err(); err != nil {
					log.Printf("ack user event %s: %v", message.ID, err)
				}
				c.saveCheckpoint(ctx, message.ID)
			}
		}
	}
}

func (c *UserEventConsumer) ensureGroup(ctx context.Context) {
	err := c.client.XGroupCreateMkStream(ctx, c.stream, c.group, "0").Err()
	if err != nil && !strings.Contains(err.Error(), "BUSYGROUP") {
		log.Printf("create user event consumer group: %v", err)
	}
}

func (c *UserEventConsumer) replayExisting(ctx context.Context) {
	lastID := "0"
	if stored, err := c.client.Get(ctx, c.checkpointKey).Result(); err == nil && stored != "" {
		lastID = stored
	} else if err != nil && !errors.Is(err, redis.Nil) {
		log.Printf("read user event checkpoint: %v", err)
	}

	for {
		streams, err := c.client.XRead(ctx, &redis.XReadArgs{
			Streams: []string{c.stream, lastID},
			Count:   100,
		}).Result()
		if errors.Is(err, redis.Nil) {
			return
		}
		if err != nil {
			log.Printf("replay user events: %v", err)
			return
		}

		var read int
		for _, stream := range streams {
			for _, message := range stream.Messages {
				if err := c.handle(ctx, message.Values); err != nil {
					if errors.Is(err, repository.ErrDuplicate) {
						log.Printf("skip duplicate replayed user event %s: %v", message.ID, err)
					} else {
						log.Printf("replay user event %s: %v", message.ID, err)
						return
					}
				}
				c.saveCheckpoint(ctx, message.ID)
				lastID = message.ID
				read++
			}
		}
		if read == 0 {
			return
		}
	}
}

func (c *UserEventConsumer) saveCheckpoint(ctx context.Context, messageID string) {
	if err := c.client.Set(ctx, c.checkpointKey, messageID, 0).Err(); err != nil {
		log.Printf("save user event checkpoint %s: %v", messageID, err)
	}
}

func (c *UserEventConsumer) handle(ctx context.Context, values map[string]any) error {
	eventType := value(values, "type")
	userID, err := strconv.ParseInt(value(values, "userId"), 10, 64)
	if err != nil {
		return err
	}
	email := normalizeEmail(value(values, "email"))

	switch eventType {
	case "UserCreated":
		roleIDs, err := parseRoleIDs(value(values, "roleIds"))
		if err != nil {
			return err
		}
		banned, _ := strconv.ParseBool(value(values, "isBanned"))
		_, err = c.users.Provision(ctx, userID, email, roleIDs, banned)
		return err
	case "UserEmailUpdated":
		_, err = c.users.UpdateEmail(ctx, userID, email)
		return err
	case "UserBanUpdated":
		banned, _ := strconv.ParseBool(value(values, "isBanned"))
		_, err = c.users.SetBanned(ctx, userID, banned)
		return err
	case "UserRolesUpdated":
		roleIDs, err := parseRoleIDs(value(values, "roleIds"))
		if err != nil {
			return err
		}
		_, err = c.users.SyncRoles(ctx, userID, roleIDs)
		return err
	case "UserDeleted":
		return c.users.MarkDeleted(ctx, userID)
	default:
		return nil
	}
}

func value(values map[string]any, key string) string {
	raw, ok := values[key]
	if !ok || raw == nil {
		return ""
	}
	return strings.TrimSpace(fmt.Sprint(raw))
}

func parseRoleIDs(raw string) ([]int64, error) {
	if strings.TrimSpace(raw) == "" {
		return nil, nil
	}

	parts := strings.Split(raw, ",")
	roleIDs := make([]int64, 0, len(parts))
	for _, part := range parts {
		id, err := strconv.ParseInt(strings.TrimSpace(part), 10, 64)
		if err != nil {
			return nil, err
		}
		roleIDs = append(roleIDs, id)
	}
	return roleIDs, nil
}
