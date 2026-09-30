package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
public class WorkflowOrchestrator {

    private final Map<WorkflowStage, StageExecutor> executors;
    private final PolicyGuardrail policyGuardrail;

    public WorkflowOrchestrator(
            List<StageExecutor> stageExecutors,
            PolicyGuardrail policyGuardrail) {

        this.executors = new EnumMap<>(WorkflowStage.class);

        for (StageExecutor executor : stageExecutors) {
            this.executors.put(
                    executor.getStage(),
                    executor
            );
        }

        this.policyGuardrail = policyGuardrail;
    }

    public WorkflowContext createWorkflow(String requirement) {

        String workflowId =
                UUID.randomUUID().toString();

        return new WorkflowContext(
                workflowId,
                requirement
        );
    }

    public StageExecutionResult executeStage(
            WorkflowGraph graph,
            WorkflowContext context,
            WorkflowStage stage) {

        WorkflowTask task =
                graph.getTask(stage);

        if (task == null) {
            return StageExecutionResult.failure(
                    "Unknown workflow stage: " + stage
            );
        }

        // Entry gate.
        if (!graph.areDependenciesCompleted(stage)) {
            return StageExecutionResult.failure(
                    "Dependencies are not completed for stage: " + stage
            );
        }

        // Policy guardrail.
        StageExecutionResult policyResult =
                policyGuardrail.validate(
                        context,
                        stage
                );

        if (!policyResult.isSuccess()) {

            task.setStatus(
                    WorkflowStatus.SAFE_STOPPED
            );

            context.addDecision(
                    stage
                            + " blocked by policy: "
                            + policyResult.getMessage()
            );

            context.getMetrics().recordFailure();

            return policyResult;
        }

        // Human approval gate.
        if (stage == WorkflowStage.RELEASE_READINESS
                && !context.getApprovalGate().isApproved()) {

            task.setStatus(
                    WorkflowStatus.WAITING_FOR_APPROVAL
            );

            context.addDecision(
                    "Release readiness paused pending human approval"
            );

            return StageExecutionResult.failure(
                    "Human approval is required before release readiness"
            );
        }

        StageExecutor executor =
                executors.get(stage);

        if (executor == null) {
            return StageExecutionResult.failure(
                    "No executor registered for stage: " + stage
            );
        }

        task.setStatus(
                WorkflowStatus.RUNNING
        );

        StageExecutionResult result =
                executor.execute(context);

        if (result.isSuccess()) {

            task.setStatus(
                    WorkflowStatus.COMPLETED
            );

            context.getMetrics().recordSuccess();

            if (stage == WorkflowStage.RELEASE_READINESS) {
                context.getMetrics().finish();
            }

            context.addDecision(
                    stage + " completed successfully"
            );

        } else {

            context.getMetrics().recordFailure();

            if (task.canRetry()) {

                task.incrementRetryCount();

                context.getMetrics().recordRetry();

                task.setStatus(
                        WorkflowStatus.PENDING
                );

                context.addDecision(
                        stage
                                + " failed. Retry "
                                + task.getRetryCount()
                                + " of "
                                + task.getMaxRetries()
                );

            } else {

                task.setStatus(
                        WorkflowStatus.SAFE_STOPPED
                );

                context.addDecision(
                        stage
                                + " reached maximum retries. "
                                + "Workflow safely stopped."
                );
            }
        }

        return result;
    }

    public void approveRelease(
            WorkflowContext context,
            String approvedBy) {

        context.getApprovalGate().approve(
                approvedBy
        );

        context.addDecision(
                "Release approved by " + approvedBy
        );
    }

    public boolean isReleaseApproved(
            WorkflowContext context) {

        return context
                .getApprovalGate()
                .isApproved();
    }

