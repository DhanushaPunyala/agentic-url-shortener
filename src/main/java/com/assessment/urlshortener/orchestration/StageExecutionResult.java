package com.assessment.urlshortener.orchestration;

public class StageExecutionResult {

    private final boolean success;
    private final String message;
    private final Object output;

    public StageExecutionResult(
            boolean success,
            String message,
            Object output) {

        this.success = success;
        this.message = message;
        this.output = output;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getOutput() {
        return output;
    }

    public static StageExecutionResult success(
            String message,
            Object output) {

        return new StageExecutionResult(
                true,
                message,
                output
        );
    }

    public static StageExecutionResult failure(String message) {

        return new StageExecutionResult(
                false,
                message,
                null
        );
    }
}