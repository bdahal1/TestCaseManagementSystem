package com.tcms.dto;

import jakarta.validation.constraints.*;

import com.tcms.enums.TestTypes;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TestCaseInfoDTO {
    public interface Create {}
    @NotBlank(groups=Create.class) @Size(max=255) @Pattern(regexp="(?s).*\\S.*")
    String testName;
    @NotNull(groups=Create.class) @Positive
    Integer userId;
    @NotNull(groups=Create.class) @Positive
    Integer projectId;
    Integer[] selectedTags;
    @NotNull(groups=Create.class)
    TestTypes testType;
    @Size(max=100000)
    String gherkin;
    com.tcms.enums.CasePriority priority;
    com.tcms.enums.AutomationStatus automationStatus;
    @Size(max=100) String component;
    @PositiveOrZero Integer ownerId;
    @Min(0) @Max(10080) Integer estimatedMinutes;
    @Size(max=10000) String description;
    @Size(max=10000) String preconditions;
    @Size(max=20)
    java.util.Map<@NotBlank @Size(max=80) String, @NotNull @Size(max=1000) String> customFields;
}
