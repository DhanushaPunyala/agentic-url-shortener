# Scenario Evidence

## Overview

The prototype demonstrates three required engineering scenarios:

1. Greenfield development
2. Brownfield change
3. Ambiguous requirement

Each scenario demonstrates requirement handling, decomposition, orchestration, validation, controlled autonomy, and traceability.

---

# Scenario 1 – Greenfield Development

## Requirement

Build a new URL shortener service with expiration, click analytics, authentication, security, and performance requirements.

## Objective

Demonstrate how a new requirement moves through the complete SDLC workflow from requirement analysis to release readiness.

## Decomposition

The requirement is decomposed into:

1. Requirement Analysis
2. Architecture Design
3. Implementation
4. Testing
5. Documentation
6. Release Readiness

## Orchestration

```text
Requirement
    |
    v
Requirement Analysis
    |
    v
Architecture Design
    |
    v
Implementation
    |
 +--+--+
 |     |
 v     v
Testing Documentation
 |     |
 +--+--+
    |
    v
Synchronization
    |
    v
Human Approval
    |
    v
Release Readiness
```

## Validation Evidence

The automated scenario validates that:

- Requirement analysis completes successfully.
- Architecture design completes successfully.
- Implementation completes successfully.
- Testing and documentation execute after implementation.
- Both validation branches complete.
- Release readiness is blocked before human approval.
- Workflow enters `WAITING_FOR_APPROVAL`.
- Human approval is recorded.
- Release readiness completes after approval.
- Engineering outputs are retained.
- Decision history exists.
- Audit events exist.

## Expected Final State

```text
REQUIREMENT_ANALYSIS = COMPLETED
ARCHITECTURE_DESIGN  = COMPLETED
IMPLEMENTATION       = COMPLETED
TESTING              = COMPLETED
DOCUMENTATION        = COMPLETED
RELEASE_READINESS    = COMPLETED
```

---

# Scenario 2 – Brownfield Change

## Requirement

Enhance an existing URL shortener service with expiration, click analytics, authentication, security, and performance requirements.

## Objective

Demonstrate safe modification of an existing completed system when an upstream architecture decision changes.

## Initial State

The scenario simulates an existing application whose SDLC stages have already completed.

Existing outputs include:

- Requirement context
- Architecture
- Implementation
- Testing
- Documentation
- Release readiness

The existing release is also treated as previously human-approved.

## Change

A new architecture requirement is introduced to support expiration and analytics.

## Impact Analysis

The architecture change affects downstream engineering work.

The orchestrator replans:

```text
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

## Stale Output Invalidation

Previous downstream outputs are removed.

The existing normalized requirement remains valid because the change begins at the architecture stage.

## Approval Invalidation

Previous human release approval is revoked because the underlying engineering artifacts changed.

This prevents an old approval from authorizing changed work.

## Re-execution

The impacted stages execute again:

1. Architecture Design
2. Implementation
3. Testing and Documentation
4. Release Readiness approval gate

## Validation Evidence

The automated scenario verifies:

- Architecture returns to `PENDING`.
- Implementation returns to `PENDING`.
- Testing returns to `PENDING`.
- Documentation returns to `PENDING`.
- Release readiness returns to `PENDING`.
- Stale outputs are removed.
- Requirement context is preserved.
- Previous approval is revoked.
- Architecture executes again.
- Implementation executes again.
- Testing and documentation execute again.
- Fresh human approval is required.
- Release completes only after fresh approval.
- Dynamic replanning appears in decision lineage.
- Audit evidence exists.

## Expected Final State

```text
REQUIREMENT_ANALYSIS = COMPLETED
ARCHITECTURE_DESIGN  = COMPLETED
IMPLEMENTATION       = COMPLETED
TESTING              = COMPLETED
DOCUMENTATION        = COMPLETED
RELEASE_READINESS    = COMPLETED

