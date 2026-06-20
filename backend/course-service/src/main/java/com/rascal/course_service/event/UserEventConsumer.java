package com.rascal.course_service.event;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rascal.course_service.dto.response.UserLookupResponse;
import com.rascal.course_service.service.CourseUserCacheService;

@Component
@ConditionalOnProperty(
    value = "app.user-cache.consumer.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class UserEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventConsumer.class);
    private static final String GROUP = "course-service";

    private final StringRedisTemplate redisTemplate;
    private final CourseUserCacheService courseUserCacheService;
    private final String stream;
    private final String consumerName;

    public UserEventConsumer(
        StringRedisTemplate redisTemplate,
        CourseUserCacheService courseUserCacheService,
        @Value("${redis.streams.user_events:user:events}") String stream
    ) {
        this.redisTemplate = redisTemplate;
        this.courseUserCacheService = courseUserCacheService;
        this.stream = stream;
        this.consumerName = hostname();
    }

    @SuppressWarnings("unchecked")
    @Scheduled(fixedDelay = 5000)
    public void consume() {
        List<MapRecord<String, Object, Object>> records;
        try {
            ensureGroup();
            records = redisTemplate.opsForStream().read(
                Consumer.from(GROUP, consumerName),
                StreamReadOptions.empty().count(50).block(Duration.ofSeconds(1)),
                StreamOffset.create(stream, ReadOffset.lastConsumed())
            );
        } catch (RuntimeException err) {
            log.warn("Failed to read user events", err);
            return;
        }

        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handle(record.getValue());
                redisTemplate.opsForStream().acknowledge(stream, GROUP, record.getId());
            } catch (Exception err) {
                log.warn("Failed to handle user event {}", record.getId(), err);
            }
        }
    }

    private void handle(Map<Object, Object> payload) {
        String type = string(payload.get("type"));
        Long userId = longValue(payload.get("userId"));
        if (userId == null) return;

        if ("UserDeleted".equals(type)) {
            courseUserCacheService.softDelete(userId);
            return;
        }

        String name = blankToNull(string(payload.get("name")));
        Character gender = character(payload.get("gender"));
        Integer batch = integer(payload.get("batch"));
        if (name == null || gender == null || batch == null) {
            log.debug("Skip user event {} for user {} because lookup payload is incomplete", type, userId);
            return;
        }

        String status = blankToNull(string(payload.get("status")));
        courseUserCacheService.upsert(new UserLookupResponse(userId, name, gender, batch, status));
    }

    private void ensureGroup() {
        try {
            redisTemplate.opsForStream().createGroup(stream, ReadOffset.from("0-0"), GROUP);
        } catch (RedisSystemException err) {
            if (!err.getMessage().contains("BUSYGROUP")) {
                log.debug("User event stream group not ready yet", err);
            }
        } catch (RuntimeException err) {
            log.debug("User event stream group cannot be checked yet", err);
        }
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Long longValue(Object value) {
        try { return value == null ? null : Long.parseLong(value.toString()); }
        catch (NumberFormatException ex) { return null; }
    }

    private Integer integer(Object value) {
        try {
            String parsed = string(value);
            return parsed.isBlank() ? null : Integer.parseInt(parsed);
        } catch (NumberFormatException ex) { return null; }
    }

    private Character character(Object value) {
        String parsed = string(value);
        return parsed.isBlank() ? null : parsed.charAt(0);
    }

    private String hostname() {
        try { return InetAddress.getLocalHost().getHostName(); } 
        catch (Exception err) { return "course-service-1"; }
    }

}
