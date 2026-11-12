package com.zegreatrob.tools.tagger

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.process.CommandLineArgumentProvider

abstract class GithubReleaseCreateArguments : CommandLineArgumentProvider {
    @get:Input
    abstract val version: Property<String>

    @get:Input
    abstract val draft: Property<Boolean>

    override fun asArguments(): Iterable<String> {
        val draftFlag = if (draft.get()) "--draft " else ""
        val script = """
            version="$1"

            release_exists() {
                diagnostic=$(gh release view "${'$'}version" 2>&1) || return 1
                echo "Release ${'$'}version already exists, skipping creation"
            }

            attempt_release() {
                create_attempted=false
                if release_exists; then return 0; fi
                [ "${'$'}diagnostic" = "release not found" ] || return 1
                create_attempted=true
                diagnostic=$(gh release create "${'$'}version" --verify-tag $draftFlag--title "${'$'}version" --notes "${'$'}version" 2>&1) || return 1
                printf '%s\n' "${'$'}diagnostic"
            }

            matches_diagnostic() {
                printf '%s\n' "${'$'}diagnostic" | LC_ALL=C grep -Eiq "$1"
            }

            classify_failure() {
                if matches_diagnostic 'secondary rate limit'; then
                    echo rate_limited
                elif matches_diagnostic 'HTTP (500|502|503|504)([^0-9]|$)|connection reset|i/o timeout|context deadline exceeded|connection timed out|TLS handshake timeout|remote error: tls: internal error'; then
                    echo transient
                else
                    return 1
                fi
            }

            reconcile_create() {
                [ "${'$'}failure" = transient ] && [ "${'$'}create_attempted" = true ] || return 1
                (release_exists)
            }

            for delay in 1 2 4 final; do
                if attempt_release; then exit 0; fi
                failure=$(classify_failure) || break
                if [ "${'$'}delay" = final ]; then
                    if reconcile_create; then exit 0; fi
                    break
                fi
                wait_seconds="${'$'}delay"
                if [ "${'$'}failure" = rate_limited ]; then wait_seconds=60; fi
                printf 'Retrying GitHub release %s after: %s\n' "${'$'}version" "${'$'}diagnostic" >&2
                jitter=$(od -An -N1 -tu1 /dev/urandom | tr -d ' ')
                sleep "${'$'}wait_seconds.${'$'}(printf '%03d' "${'$'}((jitter * 1000 / 256))")" || exit 1
            done
            printf '%s\n' "${'$'}diagnostic" >&2
            exit 1
        """.trimIndent()
        return listOf("-c", script, "sh", version.get())
    }
}