Fresh Human Approval = REQUIRED
```

---

# Scenario 3 – Ambiguous Requirement

## Requirement

```text
Build a URL shortener
```

## Objective

Demonstrate how the system handles an underspecified requirement without silently treating missing information as confirmed.

## Ambiguity Detection

Requirement analysis identifies missing engineering information.

The prototype checks for missing requirements related to:

- URL expiration
- Analytics/click tracking
- Authentication/security
- Scale/performance

## Controlled Assumptions

When ambiguity exists, the prototype records a safe assumption:

```text
Use safe prototype defaults until ambiguous requirements
receive human clarification.
```

The assumption is explicitly represented in workflow context rather than hidden.

## Requirement Decomposition

The requirement-analysis output also includes:

- Normalized requirement
- Ambiguities
- Assumptions
- Acceptance criteria
- Engineering tasks

## Controlled Execution

The prototype continues with bounded prototype assumptions:

```text
Ambiguous Requirement
        |
        v
Requirement Analysis
        |
        v
Ambiguities + Assumptions
        |
        v
Architecture Design
        |
        v
Implementation
        |
   +----+----+
   |         |
   v         v
Testing   Documentation
   |         |
   +----+----+
        |
        v
Human Approval
        |
        v
Release Readiness
```

## Validation Evidence

The scenario validates that:

- Requirement analysis succeeds.
- Ambiguities are explicitly identified.
- Expiration ambiguity is detected.
- Analytics ambiguity is detected.
- Authentication/security ambiguity is detected.
- Scale/performance ambiguity is detected.
- Safe prototype assumptions are recorded.
- Acceptance criteria are generated.
- Engineering tasks are generated.
- Architecture completes.
- Implementation completes.
- Testing completes.
- Documentation completes.
- Release cannot complete without human approval.
- Human approval is recorded.
- Release readiness completes.
- Decision history exists.
- Audit events exist.

## Expected Final State

```text
Ambiguities Detected = YES
Assumptions Recorded = YES
Human Approval       = REQUIRED

REQUIREMENT_ANALYSIS = COMPLETED
ARCHITECTURE_DESIGN  = COMPLETED
IMPLEMENTATION       = COMPLETED
TESTING              = COMPLETED
DOCUMENTATION        = COMPLETED
RELEASE_READINESS    = COMPLETED
```

---

# Cross-Scenario Control Evidence

The three scenarios collectively demonstrate:

| Capability | Greenfield | Brownfield | Ambiguous |
|---|---|---|---|
| Requirement understanding | Yes | Yes | Yes |
| Requirement decomposition | Yes | Yes | Yes |
| Explicit dependency graph | Yes | Yes | Yes |
| Sequential execution | Yes | Yes | Yes |
| Parallel validation | Yes | Yes | Yes |
| Synchronization | Yes | Yes | Yes |
| Cross-stage context | Yes | Yes | Yes |
| Human approval | Yes | Yes | Yes |
| Decision lineage | Yes | Yes | Yes |
| Audit evidence | Yes | Yes | Yes |
| Dynamic replanning | Not triggered | Yes | Not triggered |
| Stale-output invalidation | Not triggered | Yes | Not triggered |
| Ambiguity handling | Not required | Not required | Yes |

---

# Additional Resilience Evidence

Separate automated tests validate orchestration controls beyond the three primary scenarios.

These include:

- Dependency blocking
- Bounded retry behavior
- Fallback behavior
- Rollback
- Safe stop
- Security policy guardrails
- Stale-output invalidation
- Structured audit events
- Workflow metrics
- REST workflow operations

---

# Test Execution

Run all automated tests using:

```powershell
.\mvnw.cmd test
```

Build and package the complete application using:

```powershell
.\mvnw.cmd clean package
```

A successful execution should finish with:

```text
BUILD SUCCESS
```

---

# Reviewer Traceability

The three primary automated scenarios are implemented in:

```text
src/test/java/com/assessment/urlshortener/orchestration/
WorkflowOrchestratorTest.java
```

Relevant test methods:

```text
shouldExecuteGreenfieldScenarioFromRequirementToRelease()

shouldExecuteBrownfieldScenarioWithImpactAnalysisAndReplanning()

shouldExecuteAmbiguousRequirementScenarioWithControlledAssumptions()
```

Together, these scenarios provide executable evidence of requirement decomposition, orchestration, validation, controlled autonomy, change handling, and human oversight.