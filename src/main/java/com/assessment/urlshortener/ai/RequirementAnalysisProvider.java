package com.assessment.urlshortener.ai;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RequirementAnalysisProvider {

    private final boolean aiEnabled;
    private final ObjectProvider<OpenAiRequirementAnalyzer> aiAnalyzerProvider;

    public RequirementAnalysisProvider(
            @Value("${app.ai.enabled:false}") boolean aiEnabled,
            ObjectProvider<OpenAiRequirementAnalyzer> aiAnalyzerProvider) {

        this.aiEnabled = aiEnabled;
        this.aiAnalyzerProvider = aiAnalyzerProvider;
    }

    public Optional<AiRequirementAnalysis> analyze(String requirement) {

        if (!aiEnabled) {
            return Optional.empty();
        }

        OpenAiRequirementAnalyzer analyzer =
                aiAnalyzerProvider.getIfAvailable();

        if (analyzer == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(
                    analyzer.analyze(requirement)
            );
        } catch (Exception exception) {
            return Optional.empty();
        }
    }
}