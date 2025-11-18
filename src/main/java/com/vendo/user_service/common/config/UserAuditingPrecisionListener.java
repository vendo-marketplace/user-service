package com.vendo.user_service.common.config;

import com.vendo.user_service.model.User;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class UserAuditingPrecisionListener {

    @EventListener
    public void handleBeforeConvert(BeforeConvertEvent<User> event) {
        User user = event.getSource();

        if(user.getCreatedAt() == null) {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
        }
        else {
            user.setCreatedAt(user.getCreatedAt().truncatedTo(ChronoUnit.MILLIS));
            user.setUpdatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS));
        }
    }
}
