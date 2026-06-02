local redis = require("resty.redis")
local cjson = require("cjson.safe")

local MySessionInjector = {
    PRIORITY = 1000,
    VERSION = "1.0.0",
}

function MySessionInjector:access(conf)
    if kong.request.get_method() == "OPTIONS" then return end

    kong.service.request.clear_header(conf.session_header_user_id)
    kong.service.request.clear_header(conf.session_header_user_roles)
    kong.service.request.clear_header(conf.session_header_user_authorities)
    kong.service.request.clear_header(conf.session_header_internal_signature)

    local auth_header = kong.request.get_header("authorization")
    -- local auth_header = kong.request.get_header("cookie")
    -- kong.log.warn("[====> Cookie]", auth_header)
    if not auth_header then
        return kong.response.exit(401, {
                status = 401,
                errorType = "Unauthorized",
                message = "Harap login terlebih dahulu"
            }
        )
    end

    local token = auth_header:match("^[Ss]ession%s+(.+)$")
    -- local token = auth_header:match("^[Bb]earer%s+(.+)$")
    -- local token = auth_header:match("^.+=(.+)$")
    if not token then
        return kong.response.exit(401, {
            status = 401,
            errorType = "Unauthorized",
            message = "Format token tidak valid"
        })
    end


    local red = redis:new()
    red:set_timeouts(500, 500, 500) -- connect_timeout, send_timeout, read_timeout (ms)

    local ok, err = red:connect(conf.redis_host, conf.redis_port)
    if not ok then
        kong.log.err("[session-injector] Redis connect failed: ", err)
        return kong.response.exit(503, {
            status = 503,
            errortype = "infrastructure error",
            message = "Session store unavailable"
        })
    end

    local key = conf.redis_key_prefix .. token
    local session_raw, redis_err = red:get(key)

    if redis_err then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        kong.log.err("[session-injector] Redis GET error: ", redis_err)
        return kong.response.exit(503, {
            status = 503,
            errortype = "infrastructure error",
            message = "Session store error"
        })
    end

    if not session_raw or session_raw == ngx.null then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        return kong.response.exit(401, {
            status = 401,
            errorType = "Unauthorized",
            message = "Session not found or expired"
        })
    end


    local session, decode_err = cjson.decode(session_raw)
    if not session then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        kong.log.err("[session-injector] JSON decode failed: ", decode_err)
        return kong.response.exit(500, {
            status = 500,
            errorType = "Internal Server Error",
            message = "Malformed session data"
        })
    end

    local subject = session.subject or session.userId or session.user_id
    if not subject then
        local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
        if not keep_alive_ok then
            kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
        end
        kong.log.err("[session-injector] Session subject missing")
        return kong.response.exit(500, {
            status = 500,
            errorType = "Internal Server Error",
            message = "Malformed session data"
        })
    end

    local banned, ban_err = red:sismember(conf.redis_ban_key, tostring(subject))
    local keep_alive_ok, keep_alive_err = red:set_keepalive(10000, 100)
    if not keep_alive_ok then
        kong.log.warn("[session-injector] Redis set_keepalive failed: ", keep_alive_err)
    end

    if ban_err then
        kong.log.err("[session-injector] Redis ban check error: ", ban_err)
        return kong.response.exit(503, {
            status = 503,
            errortype = "infrastructure error",
            message = "Session store error"
        })
    end

    if banned == 1 then
        return kong.response.exit(403, {
            status = 403,
            errorType = "Forbidden",
            message = "User is banned"
        })
    end

    kong.service.request.set_header(
        conf.session_header_user_id,
        tostring(subject)
    )

    if session.roles then
        if type(session.roles) == "table" then
            kong.service.request.set_header(
                conf.session_header_user_roles,
                table.concat(session.roles, ",")
            )
        else
            kong.service.request.set_header(
                conf.session_header_user_roles,
                tostring(session.roles)
            )
        end
    end

    if session.authorities then
        if type(session.authorities) == "table" then
            kong.service.request.set_header(
                conf.session_header_user_authorities,
                table.concat(session.authorities, ",")
            )
        else
            kong.service.request.set_header(
                conf.session_header_user_authorities,
                tostring(session.authorities)
            )
        end
    end

    kong.service.request.set_header(
        conf.session_header_internal_signature,
        "atlanta331"
    )


    kong.service.request.clear_header("authorization")
    kong.service.request.clear_header("Authorization")

    kong.log.debug("[session-injector] Injected session for user: ", subject)

end

return MySessionInjector
