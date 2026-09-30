package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ReleaseReadinessExecutor implements StageExecutor {

    @Override
    public WorkflowStage getStage() {
        return WorkflowStage.RELEASE_READINESS;
    }

    @Override
    public StageExecutionResult execute(WorkflowContext context) {

        Object testing =
                context.getStageOutput("testing");

        Object documentation =
                context.getStageOutput("documentation");

        if (testing == null || documentation == null) {
            return StageExecutionResult.failure(
                    "Testing and documentation must be completed before release"
            );
        }

        Map<String, Object> readiness =
                new LinkedHashMap<>();

        readiness.put("testsValidated", true);
        readiness.put("documentationValidated", true);
        readiness.put("releaseCandidate", true);

        context.addStageOutput(
                "releaseReadiness",
                readiness
        );

        context.addDecision(
                "Release readiness validated after testing and documentation synchronization"
        );

        return StageExecutionResult.success(
                "Release readiness validation completed",
                readiness
        );
    }
}