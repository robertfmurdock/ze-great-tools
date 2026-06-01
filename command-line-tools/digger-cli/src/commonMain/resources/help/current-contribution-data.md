## Output and Field Notes

Fields in the resulting contribution object:

- `storyId`: Story/ticket identifier extracted from commit messages.
- `semver`: Highest semantic version impact in the contribution window (`major`, `minor`, `patch`, or `none`).
- `ease`: Optional ease score extracted from commit messages.
- `dateTime`, `firstCommitDateTime`, `tagDateTime`: ISO 8601 timestamps.

## Regex Customization

Override parsing behavior with:
`--major-regex`, `--minor-regex`, `--patch-regex`, `--none-regex`, `--story-id-regex`, `--ease-regex`, `--tag-regex`.

## CI Examples

Extract story ID in a script:

```
STORY_ID=$(digger current-contribution-data --format=json . | jq -r '.data.storyId')
echo "story-id=$STORY_ID"
```
