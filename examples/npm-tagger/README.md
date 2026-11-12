# NPM Project with Tagger

This example installs the published Tagger CLI as a project development
dependency and calculates a version from commit messages. The commands only
inspect Git history; they do not create or push a tag.

## Try it

Use Node.js 20 or newer and Git. From a clean copy of this directory, install
the pinned package and create a baseline release:

```bash
npm install
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

Add a patch commit and ask the local package executable to calculate the next
version:

```bash
printf 'Release notes demo\n' > README.txt
git add README.txt
git commit -m "[patch] Document the release process"
git push
npx tagger calculate-version
```

Expected output:

```text
1.2.4
```

The local bare remote gives the sample branch an upstream, and pushing the
commit puts the checkout in the same state as a workflow triggered by that
push. The `.tagger` file sets `main` as the release branch. See the [Tagger CLI
guide](../../command-line-tools/tagger-cli/README.md) for configuration,
structured output, and other commands.
