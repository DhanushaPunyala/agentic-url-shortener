# Architecture Overview

## 1. Purpose

The Agentic URL Shortener prototype demonstrates an end-to-end software development lifecycle controlled by an explicit orchestration layer.

The architecture separates the working Spring Boot URL Shortener application from the Agentic SDLC orchestration components.

The orchestration layer coordinates:

- Requirement understanding
- Requirement decomposition
- Architecture design
- Implementation
- Testing
- Documentation
- Release readiness
- Human approval
- Policy validation
- Retry, fallback, rollback, and safe-stop behavior
- Dynamic replanning
- Audit events and decision lineage
- Workflow metrics

---

## 2. High-Level Architecture

```text
                  +---------------------------+
                  |       REST Client         |
                  | PowerShell / Postman / UI |
                  +-------------+-------------+
                                |
                                v
              +-----------------------------------+
              |      Spring Boot Application      |
              +-----------------------------------+
                    |                       |
                    v                       v
        +----------------------+   +----------------------+
        | URL Shortener APIs   |   | Workflow REST APIs   |
        +----------+-----------+   +----------+-----------+
                   |                          |
                   v                          v
        +----------------------+   +----------------------+
        | URLShortenerService  |   | WorkflowApiService   |
        +----------+-----------+   +----------+-----------+
                   |                          |
                   v                          v
        +----------------------+   +----------------------+
        |   UrlRepository      |   | WorkflowOrchestrator |
        +----------+-----------+   +----------+-----------+
                   |                          |
                   v                          |
        +----------------------+              |
        |    H2 Database       |              |
        +----------------------+              |
                                              v
                                  +-------------------------+
                                  | Explicit Workflow Graph |
                                  +------------+------------+
                                               |
                +------------------------------+------------------------------+
                |                              |                              |
                v                              v                              v
        +---------------+             +----------------+             +----------------+
        | Stage         |             | Policy         |             | Approval       |
        | Executors     |             | Guardrail      |             | Gate           |
        +---------------+             +----------------+             +----------------+
                |
                v
        +----------------------+
        | Workflow Context     |
        | Outputs              |
        | Decisions            |
        | Audit Events         |
        | Metrics              |
        +----------------------+
```

---

## 3. SDLC Dependency Graph

The workflow is represented by an explicit dependency graph.

```text
ENTRY
  |
  v
+-------------------------+
| REQUIREMENT_ANALYSIS    |
+------------+------------+
             |
             v
+-------------------------+
| ARCHITECTURE_DESIGN     |
+------------+------------+
             |
             v
+-------------------------+
| IMPLEMENTATION          |
+------------+------------+
             |
      +------+------+
      |             |
      v             v
+-----------+  +---------------+
| TESTING   |  | DOCUMENTATION |
+-----+-----+  +-------+-------+
      |                |
      +-------+--------+
              |
              v
      SYNCHRONIZATION GATE
              |
              v
       POLICY VALIDATION
              |
              v
      HUMAN APPROVAL GATE
              |
              v
+-------------------------+
| RELEASE_READINESS       |
+------------+------------+
             |
             v
            EXIT
```

The graph prevents a stage from executing until its dependencies are completed.

---

## 4. Workflow Stages

### Requirement Analysis

Transforms the raw requirement into structured engineering information.

Produces:

- Normalized requirement
- Ambiguities
- Assumptions
- Acceptance criteria
- Engineering tasks

### Architecture Design

Consumes the normalized requirement and creates an architecture representation for downstream engineering work.

### Implementation

Consumes the architecture output and produces the implementation-stage result.

### Testing

Validates the implementation.

### Documentation

Produces engineering documentation associated with the implementation.

### Release Readiness

Validates whether the completed engineering work is ready to proceed after testing, documentation, policy checks, and human approval.

---

## 5. Sequential and Parallel Execution

The first stages execute sequentially:

