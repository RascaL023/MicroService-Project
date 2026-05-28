package repository

const (
	userSessionsKeyPrefix = "user:"
	userSessionsKeySuffix = ":sessions"

	createSessionScript = `
local sessionKey = KEYS[1]
local subjectSessionsKey = KEYS[2]

local sessionValue = ARGV[1]
local sessionTTL = ARGV[3]
local token = ARGV[2]

redis.call("SET", sessionKey, sessionValue, "PX", sessionTTL)
redis.call("SADD", subjectSessionsKey, token)
redis.call("PEXPIRE", subjectSessionsKey, sessionTTL)

return 1
`
	deleteSessionScript = `
local session = redis.call("GET", KEYS[1])
if not session then
	return 0
end

local data = cjson.decode(session)
local userID = data.userId or data.subject
local subjectSessionsKeyPrefix = ARGV[2]
local subjectSessionsKeySuffix = ARGV[3]

redis.call("DEL", KEYS[1])
if userID then
	redis.call("SREM", subjectSessionsKeyPrefix .. userID .. subjectSessionsKeySuffix, ARGV[1])
end

return 1
`
	revokeSubjectSessionsScript = `
local subjectSessionsKey = KEYS[1]
local sessionKeyPrefix = ARGV[1]

local sessions = redis.call("SMEMBERS", subjectSessionsKey)
local sessionKeys = {}

for _, sid in ipairs(sessions) do
    table.insert(sessionKeys, sessionKeyPrefix .. sid)
end

if #sessionKeys > 0 then
    redis.call("DEL", unpack(sessionKeys))
end

redis.call("DEL", subjectSessionsKey)

return #sessions
`
	banSubjectScript = `
local banKey = KEYS[1]
local subjectSessionsKey = KEYS[2]
local subject = ARGV[1]
local sessionKeyPrefix = ARGV[2]
local shouldRevoke = ARGV[3]

redis.call("SADD", banKey, subject)
if shouldRevoke ~= "1" then
	return 0
end

local sessions = redis.call("SMEMBERS", subjectSessionsKey)
local sessionKeys = {}

for _, sid in ipairs(sessions) do
    table.insert(sessionKeys, sessionKeyPrefix .. sid)
end

if #sessionKeys > 0 then
    redis.call("DEL", unpack(sessionKeys))
end

redis.call("DEL", subjectSessionsKey)

return #sessions
`
)
