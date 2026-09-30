package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DocumentationExecutor implements StageExecutor {

    @Override
    public WorkflowStage getStage() {
        return WorkflowStage.DOCUMENTATION;
    }

    @Override
    public StageExecutionResult execute(WorkflowContext context) {

        Object implementation =
                context.getStageOutput("implementation");

        if (implementation == null) {
            return StageExecutionResult.failure(
                    "Implementation output is missing"
            );
        }

        Map<String, String> documentation =
                new LinkedHashMap<>();

        documentation.put(
                "setup",
                "Application setup and execution instructions"
        );

        documentation.put(
                "api",
                "URL creation, redirect and analytics API documentation"
        );

        documentation.put(
                "architecture",
                "Architecture and orchestration design overview"
        );

        documentation.put(
                "testing",
                "Testing strategy and validation evidence"
        );

        documentation.put(
                "limitations",
                "Known limitations, assumptions and trade-offs"
        );

        context.addStageOutput(
                "documentation",
                documentation
        );

        context.addDecision(
                "Documentation artifacts prepared from implementation context"
        );

        return StageExecutionResult.success(
                "Documentation stage completed",
                documentation
        );
    }
}