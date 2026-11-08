package com.tcms.automation;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Configuration
@EnableScheduling
public class AutomationPreviewCleanup {
    private final AutomationJobPreviewRepository repository;
    public AutomationPreviewCleanup(AutomationJobPreviewRepository repository) { this.repository = repository; }

    @Scheduled(initialDelay = 60000, fixedDelay = 60000)
    @Transactional
    public void expirePreviews() { repository.deleteExpired(Instant.now().minusSeconds(900)); }
}
