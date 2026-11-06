package com.tcms.dto.plans;

import com.tcms.enums.TestResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

public final class PlanRequests {
    private PlanRequests() {}
    public record Metadata(@NotBlank @Size(max=255) String name, @Size(max=100) String release,
                           @Size(max=100) String build, @Size(max=100) String environment,
                           LocalDate dueDate, @Size(max=4000) String description) {}
    public record Create(@Positive int projectId, @NotNull @Valid Metadata metadata,
                         @NotEmpty @Size(max=1000) List<@NotNull @Positive Integer> caseIds,
                         @Positive Integer assigneeId) {}
    public record Assignment(@NotEmpty @Size(max=1000) List<@NotNull @Positive Integer> caseIds,
                             @Positive Integer assigneeId) {}
    public record Cases(@NotEmpty @Size(max=1000) List<@NotNull @Positive Integer> caseIds) {}
    public record Result(@NotNull TestResult status, @Size(max=4000) String comment,
                         @Size(max=10000) String actualResult, @Size(max=2000) String defectUrl,
                         @NotNull @PositiveOrZero Long expectedRevision) {}
}
