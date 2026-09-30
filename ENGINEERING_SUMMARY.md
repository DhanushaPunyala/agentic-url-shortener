# Engineering Summary

## 1. Project Objective

This project implements a working Spring Boot URL Shortener together with an Agentic SDLC orchestration layer.

The objective is to demonstrate how a software requirement can move through a controlled engineering lifecycle from requirement understanding to a reviewable release-readiness outcome.

The solution focuses on controlled autonomy rather than unrestricted automation. Engineering stages can execute automatically within defined boundaries, while high-impact release activity requires explicit human approval.

---

## 2. What Was Built

The solution contains two major areas.

### URL Shortener Application

The working application supports:

- Short URL creation
- Redirect to the original URL
- URL expiration
- Click analytics
- URL statistics
- Input validation
- Custom error handling
- REST APIs
- H2 persistence for prototype execution

### Agentic SDLC Orchestration

The orchestration layer supports:

- Requirement analysis
- Requirement decomposition
- Architecture design
- Implementation
- Testing
- Documentation
- Release readiness
- Explicit stage dependencies
- Sequential execution
- Parallel testing and documentation
- Synchronization
- Human approval
- Policy guardrails
- Bounded retries
- Fallback handling
- Rollback
- Safe-stop behavior
- Dynamic replanning
- Decision lineage
- Audit events
- Workflow metrics

---

## 3. Orchestration Model

The primary SDLC flow is:

Requirement Analysis
→ Architecture Design
→ Implementation
→ Testing + Documentation
→ Human Approval
→ Release Readiness

Testing and documentation execute as independent parallel branches after implementation.

Both branches must complete before the workflow can proceed toward release readiness.

Dependencies are explicitly represented by the workflow graph, preventing downstream execution when prerequisite stages are incomplete.

---

## 4. Controlled Autonomy

The prototype establishes boundaries around automated execution.

Automated components can perform requirement analysis, architecture generation, implementation-stage processing, validation, documentation, failure handling, and replanning.

Release readiness is treated as a higher-impact operation.

When the workflow reaches this boundary without approval, it enters:

`WAITING_FOR_APPROVAL`

A human reviewer must explicitly approve the workflow before release readiness can complete.

If upstream engineering work changes, previous approval is invalidated and fresh approval is required.

---

## 5. Requirement Understanding

The requirement-analysis stage converts an input requirement into structured engineering information including:

- Normalized requirement
- Ambiguities
- Assumptions
- Acceptance criteria
- Engineering tasks

This allows the workflow to retain requirement context and provide downstream stages with structured information.

For ambiguous requirements, missing information is explicitly identified rather than silently treated as confirmed.

---

## 6. Failure and Recovery Controls

### Bounded Retry

Stage retries are limited by configured retry counts.

This prevents infinite autonomous retry loops.

### Fallback

Fallback behavior can be used when permitted by dependency and policy checks.

Fallback decisions are retained in the decision history.

### Rollback

Rollback invalidates affected downstream stages and removes stale outputs.

Previous release approval is also revoked.

### Safe Stop

When retry limits are exhausted or execution should not safely continue, a stage can enter:

`SAFE_STOPPED`

The reason remains available in the workflow decision/audit history.

### Dynamic Replanning

When an upstream engineering decision changes, affected downstream stages are reset and stale outputs are invalidated.

This allows the workflow to respond to change without blindly continuing with outdated engineering artifacts.

---

## 7. Policy and Security Controls

The prototype includes policy guardrails that:

- Reject empty requirements
- Block explicit requests to bypass security controls
- Require testing before release
- Require documentation before release

Human approval provides an additional control at the release boundary.

---

## 8. Traceability and Observability

WorkflowContext maintains:

- Stage outputs
- Decision history
- Audit events
- Approval state
- Metrics

Audit events associate decisions with a workflow ID and timestamp.

Operational metrics include:

- Successful stages
- Failed stages
- Success rate
- Retry count
- Rollback count
- Mean time to recovery
- End-to-end latency

