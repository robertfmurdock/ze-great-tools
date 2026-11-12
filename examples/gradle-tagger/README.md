# Gradle Project with Tagger

This example shows a small Gradle project calculating its next release version
from Git history. It uses the published Tagger Gradle plugin and does not
publish artifacts or create a tag.

## Try it

Use Java 17 or newer. The included Gradle wrapper downloads Gradle 9.9.0 on
first use. From a clean copy of this directory, initialize a repository and
create a baseline release:

```bash
git init --initial-branch=main
git config user.name "Example Developer"
git config user.email "developer@example.com"
git add .
git commit -m "[none] Start release-notes demo"
git tag -a 1.2.3 -m "Release 1.2.3"
git init --bare ../release-notes-demo-remote.git
git remote add origin ../release-notes-demo-remote.git
git push --set-upstream origin main --tags
```

Now add a commit that asks for a patch version and calculate the next version:

```bash
printf 'Release notes demo\n' > README.txt
git add README.txt
git commit -m "[patch] Document the release process"
git push
./gradlew calculateVersion -q
```

Expected output:

```text
1.2.4
```

The local bare remote gives the sample branch an upstream, and pushing the
commit puts the checkout in the same state as a workflow triggered by that
push. The `[patch]` marker selects the patch increment, and the annotated
`1.2.3` tag provides the version baseline. Run `./gradlew tasks --group versioning`
to explore the plugin tasks. See the
[Tagger plugin guide](../../tools/tagger-plugin/README.md)
for configuration and release workflow details.
