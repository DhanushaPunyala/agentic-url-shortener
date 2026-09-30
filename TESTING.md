# Testing and Validation Approach

## 1. Purpose

The prototype uses automated testing to validate both:

1. The working URL Shortener application.
2. The Agentic SDLC orchestration and control model.

Testing focuses not only on successful execution, but also on dependency enforcement, human oversight, failure handling, recovery, policy controls, dynamic replanning, and traceability.

---

## 2. Test Technology

The project uses:

- JUnit 5
- Spring Boot Test
- Spring MockMvc
- Maven Surefire
- Maven Wrapper

Run the complete automated test suite with:

```powershell
.\mvnw.cmd test
```

Build and validate the complete project with:

```powershell
.\mvnw.cmd clean package
```

Successful validation finishes with:

```text
BUILD SUCCESS
```

---

## 3. URL Shortener Validation

The application tests validate the working Spring Boot functionality.

### URL Creation

Tests verify that a valid original URL can be submitted and converted into a shortened URL representation.

### Redirect

Tests verify that a valid short code resolves to the original URL.

### Unknown Short Code

Invalid or unknown short codes are handled through the application's custom not-found behavior.

### Expiration

Expired URLs are rejected rather than redirected.

### Click Analytics

Redirect activity updates click-count information.

Statistics retrieval does not itself increment the click count.

### Request Validation

Invalid request data is rejected using application validation rules.

---

## 4. Workflow Dependency Validation

The workflow graph defines dependencies between SDLC stages.

A stage cannot execute when its required upstream stage has not completed.

Example:

```text
ARCHITECTURE_DESIGN
        |
        v
IMPLEMENTATION
```

Attempting to execute `IMPLEMENTATION` before `ARCHITECTURE_DESIGN` completes is blocked.

This validates the workflow entry/dependency gate.

---

## 5. Human Approval Validation

Release readiness is protected by a human approval gate.

The test flow verifies:

```text
Testing Complete
      +
Documentation Complete
        |
        v
Release Readiness
        |
        v
WAITING_FOR_APPROVAL
```

The release stage does not complete until an authorized human approval action is recorded.

After approval:

```text
Human Approval
      |
      v
Release Readiness
      |
      v
COMPLETED
```

---

## 6. Parallel Execution Validation

Testing and documentation are designed as independent branches after implementation.

```text
        IMPLEMENTATION
             |
       +-----+-----+
       |           |
       v           v
    TESTING   DOCUMENTATION
       |           |
       +-----+-----+
             |
             v
      SYNCHRONIZATION
```

Automated testing verifies that both branches complete and the synchronization decision is recorded.

---

## 7. Retry Boundary Validation

The orchestration tests include a deliberately failing stage executor.

Repeated failures verify that:

- Failure is recorded.
- Retry count increases.
- Retry count cannot exceed the configured limit.
- The workflow eventually enters `SAFE_STOPPED`.

This demonstrates bounded retries rather than unlimited autonomous retry behavior.

---

## 8. Fallback Validation

Fallback testing verifies that a controlled fallback can be applied when required dependencies are complete.

The fallback output is stored in workflow context and the decision is recorded.

A separate test verifies that fallback execution is blocked when dependencies are incomplete.

---

## 9. Rollback Validation

Rollback tests verify that an affected stage can be rolled back safely.

The workflow validates that:

- The stage enters `ROLLED_BACK`.
- Rollback metrics are updated.
- Downstream stages are invalidated.
- Stale downstream outputs are removed.
- Existing release approval is revoked.
- Rollback decisions remain in the decision history.

---

## 10. Safe-Stop Validation

Safe-stop behavior is tested independently.

The test verifies:

- Stage status becomes `SAFE_STOPPED`.
- Failure metrics are updated.
- The safe-stop reason is retained in decision history.

This provides evidence that the orchestration layer can terminate unsafe execution in a controlled way.

---

## 11. Dynamic Replanning Validation

Dynamic replanning is validated when an upstream architecture decision changes.

The tests verify that affected downstream stages return to `PENDING`.

```text
Architecture Change
        |
        v
ARCHITECTURE_DESIGN
        |
        v
IMPLEMENTATION
        |
   +----+----+
   |         |
   v         v
TESTING   DOCUMENTATION
   |         |
   +----+----+
        |
        v
RELEASE_READINESS
```

Stale downstream outputs are removed.

Previous release approval is revoked.

The decision history records that dynamic replanning occurred.

---

## 12. Policy Guardrail Validation

Security-policy testing verifies that explicitly unsafe requirements are blocked.

Example:

```text
Build URL shortener and bypass authentication
```

The policy guardrail rejects this request rather than allowing normal workflow execution.

Release-related guardrails also require testing and documentation outputs before release processing.

---

## 13. Audit Validation

Decision recording creates structured audit events.

Tests verify that an audit event contains:

- Workflow ID
- Event description
- Timestamp

This provides basic traceability between workflow execution and engineering decisions.

---

## 14. Metrics Validation

