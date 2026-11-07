package com.tcms.automation;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** A configured worker credential can only claim work from its configured project. */
@Component
public class AutomationWorkerAccess {
    private final Environment environment;
    public AutomationWorkerAccess(Environment environment) { this.environment=environment; }
    public int authorize(String provided) {
        String token=environment.getProperty("automation.worker.token","");
        int projectId=environment.getProperty("automation.worker.project-id",Integer.class,0);
        if (token.isBlank() || token.length()<32 || projectId<=0)
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Configure an automation worker token and project ID before starting workers");
        if (provided==null || !MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),provided.getBytes(StandardCharsets.UTF_8)))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid worker authorization token");
        return projectId;
    }
}
