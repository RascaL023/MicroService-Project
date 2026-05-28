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
local sessionKey = KEYS[1] 
local userIdSessionsKey = KEYS[2]

local sessionValue = ARGV[1]
local sessionTTL = ARGV[3]
local userIdSessionsValue = ARGV[2]

redis.call("SET", sessionKey, sessionValue, "PX", sessionTTL)
redis.call("SADD", userIdSessionsKey, userIdSessionsValue)
redis.call("PEXPIRE", userIdSessionsKey, sessionTTL)

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
	banUsersSessionScript = `
local bannedKey = KEYS[1]
local userSessionsKey = KEYS[2]
local userId = ARGV[1]
local sessionKeyPrefix = ARGV[2]

redis.call("SADD", bannedKey, userId)
local sessions = redis.call("SMEMBERS", userSessionsKey)
local sessionKeys = {}

for _, sid in ipairs(sessions) do
    table.insert(sessionKeys, sessionKeyPrefix .. sid)
end

if #sessionKeys > 0 then
    redis.call("DEL", unpack(sessionKeys))
end

redis.call("DEL", userSessionsKey)

return #sessions
`
	unBanUserScript = `
local bannedKey = KEYS[1]
local userID = ARGV[1]

redis.call("SREM", bannedKey, userID)
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
	client       *redis.Client
	keyPrefix    string
	banKeyPrefix string
}

func NewSessionRepository(client *redis.Client, keyPrefix, banKeyPrefix string) *SessionRepository {
	return &SessionRepository{
		client:       client,
		keyPrefix:    keyPrefix,
		banKeyPrefix: banKeyPrefix,
	}
}

func (r *SessionRepository) Create(ctx context.Context, token string, session Session, ttl time.Duration) error {
	now := time.Now().UTC()

	if session.Subject == 0 {
		if session.UserID == 0 { return ErrNotFound }
		session.Subject = session.UserID
	}

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

func (r *SessionRepository) Ban(ctx context.Context, userID int64) error {
	err := r.client.Eval(
		ctx,
		banUsersSessionScript,
		[]string{r.banKeyPrefix, r.userSessionsKey(userID)},
		userID,
		r.keyPrefix,
	).Err()
	if err != nil { return err }

	return nil
}

func (r *SessionRepository) UnBan(ctx context.Context, userID int64) error {
	err := r.client.Eval(
		ctx,
		unBanUserScript,
		[]string{r.banKeyPrefix},
		userID,
	).Err()
	if err != nil { return err }

	return nil
}

func (r *SessionRepository) key(token string) string { return r.keyPrefix + token }

func (r *SessionRepository) userSessionsKey(userID int64) string {
	return "user:" + strconv.FormatInt(userID, 10) + ":sessions"
}