These provide visibility into workflow execution and recovery behavior.

---

## 9. Validation Scenarios

### Greenfield Scenario

Demonstrates a new application requirement moving through the complete engineering lifecycle.

Validated behavior includes:

- Requirement decomposition
- Architecture
- Implementation
- Parallel testing and documentation
- Synchronization
- Human approval
- Release readiness
- Audit evidence

### Brownfield Scenario

Demonstrates modification of an existing completed system.

Validated behavior includes:

- Architecture change
- Impacted-stage identification
- Dynamic replanning
- Stale-output invalidation
- Re-execution of impacted stages
- Revalidation
- Previous approval invalidation
- Fresh human approval

### Ambiguous Requirement Scenario

Uses the intentionally underspecified requirement:

`Build a URL shortener`

Validated behavior includes:

- Ambiguity detection
- Safe prototype assumptions
- Acceptance criteria
- Task decomposition
- Controlled downstream execution
- Testing and documentation
- Human approval
- Release readiness
- Audit evidence

---

## 10. Testing

Automated testing covers both the working URL Shortener and orchestration layer.

The test suite validates areas including:

- URL creation
- Redirects
- Expiration
- Analytics
- Error handling
- Dependency gates
- Parallel execution
- Approval gates
- Retry boundaries
- Fallbacks
- Rollbacks
- Safe stops
- Dynamic replanning
- Policy guardrails
- Audit events
- Metrics
- Workflow REST endpoints
- Greenfield execution
- Brownfield execution
- Ambiguous-requirement execution

The final project is built and tested using the Maven Wrapper.

Commands:

`.\mvnw.cmd test`

and:

`.\mvnw.cmd clean package`

The completed prototype successfully packages as a runnable Spring Boot application.

---

## 11. Key Engineering Decisions

### Explicit Workflow Graph

An explicit dependency graph was chosen so stage ordering, dependencies, and downstream invalidation remain understandable and testable.

### Modular Stage Executors

Each SDLC stage uses a separate executor, allowing individual stages to be tested or replaced independently.

### Deterministic Prototype Agents

The current executors use deterministic logic rather than external LLM calls.

This makes the prototype reproducible and allows orchestration behavior to be demonstrated without external AI-service dependencies.

### Human-Controlled Release Boundary

Release approval remains under human control because it represents a higher-impact action.

### Parallel Validation

Testing and documentation execute independently after implementation and synchronize before release processing.

---

## 12. Current Limitations

The prototype intentionally keeps infrastructure lightweight.

Current limitations include:

- Workflow state is stored in memory.
- H2 is used as the URL database.
- Workflow state does not survive application restart.
- Stage executors do not call external AI/LLM services.
- Audit events are not stored in durable external storage.
- Retry execution does not use a production backoff scheduler.
- The TestingExecutor represents the testing workflow stage; the Maven test suite supplies the executable test evidence.

These limitations are appropriate for the prototype scope and are documented rather than hidden.

---

## 13. Production Evolution

A production version could extend the prototype with:

- Persistent workflow storage
- PostgreSQL or another production database
- Specialized AI/LLM agents
- Durable workflow orchestration
- Enterprise approval integration
- Enterprise policy engines
- Persistent audit storage
- OpenTelemetry-based observability
- Distributed tracing
- Retry backoff and scheduling
- Authentication and authorization for workflow APIs

The current modular design provides extension points for these capabilities.

---

## 14. Final Outcome

The project demonstrates more than a working URL Shortener.

It demonstrates an end-to-end Agentic SDLC execution model capable of:

- Understanding requirements
- Decomposing engineering work
- Coordinating dependent stages
- Executing parallel work
- Synchronizing results
- Maintaining cross-stage context
- Enforcing policy boundaries
- Requiring human approval
- Handling failures safely
- Replanning after upstream changes
- Preserving decision lineage
- Capturing operational metrics
- Producing reviewable engineering outcomes

The resulting design combines automated engineering execution with explicit controls, validation, traceability, and human oversight.