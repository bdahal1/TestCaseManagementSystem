package com.tcms.controller;

import com.tcms.dto.reports.ReportResponse;
import com.tcms.services.ProjectReports;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/projectReports")
public class ProjectReportsController {
    private final ProjectReports reports;
    public ProjectReportsController(ProjectReports reports) { this.reports=reports; }
    @GetMapping("/{projectId}")
    @PreAuthorize("@projectAccess.project(#p0)")
    public ReportResponse report(@PathVariable int projectId, @RequestParam(required=false) LocalDate from,
                                 @RequestParam(required=false) LocalDate to, @RequestParam(defaultValue="") String environment,
                                 @RequestParam(defaultValue="") String release) {
        return reports.report(projectId,from,to,environment,release);
    }
}
