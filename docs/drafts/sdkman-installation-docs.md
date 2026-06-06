# SDKMAN Installation Documentation (DRAFT)

This documentation is ready to merge into the CLI READMEs once SDKMAN distribution is live and tested.

## For tagger-cli/README.md

Replace the "JVM Distribution" section with:

```markdown
### JVM Distribution

For environments without Node.js or when you prefer a JVM-based tool, tagger is available through SDKMAN! or can be built
from source.

#### SDKMAN! Installation (Recommended for JVM users)

[SDKMAN!](https://sdkman.io) is a tool for managing parallel versions of multiple Software Development Kits on Unix-based
systems.

**Install SDKMAN!** (if not already installed):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

**Install tagger:**

```bash
sdk install tagger
```

**Verify installation:**

```bash
tagger --version
```

**Version management:**

```bash
# List available versions
sdk list tagger

# Install specific version
sdk install tagger 1.2.3

# Switch between installed versions
sdk use tagger 1.2.3

# Set default version
sdk default tagger 1.2.3

# Upgrade to latest version
sdk upgrade tagger
```

**Requirements:** Java Runtime Environment (JRE) 8 or higher

#### Building from Source

You can also build the JVM distribution locally:

```bash
# Clone the repository
git clone https://github.com/robertfmurdock/ze-great-tools.git
cd ze-great-tools

# Build the JVM distribution
./gradlew :command-line-tools:tagger-cli:jvmDistZip

# Extract the distribution
unzip command-line-tools/tagger-cli/build/distributions/tagger-cli-jvm.zip -d ~/bin/

# Run directly
~/bin/tagger-cli-jvm/bin/tagger calculate-version

# Or add to PATH
export PATH="$PATH:$HOME/bin/tagger-cli-jvm/bin"
tagger calculate-version
```

The distribution includes:

- `bin/tagger` - Shell script wrapper for Unix-like systems
- `bin/tagger.bat` - Batch script wrapper for Windows
- `lib/` - All required JVM dependencies
```

## For digger-cli/README.md

Replace the "JVM Distribution (Alternative)" section with:

```markdown
### JVM Distribution (Alternative)

For environments without Node.js, digger is available through SDKMAN! or can be built from source.

#### SDKMAN! Installation

[SDKMAN!](https://sdkman.io) is a tool for managing parallel versions of multiple Software Development Kits on Unix-based
systems.

**Install SDKMAN!** (if not already installed):

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

**Install digger:**

```bash
sdk install digger
```

**Verify installation:**

```bash
digger --version
```

**Version management:**

```bash
# List available versions
sdk list digger

# Install specific version
sdk install digger 1.2.3

# Switch between installed versions
sdk use digger 1.2.3

# Set default version
sdk default digger 1.2.3

# Upgrade to latest version
sdk upgrade digger
```

**Requirements:** Java Runtime Environment (JRE) 8 or higher

#### Build from Source

You can also build the JVM distribution locally:

```bash
./gradlew :command-line-tools:digger-cli:jvmDistZip
unzip command-line-tools/digger-cli/build/distributions/digger-cli-jvm.zip -d /path/to/install
```

#### Distribution Structure

The JVM distribution archive contains:
- `bin/digger` - Unix/Linux/macOS executable script
- `bin/digger.bat` - Windows executable script
- `lib/` - All required JVM dependencies

Add the `bin` directory to your PATH for easy access:

```bash
export PATH="/path/to/digger-cli-jvm/bin:$PATH"
digger --version
```
```

## Links Verified
- https://sdkman.io - verified 200 OK
- https://get.sdkman.io - verified 200 OK
