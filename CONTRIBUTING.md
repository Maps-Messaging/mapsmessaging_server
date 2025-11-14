# Contributing Guidelines

## 📘 Conventional Commit Standard

All MapsMessaging repositories follow the [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/) standard.

Every commit message must follow this pattern:

```
<type>(<scope>): <subject>

[optional body]

[JIRA: MAPS-### | NO-ISSUE | BREAKING CHANGE: ...]
```

### Example
```
feat(server): add MQTT 5 bridge support

Implements protocol translation between MQTT 3.1.1 and 5.0.

JIRA: MAPS-123
```

---

## 🔹 Allowed Types
| Type | Purpose |
|------|----------|
| **feat** | A new feature |
| **fix** | A bug fix |
| **refactor** | Code change that neither fixes a bug nor adds a feature |
| **perf** | Performance improvements |
| **test** | Adding or modifying tests |
| **build** | Build system or dependency changes |
| **ci** | Continuous Integration or pipeline updates (e.g., Buildkite) |
| **docs** | Documentation only |
| **style** | Code style changes (formatting, etc.) |
| **chore** | Maintenance or non-functional changes |
| **revert** | Reverts a previous commit |

---

## 🔸 Scopes
Examples include:  
`server`, `ml`, `scheduler`, `config`, `schema`, `protocol`, `mqtt`, `amqp`, `nats`, `coap`, `lora`, `rest`, `buildkite`, `docs`.

---

## 🧱 Footer Fields
| Footer | Description |
|---------|--------------|
| **JIRA:** | Reference to a Jira issue, e.g. `MAPS-123` |
| **NO-ISSUE:** | Used when no Jira ticket applies |
| **BREAKING CHANGE:** | Describes an API or behavior change |
| **DEPRECATED:** | Marks functionality as deprecated |
| **SECURITY:** | Notes security-related commits |

---

## ✍️ Header Rules
- Limit header line to **100 characters**.
- Use **imperative mood** (e.g., “add”, not “added”).
- Don’t end the subject line with a period.
- Use lowercase for type and scope.
- Example:
  ```
  fix(protocol): handle empty MQTT 5 property lists
  ```

---

## ⚙️ IntelliJ Setup

1. Install **Conventional Commit** plugin.
    - Go to **Settings → Plugins → Marketplace → "Conventional Commit"**.
2. Enable these options under **Settings → Tools → Conventional Commit**:
    - ✅ Validate commit messages
    - ✅ Enable template completion
    - ✅ Expand template on type completion
3. Optional:
    - **Settings → Version Control → Commit → Commit Message Template**  
      Add this:
      ```
      type(scope): short summary
 
      [optional body]
 
      [JIRA: MAPS-### | NO-ISSUE]
      ```

---

## 🧰 Command Line Setup

For CLI contributors, install `commitlint` and use it as a local Git hook.

```bash
npm install --save-dev @commitlint/{config-conventional,cli}
echo "module.exports = {extends: ['@commitlint/config-conventional']}" > commitlint.config.js
echo '#!/bin/sh\nnpx commitlint --edit "$1"' > .git/hooks/commit-msg
chmod +x .git/hooks/commit-msg
```

To enforce Jira ID or `NO-ISSUE`, you can extend the config:
```js
rules: {
  'footer-leading-blank': [2, 'always'],
  'footer-empty': [2, 'never'],
  'references-empty': [2, 'never'],
  'footer-max-line-length': [2, 'always', 120]
}
```

---

## 🔐 Buildkite Enforcement

Buildkite CI checks commit messages on each branch before merging.

Example snippet:
```yaml
steps:
  - label: ":white_check_mark: Validate Commit Messages"
    command: npx commitlint --from origin/main --to HEAD
```

---

## 🧭 Team Standard (One-Pager to Share)

**Format:**  
`type(scope): subject` + optional body + footer(s)

**Jira:**  
Every commit must include a Jira ID (`MAPS-###`) or `NO-ISSUE`.

**Breaking changes:**  
Use `!` after type/scope or footer `BREAKING CHANGE:`.

**Header:**  
≤100 characters, lowercase type/scope, imperative mood.

**IDE setup:**  
Install the *Conventional Commit* plugin, enable validation and template completion.

**CLI setup:**  
Use `commitlint` with a Git hook to block invalid commits.

**Branch naming:**  
`MAPS-###-short-desc`

**Pull Requests:**  
Title must mirror the commit header.

**CI:**  
Buildkite runs commit message validation before merging.

---

## 🏁 Summary

By following this standard:
- All commits stay consistent and parseable.
- Release notes can be generated automatically.
- Jira integration links issues directly from commit logs.
- Developers across IDEs and pipelines share one format.

```
feat(config): add support for external schema mapping

Adds ability to import Avro/Protobuf/JSON schema files from external repos.

JIRA: MAPS-321
```
