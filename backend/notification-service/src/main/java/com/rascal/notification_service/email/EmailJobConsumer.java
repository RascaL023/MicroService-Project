package com.rascal.notification_service.email;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmailJobConsumer {

    private static final Logger log = LoggerFactory.getLogger(EmailJobConsumer.class);
    private static final String GROUP = "notification-service";

    private final StringRedisTemplate redisTemplate;
    private final EmailSender emailSender;
    private final String stream;
    private final String consumerName;

    public EmailJobConsumer(
        StringRedisTemplate redisTemplate,
        EmailSender emailSender,
        @Value("${notification.streams.emails:notification:emails}") String stream
    ) {
        this.redisTemplate = redisTemplate;
        this.emailSender = emailSender;
        this.stream = stream;
        this.consumerName = hostname();
    }

    @Scheduled(fixedDelay = 3000)
    public void consume() {
        ensureGroup();

        List<MapRecord<String, Object, Object>> records;
        try {
            records = redisTemplate.opsForStream().read(
                Consumer.from(GROUP, consumerName),
                StreamReadOptions.empty().count(10).block(Duration.ofSeconds(2)),
                StreamOffset.create(stream, ReadOffset.lastConsumed())
            );
        } catch (RuntimeException err) {
            log.warn("Failed to read email jobs", err);
            return;
        }

        if (records == null || records.isEmpty()) return;

        for (MapRecord<String, Object, Object> record : records) {
            try {
                handle(record.getValue());
                redisTemplate.opsForStream().acknowledge(stream, GROUP, record.getId());
            } catch (Exception err) {
                log.warn("Failed to handle email job {}", record.getId(), err);
            }
        }
    }

    private void handle(Map<Object, Object> payload) throws Exception {
        String type = string(payload.get("type"));
        if (!"ACTIVATION_EMAIL".equals(type)) return;

        log.info("Sending activation email job to {}", string(payload.get("to")));
        emailSender.sendActivationEmail(
            string(payload.get("to")),
            string(payload.get("activationUrl"))
        );
    }

    private void ensureGroup() {
        try {
            redisTemplate.opsForStream().createGroup(stream, ReadOffset.from("0-0"), GROUP);
        } catch (RedisSystemException err) {
            if (!err.getMessage().contains("BUSYGROUP")) {
                log.debug("Email stream group not ready yet", err);
            }
        }
    }

    private String string(Object value) {
        return value == null ? "" : value.toString();
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception err) {
            return "notification-service-1";
        }
    }
}
