# Agentic URL Shortener – End-to-End SDLC Orchestration Prototype

## 1. Overview

This project demonstrates an agentic software engineering workflow that transforms a software requirement into a reviewable engineering outcome through controlled, multi-stage SDLC orchestration.

The working application is a URL Shortener built using Java and Spring Boot. In addition to the application itself, the project contains an orchestration layer responsible for:

- Requirement understanding and decomposition
- Architecture design
- Implementation planning
- Testing
- Documentation
- Release readiness
- Human approval gates
- Dependency management
- Parallel execution and synchronization
- Bounded retries
- Fallback handling
- Rollback
- Safe-stop behavior
- Dynamic replanning
- Policy guardrails
- Decision lineage and audit events
- Workflow metrics

The prototype demonstrates controlled autonomy: automated stages can perform engineering work, while high-impact release activity remains subject to human approval.

---

## 2. Technology Stack

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Jakarta Validation
- H2 Database
- Spring Boot Actuator
- JUnit 5
- Spring MockMvc
- Maven
- REST APIs

---

## 3. Application Functionality

The URL Shortener supports:

- Creating shortened URLs
- Redirecting short codes to original URLs
- URL expiration
- Click analytics
- URL statistics
- Request validation
- Custom 404 handling for unknown short codes
- Custom 410 handling for expired URLs

The application uses an in-memory H2 database for prototype execution.

---

## 4. Agentic SDLC Architecture

The orchestration workflow is modeled as an explicit dependency graph.

```text
Requirement
    |
    v
+----------------------+
| Requirement Analysis |
+----------------------+
    |
    v
+----------------------+
| Architecture Design  |
+----------------------+
    |
    v
+----------------------+
|    Implementation    |
+----------------------+
    |
    +-----------------------+
    |                       |
    v                       v
+-----------+       +----------------+
|  Testing  |       | Documentation  |
+-----------+       +----------------+
    |                       |
    +-----------+-----------+
                |
                v
        Synchronization Gate
                |
                v
        Human Approval Gate
                |
                v
+----------------------+
|  Release Readiness   |
+----------------------+
```

Testing and documentation execute as parallel validation branches after implementation. Both branches must synchronize before release readiness.

Release readiness is protected by a human approval gate.

---

## 5. Core Orchestration Components

### WorkflowGraph

Defines SDLC stages and their dependencies.

Stages:

1. REQUIREMENT_ANALYSIS
2. ARCHITECTURE_DESIGN
3. IMPLEMENTATION
4. TESTING
5. DOCUMENTATION
6. RELEASE_READINESS

### WorkflowOrchestrator

Coordinates stage execution and controls:

- Dependency checks
- Stage execution
- Parallel validation
- Human approvals
- Retry boundaries
- Fallbacks
- Rollbacks
- Safe stops
- Dynamic replanning
- Decision recording

### WorkflowContext

Maintains cross-stage context including:

- Original requirement
- Stage outputs
- Decision history
- Audit events
- Metrics
- Approval state

This allows information and decisions to remain available throughout the workflow.

### StageExecutor

Each SDLC stage is represented by an independent executor.

Implemented executors include:

- RequirementAnalysisExecutor
- ArchitectureDesignExecutor
- ImplementationExecutor
- TestingExecutor
- DocumentationExecutor
- ReleaseReadinessExecutor

This keeps the orchestration architecture modular and testable.

---

## 6. Requirement Understanding and Decomposition

The Requirement Analysis stage converts a raw requirement into structured engineering information.

It produces:

- Normalized requirement
- Identified ambiguities
- Assumptions
- Acceptance criteria
- Engineering tasks

For example, an underspecified requirement such as:

```text
Build a URL shortener
```

is analyzed for missing information such as:

- URL expiration behavior
- Analytics requirements
- Authentication/security requirements
- Scale/performance expectations

Safe prototype assumptions are recorded when ambiguity exists.

---

## 7. Controlled Autonomy

The prototype separates automated execution from human-controlled decisions.

Automated stages can:

- Analyze requirements
- Produce architecture outputs
- Produce implementation outputs
- Execute validation stages
- Generate documentation
- Detect failures
- Trigger bounded retries
- Apply controlled fallbacks
- Replan affected stages

The system does not autonomously complete the release process without human approval.

Before release readiness, the workflow enters:

```text
WAITING_FOR_APPROVAL
```

A human reviewer must explicitly approve the workflow before execution can continue.

---

## 8. Entry Gates and Dependencies

Each workflow stage has dependencies.

For example:

```text
IMPLEMENTATION
depends on
ARCHITECTURE_DESIGN
```

If an upstream dependency is incomplete, execution is blocked.

This prevents downstream stages from running with invalid or incomplete context.

Testing and documentation require successful implementation.

Release readiness requires successful testing and documentation.

---

## 9. Parallel Execution and Synchronization

Testing and documentation are independent after implementation and therefore execute as parallel branches.

```text
                 IMPLEMENTATION
                      |
             +--------+--------+
             |                 |
             v                 v
          TESTING        DOCUMENTATION
             |                 |
             +--------+--------+
                      |
                SYNCHRONIZATION
                      |
                      v
              RELEASE READINESS
```

