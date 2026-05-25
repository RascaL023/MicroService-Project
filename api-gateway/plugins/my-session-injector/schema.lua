return {
    name = "my-session-injector",
    fields = {
        { config = {
            type = "record",
            fields = {
                { redis_host = {
                    type = "string",
                    default = "127.0.0.1"
                }},
                { redis_port = {
                    type = "number",
                    default = 6379
                }},
                { redis_key_prefix = {
                    type = "string",
                    default = "session:"
                }},

                { session_header_user_id = {
                    type = "string",
                    default = "X-User-Id"
                }},
                { session_header_user_roles = {
                    type = "string",
                    default = "X-User-Roles"
                }},
                { session_header_user_authorities = {
                    type = "string",
                    default = "X-User-Authorities"
                }},
                { session_header_internal_signature = {
                    type = "string",
                    default = "X-Internal-Signature"
                }},
            }
        }}
    }
}
