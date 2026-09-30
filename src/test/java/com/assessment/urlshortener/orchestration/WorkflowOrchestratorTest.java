package com.assessment.urlshortener.orchestration;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class WorkflowOrchestratorTest {

    @Test
    void shouldBlockImplementationWhenDependenciesAreNotCompleted() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        StageExecutionResult result =
                orchestrator.executeStage(
                        graph,
                        context,
                        WorkflowStage.IMPLEMENTATION
                );

        assertFalse(result.isSuccess());

        assertEquals(
                "Dependencies are not completed for stage: IMPLEMENTATION",
                result.getMessage()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.IMPLEMENTATION
                ).getStatus()
        );
    }

    @Test
    void shouldRequireHumanApprovalBeforeReleaseReadiness() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        TestingExecutor testingExecutor =
                new TestingExecutor();

        DocumentationExecutor documentationExecutor =
                new DocumentationExecutor();

        ReleaseReadinessExecutor releaseExecutor =
                new ReleaseReadinessExecutor();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(
                                testingExecutor,
                                documentationExecutor,
                                releaseExecutor
                        ),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        context.addStageOutput(
                "testing",
                "Tests passed"
        );

        context.addStageOutput(
                "documentation",
                "Documentation completed"
        );

        graph.getTask(
                WorkflowStage.TESTING
        ).setStatus(WorkflowStatus.COMPLETED);

        graph.getTask(
                WorkflowStage.DOCUMENTATION
        ).setStatus(WorkflowStatus.COMPLETED);

        StageExecutionResult result =
                orchestrator.executeStage(
                        graph,
                        context,
                        WorkflowStage.RELEASE_READINESS
                );

        assertFalse(result.isSuccess());

        assertEquals(
                WorkflowStatus.WAITING_FOR_APPROVAL,
                graph.getTask(
                        WorkflowStage.RELEASE_READINESS
                ).getStatus()
        );

        assertEquals(
                "Human approval is required before release readiness",
                result.getMessage()
        );
    }

    @Test
    void shouldCompleteReleaseReadinessAfterHumanApproval() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        ReleaseReadinessExecutor releaseExecutor =
                new ReleaseReadinessExecutor();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(releaseExecutor),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        context.addStageOutput(
                "testing",
                "Tests passed"
        );

        context.addStageOutput(
                "documentation",
                "Documentation completed"
        );

        graph.getTask(
                WorkflowStage.TESTING
        ).setStatus(WorkflowStatus.COMPLETED);

        graph.getTask(
                WorkflowStage.DOCUMENTATION
        ).setStatus(WorkflowStatus.COMPLETED);

        orchestrator.approveRelease(
                context,
                "Human Reviewer"
        );

        StageExecutionResult result =
                orchestrator.executeStage(
                        graph,
                        context,
                        WorkflowStage.RELEASE_READINESS
                );

        assertTrue(result.isSuccess());

        assertTrue(
                orchestrator.isReleaseApproved(context)
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.RELEASE_READINESS
                ).getStatus()
        );

        assertEquals(
                1,
                context.getMetrics().getSuccessfulStages()
        );
    }

    @Test
    void shouldReplanDownstreamStagesWhenArchitectureChanges() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        // Simulate a previously completed workflow.
        for (WorkflowTask task : graph.getTasks().values()) {
            task.setStatus(WorkflowStatus.COMPLETED);
        }

        // Simulate previous human approval.
        orchestrator.approveRelease(
                context,
                "Human Reviewer"
        );

        assertTrue(
                orchestrator.isReleaseApproved(context)
        );

        // Architecture changes after downstream work was completed.
        orchestrator.replanFromStage(
                graph,
                context,
                WorkflowStage.ARCHITECTURE_DESIGN,
                "Architecture updated to support a new reliability requirement"
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.ARCHITECTURE_DESIGN
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.IMPLEMENTATION
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.TESTING
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.DOCUMENTATION
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.RELEASE_READINESS
                ).getStatus()
        );

        assertFalse(
                orchestrator.isReleaseApproved(context)
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "Dynamic replanning triggered"
                                )
                        )
        );
    }

    @Test
    void shouldApplyFallbackAndRecordDecision() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        // IMPLEMENTATION depends on ARCHITECTURE_DESIGN.
        // Complete the dependency before applying fallback.
        graph.getTask(
                WorkflowStage.ARCHITECTURE_DESIGN
        ).setStatus(
                WorkflowStatus.COMPLETED
        );

        orchestrator.applyFallback(
                graph,
                context,
                WorkflowStage.IMPLEMENTATION,
                "Use the existing stable implementation"
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.IMPLEMENTATION
                ).getStatus()
        );

        assertEquals(
                "Use the existing stable implementation",
                context.getStageOutput(
                        "IMPLEMENTATION_fallback"
                )
        );

        assertEquals(
                1,
                context.getMetrics().getSuccessfulStages()
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "Fallback applied for IMPLEMENTATION"
                                )
                        )
        );
    }

    @Test
    void shouldRollbackStageAndRecordMetric() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        graph.getTask(
                WorkflowStage.IMPLEMENTATION
        ).setStatus(WorkflowStatus.COMPLETED);

        orchestrator.rollbackStage(
                graph,
                context,
                WorkflowStage.IMPLEMENTATION
        );

        assertEquals(
                WorkflowStatus.ROLLED_BACK,
                graph.getTask(
                        WorkflowStage.IMPLEMENTATION
                ).getStatus()
        );

        assertEquals(
                1,
                context.getMetrics().getRollbackCount()
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "IMPLEMENTATION rolled back"
                                )
                        )
        );
    }

    @Test
