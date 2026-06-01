Digger extracts contribution metadata from Git history for CI and reporting workflows.
**Use `--format=json` for automation** and text mode when writing output files.

Typical CI/build script usage:

```
digger current-contribution-data --format=json path/to/repo
digger all-contribution-data --format=json path/to/repo
```

| Command                     | Purpose                                             |
|-----------------------------|-----------------------------------------------------|
| `current-contribution-data` | Data since the latest version tag                   |
| `all-contribution-data`     | Data grouped across all tagged contribution windows |
| `guide`                     | Fit-check, best practices, and workflow philosophy  |

For fit assessment and philosophy: digger guide
