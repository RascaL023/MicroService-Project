package repository

import (
	"context"
	"encoding/json"
	"errors"
	"strconv"
	"time"

	"auth-service/internal/entity"

	"github.com/redis/go-redis/v9"
)

type SessionRepository struct {
	client    *redis.Client
	keyPrefix string
	banKey    string
}

func NewSessionRepository(client *redis.Client, keyPrefix, banKey string) *SessionRepository {
	return &SessionRepository{
		client:    client,
		keyPrefix: keyPrefix,
		banKey:    banKey,
	}
}

func (r *SessionRepository) Create(ctx context.Context, token string, session entity.Session, ttl time.Duration) error {
	now := time.Now().UTC()

	if session.Subject == 0 {
		if session.UserID == 0 {
			return ErrNotFound
		}
		session.Subject = session.UserID
	}

	session.IssuedAt = now
	session.ExpiresAt = now.Add(ttl)

	payload, err := json.Marshal(session)
	if err != nil {
		return err
	}

	return r.client.Eval(
		ctx,
		createSessionScript,
		[]string{r.key(token), r.userSessionsKey(session.Subject)},
		payload,
		token,
		strconv.FormatInt(ttl.Milliseconds(), 10),
	).Err()
}

func (r *SessionRepository) Get(ctx context.Context, token string) (entity.Session, error) {
	raw, err := r.client.Get(ctx, r.key(token)).Result()
	if errors.Is(err, redis.Nil) {
		return entity.Session{}, ErrNotFound
	}
	if err != nil {
		return entity.Session{}, err
	}

	var session entity.Session
	if err := json.Unmarshal([]byte(raw), &session); err != nil {
		return entity.Session{}, err
	}
	if session.Subject == 0 {
		session.Subject = session.UserID
	}

	return session, nil
}

func (r *SessionRepository) Delete(ctx context.Context, token string) error {
	deleted, err := r.client.Eval(
		ctx,
		deleteSessionScript,
		[]string{r.key(token)},
		token,
		userSessionsKeyPrefix,
		userSessionsKeySuffix,
	).Int()
	if err != nil {
		return err
	}
	if deleted == 0 {
		return ErrNotFound
	}

	return nil
}

func (r *SessionRepository) RevokeSubject(ctx context.Context, subject int64) (int, error) {
	return r.client.Eval(
		ctx,
		revokeSubjectSessionsScript,
		[]string{r.userSessionsKey(subject)},
		r.keyPrefix,
	).Int()
}

func (r *SessionRepository) key(token string) string { return r.keyPrefix + token }

func (r *SessionRepository) userSessionsKey(userID int64) string {
	return userSessionsKeyPrefix + strconv.FormatInt(userID, 10) + userSessionsKeySuffix
}