void shouldSafeStopStageAndRecordReason() {

    PolicyGuardrail policyGuardrail =
            new PolicyGuardrail();

    WorkflowOrchestrator orchestrator =
            new WorkflowOrchestrator(
                    List.of(),
                    policyGuardrail
            );

    WorkflowGraph graph =
            new WorkflowGraph();

    WorkflowContext context =
            orchestrator.createWorkflow(
                    "Build a URL shortener service"
            );

    orchestrator.safeStopStage(
            graph,
            context,
            WorkflowStage.IMPLEMENTATION,
            "Security validation failed"
    );

    assertEquals(
            WorkflowStatus.SAFE_STOPPED,
            graph.getTask(
                    WorkflowStage.IMPLEMENTATION
            ).getStatus()
    );

    // Safe-stop must be reflected in failure metrics.
    assertEquals(
            1,
            context.getMetrics().getFailedStages()
    );

    // Reason must remain visible in the audit/decision trail.
    assertTrue(
            context.getDecisionLog().stream()
                    .anyMatch(decision ->
                            decision.contains(
                                    "Security validation failed"
                            )
                    )
    );
}
    @Test
    void shouldBoundRetriesAndEventuallySafeStop() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        StageExecutor failingExecutor =
                new StageExecutor() {

                    @Override
                    public WorkflowStage getStage() {
                        return WorkflowStage.REQUIREMENT_ANALYSIS;
                    }

                    @Override
                    public StageExecutionResult execute(
                            WorkflowContext context) {

                        return StageExecutionResult.failure(
                                "Simulated agent failure"
                        );
                    }
                };

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(failingExecutor),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        // First failure -> retry 1
        orchestrator.executeStage(
                graph,
                context,
                WorkflowStage.REQUIREMENT_ANALYSIS
        );

        // Second failure -> retry 2
        orchestrator.executeStage(
                graph,
                context,
                WorkflowStage.REQUIREMENT_ANALYSIS
        );

        // Third failure -> retry limit reached -> safe stop
        orchestrator.executeStage(
                graph,
                context,
                WorkflowStage.REQUIREMENT_ANALYSIS
        );

        WorkflowTask task =
                graph.getTask(
                        WorkflowStage.REQUIREMENT_ANALYSIS
                );

        assertEquals(
                2,
                task.getRetryCount()
        );

        assertEquals(
                WorkflowStatus.SAFE_STOPPED,
                task.getStatus()
        );

        assertEquals(
                2,
                context.getMetrics().getRetryCount()
        );

        assertEquals(
                3,
                context.getMetrics().getFailedStages()
        );
    }

    @Test
    void shouldCreateStructuredAuditEventForDecision() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        policyGuardrail
                );

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        context.addDecision(
                "Architecture decision recorded"
        );

        assertEquals(
                1,
                context.getAuditEvents().size()
        );

        AuditEvent event =
                context.getAuditEvents().get(0);

        assertEquals(
                context.getWorkflowId(),
                event.getWorkflowId()
        );

        assertEquals(
                "Architecture decision recorded",
                event.getEvent()
        );

        assertNotNull(
                event.getTimestamp()
        );
    }

    @Test
    void shouldExecuteTestingAndDocumentationInParallel() {

        PolicyGuardrail policyGuardrail =
                new PolicyGuardrail();

        StageExecutor testingExecutor =
                new TestingExecutor();

        StageExecutor documentationExecutor =
                new DocumentationExecutor();

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(
                                testingExecutor,
                                documentationExecutor
                        ),
                        policyGuardrail
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        graph.getTask(
                WorkflowStage.IMPLEMENTATION
        ).setStatus(
                WorkflowStatus.COMPLETED
        );

        context.addStageOutput(
                "implementation",
                "URL shortener implementation completed"
        );

        orchestrator.executeParallelValidationStages(
                graph,
                context
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.TESTING
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.DOCUMENTATION
                ).getStatus()
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "parallel branches synchronized"
                                )
                        )
        );
    }

    @Test
    void shouldRemoveStaleOutputsWhenArchitectureChanges() {

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        new PolicyGuardrail()
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        context.addStageOutput(
                "architecture",
                "Old architecture"
        );

        context.addStageOutput(
                "implementation",
                "Old implementation"
        );

        context.addStageOutput(
                "testing",
                "Old testing result"
        );

        context.addStageOutput(
                "documentation",
                "Old documentation"
        );

        context.addStageOutput(
                "releaseReadiness",
                "Old release result"
        );

        context.getApprovalGate().approve(
                "Engineering Manager"
        );

        orchestrator.replanFromStage(
                graph,
                context,
                WorkflowStage.ARCHITECTURE_DESIGN,
                "Architecture requirements changed"
        );

        assertNull(
                context.getStageOutput("architecture")
        );

        assertNull(
                context.getStageOutput("implementation")
        );

        assertNull(
                context.getStageOutput("testing")
        );

        assertNull(
                context.getStageOutput("documentation")
        );

        assertNull(
                context.getStageOutput("releaseReadiness")
        );

        assertFalse(
                context.getApprovalGate().isApproved()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.ARCHITECTURE_DESIGN
                ).getStatus()
        );
    }

    @Test
    void shouldExecuteCompleteWorkflowAfterHumanApproval() {

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(
                                new RequirementAnalysisExecutor(),
                                new ArchitectureDesignExecutor(),
                                new ImplementationExecutor(),
                                new TestingExecutor(),
                                new DocumentationExecutor(),
                                new ReleaseReadinessExecutor()
                        ),
                        new PolicyGuardrail()
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        orchestrator.approveRelease(
                context,
                "Engineering Manager"
        );

        StageExecutionResult result =
                orchestrator.executeFullWorkflow(
                        graph,
                        context
                );

        assertTrue(result.isSuccess());

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.REQUIREMENT_ANALYSIS
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.ARCHITECTURE_DESIGN
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.IMPLEMENTATION
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.TESTING
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.DOCUMENTATION
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.COMPLETED,
                graph.getTask(
                        WorkflowStage.RELEASE_READINESS
                ).getStatus()
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "parallel branches synchronized"
                                )
                        )
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "Release approved by Engineering Manager"
                                )
                        )
        );
    }

    @Test
    void shouldInvalidateDownstreamOutputsWhenImplementationRollsBack() {

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        new PolicyGuardrail()
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        graph.getTask(
                WorkflowStage.IMPLEMENTATION
        ).setStatus(WorkflowStatus.COMPLETED);

        graph.getTask(
                WorkflowStage.TESTING
        ).setStatus(WorkflowStatus.COMPLETED);

        graph.getTask(
                WorkflowStage.DOCUMENTATION
        ).setStatus(WorkflowStatus.COMPLETED);

        graph.getTask(
                WorkflowStage.RELEASE_READINESS
        ).setStatus(WorkflowStatus.COMPLETED);

        context.addStageOutput(
                "implementation",
                "Old implementation"
        );

        context.addStageOutput(
                "testing",
                "Old testing"
        );

        context.addStageOutput(
                "documentation",
                "Old documentation"
        );

        context.addStageOutput(
                "releaseReadiness",
                "Old release result"
        );

        orchestrator.approveRelease(
                context,
                "Engineering Manager"
        );

        assertTrue(
                context.getApprovalGate().isApproved()
        );

        orchestrator.rollbackStage(
                graph,
                context,
                WorkflowStage.IMPLEMENTATION
        );

        assertEquals(
                WorkflowStatus.ROLLED_BACK,
                graph.getTask(
                        WorkflowStage.IMPLEMENTATION
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.TESTING
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.DOCUMENTATION
                ).getStatus()
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.RELEASE_READINESS
                ).getStatus()
        );

        assertNull(
                context.getStageOutput("implementation")
        );

        assertNull(
                context.getStageOutput("testing")
        );

        assertNull(
                context.getStageOutput("documentation")
        );

        assertNull(
                context.getStageOutput("releaseReadiness")
        );

        assertFalse(
                context.getApprovalGate().isApproved()
        );

        assertEquals(
                1,
                context.getMetrics().getRollbackCount()
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "Downstream stages and stale outputs invalidated"
                                )
                        )
        );
    }

    @Test
    void shouldBlockFallbackWhenDependenciesAreIncomplete() {

        WorkflowOrchestrator orchestrator =
                new WorkflowOrchestrator(
                        List.of(),
                        new PolicyGuardrail()
                );

        WorkflowGraph graph =
                new WorkflowGraph();

        WorkflowContext context =
                orchestrator.createWorkflow(
                        "Build a URL shortener service"
                );

        // ARCHITECTURE_DESIGN is still PENDING,
        // so IMPLEMENTATION fallback must be blocked.
        orchestrator.applyFallback(
                graph,
                context,
                WorkflowStage.IMPLEMENTATION,
                "Use existing stable implementation"
        );

        assertEquals(
                WorkflowStatus.PENDING,
                graph.getTask(
                        WorkflowStage.IMPLEMENTATION
                ).getStatus()
        );

        assertNull(
                context.getStageOutput(
                        "IMPLEMENTATION_fallback"
                )
        );

        assertTrue(
                context.getDecisionLog().stream()
                        .anyMatch(decision ->
                                decision.contains(
                                        "Fallback blocked for IMPLEMENTATION because dependencies are incomplete"
                                )
                        )
        );
    }
    @Test