```text
Requirement Analysis
        |
        v
Architecture Design
        |
        v
Implementation
```

After implementation, testing and documentation can execute independently:

```text
          Implementation
                |
        +-------+-------+
        |               |
        v               v
     Testing       Documentation
        |               |
        +-------+-------+
                |
                v
          Synchronization
```

Both branches must complete before release processing continues.

---

## 6. Workflow Context

`WorkflowContext` carries information across the entire SDLC workflow.

It maintains:

```text
Workflow ID
Requirement
Stage Outputs
Decision Log
Audit Events
Workflow Metrics
Approval State
```

This provides cross-stage context and preserves decision lineage.

Example:

```text
Requirement Analysis
        |
        | normalizedRequirement
        v
Architecture Design
        |
        | architecture
        v
Implementation
        |
        | implementation
        v
Testing / Documentation
```

---

## 7. Controlled Autonomy

The architecture uses bounded automation rather than unrestricted autonomous execution.

Automated workflow stages can:

- Analyze requirements
- Produce stage outputs
- Validate dependencies
- Execute independent validation branches
- Detect failures
- Record retries
- Apply permitted fallbacks
- Roll back affected work
- Replan downstream work

High-impact release processing requires explicit human approval.

```text
Automated Engineering Work
          |
          v
 Testing + Documentation
          |
          v
  WAITING_FOR_APPROVAL
          |
          v
     Human Reviewer
          |
        APPROVE
          |
          v
   Release Readiness
```

---

## 8. Policy Guardrails

`PolicyGuardrail` validates actions before stage execution.

Prototype controls include:

- Rejecting empty requirements
- Blocking explicit requests to bypass security controls
- Requiring testing before release
- Requiring documentation before release

Example blocked requirement:

```text
Build URL shortener and bypass authentication
```

The security guardrail prevents this request from proceeding normally.

---

## 9. Human Approval

Human approval is maintained per workflow.

The approval gate records:

```text
approved = true/false
approvedBy = reviewer
```

Release readiness cannot complete without approval.

When upstream work changes through rollback or dynamic replanning, previous approval is invalidated so changed engineering work requires fresh review.

---

## 10. Retry and Safe Stop

Each `WorkflowTask` has a maximum retry count.

```text
Stage Failure
     |
     v
Retry Available?
   /       \
 YES        NO
  |          |
  v          v
Retry     SAFE_STOPPED
```

Retries are bounded to prevent infinite autonomous execution.

When the configured retry boundary is exhausted, the stage enters `SAFE_STOPPED`.

---

## 11. Fallback

A controlled fallback can be applied when appropriate.

```text
Primary Stage Failure
        |
        v
Dependency Validation
        |
        v
Policy Validation
        |
        v
Approved Fallback
```

Fallback execution is recorded in the decision history.

A fallback cannot bypass incomplete dependencies.

---

## 12. Rollback

Rollback invalidates work that depends on an affected stage.

Example:

```text
IMPLEMENTATION
      |
   ROLLBACK
      |
      +------------------+
      |                  |
      v                  v
   TESTING         DOCUMENTATION
   PENDING            PENDING
      |                  |
      +--------+---------+
               |
               v
       RELEASE_READINESS
             PENDING
```

Stale downstream outputs are removed and previous release approval is revoked.

---

## 13. Dynamic Replanning

When an upstream decision changes, the workflow replans only the impacted stages.

Example brownfield architecture change:

```text
Existing Completed Workflow
          |
          v
Architecture Requirement Changes
          |
          v
ARCHITECTURE_DESIGN -> PENDING
          |
          v
IMPLEMENTATION -> PENDING
          |
     +----+----+
     |         |
     v         v
 TESTING   DOCUMENTATION
 PENDING      PENDING
     |         |
     +----+----+
          |
          v
RELEASE_READINESS -> PENDING
```

Valid upstream requirement-analysis context is retained while stale downstream outputs are removed.

