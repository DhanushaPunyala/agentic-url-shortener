package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ImplementationExecutor implements StageExecutor {

    @Override
    public WorkflowStage getStage() {
        return WorkflowStage.IMPLEMENTATION;
    }

    @Override
    public StageExecutionResult execute(WorkflowContext context) {

        Object architecture =
                context.getStageOutput("architecture");

        if (architecture == null) {
            return StageExecutionResult.failure(
                    "Architecture design is missing"
            );
        }

        Map<String, String> implementation =
                new LinkedHashMap<>();

        implementation.put(
                "urlCreation",
                "POST /api/urls"
        );

        implementation.put(
                "redirect",
                "GET /{shortCode}"
        );

        implementation.put(
                "analytics",
                "GET /api/urls/{shortCode}/stats"
        );

        implementation.put(
                "reliability",
                "Expiration handling, validation and custom error responses"
        );

        context.addStageOutput(
                "implementation",
                implementation
        );

        context.addDecision(
                "Implementation artifacts mapped to approved architecture"
        );

        return StageExecutionResult.success(
                "Implementation stage completed",
                implementation
        );
    }
}