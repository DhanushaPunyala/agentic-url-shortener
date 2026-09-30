package com.assessment.urlshortener.orchestration;

import java.time.LocalDateTime;

public class AuditEvent {

    private final LocalDateTime timestamp;
    private final String workflowId;
    private final String event;

    public AuditEvent(
            String workflowId,
            String event) {

        this.timestamp = LocalDateTime.now();
        this.workflowId = workflowId;
        this.event = event;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getEvent() {
        return event;
    }
}