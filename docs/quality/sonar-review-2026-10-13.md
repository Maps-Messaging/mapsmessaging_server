# Server SonarCloud cleanup review — 13 October 2026

Inventory: 909 OPEN/CONFIRMED findings from the SonarCloud development analysis completed at 2026-10-13 02:06:47 UTC (13:06:47 Australia/Sydney). The Jenkinsfile publishes feature-branch analyses under the development label, so this label is not evidence that the changes have merged to development.

Reviewed against `fix/MSG-374-udp-protocol-compliance`, starting at `70a2d0010`. Every original issue key is recorded in [the review CSV](sonar-review-2026-10-13.csv), with the rule, original line, source path, assessment and reason.

- 205 findings addressed by straightforward source cleanup.
- 2 findings already addressed in the current branch.
- 702 findings deferred for targeted review or fresh-analysis verification.

These are code-review assessments, not SonarCloud status transitions. A fresh analysis is required to confirm closures and the remaining count. No issues were suppressed or marked accepted.

Cleanup includes literal constants; private/local names; equivalent method references, ternaries and patterns; numeric widening; redundant casts and record assignments while preserving null validation; explicitly documented no-op implementations; test-helper cleanup; safe Bash condition/glob syntax; and unchanged Docker package sorting.

Items left for joint review include protocol/parser complexity, null/lifecycle risks, public and wire-facing APIs, exception/interruption policy, locks/volatile/resource ownership, serialization/equality, and deployment/download semantics. In particular, BaseRestApi.hasAccess() and RestAccessControl.hasAccess() require a functional authorization review; no permission behavior was changed here.

## Verification

Java 21 compilation of all 2,217 production and 1,206 test source files succeeded using the existing cached dependencies. Bash syntax checks and git diff whitespace checks passed. All 627 existing focused regression tests passed across 62 selected classes, covering packet boundaries, AMQP framing, MQTT aliases/keep-alive, NATS handler responses, configuration round trips, KPI/twin logic, AIS mapping, schema matching, persistent counters, HA lock handling and statistics. No assertions were weakened and no failing tests were disabled.

Test-helper changes were limited to naming/import cleanup, private unused parameters, removal of the empty @BeforeAll hook, documentation of intentional no-op callbacks, and removal of an unused CTS argument array. No new tests were added for mechanical refactors.

Maven online and offline test-compile attempts failed at dependency collection: repository.mapsmessaging.io is unavailable from this environment and some cached parent/BOM POMs have repository provenance that cannot resolve offline. No project dependencies, POM versions or build profiles were changed to bypass this.

The initial focused JUnit run could not initialize Mockito's inline mock maker because this environment does not support self-attachment. Verification was rerun with Byte Buddy loaded at JVM startup, rather than changing tests or mock behavior.

No Jira was created and no Jenkins build or SonarCloud analysis was triggered.