    public void rollbackStage(
            WorkflowGraph graph,
            WorkflowContext context,
            WorkflowStage stage) {

        WorkflowTask task =
                graph.getTask(stage);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Unknown workflow stage: " + stage
            );
        }

        task.setStatus(
                WorkflowStatus.ROLLED_BACK
        );

        graph.resetDownstreamStages(stage);

        switch (stage) {

            case REQUIREMENT_ANALYSIS -> {
                context.removeStageOutput("normalizedRequirement");
                context.removeStageOutput("architecture");
                context.removeStageOutput("implementation");
                context.removeStageOutput("testing");
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case ARCHITECTURE_DESIGN -> {
                context.removeStageOutput("architecture");
                context.removeStageOutput("implementation");
                context.removeStageOutput("testing");
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case IMPLEMENTATION -> {
                context.removeStageOutput("implementation");
                context.removeStageOutput("testing");
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case TESTING -> {
                context.removeStageOutput("testing");
                context.removeStageOutput("releaseReadiness");
            }

            case DOCUMENTATION -> {
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case RELEASE_READINESS ->
                    context.removeStageOutput("releaseReadiness");
        }

        context.getApprovalGate().revoke();

        context.getMetrics().recordRollback();

        context.addDecision(
                stage
                        + " rolled back by orchestration control. "
                        + "Downstream stages and stale outputs invalidated."
        );
    }

    public void safeStopStage(
            WorkflowGraph graph,
            WorkflowContext context,
            WorkflowStage stage,
            String reason) {

        WorkflowTask task =
                graph.getTask(stage);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Unknown workflow stage: " + stage
            );
        }

        task.setStatus(
                WorkflowStatus.SAFE_STOPPED
        );

        // Record safe-stop as a failed workflow stage.
        context.getMetrics().recordFailure();

        context.addDecision(
                stage
                        + " safely stopped. Reason: "
                        + reason
        );
    }

    public void replanFromStage(
            WorkflowGraph graph,
            WorkflowContext context,
            WorkflowStage changedStage,
            String reason) {

        WorkflowTask changedTask =
                graph.getTask(changedStage);

        if (changedTask == null) {
            throw new IllegalArgumentException(
                    "Unknown workflow stage: "
                            + changedStage
            );
        }

        changedTask.setStatus(
                WorkflowStatus.PENDING
        );

        graph.resetDownstreamStages(
                changedStage
        );

        switch (changedStage) {

            case REQUIREMENT_ANALYSIS -> {
                context.removeStageOutput("normalizedRequirement");
                context.removeStageOutput("architecture");
                context.removeStageOutput("implementation");
                context.removeStageOutput("testing");
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case ARCHITECTURE_DESIGN -> {
                context.removeStageOutput("architecture");
                context.removeStageOutput("implementation");
                context.removeStageOutput("testing");
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case IMPLEMENTATION -> {
                context.removeStageOutput("implementation");
                context.removeStageOutput("testing");
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case TESTING -> {
                context.removeStageOutput("testing");
                context.removeStageOutput("releaseReadiness");
            }

            case DOCUMENTATION -> {
                context.removeStageOutput("documentation");
                context.removeStageOutput("releaseReadiness");
            }

            case RELEASE_READINESS ->
                    context.removeStageOutput("releaseReadiness");
        }

        context.getApprovalGate().revoke();

        context.addDecision(
                "Dynamic replanning triggered from "
                        + changedStage
                        + ". Reason: "
                        + reason
                        + ". Stale outputs invalidated."
        );
    }

    public void applyFallback(
            WorkflowGraph graph,
            WorkflowContext context,
            WorkflowStage stage,
            String fallbackDescription) {

        WorkflowTask task =
                graph.getTask(stage);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Unknown workflow stage: " + stage
            );
        }

        if (fallbackDescription == null
                || fallbackDescription.isBlank()) {

            throw new IllegalArgumentException(
                    "Fallback description is required"
            );
        }

        // Fallback cannot bypass dependency gates.
        if (!graph.areDependenciesCompleted(stage)) {

            context.addDecision(
                    "Fallback blocked for "
                            + stage
                            + " because dependencies are incomplete"
            );

            return;
        }

        // Fallback cannot bypass policy guardrails.
        StageExecutionResult policyResult =
                policyGuardrail.validate(
                        context,
                        stage
                );

        if (!policyResult.isSuccess()) {

            task.setStatus(
                    WorkflowStatus.SAFE_STOPPED
            );

            context.getMetrics().recordFailure();

            context.addDecision(
                    "Fallback blocked by policy for "
                            + stage
                            + ": "
                            + policyResult.getMessage()
            );

            return;
        }

        context.addStageOutput(
                stage.name() + "_fallback",
                fallbackDescription
        );

        task.setStatus(
                WorkflowStatus.COMPLETED
        );

        context.getMetrics().recordSuccess();

        context.addDecision(
                "Fallback applied for "
                        + stage
                        + ": "
                        + fallbackDescription
        );
    }

    public void executeInitialWorkflow(
            WorkflowGraph graph,
            WorkflowContext context) {

        executeStage(
                graph,
                context,
                WorkflowStage.REQUIREMENT_ANALYSIS
        );

        if (graph.getTask(
                WorkflowStage.REQUIREMENT_ANALYSIS
        ).getStatus() != WorkflowStatus.COMPLETED) {

            return;
        }

        executeStage(
                graph,
                context,
                WorkflowStage.ARCHITECTURE_DESIGN
        );

        if (graph.getTask(
                WorkflowStage.ARCHITECTURE_DESIGN
        ).getStatus() != WorkflowStatus.COMPLETED) {

            return;
        }

        executeStage(
                graph,
                context,
                WorkflowStage.IMPLEMENTATION
        );
    }

    public void executeParallelValidationStages(
            WorkflowGraph graph,
            WorkflowContext context) {

        if (graph.getTask(
                WorkflowStage.IMPLEMENTATION
        ).getStatus() != WorkflowStatus.COMPLETED) {

            context.addDecision(
                    "Parallel validation blocked because "
                            + "implementation is not completed"
            );

            return;
        }

        CompletableFuture<StageExecutionResult>
                testingFuture =
                CompletableFuture.supplyAsync(
                        () -> executeStage(
                                graph,
                                context,
                                WorkflowStage.TESTING
                        )
                );

        CompletableFuture<StageExecutionResult>
                documentationFuture =
                CompletableFuture.supplyAsync(
                        () -> executeStage(
                                graph,
                                context,
                                WorkflowStage.DOCUMENTATION
                        )
                );

        CompletableFuture.allOf(
                testingFuture,
                documentationFuture
        ).join();

        context.addDecision(
                "Testing and documentation parallel "
                        + "branches synchronized"
        );
    }

    public StageExecutionResult executeFullWorkflow(
            WorkflowGraph graph,
            WorkflowContext context) {

        executeInitialWorkflow(
                graph,
                context
        );

        if (graph.getTask(
                WorkflowStage.IMPLEMENTATION
        ).getStatus() != WorkflowStatus.COMPLETED) {

            return StageExecutionResult.failure(
                    "Workflow stopped before validation stages"
            );
        }

        executeParallelValidationStages(
                graph,
                context
        );

        if (graph.getTask(
                WorkflowStage.TESTING
        ).getStatus() != WorkflowStatus.COMPLETED
                ||
                graph.getTask(
                        WorkflowStage.DOCUMENTATION
                ).getStatus() != WorkflowStatus.COMPLETED) {

            return StageExecutionResult.failure(
                    "Testing or documentation "
                            + "did not complete successfully"
            );
        }

        if (!context
                .getApprovalGate()
                .isApproved()) {

            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).setStatus(
                    WorkflowStatus.WAITING_FOR_APPROVAL
            );

            context.addDecision(
                    "Full workflow paused pending "
                            + "human release approval"
            );

            return StageExecutionResult.failure(
                    "Human approval is required "
                            + "before release readiness"
            );
        }

        return executeStage(
                graph,
                context,
                WorkflowStage.RELEASE_READINESS
        );
    }
}