The workflow waits for both branches before continuing.

This demonstrates both sequential and parallel orchestration paths.

---

## 10. Human Approval Gate

Release readiness is treated as a high-impact action.

Without approval, execution stops with:

```text
WAITING_FOR_APPROVAL
```

After approval, the workflow records the reviewer and allows release-readiness processing to continue.

If upstream work changes through rollback or replanning, previous approval is revoked and fresh approval is required.

---

## 11. Retry Handling

Workflow stages use bounded retry counts.

When execution fails:

1. Failure is recorded.
2. Retry count is incremented.
3. Retry is allowed only while the configured retry limit has not been exhausted.
4. After the retry limit is reached, the workflow enters a safe-stop state.

This prevents uncontrolled retry loops.

---

## 12. Fallback Handling

The orchestrator supports controlled fallback behavior.

A fallback can only be applied when the required upstream dependencies are complete.

Fallback decisions are recorded in the workflow decision history for traceability.

---

## 13. Rollback

A completed stage can be rolled back when its output becomes invalid.

Rollback:

- Marks the affected stage as rolled back
- Invalidates downstream stages
- Removes stale downstream outputs
- Revokes previous release approval
- Records rollback metrics
- Records the decision in the audit trail

This prevents downstream engineering outputs from remaining valid after an upstream change.

---

## 14. Dynamic Replanning

When an upstream engineering decision changes, the workflow can dynamically replan from that stage.

For example:

```text
Architecture change
       |
       v
Architecture -> PENDING
Implementation -> PENDING
Testing -> PENDING
Documentation -> PENDING
Release Readiness -> PENDING
```

Stale outputs are removed and previous release approval is revoked.

Only the impacted portion of the workflow needs to be executed again.

---

## 15. Safe Stop

When continued autonomous execution would be unsafe or retry limits are exhausted, the workflow can enter:

```text
SAFE_STOPPED
```

The reason is recorded in the decision/audit history.

This provides a controlled failure mode rather than allowing uncontrolled execution.

---

## 16. Policy Guardrails

PolicyGuardrail validates workflow actions before execution.

Current prototype guardrails include:

- Empty requirement rejection
- Blocking explicit security-control bypass requests
- Requiring testing before release
- Requiring documentation before release

For example, requirements requesting actions such as:

```text
bypass authentication
```

are blocked.

Human approval is additionally enforced by the orchestrator before release readiness.

---

## 17. Auditability and Decision Lineage

Workflow decisions are recorded in the WorkflowContext.

AuditEvent records include:

- Workflow ID
- Timestamp
- Event description

Examples of recorded events include:

- Stage completion
- Retry decisions
- Fallback decisions
- Rollbacks
- Safe stops
- Dynamic replanning
- Parallel branch synchronization
- Human release approval

This creates traceable decision lineage across the workflow.

---

## 18. Workflow Metrics

The prototype captures operational metrics including:

- Successful stages
- Failed stages
- Success rate
- Retry count
- Rollback count
- Mean time to recovery
- End-to-end workflow latency

Metrics are exposed through the workflow API response.

---

# 19. Required Demonstration Scenarios

## Scenario 1 – Greenfield Development

### Requirement

Build a new URL shortener service with expiration, click analytics, authentication, security, and performance requirements.

### Decomposition

The workflow decomposes the requirement into:

1. Requirement analysis
2. Architecture design
3. Implementation
4. Testing
5. Documentation
6. Release readiness

### Orchestration

The stages execute according to their dependencies.

Testing and documentation execute as parallel branches and synchronize before release.

### Validation

The scenario verifies:

- Requirement analysis completes
- Architecture is generated
- Implementation completes
- Testing completes
- Documentation completes
- Release is blocked before human approval
- Human approval is recorded
- Release readiness completes
- Decision and audit evidence exists

---

## Scenario 2 – Brownfield Change

### Requirement

Enhance an existing URL shortener with expiration, click analytics, authentication, security, and performance requirements.

### Existing State

The scenario begins with an already completed application and previously approved release.

### Impact Analysis

An architecture change is introduced to support the new requirements.

The orchestrator determines that downstream outputs are no longer valid.

### Dynamic Replanning

The following stages are invalidated:

```text
ARCHITECTURE_DESIGN
IMPLEMENTATION
TESTING
DOCUMENTATION
RELEASE_READINESS
```

Existing requirement-analysis context remains valid.

Previous release approval is revoked.

### Validation

The scenario verifies:

- Stale outputs are removed
- Impacted stages return to PENDING
- Architecture is executed again
- Implementation is executed again
- Testing and documentation run again
- Previous approval is invalidated
- Fresh human approval is required
- Release readiness completes after approval
- Replanning is recorded in the decision/audit trail

---

## Scenario 3 – Ambiguous Requirement

### Requirement

```text
Build a URL shortener
```

This requirement intentionally omits several engineering details.

### Requirement Analysis

The workflow identifies missing information including:

- Expiration behavior
- Analytics
- Authentication/security
- Scale/performance expectations

### Controlled Assumptions