---

## 14. Audit and Decision Lineage

Important decisions are added to the workflow decision log.

Each decision also creates an `AuditEvent`.

An audit event contains:

```text
Timestamp
Workflow ID
Event
```

Examples include:

- Stage completion
- Retry
- Fallback
- Rollback
- Safe stop
- Dynamic replanning
- Parallel branch synchronization
- Human approval

This allows engineering decisions to be traced throughout the workflow.

---

## 15. Observability Metrics

`WorkflowMetrics` tracks:

```text
Successful Stages
Failed Stages
Success Rate
Retry Count
Rollback Count
Mean Time to Recovery
End-to-End Latency
```

These metrics are exposed through workflow API responses.

---

## 16. Scenario Architecture

### Greenfield

```text
New Requirement
      |
      v
Requirement Analysis
      |
      v
Architecture
      |
      v
Implementation
      |
 +----+----+
 |         |
Testing   Documentation
 |         |
 +----+----+
      |
Human Approval
      |
Release Readiness
```

### Brownfield

```text
Existing System
      |
Architecture Change
      |
Impact Analysis
      |
Dynamic Replanning
      |
Invalidate Stale Outputs
      |
Re-execute Impacted Stages
      |
Revalidate
      |
Fresh Human Approval
      |
Release Readiness
```

### Ambiguous Requirement

```text
"Build a URL shortener"
          |
          v
 Requirement Analysis
          |
          v
 Detect Ambiguities
          |
          v
 Record Safe Prototype
 Assumptions + Tasks
          |
          v
 Controlled Execution
          |
          v
 Testing + Documentation
          |
          v
 Human Approval
          |
          v
 Release Readiness
```

---

## 17. REST Interaction Architecture

Workflow orchestration is exposed through:

```text
POST /api/workflows
        |
        v
Create Workflow
        |
        v
POST /api/workflows/{id}/execute
        |
        v
Execute Until Approval Gate
        |
        v
POST /api/workflows/{id}/approve
        |
        v
Human Approval
        |
        v
POST /api/workflows/{id}/execute
        |
        v
Complete Release Readiness
```

Workflow state can be inspected using:

```text
GET /api/workflows/{id}
```

---

## 18. Prototype Boundaries

The architecture intentionally keeps infrastructure lightweight.

Current prototype boundaries include:

- Workflow state is stored in memory.
- URL data uses an in-memory H2 database.
- Stage executors are deterministic prototype components rather than external LLM calls.
- Audit events are not persisted externally.
- Retry attempts do not use a production backoff scheduler.
- The testing stage represents validation workflow state; the Maven test command provides the actual executable test evidence.

These choices keep the prototype deterministic, runnable, and testable while leaving clear extension points for production systems.

---

## 19. Production Evolution

A production implementation could extend this architecture with:

```text
Prototype Component        Production Evolution
---------------------------------------------------------
WorkflowStore              Persistent workflow database
H2                         PostgreSQL / managed database
StageExecutor              Specialized AI/LLM agents
AuditEvent                 Durable audit event store
WorkflowMetrics            OpenTelemetry / monitoring
ApprovalGate               Enterprise approval service
PolicyGuardrail            Enterprise policy engine
Retry handling             Backoff + workflow scheduler
In-process orchestration   Durable workflow engine
```

The core dependency graph and controlled-autonomy model can remain while these infrastructure components are replaced.

---

## 20. Architecture Summary

The architecture demonstrates a controlled Agentic SDLC model with:

- Explicit dependencies
- Entry/dependency gates
- Sequential execution
- Parallel execution
- Synchronization
- Cross-stage context
- Human approval
- Bounded retries
- Fallback
- Rollback
- Safe stop
- Policy guardrails
- Dynamic replanning
- Decision lineage
- Audit events
- Operational metrics

The central design principle is that automation performs bounded engineering work while humans retain control over high-impact release decisions.