package com.tcms.automation.worker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class AutomationWorkerSettingsTest {
    @TempDir Path directory;

    @Test
    void usesTheSameLocalCredentialAsTheBackendAndDoesNotEvaluateShellText() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "# local settings\nAUTOMATION_WORKER_TOKEN=local-test-token\nTMS_URL=http://localhost:8080/dhtcms/api/v1\nLITERAL=$(echo do-not-run)\n");
        assertThat(AutomationWorkerMain.setting("AUTOMATION_WORKER_TOKEN", Map.of(), file, null)).isEqualTo("local-test-token");
        assertThat(AutomationWorkerMain.setting("LITERAL", Map.of(), file, null)).isEqualTo("$(echo do-not-run)");
        assertThat(AutomationWorkerMain.setting("AUTOMATION_WORKER_TOKEN", Map.of("AUTOMATION_WORKER_TOKEN", "environment-token"), file, null)).isEqualTo("environment-token");
        assertThat(AutomationWorkerMain.setting("AUTOMATION_WORKER_TOKEN", Map.of("AUTOMATION_WORKER_TOKEN", ""), file, null)).isEmpty();
    }

    @Test
    void localSettingsAreOptional() {
        assertThat(AutomationWorkerMain.setting("TMS_URL", Map.of(), directory.resolve("missing"), "default-url")).isEqualTo("default-url");
        assertThat(AutomationWorkerMain.setting("AUTOMATION_WORKER_TOKEN", Map.of(), directory.resolve("missing"), null)).isNull();
    }
}
