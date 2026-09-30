package com.assessment.urlshortener.orchestration;

public interface StageExecutor {

    WorkflowStage getStage();

    StageExecutionResult execute(WorkflowContext context);
}