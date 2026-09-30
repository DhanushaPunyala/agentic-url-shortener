package com.assessment.urlshortener.orchestration;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WorkflowContext {

    private final String workflowId;
    private final String requirement;

    private final Map<String, Object> stageOutputs;
    private final List<String> decisionLog;
    private final List<AuditEvent> auditEvents;

    private final WorkflowMetrics metrics;
    private final ApprovalGate approvalGate;

    public WorkflowContext(
            String workflowId,
            String requirement) {

        this.workflowId = workflowId;
        this.requirement = requirement;

        // Thread-safe because multiple workflow stages
        // may execute in parallel.
        this.stageOutputs =
                new ConcurrentHashMap<>();

        this.decisionLog =
                new CopyOnWriteArrayList<>();

        this.auditEvents =
                new CopyOnWriteArrayList<>();

        this.metrics = new WorkflowMetrics();
        this.metrics.start();

        this.approvalGate = new ApprovalGate();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getRequirement() {
        return requirement;
    }

    public void addStageOutput(
            String key,
            Object value) {

        stageOutputs.put(key, value);
    }

    public void removeStageOutput(
            String key) {

        stageOutputs.remove(key);
    }

    public Object getStageOutput(
            String key) {

        return stageOutputs.get(key);
    }

    public Map<String, Object> getStageOutputs() {
        return Map.copyOf(stageOutputs);
    }

    public void addDecision(
            String decision) {

        decisionLog.add(decision);

        auditEvents.add(
                new AuditEvent(
                        workflowId,
                        decision
                )
        );
    }

    public List<String> getDecisionLog() {
        return List.copyOf(decisionLog);
    }

    public List<AuditEvent> getAuditEvents() {
        return List.copyOf(auditEvents);
    }

    public WorkflowMetrics getMetrics() {
        return metrics;
    }

    public ApprovalGate getApprovalGate() {
        return approvalGate;
    }
}