Workflow responses expose operational metrics including:

- Success rate
- Retry count
- Rollback count
- Mean time to recovery
- End-to-end latency

Controller-level testing verifies that these metric fields are exposed through the workflow REST API.

---

# 15. Required Scenario Validation

## Greenfield Scenario

The Greenfield test validates the complete lifecycle of a new requirement.

It verifies:

- Requirement analysis
- Architecture
- Implementation
- Parallel testing/documentation
- Synchronization
- Human approval
- Release readiness
- Stage outputs
- Decision history
- Audit evidence

Test:

```text
shouldExecuteGreenfieldScenarioFromRequirementToRelease
```

---

## Brownfield Scenario

The Brownfield test starts from an existing completed system and introduces an architecture change.

It verifies:

- Impacted downstream stages are invalidated.
- Stale outputs are removed.
- Valid upstream requirement context is retained.
- Existing approval is revoked.
- Architecture and implementation execute again.
- Testing and documentation execute again.
- Fresh human approval is required.
- Release readiness completes.
- Dynamic replanning is recorded.

Test:

```text
shouldExecuteBrownfieldScenarioWithImpactAnalysisAndReplanning
```

---

## Ambiguous Requirement Scenario

The ambiguous scenario starts with:

```text
Build a URL shortener
```

It verifies that the system identifies missing information including:

- Expiration behavior
- Analytics
- Authentication/security
- Scale/performance

It also validates:

- Safe assumptions
- Acceptance criteria
- Engineering task decomposition
- Architecture execution
- Implementation
- Testing
- Documentation
- Human approval
- Release readiness
- Audit evidence

Test:

```text
shouldExecuteAmbiguousRequirementScenarioWithControlledAssumptions
```

---

## 16. Workflow REST API Validation

Controller tests validate workflow interaction through REST APIs.

Covered operations include:

```text
POST /api/workflows
GET  /api/workflows/{workflowId}
POST /api/workflows/{workflowId}/execute
POST /api/workflows/{workflowId}/approve
```

Tests verify:

- Workflow creation
- Input validation
- Workflow retrieval
- Execution until approval
- Approval through API
- Completion after approval
- Metrics exposure
- Unknown workflow handling

---

## 17. Manual End-to-End Validation

The workflow REST API can also be demonstrated manually.

### Create Workflow

```powershell
$body = @{
    requirement = "Build a URL shortener service with expiration and click analytics"
} | ConvertTo-Json

$workflow = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/workflows" `
    -ContentType "application/json" `
    -Body $body

$workflow
```

Save the returned workflow ID:

```powershell
$id = $workflow.workflowId
```

### Execute

```powershell
Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/workflows/$id/execute"
```

Expected behavior:

```text
REQUIREMENT_ANALYSIS -> COMPLETED
ARCHITECTURE_DESIGN  -> COMPLETED
IMPLEMENTATION       -> COMPLETED
TESTING              -> COMPLETED
DOCUMENTATION        -> COMPLETED
RELEASE_READINESS    -> WAITING_FOR_APPROVAL
```

### Approve

```powershell
$approval = @{
    approvedBy = "Engineering Manager"
} | ConvertTo-Json

Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/workflows/$id/approve" `
    -ContentType "application/json" `
    -Body $approval
```

### Continue Execution

```powershell
Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/workflows/$id/execute"
```

Expected final state:

```text
RELEASE_READINESS -> COMPLETED
```

### Inspect Workflow

```powershell
Invoke-RestMethod `
    -Method Get `
    -Uri "http://localhost:8080/api/workflows/$id"
```

This returns workflow status, outputs, decisions, approval information, and metrics.

---

## 18. Test Evidence Location

Primary orchestration tests are located at:

```text
src/test/java/com/assessment/urlshortener/orchestration/
WorkflowOrchestratorTest.java
```

Workflow REST API tests are located at:

```text
src/test/java/com/assessment/urlshortener/orchestration/
WorkflowControllerTest.java
```

URL Shortener service and controller tests are located under:

```text
src/test/java/com/assessment/urlshortener/
```

---

## 19. Prototype Testing Limitation

`TestingExecutor` represents the testing stage inside the orchestration model.

It does not directly launch Maven.

The actual executable validation is performed through:

```powershell
.\mvnw.cmd test
```

This distinction is intentional and documented so the prototype does not claim that the orchestration testing stage itself executes the Maven test suite.

---

## 20. Validation Summary

The testing approach demonstrates both positive and negative workflow behavior.

The prototype validates:

- Successful application behavior
- Successful SDLC execution
- Invalid dependency execution
- Missing human approval
- Failure and retry limits
- Fallback controls
- Rollback behavior
- Safe stopping
- Security-policy rejection
- Dynamic replanning
- Stale-output invalidation
- Parallel execution and synchronization
- Auditability
- Metrics
- Greenfield execution
- Brownfield modification
- Ambiguous-requirement handling

Together, these tests provide executable evidence that the prototype performs controlled multi-stage SDLC orchestration rather than only demonstrating a successful happy path.