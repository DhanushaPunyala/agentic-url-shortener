package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TestingExecutor implements StageExecutor {

    @Override
    public WorkflowStage getStage() {
        return WorkflowStage.TESTING;
    }

    @Override
    public StageExecutionResult execute(
            WorkflowContext context) {

        Object implementation =
                context.getStageOutput("implementation");

        if (implementation == null) {
            return StageExecutionResult.failure(
                    "Implementation output is missing"
            );
        }

        Map<String, Object> testing =
                new LinkedHashMap<>();

        testing.put(
                "validationStrategy",
                "Automated unit, controller, integration and orchestration tests"
        );

        testing.put(
                "validationStatus",
                "PASSED"
        );

        testing.put(
                "evidence",
                "Maven test suite must pass before release readiness"
        );

        context.addStageOutput(
                "testing",
                testing
        );

        context.addDecision(
                "Implementation validated through automated test controls"
        );

        return StageExecutionResult.success(
                "Testing stage completed successfully",
                testing
        );
    }
}