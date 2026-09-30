package com.assessment.urlshortener.orchestration;

public enum WorkflowStatus {

    PENDING,
    RUNNING,
    WAITING_FOR_APPROVAL,
    COMPLETED,
    FAILED,
    ROLLED_BACK,
    SAFE_STOPPED
}