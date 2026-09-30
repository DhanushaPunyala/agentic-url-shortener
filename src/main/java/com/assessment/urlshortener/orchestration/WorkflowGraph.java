package com.assessment.urlshortener.orchestration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class WorkflowGraph {

    private final Map<WorkflowStage, WorkflowTask> tasks =
            new EnumMap<>(WorkflowStage.class);

    public WorkflowGraph() {

        tasks.put(
                WorkflowStage.REQUIREMENT_ANALYSIS,
                new WorkflowTask(
                        WorkflowStage.REQUIREMENT_ANALYSIS,
                        List.of(),
                        2));

        tasks.put(
                WorkflowStage.ARCHITECTURE_DESIGN,
                new WorkflowTask(
                        WorkflowStage.ARCHITECTURE_DESIGN,
                        List.of(WorkflowStage.REQUIREMENT_ANALYSIS),
                        2));

        tasks.put(
                WorkflowStage.IMPLEMENTATION,
                new WorkflowTask(
                        WorkflowStage.IMPLEMENTATION,
                        List.of(WorkflowStage.ARCHITECTURE_DESIGN),
                        2));

        tasks.put(
                WorkflowStage.TESTING,
                new WorkflowTask(
                        WorkflowStage.TESTING,
                        List.of(WorkflowStage.IMPLEMENTATION),
                        2));

        tasks.put(
                WorkflowStage.DOCUMENTATION,
                new WorkflowTask(
                        WorkflowStage.DOCUMENTATION,
                        List.of(WorkflowStage.IMPLEMENTATION),
                        2));

        tasks.put(
                WorkflowStage.RELEASE_READINESS,
                new WorkflowTask(
                        WorkflowStage.RELEASE_READINESS,
                        List.of(
                                WorkflowStage.TESTING,
                                WorkflowStage.DOCUMENTATION),
                        1));
    }

    public WorkflowTask getTask(WorkflowStage stage) {
        return tasks.get(stage);
    }

    public Map<WorkflowStage, WorkflowTask> getTasks() {
        return Map.copyOf(tasks);
    }

    public boolean areDependenciesCompleted(WorkflowStage stage) {

        WorkflowTask task = tasks.get(stage);

        return task.getDependencies()
                .stream()
                .allMatch(dependency ->
                        tasks.get(dependency).getStatus()
                                == WorkflowStatus.COMPLETED);
    }
    public void resetDownstreamStages(WorkflowStage changedStage) {

    for (WorkflowTask task : tasks.values()) {

        if (task.getDependencies().contains(changedStage)) {

            task.setStatus(WorkflowStatus.PENDING);

            resetDownstreamStages(task.getStage());
        }
    }
}
}