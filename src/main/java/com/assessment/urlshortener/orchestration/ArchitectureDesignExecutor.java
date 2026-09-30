package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ArchitectureDesignExecutor implements StageExecutor {

    @Override
    public WorkflowStage getStage() {
        return WorkflowStage.ARCHITECTURE_DESIGN;
    }

    @Override
    public StageExecutionResult execute(WorkflowContext context) {

        Object requirement =
                context.getStageOutput("normalizedRequirement");

        if (requirement == null) {
            return StageExecutionResult.failure(
                    "Normalized requirement is missing"
            );
        }

        Map<String, String> architecture = new LinkedHashMap<>();

        architecture.put(
                "apiLayer",
                "REST controllers for URL creation, redirect and analytics"
        );

        architecture.put(
                "serviceLayer",
                "Business logic for shortening, expiration and click tracking"
        );

        architecture.put(
                "persistenceLayer",
                "Spring Data JPA repository with H2 persistence"
        );

        architecture.put(
                "orchestrationLayer",
                "Stateful dependency graph coordinating SDLC stages"
        );

        context.addStageOutput(
                "architecture",
                architecture
        );

        context.addDecision(
                "Layered architecture selected with explicit orchestration layer"
        );

        return StageExecutionResult.success(
                "Architecture design completed",
                architecture
        );
    }
}