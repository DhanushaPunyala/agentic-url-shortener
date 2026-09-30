package com.assessment.urlshortener.orchestration;

import com.assessment.urlshortener.orchestration.dto.WorkflowResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class WorkflowApiService {

    private final WorkflowStore workflowStore;
    private final WorkflowOrchestrator workflowOrchestrator;

    public WorkflowApiService(
            WorkflowStore workflowStore,
            WorkflowOrchestrator workflowOrchestrator) {

        this.workflowStore = workflowStore;
        this.workflowOrchestrator = workflowOrchestrator;
    }

    public WorkflowResponse createWorkflow(String requirement) {

        String workflowId = UUID.randomUUID().toString();

        WorkflowContext context =
                new WorkflowContext(workflowId, requirement);

        WorkflowGraph graph = new WorkflowGraph();

        WorkflowInstance instance =
                new WorkflowInstance(context, graph);

        workflowStore.save(instance);

        context.addDecision(
                "Workflow created for requirement: " + requirement
        );

        return buildResponse(instance);
    }

    public WorkflowResponse getWorkflow(String workflowId) {

        WorkflowInstance instance = getInstance(workflowId);

        return buildResponse(instance);
    }

    public WorkflowResponse executeWorkflow(String workflowId) {

        WorkflowInstance instance = getInstance(workflowId);

        WorkflowContext context = instance.getContext();
        WorkflowGraph graph = instance.getGraph();

        WorkflowTask releaseTask =
                graph.getTask(WorkflowStage.RELEASE_READINESS);

        /*
         * If testing and documentation are complete and
         * human approval has been granted, execute only
         * the release-readiness stage.
         */
        if (graph.getTask(WorkflowStage.TESTING).getStatus()
                        == WorkflowStatus.COMPLETED
                && graph.getTask(WorkflowStage.DOCUMENTATION).getStatus()
                        == WorkflowStatus.COMPLETED
                && context.getApprovalGate().isApproved()
                && releaseTask.getStatus()
                        != WorkflowStatus.COMPLETED) {

            workflowOrchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.RELEASE_READINESS
            );

            return buildResponse(instance);
        }

        /*
         * Do not rerun an already completed workflow.
         */
        if (releaseTask.getStatus()
                == WorkflowStatus.COMPLETED) {

            return buildResponse(instance);
        }

        workflowOrchestrator.executeFullWorkflow(
                graph,
                context
        );

        return buildResponse(instance);
    }

    public WorkflowResponse approveWorkflow(
            String workflowId,
            String approvedBy) {

        WorkflowInstance instance = getInstance(workflowId);

        workflowOrchestrator.approveRelease(
                instance.getContext(),
                approvedBy
        );

        return buildResponse(instance);
    }

    private WorkflowInstance getInstance(String workflowId) {

        return workflowStore.findById(workflowId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Workflow not found: " + workflowId
                        )
                );
    }

    private WorkflowResponse buildResponse(
            WorkflowInstance instance) {

        WorkflowContext context = instance.getContext();
        WorkflowGraph graph = instance.getGraph();

        Map<String, WorkflowStatus> statuses =
                new LinkedHashMap<>();

        for (WorkflowStage stage : WorkflowStage.values()) {

            statuses.put(
                    stage.name(),
                    graph.getTask(stage).getStatus()
            );
        }

        WorkflowMetrics metrics =
                context.getMetrics();

        return new WorkflowResponse(
                context.getWorkflowId(),
                context.getRequirement(),
                statuses,
                context.getStageOutputs(),
                context.getDecisionLog(),
                context.getApprovalGate().isApproved(),
                context.getApprovalGate().getApprovedBy(),
                metrics.getSuccessRate(),
                metrics.getRetryCount(),
                metrics.getRollbackCount(),
                metrics.getMeanTimeToRecoveryMillis(),
                metrics.getEndToEndLatencyMillis()
        );
    }
}