The prototype records safe assumptions rather than silently treating unspecified requirements as confirmed facts.

The requirement is also decomposed into acceptance criteria and engineering tasks.

### Orchestration

Execution continues using bounded prototype assumptions through:

```text
Requirement Analysis
        |
Architecture
        |
Implementation
        |
Testing + Documentation
        |
Human Approval
        |
Release Readiness
```

### Validation

The scenario verifies:

- Ambiguities are detected
- Assumptions are recorded
- Acceptance criteria are produced
- Tasks are produced
- Architecture and implementation complete
- Testing and documentation complete
- Release stops for human approval
- Human approval is recorded
- Release readiness completes
- Audit evidence is retained

---

# 20. REST Workflow API

The orchestration layer is exposed through REST endpoints.

### Create Workflow

```http
POST /api/workflows
```

Example request:

```json
{
  "requirement": "Build a URL shortener service with expiration and click analytics"
}
```

### Get Workflow

```http
GET /api/workflows/{workflowId}
```

### Execute Workflow

```http
POST /api/workflows/{workflowId}/execute
```

### Approve Workflow

```http
POST /api/workflows/{workflowId}/approve
```

Example:

```json
{
  "approvedBy": "Engineering Manager"
}
```

Workflow responses expose stage statuses, outputs, decisions, approval state, and workflow metrics.

---

# 21. Running the Application

## Prerequisites

Install:

- Java 21
- Git
- VS Code or IntelliJ IDEA

The project includes the Maven Wrapper, so a separate Maven installation is not required.

## Windows

From the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

## Build

```powershell
.\mvnw.cmd clean package
```

## Run Tests

```powershell
.\mvnw.cmd test
```

---

# 22. Testing Approach

The project uses automated tests for application behavior and orchestration behavior.

Testing covers:

- URL creation
- Redirect behavior
- URL expiration
- Click analytics
- Error handling
- Workflow dependency gates
- Human approval gates
- Parallel execution
- Retry boundaries
- Fallback behavior
- Rollback
- Safe-stop behavior
- Dynamic replanning
- Security guardrails
- Audit events
- Workflow metrics
- Workflow REST APIs
- Greenfield scenario
- Brownfield scenario
- Ambiguous-requirement scenario

JUnit 5 and Spring MockMvc are used for automated validation.

---

# 23. Limitations

This project is a prototype and intentionally has several limitations.

### In-Memory Workflow Store

Workflow state is stored in memory.

Restarting the application clears workflow execution state.

A production implementation should use persistent workflow storage.

### H2 Database

The URL Shortener uses an in-memory H2 database.

Production deployment would require a persistent database.

### Simulated Engineering Agents

Stage executors currently produce deterministic prototype engineering outputs rather than invoking external AI/LLM agents.

The orchestration architecture allows those executors to be replaced by real agent implementations later.

### Test Executor

The TestingExecutor represents the testing stage and records validation output. The Maven test suite is executed externally as part of prototype validation rather than being launched directly by the executor.

### Retry Execution

Retry counts are bounded by the workflow model, while retry attempts are currently driven by repeated orchestration execution rather than an automated backoff scheduler.

### Audit Persistence

Audit events are retained in workflow memory for the prototype.

Production systems should persist audit records in durable, tamper-resistant storage.

---

# 24. Engineering Trade-offs

### Deterministic Executors vs External AI Agents

Deterministic executors were selected to make the prototype reproducible and testable.

The trade-off is that the current implementation demonstrates agentic orchestration behavior rather than integration with a production LLM platform.

### In-Memory State vs Persistent Workflow Engine

In-memory state keeps the prototype simple and runnable without external infrastructure.

The trade-off is that workflow state does not survive application restart.

### Explicit Dependency Graph

An explicit graph makes dependencies and invalidation behavior easy to understand and test.

The trade-off is that the workflow topology is currently defined in code rather than dynamically configured.

### Human Approval Before Release

Human approval reduces autonomous deployment risk.

The trade-off is that release execution cannot be completely unattended.

### Parallel Testing and Documentation

Parallel branches demonstrate concurrency and reduce unnecessary sequential workflow latency.

The trade-off is additional synchronization and concurrency complexity.

---

# 25. Final Engineering Summary

This prototype demonstrates an end-to-end agentic SDLC execution model around a working Spring Boot URL Shortener.

The solution provides:

- Structured requirement understanding
- Requirement decomposition
- Explicit workflow dependencies
- Sequential orchestration
- Parallel orchestration and synchronization
- Cross-stage context propagation
- Human approval gates
- Policy guardrails
- Bounded retries
- Fallback handling
- Rollback
- Safe-stop behavior
- Dynamic replanning
- Decision lineage
- Audit events
- Workflow metrics
- REST-based workflow interaction
- Greenfield validation
- Brownfield validation
- Ambiguous-requirement validation

The design intentionally keeps high-impact release decisions under human control while allowing lower-risk engineering stages to execute autonomously within defined boundaries.

The architecture is modular so deterministic prototype executors can later be replaced with specialized AI agents, persistent workflow infrastructure, production observability, and enterprise policy integrations without redesigning the core SDLC dependency model.