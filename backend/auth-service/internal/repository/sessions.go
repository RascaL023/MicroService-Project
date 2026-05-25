package repository

import (
	"context"
	"encoding/json"
	"errors"
	"strconv"
	"time"

	"github.com/redis/go-redis/v9"
)

const (
	createSessionScript = `
redis.call("SET", KEYS[1], ARGV[1], "PX", ARGV[3])
redis.call("SADD", KEYS[2], ARGV[2])
redis.call("PEXPIRE", KEYS[2], ARGV[3])
return 1
`
	deleteSessionScript = `
local session = redis.call("GET", KEYS[1])
if not session then
	return 0
end

local data = cjson.decode(session)
local userID = data.userId or data.subject

redis.call("DEL", KEYS[1])
if userID then
	redis.call("SREM", "user:" .. userID .. ":sessions", ARGV[1])
end

return 1
`
)

type Session struct {
	Subject     int64     `json:"subject"`
	UserID      int64     `json:"userId,omitempty"`
	Username    string    `json:"username,omitempty"`
	Roles       []string  `json:"roles"`
	Authorities []string  `json:"authorities"`
	IssuedAt    time.Time `json:"issuedAt"`
	ExpiresAt   time.Time `json:"expiresAt"`
}

type SessionRepository struct {
	client    *redis.Client
	keyPrefix string
}

func NewSessionRepository(client *redis.Client, keyPrefix string) *SessionRepository {
	return &SessionRepository{
		client:    client,
		keyPrefix: keyPrefix,
	}
}

func (r *SessionRepository) Create(ctx context.Context, token string, session Session, ttl time.Duration) error {
	now := time.Now().UTC()

	if session.Subject == 0 { session.Subject = session.UserID }
	if session.UserID == 0 { session.UserID = session.Subject }
	session.IssuedAt = now
	session.ExpiresAt = now.Add(ttl)

	payload, err := json.Marshal(session)
	if err != nil { return err }

	return r.client.Eval(
		ctx,
		createSessionScript,
		[]string{r.key(token), r.userSessionsKey(session.Subject)},
		payload,
		token,
		strconv.FormatInt(ttl.Milliseconds(), 10),
	).Err()
}

func (r *SessionRepository) Get(ctx context.Context, token string) (Session, error) {
	raw, err := r.client.Get(ctx, r.key(token)).Result()
	if errors.Is(err, redis.Nil) {
		return Session{}, ErrNotFound
	}
	if err != nil {
		return Session{}, err
	}

	var session Session
	if err := json.Unmarshal([]byte(raw), &session); err != nil {
		return Session{}, err
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
	).Int()
	if err != nil { return err }
	if deleted == 0 { return ErrNotFound }

	return nil
}

func (r *SessionRepository) key(token string) string {
	return r.keyPrefix + token
}

func (r *SessionRepository) userSessionsKey(userID int64) string {
	return "user:" + strconv.FormatInt(userID, 10) + ":sessions"
}
