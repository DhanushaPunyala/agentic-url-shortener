package com.assessment.urlshortener.orchestration.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateWorkflowRequest {

    @NotBlank(message = "Requirement is required")
    private String requirement;

    public CreateWorkflowRequest() {
    }

    public CreateWorkflowRequest(String requirement) {
        this.requirement = requirement;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }
}
