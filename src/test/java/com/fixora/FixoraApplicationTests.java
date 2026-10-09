package com.fixora;

import com.fixora.controller.*;
import com.fixora.service.ai.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the Spring context builds and the main layers are wired. */
@SpringBootTest
@ActiveProfiles("test")
class FixoraApplicationTests {

    @Autowired private HealthController healthController;
    @Autowired private ServiceController serviceController;
    @Autowired private BookingController bookingController;
    @Autowired private AdminController adminController;
    @Autowired private AiController aiController;
    @Autowired private AiService aiService;

    @Test
    void contextLoadsWithEveryControllerWired() {
        assertThat(healthController).isNotNull();
        assertThat(serviceController).isNotNull();
        assertThat(bookingController).isNotNull();
        assertThat(adminController).isNotNull();
        assertThat(aiController).isNotNull();
        assertThat(aiService).isNotNull();
    }
}
