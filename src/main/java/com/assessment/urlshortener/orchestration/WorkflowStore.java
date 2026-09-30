package com.assessment.urlshortener.orchestration;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WorkflowStore {

    private final Map<String, WorkflowInstance> workflows =
            new ConcurrentHashMap<>();

    public void save(WorkflowInstance workflowInstance) {

        if (workflowInstance == null
                || workflowInstance.getContext() == null) {

            throw new IllegalArgumentException(
                    "Workflow instance and context are required"
            );
        }

        workflows.put(
                workflowInstance.getContext().getWorkflowId(),
                workflowInstance
        );
    }

    public Optional<WorkflowInstance> findById(
            String workflowId) {

        if (workflowId == null || workflowId.isBlank()) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                workflows.get(workflowId)
        );
    }

    public boolean existsById(String workflowId) {

        return workflowId != null
                && workflows.containsKey(workflowId);
    }

    public int size() {
        return workflows.size();
    }
}