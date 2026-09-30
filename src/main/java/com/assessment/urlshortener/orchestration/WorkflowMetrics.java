package com.assessment.urlshortener.orchestration;

public class WorkflowMetrics {

    private int successfulStages;
    private int failedStages;
    private int retryCount;
    private int rollbackCount;

    private long startTime;
    private long endTime;

    private long failureStartTime;
    private long totalRecoveryTimeMillis;
    private int recoveryCount;

    public synchronized void start() {
        startTime = System.currentTimeMillis();
    }

    public synchronized void finish() {
        endTime = System.currentTimeMillis();
    }

    public synchronized void recordSuccess() {
        successfulStages++;

        if (failureStartTime > 0) {
            totalRecoveryTimeMillis +=
                    System.currentTimeMillis() - failureStartTime;

            recoveryCount++;
            failureStartTime = 0;
        }
    }

    public synchronized void recordFailure() {
        failedStages++;

        if (failureStartTime == 0) {
            failureStartTime = System.currentTimeMillis();
        }
    }

    public synchronized void recordRetry() {
        retryCount++;
    }

    public synchronized void recordRollback() {
        rollbackCount++;
    }

    public synchronized int getSuccessfulStages() {
        return successfulStages;
    }

    public synchronized int getFailedStages() {
        return failedStages;
    }

    public synchronized int getRetryCount() {
        return retryCount;
    }

    public synchronized int getRollbackCount() {
        return rollbackCount;
    }

    public synchronized double getSuccessRate() {

        int total =
                successfulStages + failedStages;

        if (total == 0) {
            return 0.0;
        }

        return (successfulStages * 100.0) / total;
    }

    public synchronized long getMeanTimeToRecoveryMillis() {

        if (recoveryCount == 0) {
            return 0;
        }

        return totalRecoveryTimeMillis / recoveryCount;
    }

    public synchronized long getEndToEndLatencyMillis() {

        if (startTime == 0) {
            return 0;
        }

        long effectiveEndTime =
                endTime == 0
                        ? System.currentTimeMillis()
                        : endTime;

        return effectiveEndTime - startTime;
    }
}