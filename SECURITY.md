# Security FAQ

## Why does Socket.dev flag this for shellAccess?

Tagger and Digger execute git commands via shell. Socket.dev correctly detects `child_process.spawnSync` (Node.js) and `ProcessBuilder` (JVM).

## What commands does it execute?

Git commands: `git tag`, `git log`, `git describe`, `git rev-parse`, `git config`. See implementation in `/tools/git-adapter/src/commonMain/kotlin/com/zegreatrob/tools/adapter/git/GitAdapter.kt`.

## Is this safe?

Yes. Commands are constructed as Lists (not strings), preventing injection. No user input flows directly to command construction. Environment variables are explicitly controlled.

## What safety measures are in place?

- Commands built as structured Lists: `/tools/git-adapter/src/commonMain/kotlin/com/zegreatrob/tools/adapter/git/GitAdapter.kt`
- Platform-isolated execution: `/tools/git-adapter/src/jsMain/kotlin/com/zegreatrob/tools/adapter/git/RunProcess.js.kt` (Node.js, 35 lines), `/tools/git-adapter/src/jvmMain/kotlin/com/zegreatrob/tools/adapter/git/RunProcess.jvm.kt` (JVM, 22 lines)
- Controlled environment variables
- No string concatenation in command construction

## Can I audit the code?

Yes. All source is in `/tools/git-adapter/`. Start with `GitAdapter.kt` for command construction, then `RunProcess.js.kt` and `RunProcess.jvm.kt` for platform execution.

**Runtime audit:** Use the `--show-commands` flag to see exactly what git commands are executed:

```bash
tagger --show-commands calculate-version
digger --show-commands current-contribution-data $(pwd)
```

All git commands are logged to stderr before execution. This allows security audits, compliance verification, and debugging without reading source code.

## Reporting vulnerabilities

Email security concerns to the maintainer listed in package.json.
