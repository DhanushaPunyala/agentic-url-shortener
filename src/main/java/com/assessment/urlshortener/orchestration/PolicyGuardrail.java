package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

@Component
public class PolicyGuardrail {

    public StageExecutionResult validate(
            WorkflowContext context,
            WorkflowStage stage) {

        if (context == null) {
            return StageExecutionResult.failure(
                    "Workflow context is required"
            );
        }

        if (stage == null) {
            return StageExecutionResult.failure(
                    "Workflow stage is required"
            );
        }

        String requirement = context.getRequirement();

        if (requirement == null || requirement.isBlank()) {
            return StageExecutionResult.failure(
                    "Security policy rejected an empty requirement"
            );
        }

        /*
         * SECURITY GUARDRAIL
         * Prevent requirements that explicitly request
         * bypassing security controls.
         */
        String normalizedRequirement = requirement.toLowerCase();

        if (normalizedRequirement.contains("disable security")
                || normalizedRequirement.contains("bypass authentication")
                || normalizedRequirement.contains("skip authorization")) {

            return StageExecutionResult.failure(
                    "Security policy blocked a request to bypass security controls"
            );
        }

        /*
         * CHANGE-CONTROL GUARDRAIL
         * Release readiness cannot execute until testing
         * and documentation evidence exists.
         */
        if (stage == WorkflowStage.RELEASE_READINESS) {

            if (context.getStageOutput("testing") == null) {
                return StageExecutionResult.failure(
                        "Change-control policy requires successful testing before release"
                );
            }

            if (context.getStageOutput("documentation") == null) {
                return StageExecutionResult.failure(
                        "Change-control policy requires documentation before release"
                );
            }
        }

        return StageExecutionResult.success(
                "Security, compliance, and change-control policies passed",
                null
        );
    }
}