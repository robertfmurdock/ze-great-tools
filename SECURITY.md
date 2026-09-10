# Security FAQ

## Why does Socket.dev flag this for shellAccess?

Tagger and Digger CLI tools execute Git commands to extract version and contribution information from repositories.
Socket.dev correctly detects process spawning via `child_process.spawnSync` (Node.js) and `ProcessBuilder` (JVM).

Execution is direct and non-shell (`shell: false`, `windowsHide: true` on Node.js), avoiding any shell interpreters or
intermediate shell parsing.

## What commands does it execute?

Git commands: `git tag`, `git log`, `git describe`, `git rev-parse`, `git config`. See implementation
in [tools/git-adapter/src/commonMain/kotlin/com/zegreatrob/tools/adapter/git/GitAdapter.kt](tools/git-adapter/src/commonMain/kotlin/com/zegreatrob/tools/adapter/git/GitAdapter.kt).

## Is this safe?

Yes. Commands are constructed as structured Lists (not shell strings), preventing injection. No user input flows
directly to command construction. Environment variables are explicitly controlled and passed without shell evaluation.

## What safety measures are in place?

- **Direct, Non-Shell Execution**: Node.js execution explicitly configures `{ shell: false, windowsHide: true }`
  in [tools/git-adapter/src/jsMain/kotlin/com/zegreatrob/tools/adapter/git/RunProcess.js.kt](tools/git-adapter/src/jsMain/kotlin/com/zegreatrob/tools/adapter/git/RunProcess.js.kt).
- **Structured Argument Lists**: Commands are constructed as immutable `List<String>` collections
  in [tools/git-adapter/src/commonMain/kotlin/com/zegreatrob/tools/adapter/git/GitAdapter.kt](tools/git-adapter/src/commonMain/kotlin/com/zegreatrob/tools/adapter/git/GitAdapter.kt).
- **Argument Precondition Validation**: Executable commands and argument lists are validated before invocation.
- **Platform-Isolated Execution**: Process spawning is encapsulated in platform-specific adapters
  ([RunProcess.js.kt](tools/git-adapter/src/jsMain/kotlin/com/zegreatrob/tools/adapter/git/RunProcess.js.kt)
  and [RunProcess.jvm.kt](tools/git-adapter/src/jvmMain/kotlin/com/zegreatrob/tools/adapter/git/RunProcess.jvm.kt)).
- **No Shell Concatenation**: No string concatenation, escaping hacks, or command interpolation are used anywhere in the
  codebase.
- **Controlled Environment**: Process environment variables are explicitly merged with standard system environments.
- **Socket Capability Policy**: Repository-level capability declarations and policies are formalized
  in [socket.yml](socket.yml).

## Build Provenance & Supply Chain Verification

NPM packages (`@continuous-excellence/tagger` and `@continuous-excellence/digger`) are published directly from GitHub
Actions CI with cryptographic build provenance (`--provenance`) signed by Sigstore via OIDC tokens.

This generates verifiable SLSA attestations linking each published NPM artifact directly to the exact commit SHA and
GitHub Actions workflow run in `robertfmurdock/ze-great-tools`.

### Verifying Provenance

You can verify the authenticity and provenance of published packages using npm:

```bash
# Verify signatures and provenance via npm
npm audit signatures
```

## Can I audit the code?

Yes. All source is in [tools/git-adapter/](tools/git-adapter/). Start with `GitAdapter.kt` for command construction,
then `RunProcess.js.kt` and `RunProcess.jvm.kt` for platform execution.

**Runtime audit:** Use the `--show-commands` flag to see exactly what git commands are executed:

```bash
tagger --show-commands calculate-version
digger --show-commands current-contribution-data $(pwd)
```

All git commands are logged to stderr before execution. This allows security audits, compliance verification, and
debugging without reading source code.

## Reporting vulnerabilities

Email security concerns to the maintainer listed in package.json.
