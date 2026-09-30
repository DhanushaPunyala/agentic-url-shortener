package com.assessment.urlshortener.orchestration;

public class WorkflowInstance {

    private final WorkflowContext context;
    private final WorkflowGraph graph;

    public WorkflowInstance(
            WorkflowContext context,
            WorkflowGraph graph) {

        this.context = context;
        this.graph = graph;
    }

    public WorkflowContext getContext() {
        return context;
    }

    public WorkflowGraph getGraph() {
        return graph;
    }
}