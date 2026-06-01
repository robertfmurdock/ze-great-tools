## Contribution Boundaries

This command groups commits into contribution windows using tag boundaries.
Each window spans from one version tag to the next and produces one contribution object.

## Output Shape

- `--format=text`: writes a JSON array to file.
- `--format=json`: prints envelope JSON with contributions at `data[]`.
