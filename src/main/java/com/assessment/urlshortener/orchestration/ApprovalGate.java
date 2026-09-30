package com.assessment.urlshortener.orchestration;

public class ApprovalGate {

    private boolean approved;
    private String approvedBy;

    public boolean isApproved() {
        return approved;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void approve(String approvedBy) {

        if (approvedBy == null || approvedBy.isBlank()) {
            throw new IllegalArgumentException(
                    "Approver name is required"
            );
        }

        this.approved = true;
        this.approvedBy = approvedBy;
    }

    public void revoke() {
        this.approved = false;
        this.approvedBy = null;
    }
}