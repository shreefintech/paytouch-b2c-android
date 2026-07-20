#!/bin/sh
# Run this once after cloning to activate the project's git hooks.
git config core.hooksPath .githooks
echo "✅ Git hooks configured. Hooks in .githooks/ are now active."
