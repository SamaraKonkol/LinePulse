package com.linepulse.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.linepulse.common.TooManyRequestsException;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LoginAttemptTrackerTest {
    private final String registration = "RATE-" + UUID.randomUUID();

    @AfterEach
    void tearDown() {
        LoginAttemptTracker.clearForTests(registration);
    }

    @Test
    void shouldBlockAfterFiveFailedAttempts() {
        for (int attempt = 0; attempt < 5; attempt++) {
            LoginAttemptTracker.recordFailure(registration);
        }

        assertThatThrownBy(() -> LoginAttemptTracker.checkAllowed(registration))
                .isInstanceOf(TooManyRequestsException.class);
    }

    @Test
    void shouldClearFailuresAfterSuccessfulLogin() {
        for (int attempt = 0; attempt < 5; attempt++) {
            LoginAttemptTracker.recordFailure(registration);
        }
        LoginAttemptTracker.recordSuccess(registration);
        LoginAttemptTracker.checkAllowed(registration);
    }
}
