
# Developer AI Usage Policy

## 1. Purpose

This policy defines how Artificial Intelligence (AI) tools may be used in the development process.
The goal is to embrace AI as a productivity tool while ensuring code quality, correctness,
maintainability, and architectural integrity.

AI is an assistant, not an authority.

---

## 2. General Principles

1. AI-generated code is treated the same as human-written code.
2. The developer submitting the code is fully responsible for its correctness and design.
3. AI output must never bypass architectural standards or review processes.
4. Simplicity is preferred over sophistication.
5. All production code must be explainable by the developer who submits it.

---

## 3. Mandatory Developer Responsibilities

### 3.1 Explainability Requirement

The developer must be able to:

- Clearly explain what the code does.
- Justify architectural decisions.
- Describe trade-offs made in the solution.
- Identify risks and edge cases.

If the developer cannot explain the code during peer review, the change must not be merged.

---

### 3.2 Simplicity Rule

AI-generated solutions must follow the principle:

> The simplest solution that correctly solves the problem is preferred.

The following must be avoided unless justified:

- Unnecessary abstraction layers
- Overuse of generics or complex inheritance
- Excessive design patterns
- Premature optimization
- Over-engineered frameworks or utilities

Reviewers may request simplification before approval.

---

### 3.3 Hallucination Safeguards

AI may invent:

- Non-existent APIs
- Incorrect specifications
- Deprecated methods
- Fabricated documentation references

Before merging:

- All external APIs must be verified against official documentation.
- All dependencies must be confirmed to exist and be maintained.
- Any generated specification must be validated against authoritative sources.

Unverified assumptions are not acceptable.

---

### 3.4 Duplicate Implementation Protection

AI frequently produces duplicate or parallel implementations when inheritance
or extension would be more appropriate.

Before merging, developers must verify:

- The functionality does not already exist.
- The solution aligns with existing architecture.
- Reuse has been considered before adding new components.
- Inheritance hierarchies are respected and not bypassed.

Refactoring existing code is preferred over duplication.

---

## 4. Review Requirements for AI-Assisted Code

Pull requests that include AI-assisted code must:

1. Clearly indicate AI assistance in the PR description.
2. Include reasoning for design choices.
3. Include unit or integration tests.
4. Include validation of any external references.
5. Pass all automated checks and quality gates.

Reviewers should specifically assess:

- Architectural alignment
- Simplicity
- Duplication risk
- Correctness of assumptions

---

## 5. Security and Compliance

AI must not be used to:

- Generate proprietary logic without review.
- Insert third-party code without license verification.
- Bypass security controls.
- Introduce secrets, keys, or credentials into source control.

All licensing of generated code must comply with repository licensing rules.

---

## 6. Approved Use Cases

AI may be used for:

- Boilerplate generation
- Test scaffolding
- Refactoring suggestions
- Documentation drafting
- Code explanation
- Static analysis interpretation

AI must not replace architectural decision-making.

---

## 7. Prohibited Practices

The following are not permitted:

- Blindly merging AI-generated code.
- Copy-pasting without verification.
- Allowing AI to define system architecture without review.
- Using AI output as authoritative documentation.

---

## 8. Accountability

The developer who submits AI-assisted code is accountable for:

- Correctness
- Performance characteristics
- Security impact
- Maintainability
- Long-term ownership

"AI generated it" is not an acceptable explanation during review.

---

## 9. Continuous Improvement

This policy will evolve as AI tooling improves.

Any incident caused by AI-generated code must result in:

- Root cause analysis
- Policy refinement if required
- Documentation updates

---

## 10. Enforcement Appendix

### 10.1 Pull Request Template Requirements

All PR templates must include:

- ☐ Was AI used in generating any part of this change?
- ☐ If yes, describe where and how.
- ☐ Explanation of the design and why it is the simplest viable solution.
- ☐ Confirmation that no duplicate functionality exists.
- ☐ Confirmation that external APIs and references were validated.
- ☐ Evidence of test coverage (unit/integration).

PRs missing required sections may be rejected.

---

### 10.2 Required Status Checks

The following checks must pass before merge:

- Build success
- Unit test execution
- Integration tests (if applicable)
- Static analysis / linting
- Security scan (if configured)
- AI compliance check (if implemented)

Branch protection rules must enforce these checks.

---

### 10.3 Reviewer Guidance

Reviewers must explicitly evaluate:

- Does the developer understand the code?
- Is the solution minimal and appropriate?
- Does it align with architectural standards?
- Is there duplication or unnecessary abstraction?
- Were forward-port obligations met (if hotfix)?

If uncertainty exists, request clarification or simplification before approval.

---

## 11. Summary

AI is a tool to accelerate development, not to replace engineering judgment.

Responsibility remains with the developer.
