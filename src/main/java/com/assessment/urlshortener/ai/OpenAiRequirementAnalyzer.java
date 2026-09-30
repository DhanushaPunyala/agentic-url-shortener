package com.assessment.urlshortener.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatModel;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;

public class OpenAiRequirementAnalyzer {

    private final OpenAIClient client;

    public OpenAiRequirementAnalyzer() {
        this.client = OpenAIOkHttpClient.fromEnv();
    }

    public AiRequirementAnalysis analyze(String requirement) {

        String prompt = """
                You are a senior software engineering requirement-analysis agent.

                Analyze the following software requirement:

                %s

                Produce a structured engineering analysis.

                normalizedRequirement:
                Rewrite the requirement as a clear engineering problem.

                ambiguities:
                Identify important missing or unclear requirements.

                assumptions:
                Record safe prototype assumptions. Do not silently invent
                business-critical requirements.

                acceptanceCriteria:
                Produce measurable acceptance criteria.

                engineeringTasks:
                Decompose the requirement into actionable engineering tasks.

                Consider security, reliability, performance, observability,
                testing, API behavior, data handling, and operational concerns.
                """.formatted(requirement);

        StructuredResponseCreateParams<AiRequirementAnalysis> params =
                ResponseCreateParams.builder()
                        .input(prompt)
                        .text(AiRequirementAnalysis.class)
                        .model(ChatModel.GPT_5)
                        .build();

        return client.responses()
                .create(params)
                .output()
                .stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "OpenAI returned no structured requirement analysis"));
    }
}