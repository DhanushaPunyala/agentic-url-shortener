package com.assessment.urlshortener.ai;

import com.assessment.urlshortener.orchestration.RequirementAnalysisExecutor;
import com.assessment.urlshortener.orchestration.StageExecutionResult;
import com.assessment.urlshortener.orchestration.WorkflowContext;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequirementAnalysisFallbackTest {

    @Test
    void shouldUseDeterministicFallbackWhenAiProviderIsUnavailable() {

        RequirementAnalysisExecutor executor =
                new RequirementAnalysisExecutor();

        WorkflowContext context =
                new WorkflowContext(
                        "fallback-test-workflow",
                        "Build a URL shortener"
                );

        StageExecutionResult result =
                executor.execute(context);

        assertTrue(result.isSuccess());

        Object output =
                context.getStageOutput("requirementAnalysis");

        assertNotNull(output);
        assertInstanceOf(Map.class, output);

        @SuppressWarnings("unchecked")
        Map<String, Object> analysis =
                (Map<String, Object>) output;

        assertEquals(
                "DETERMINISTIC_FALLBACK",
                analysis.get("analysisMode")
        );

        assertEquals(
                "Build a URL shortener",
                analysis.get("normalizedRequirement")
        );

        assertNotNull(
                analysis.get("ambiguities")
        );

        assertNotNull(
                analysis.get("assumptions")
        );

        assertNotNull(
                analysis.get("acceptanceCriteria")
        );

        assertNotNull(
                analysis.get("tasks")
        );
    }
}
