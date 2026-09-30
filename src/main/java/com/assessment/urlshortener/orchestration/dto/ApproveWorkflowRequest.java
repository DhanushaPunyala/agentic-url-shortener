package com.assessment.urlshortener.orchestration.dto;

import jakarta.validation.constraints.NotBlank;

public class ApproveWorkflowRequest {

    @NotBlank(message = "Approver name is required")
    private String approvedBy;

    public ApproveWorkflowRequest() {
    }

    public ApproveWorkflowRequest(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}