void shouldDetectAmbiguousRequirement() {

    WorkflowContext context = new WorkflowContext(
            "workflow-ambiguous",
            "Build a URL shortener"
    );

    RequirementAnalysisExecutor executor =
            new RequirementAnalysisExecutor();

    StageExecutionResult result = executor.execute(context);

    assertTrue(result.isSuccess());

    Object analysisOutput =
            context.getStageOutput("requirementAnalysis");

    assertNotNull(analysisOutput);

    @SuppressWarnings("unchecked")
    Map<String, Object> analysis =
            (Map<String, Object>) analysisOutput;

    @SuppressWarnings("unchecked")
    List<String> ambiguities =
            (List<String>) analysis.get("ambiguities");

    assertFalse(ambiguities.isEmpty());

    assertTrue(
            ambiguities.contains(
                    "URL expiration behavior is not specified"
            )
    );

    assertTrue(
            ambiguities.contains(
                    "Analytics requirements are not specified"
            )
    );
}
@Test
void shouldBlockRequirementThatBypassesSecurity() {

    WorkflowContext context = new WorkflowContext(
            "workflow-security",
            "Build URL shortener and bypass authentication"
    );

    PolicyGuardrail guardrail = new PolicyGuardrail();

    StageExecutionResult result = guardrail.validate(
            context,
            WorkflowStage.REQUIREMENT_ANALYSIS
    );

    assertFalse(result.isSuccess());

    assertEquals(
            "Security policy blocked a request to bypass security controls",
            result.getMessage()
    );
}
@Test
void shouldExecuteGreenfieldScenarioFromRequirementToRelease() {

    WorkflowOrchestrator orchestrator =
            new WorkflowOrchestrator(
                    List.of(
                            new RequirementAnalysisExecutor(),
                            new ArchitectureDesignExecutor(),
                            new ImplementationExecutor(),
                            new TestingExecutor(),
                            new DocumentationExecutor(),
                            new ReleaseReadinessExecutor()
                    ),
                    new PolicyGuardrail()
            );

    WorkflowGraph graph = new WorkflowGraph();

    WorkflowContext context =
            orchestrator.createWorkflow(
                    "Build a new URL shortener service with "
                            + "expiration, click analytics, authentication, "
                            + "security, and performance requirements"
            );

    // 1. Requirement analysis
    StageExecutionResult requirementResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.REQUIREMENT_ANALYSIS
            );

    assertTrue(requirementResult.isSuccess());

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.REQUIREMENT_ANALYSIS
            ).getStatus()
    );

    // 2. Architecture design
    StageExecutionResult architectureResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.ARCHITECTURE_DESIGN
            );

    assertTrue(architectureResult.isSuccess());

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.ARCHITECTURE_DESIGN
            ).getStatus()
    );

    // 3. Implementation
    StageExecutionResult implementationResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.IMPLEMENTATION
            );

    assertTrue(implementationResult.isSuccess());

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.IMPLEMENTATION
            ).getStatus()
    );

    // 4. Testing and documentation run as parallel branches.
    orchestrator.executeParallelValidationStages(
            graph,
            context
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.TESTING
            ).getStatus()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.DOCUMENTATION
            ).getStatus()
    );

    // 5. Release must stop for human approval.
    StageExecutionResult beforeApproval =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.RELEASE_READINESS
            );

    assertFalse(beforeApproval.isSuccess());

    assertEquals(
            WorkflowStatus.WAITING_FOR_APPROVAL,
            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).getStatus()
    );

    // 6. Human reviewer approves the release.
    orchestrator.approveRelease(
            context,
            "Engineering Manager"
    );

    assertTrue(
            context.getApprovalGate().isApproved()
    );

    // 7. Continue release after approval.
    StageExecutionResult releaseResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.RELEASE_READINESS
            );

    assertTrue(releaseResult.isSuccess());

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).getStatus()
    );

    // 8. Validate engineering outputs.
    assertNotNull(
            context.getStageOutput("requirementAnalysis")
    );

    assertNotNull(
            context.getStageOutput("architecture")
    );

    assertNotNull(
            context.getStageOutput("implementation")
    );

    assertNotNull(
            context.getStageOutput("testing")
    );

    assertNotNull(
            context.getStageOutput("documentation")
    );

    assertNotNull(
            context.getStageOutput("releaseReadiness")
    );

    // 9. Validate decision lineage and audit evidence.
    assertFalse(
            context.getDecisionLog().isEmpty()
    );

    assertFalse(
            context.getAuditEvents().isEmpty()
    );

    assertTrue(
            context.getDecisionLog().stream()
                    .anyMatch(decision ->
                            decision.contains(
                                    "Release approved by Engineering Manager"
                            )
                    )
    );
}
@Test
void shouldExecuteBrownfieldScenarioWithImpactAnalysisAndReplanning() {

    WorkflowOrchestrator orchestrator =
            new WorkflowOrchestrator(
                    List.of(
                            new RequirementAnalysisExecutor(),
                            new ArchitectureDesignExecutor(),
                            new ImplementationExecutor(),
                            new TestingExecutor(),
                            new DocumentationExecutor(),
                            new ReleaseReadinessExecutor()
                    ),
                    new PolicyGuardrail()
            );

    WorkflowGraph graph = new WorkflowGraph();

    WorkflowContext context =
            orchestrator.createWorkflow(
                    "Enhance the existing URL shortener service "
                            + "with expiration, click analytics, authentication, "
                            + "security, and performance requirements"
            );

    // Simulate an existing completed application.
    graph.getTask(
            WorkflowStage.REQUIREMENT_ANALYSIS
    ).setStatus(WorkflowStatus.COMPLETED);

    graph.getTask(
            WorkflowStage.ARCHITECTURE_DESIGN
    ).setStatus(WorkflowStatus.COMPLETED);

    graph.getTask(
            WorkflowStage.IMPLEMENTATION
    ).setStatus(WorkflowStatus.COMPLETED);

    graph.getTask(
            WorkflowStage.TESTING
    ).setStatus(WorkflowStatus.COMPLETED);

    graph.getTask(
            WorkflowStage.DOCUMENTATION
    ).setStatus(WorkflowStatus.COMPLETED);

    graph.getTask(
            WorkflowStage.RELEASE_READINESS
    ).setStatus(WorkflowStatus.COMPLETED);

    // Preserve requirement-analysis output from the existing system.
    context.addStageOutput(
            "normalizedRequirement",
            context.getRequirement()
    );

    context.addStageOutput(
            "architecture",
            "Existing URL shortener architecture"
    );

    context.addStageOutput(
            "implementation",
            "Existing URL shortener implementation"
    );

    context.addStageOutput(
            "testing",
            "Existing tests passed"
    );

    context.addStageOutput(
            "documentation",
            "Existing documentation"
    );

    context.addStageOutput(
            "releaseReadiness",
            "Existing release approved"
    );

    // Simulate previous human approval.
    orchestrator.approveRelease(
            context,
            "Engineering Manager"
    );

    assertTrue(
            context.getApprovalGate().isApproved()
    );

    // Brownfield architecture change impacts downstream work.
    orchestrator.replanFromStage(
            graph,
            context,
            WorkflowStage.ARCHITECTURE_DESIGN,
            "Existing architecture must change to support "
                    + "new expiration and analytics requirements"
    );

    // Architecture and all dependent stages must be replanned.
    assertEquals(
            WorkflowStatus.PENDING,
            graph.getTask(
                    WorkflowStage.ARCHITECTURE_DESIGN
            ).getStatus()
    );

    assertEquals(
            WorkflowStatus.PENDING,
            graph.getTask(
                    WorkflowStage.IMPLEMENTATION
            ).getStatus()
    );

    assertEquals(
            WorkflowStatus.PENDING,
            graph.getTask(
                    WorkflowStage.TESTING
            ).getStatus()
    );

    assertEquals(
            WorkflowStatus.PENDING,
            graph.getTask(
                    WorkflowStage.DOCUMENTATION
            ).getStatus()
    );

    assertEquals(
            WorkflowStatus.PENDING,
            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).getStatus()
    );

    // Stale downstream outputs must be removed.
    assertNull(
            context.getStageOutput("architecture")
    );

    assertNull(
            context.getStageOutput("implementation")
    );

    assertNull(
            context.getStageOutput("testing")
    );

    assertNull(
            context.getStageOutput("documentation")
    );

    assertNull(
            context.getStageOutput("releaseReadiness")
    );

    // Requirement analysis remains valid.
    assertNotNull(
            context.getStageOutput("normalizedRequirement")
    );

    // Previous release approval must be invalidated.
    assertFalse(
            context.getApprovalGate().isApproved()
    );

    graph.getTask(
            WorkflowStage.REQUIREMENT_ANALYSIS
    ).setStatus(WorkflowStatus.COMPLETED);

    // Re-run impacted architecture.
    StageExecutionResult architectureResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.ARCHITECTURE_DESIGN
            );

    assertTrue(
            architectureResult.isSuccess()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.ARCHITECTURE_DESIGN
            ).getStatus()
    );

    // Re-run impacted implementation.
    StageExecutionResult implementationResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.IMPLEMENTATION
            );

    assertTrue(
            implementationResult.isSuccess()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.IMPLEMENTATION
            ).getStatus()
    );

    // Testing and documentation execute again after the change.
    orchestrator.executeParallelValidationStages(
            graph,
            context
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.TESTING
            ).getStatus()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.DOCUMENTATION
            ).getStatus()
    );

    assertNotNull(
            context.getStageOutput("testing")
    );

    assertNotNull(
            context.getStageOutput("documentation")
    );

    // Changed brownfield system requires fresh human approval.
    StageExecutionResult beforeApproval =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.RELEASE_READINESS
            );

    assertFalse(
            beforeApproval.isSuccess()
    );

    assertEquals(
            WorkflowStatus.WAITING_FOR_APPROVAL,
            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).getStatus()
    );

    // Human approves the modified system.
    orchestrator.approveRelease(
            context,
            "Engineering Manager"
    );

    assertTrue(
            context.getApprovalGate().isApproved()
    );

    // Release continues after fresh approval.
    StageExecutionResult releaseResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.RELEASE_READINESS
            );

    assertTrue(
            releaseResult.isSuccess()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).getStatus()
    );

    assertNotNull(
            context.getStageOutput("releaseReadiness")
    );

    // Verify dynamic replanning decision lineage.
    assertTrue(
            context.getDecisionLog().stream()
                    .anyMatch(decision ->
                            decision.contains(
                                    "Dynamic replanning triggered"
                            )
                    )
    );

    // Verify human approval is recorded.
    assertTrue(
            context.getDecisionLog().stream()
                    .anyMatch(decision ->
                            decision.contains(
                                    "Release approved by Engineering Manager"
                            )
                    )
    );

    // Verify audit evidence exists.
    assertFalse(
            context.getDecisionLog().isEmpty()
    );

    assertFalse(
            context.getAuditEvents().isEmpty()
    );
}
@Test
void shouldExecuteAmbiguousRequirementScenarioWithControlledAssumptions() {

    WorkflowOrchestrator orchestrator =
            new WorkflowOrchestrator(
                    List.of(
                            new RequirementAnalysisExecutor(),
                            new ArchitectureDesignExecutor(),
                            new ImplementationExecutor(),
                            new TestingExecutor(),
                            new DocumentationExecutor(),
                            new ReleaseReadinessExecutor()
                    ),
                    new PolicyGuardrail()
            );

    WorkflowGraph graph = new WorkflowGraph();

    // Intentionally ambiguous requirement.
    WorkflowContext context =
            orchestrator.createWorkflow(
                    "Build a URL shortener"
            );

    // 1. Analyze the ambiguous requirement.
    StageExecutionResult requirementResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.REQUIREMENT_ANALYSIS
            );

    assertTrue(
            requirementResult.isSuccess()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.REQUIREMENT_ANALYSIS
            ).getStatus()
    );

    // 2. Validate structured requirement analysis.
    Object analysisOutput =
            context.getStageOutput(
                    "requirementAnalysis"
            );

    assertNotNull(analysisOutput);

    @SuppressWarnings("unchecked")
    Map<String, Object> analysis =
            (Map<String, Object>) analysisOutput;

    @SuppressWarnings("unchecked")
    List<String> ambiguities =
            (List<String>) analysis.get(
                    "ambiguities"
            );

    @SuppressWarnings("unchecked")
    List<String> assumptions =
            (List<String>) analysis.get(
                    "assumptions"
            );

    @SuppressWarnings("unchecked")
    List<String> acceptanceCriteria =
            (List<String>) analysis.get(
                    "acceptanceCriteria"
            );

    @SuppressWarnings("unchecked")
    List<String> tasks =
            (List<String>) analysis.get(
                    "tasks"
            );

    // 3. Ambiguities must be explicitly identified.
    assertFalse(
            ambiguities.isEmpty()
    );

    assertTrue(
            ambiguities.contains(
                    "URL expiration behavior is not specified"
            )
    );

    assertTrue(
            ambiguities.contains(
                    "Analytics requirements are not specified"
            )
    );

    assertTrue(
            ambiguities.contains(
                    "Authentication and security requirements are not specified"
            )
    );

    assertTrue(
            ambiguities.contains(
                    "Scale and performance expectations are not specified"
            )
    );

    // 4. Controlled prototype assumptions must be recorded.
    assertNotNull(assumptions);

    assertFalse(
            assumptions.isEmpty()
    );

    assertTrue(
            assumptions.stream()
                    .anyMatch(assumption ->
                            assumption.contains(
                                    "safe prototype defaults"
                            )
                    )
    );

    // 5. Requirement must still be decomposed into engineering work.
    assertNotNull(acceptanceCriteria);

    assertFalse(
            acceptanceCriteria.isEmpty()
    );

    assertNotNull(tasks);

    assertFalse(
            tasks.isEmpty()
    );

    assertNotNull(
            context.getStageOutput(
                    "normalizedRequirement"
            )
    );

    // 6. Decision lineage must record the ambiguity.
    assertTrue(
            context.getDecisionLog().stream()
                    .anyMatch(decision ->
                            decision.contains(
                                    "ambiguities detected"
                            )
                    )
    );

    // 7. Continue through architecture using bounded assumptions.
    StageExecutionResult architectureResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.ARCHITECTURE_DESIGN
            );

    assertTrue(
            architectureResult.isSuccess()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.ARCHITECTURE_DESIGN
            ).getStatus()
    );

    // 8. Continue implementation.
    StageExecutionResult implementationResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.IMPLEMENTATION
            );

    assertTrue(
            implementationResult.isSuccess()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.IMPLEMENTATION
            ).getStatus()
    );

    // 9. Testing and documentation execute as parallel validation paths.
    orchestrator.executeParallelValidationStages(
            graph,
            context
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.TESTING
            ).getStatus()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.DOCUMENTATION
            ).getStatus()
    );

    assertNotNull(
            context.getStageOutput("testing")
    );

    assertNotNull(
            context.getStageOutput("documentation")
    );

    // 10. Controlled autonomy stops before release.
    StageExecutionResult beforeApproval =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.RELEASE_READINESS
            );

    assertFalse(
            beforeApproval.isSuccess()
    );

    assertEquals(
            WorkflowStatus.WAITING_FOR_APPROVAL,
            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).getStatus()
    );

    // 11. Human reviewer accepts the prototype assumptions.
    orchestrator.approveRelease(
            context,
            "Engineering Manager"
    );

    assertTrue(
            context.getApprovalGate().isApproved()
    );

    // 12. Release readiness continues only after approval.
    StageExecutionResult releaseResult =
            orchestrator.executeStage(
                    graph,
                    context,
                    WorkflowStage.RELEASE_READINESS
            );

    assertTrue(
            releaseResult.isSuccess()
    );

    assertEquals(
            WorkflowStatus.COMPLETED,
            graph.getTask(
                    WorkflowStage.RELEASE_READINESS
            ).getStatus()
    );

    // 13. Validate complete engineering outputs.
    assertNotNull(
            context.getStageOutput(
                    "requirementAnalysis"
            )
    );

    assertNotNull(
            context.getStageOutput(
                    "architecture"
            )
    );

    assertNotNull(
            context.getStageOutput(
                    "implementation"
            )
    );

    assertNotNull(
            context.getStageOutput(
                    "testing"
            )
    );

    assertNotNull(
            context.getStageOutput(
                    "documentation"
            )
    );

    assertNotNull(
            context.getStageOutput(
                    "releaseReadiness"
            )
    );

    // 14. Audit trail must preserve the decisions.
    assertFalse(
            context.getDecisionLog().isEmpty()
    );

    assertFalse(
            context.getAuditEvents().isEmpty()
    );

    assertTrue(
            context.getDecisionLog().stream()
                    .anyMatch(decision ->
                            decision.contains(
                                    "Release approved by Engineering Manager"
                            )
                    )
    );
}

}