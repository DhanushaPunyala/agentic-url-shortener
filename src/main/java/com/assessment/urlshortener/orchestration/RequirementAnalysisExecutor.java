package com.assessment.urlshortener.orchestration;

import com.assessment.urlshortener.ai.AiRequirementAnalysis;
import com.assessment.urlshortener.ai.RequirementAnalysisProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RequirementAnalysisExecutor implements StageExecutor {

    private RequirementAnalysisProvider analysisProvider;

    @Autowired(required = false)
    public void setAnalysisProvider(
            RequirementAnalysisProvider analysisProvider) {
        this.analysisProvider = analysisProvider;
    }

    @Override
    public WorkflowStage getStage() {
        return WorkflowStage.REQUIREMENT_ANALYSIS;
    }

    @Override
    public StageExecutionResult execute(WorkflowContext context) {

        String requirement = context.getRequirement();

        if (requirement == null || requirement.isBlank()) {
            return StageExecutionResult.failure(
                    "Requirement cannot be empty"
            );
        }

        /*
         * Controlled AI execution.
         *
         * When AI is disabled, unavailable, or fails,
         * RequirementAnalysisProvider returns Optional.empty()
         * and execution safely falls back to deterministic analysis.
         */
        if (analysisProvider != null) {

            Optional<AiRequirementAnalysis> aiAnalysis =
                    analysisProvider.analyze(requirement);

            if (aiAnalysis.isPresent()) {
                return applyAiAnalysis(
                        context,
                        aiAnalysis.get()
                );
            }
        }

        return executeDeterministicAnalysis(
                context,
                requirement
        );
    }

    private StageExecutionResult applyAiAnalysis(
            WorkflowContext context,
            AiRequirementAnalysis aiResult) {

        String normalizedRequirement =
                aiResult.normalizedRequirement;

        if (normalizedRequirement == null
                || normalizedRequirement.isBlank()) {

            normalizedRequirement =
                    context.getRequirement().trim();
        }

        List<String> ambiguities =
                safeList(aiResult.ambiguities);

        List<String> assumptions =
                safeList(aiResult.assumptions);

        List<String> acceptanceCriteria =
                safeList(aiResult.acceptanceCriteria);

        List<String> tasks =
                safeList(aiResult.engineeringTasks);

        Map<String, Object> analysis =
                new LinkedHashMap<>();

        analysis.put(
                "normalizedRequirement",
                normalizedRequirement
        );

        analysis.put(
                "ambiguities",
                ambiguities
        );

        analysis.put(
                "assumptions",
                assumptions
        );

        analysis.put(
                "acceptanceCriteria",
                acceptanceCriteria
        );

        analysis.put(
                "tasks",
                tasks
        );

        analysis.put(
                "analysisMode",
                "AI"
        );

        context.addStageOutput(
                "normalizedRequirement",
                normalizedRequirement
        );

        context.addStageOutput(
                "requirementAnalysis",
                analysis
        );

        context.addDecision(
                "Requirement analysis completed using "
                        + "the optional AI analysis path"
        );

        return StageExecutionResult.success(
                "AI requirement analysis completed",
                analysis
        );
    }

    private StageExecutionResult executeDeterministicAnalysis(
            WorkflowContext context,
            String requirement) {

        String normalizedRequirement =
                requirement.trim();

        List<String> ambiguities =
                identifyAmbiguities(
                        normalizedRequirement
                );

        List<String> assumptions =
                new ArrayList<>();

        if (!ambiguities.isEmpty()) {
            assumptions.add(
                    "Use safe prototype defaults until ambiguous "
                            + "requirements receive human clarification"
            );
        }

        List<String> acceptanceCriteria = List.of(
                "Requirement is converted into an actionable engineering plan",
                "Dependencies between SDLC stages are identified",
                "Implementation is validated through automated testing",
                "High-impact release action requires human approval"
        );

        List<String> tasks = List.of(
                "Analyze and normalize requirement",
                "Create architecture and design",
                "Implement required changes",
                "Execute testing and documentation in parallel",
                "Validate release readiness",
                "Obtain human approval before release"
        );

        Map<String, Object> analysis =
                new LinkedHashMap<>();

        analysis.put(
                "normalizedRequirement",
                normalizedRequirement
        );

        analysis.put(
                "ambiguities",
                ambiguities
        );

        analysis.put(
                "assumptions",
                assumptions
        );

        analysis.put(
                "acceptanceCriteria",
                acceptanceCriteria
        );

        analysis.put(
                "tasks",
                tasks
        );

        analysis.put(
                "analysisMode",
                "DETERMINISTIC_FALLBACK"
        );

        context.addStageOutput(
                "normalizedRequirement",
                normalizedRequirement
        );

        context.addStageOutput(
                "requirementAnalysis",
                analysis
        );

        if (ambiguities.isEmpty()) {
            context.addDecision(
                    "Requirement analyzed with no material ambiguity detected"
            );
        } else {
            context.addDecision(
                    "Requirement analyzed; ambiguities detected and "
                            + "prototype assumptions recorded"
            );
        }

        return StageExecutionResult.success(
                "Requirement analysis completed",
                analysis
        );
    }

    private List<String> safeList(
            List<String> values) {

        if (values == null) {
            return List.of();
        }

        return values;
    }

    private List<String> identifyAmbiguities(
            String requirement) {

        List<String> ambiguities =
                new ArrayList<>();

        String lower =
                requirement.toLowerCase();

        if (!lower.contains("expiration")
                && !lower.contains("expire")) {

            ambiguities.add(
                    "URL expiration behavior is not specified"
            );
        }

        if (!lower.contains("analytics")
                && !lower.contains("click")) {

            ambiguities.add(
                    "Analytics requirements are not specified"
            );
        }

        if (!lower.contains("authentication")
                && !lower.contains("security")) {

            ambiguities.add(
                    "Authentication and security requirements are not specified"
            );
        }

        if (!lower.contains("scale")
                && !lower.contains("performance")) {

            ambiguities.add(
                    "Scale and performance expectations are not specified"
            );
        }

        return ambiguities;
    }
}