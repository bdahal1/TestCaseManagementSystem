package com.tcms.automation;

import com.tcms.automation.worker.AutomationWorkerMain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;

class AutomationWorkerAccessTest {
    private static final String TOKEN = "test-worker-credential-at-least-thirty-two-characters";

    @Test
    void workerAccessStaysDisabledUntilBothSettingsAreValid() {
        for (var environment : new MockEnvironment[]{new MockEnvironment(),
                new MockEnvironment().withProperty("automation.worker.token", "short").withProperty("automation.worker.project-id", "7"),
                new MockEnvironment().withProperty("automation.worker.token", TOKEN)}) {
            assertThatThrownBy(() -> new AutomationWorkerAccess(environment).authorize(TOKEN))
                    .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));
        }
    }

    @Test
    void credentialAuthorizesOnlyItsConfiguredProject() {
        var access = new AutomationWorkerAccess(new MockEnvironment().withProperty("automation.worker.token", TOKEN)
                .withProperty("automation.worker.project-id", "7"));
        assertThat(access.authorize(TOKEN)).isEqualTo(7);
        assertThatThrownBy(() -> access.authorize("tcms-automation-worker-token-secret"))
                .isInstanceOfSatisfying(ResponseStatusException.class, ex -> assertThat(ex.getStatusCode().value()).isEqualTo(401));
    }

    @Test
    void workerCannotStartWithAnImplicitCredential() {
        assertThatThrownBy(() -> new AutomationWorkerMain("http://localhost", null, "test-worker", true, 100))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("AUTOMATION_WORKER_TOKEN");
    }
}
