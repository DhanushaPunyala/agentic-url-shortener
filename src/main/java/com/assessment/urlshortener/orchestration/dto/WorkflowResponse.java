package com.assessment.urlshortener.orchestration.dto;

import com.assessment.urlshortener.orchestration.WorkflowStatus;

import java.util.List;
import java.util.Map;

public class WorkflowResponse {

    private final String workflowId;
    private final String requirement;
    private final Map<String, WorkflowStatus> stageStatuses;
    private final Map<String, Object> stageOutputs;
    private final List<String> decisions;
    private final boolean releaseApproved;
    private final String approvedBy;

    private final double successRate;
    private final int retryCount;
    private final int rollbackCount;
    private final long meanTimeToRecoveryMillis;
    private final long endToEndLatencyMillis;

    public WorkflowResponse(
            String workflowId,
            String requirement,
            Map<String, WorkflowStatus> stageStatuses,
            Map<String, Object> stageOutputs,
            List<String> decisions,
            boolean releaseApproved,
            String approvedBy,
            double successRate,
            int retryCount,
            int rollbackCount,
            long meanTimeToRecoveryMillis,
            long endToEndLatencyMillis) {

        this.workflowId = workflowId;
        this.requirement = requirement;
        this.stageStatuses = stageStatuses;
        this.stageOutputs = stageOutputs;
        this.decisions = decisions;
        this.releaseApproved = releaseApproved;
        this.approvedBy = approvedBy;
        this.successRate = successRate;
        this.retryCount = retryCount;
        this.rollbackCount = rollbackCount;
        this.meanTimeToRecoveryMillis = meanTimeToRecoveryMillis;
        this.endToEndLatencyMillis = endToEndLatencyMillis;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getRequirement() {
        return requirement;
    }

    public Map<String, WorkflowStatus> getStageStatuses() {
        return stageStatuses;
    }

    public Map<String, Object> getStageOutputs() {
        return stageOutputs;
    }

    public List<String> getDecisions() {
        return decisions;
    }

    public boolean isReleaseApproved() {
        return releaseApproved;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public int getRollbackCount() {
        return rollbackCount;
    }

    public long getMeanTimeToRecoveryMillis() {
        return meanTimeToRecoveryMillis;
    }

    public long getEndToEndLatencyMillis() {
        return endToEndLatencyMillis;
    }
}