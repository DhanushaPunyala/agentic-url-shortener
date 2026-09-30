package com.assessment.urlshortener.orchestration;

import java.util.ArrayList;
import java.util.List;

public class WorkflowTask {

    private final WorkflowStage stage;
    private WorkflowStatus status;
    private final List<WorkflowStage> dependencies;
    private int retryCount;
    private final int maxRetries;

    public WorkflowTask(
            WorkflowStage stage,
            List<WorkflowStage> dependencies,
            int maxRetries) {

        this.stage = stage;
        this.dependencies = new ArrayList<>(dependencies);
        this.maxRetries = maxRetries;
        this.status = WorkflowStatus.PENDING;
        this.retryCount = 0;
    }

    public WorkflowStage getStage() {
        return stage;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public void setStatus(WorkflowStatus status) {
        this.status = status;
    }

    public List<WorkflowStage> getDependencies() {
        return List.copyOf(dependencies);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void incrementRetryCount() {
        retryCount++;
    }
}