Review the current git diff and create focused JUnit 5 tests for the changed Java behaviour.

Rules:

- Only add or update files under src/test/java.
- Do not modify production code.
- Do not reformat unrelated files.
- Do not change public APIs.
- Do not change license headers.
- Prefer behavioural tests over implementation tests.
- Prefer real instances over mocks.
- Use Mockito only when needed for IO, timing, network, storage, scheduler, or protocol boundaries.
- Add edge cases and regression tests.
- Run the smallest relevant Maven test command.
- Fix only test code unless I explicitly approve production changes.

When finished, summarise:

- Tests added or changed.
- Behaviour covered.
- Maven command run.
- Result.
- Any suspected production issue that should be reviewed separately.