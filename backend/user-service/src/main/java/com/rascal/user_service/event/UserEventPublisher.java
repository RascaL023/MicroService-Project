package com.rascal.user_service.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.rascal.user_service.entity.User;

@Component
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final StringRedisTemplate redisTemplate;
    private final String stream;

    public UserEventPublisher(
        StringRedisTemplate redisTemplate,
        @Value("${redis.streams.user_events:user:events}") String stream
    ) {
        this.redisTemplate = redisTemplate;
        this.stream = stream;
    }

    public void userCreated(User user, List<Long> roleIds) {
        publish("UserCreated", user, Map.of(
            "roleIds", join(roleIds),
            "status", user.getStatus()
        ));
    }

    public void userEmailUpdated(User user, String oldEmail) {
        publish("UserEmailUpdated", user, Map.of(
            "oldEmail", oldEmail
        ));
    }

    public void userProfileUpdated(User user) {
        publish("UserProfileUpdated", user, Map.of());
    }

    public void userStatusUpdated(User user) {
        publish("UserStatusUpdated", user, Map.of(
            "status", user.getStatus()
        ));
    }

    public void userRolesUpdated(User user, List<Long> roleIds) {
        publish("UserRolesUpdated", user, Map.of(
            "roleIds", join(roleIds)
        ));
    }

    public void userDeleted(User user) {
        publish("UserDeleted", user, Map.of());
    }

    private void publish(String type, User user, Map<String, String> extra) {
        Map<String, String> body = new HashMap<>();
        body.put("type", type);
        body.put("userId", String.valueOf(user.getId()));
        body.put("email", user.getEmail());
        body.put("name", user.getName());
        body.put("gender", String.valueOf(user.getGender()));
        body.put("batch", String.valueOf(user.getBatch().getId()));
        body.putAll(extra);

        try {
            redisTemplate.opsForStream().add(MapRecord.create(stream, body));
        } catch (RuntimeException err) {
            log.warn("Failed to publish {} event for user {}: {}", type, user.getId(), err.getMessage());
            log.debug("User event publish failure detail", err);
        }
    }

    private String join(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return "";

        return roleIds.stream()
            .map(String::valueOf)
            .collect(Collectors.joining(","));
    }

}
