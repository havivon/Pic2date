#!/usr/bin/env bash
# Installs the Stitch design agent globally for Claude Code (user scope),
# so it is available in EVERY project on this machine, not just this repo.
#
# Usage:
#   export STITCH_API_KEY="your-key-from-stitch-settings"
#   ./scripts/install-stitch-designer-global.sh
#
# Get an API key: open https://stitch.withgoogle.com -> Settings -> API keys.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
AGENT_SRC="$REPO_ROOT/.claude/agents/stitch-designer.md"
AGENT_DST_DIR="$HOME/.claude/agents"

if [[ -z "${STITCH_API_KEY:-}" ]]; then
  echo "ERROR: STITCH_API_KEY is not set." >&2
  echo "Generate a key at https://stitch.withgoogle.com (Settings -> API keys)," >&2
  echo "then run: export STITCH_API_KEY=\"<your-key>\" and re-run this script." >&2
  exit 1
fi

# 1. Install the agent definition at user scope (~/.claude/agents).
mkdir -p "$AGENT_DST_DIR"
cp "$AGENT_SRC" "$AGENT_DST_DIR/stitch-designer.md"
echo "Installed agent: $AGENT_DST_DIR/stitch-designer.md"

# 2. Register the Stitch MCP server at user scope.
if command -v claude >/dev/null 2>&1; then
  claude mcp remove stitch -s user >/dev/null 2>&1 || true
  claude mcp add stitch --transport http https://stitch.googleapis.com/mcp \
    --header "X-Goog-Api-Key: ${STITCH_API_KEY}" -s user
  echo "Registered Stitch MCP server at user scope."
  echo "Verify with: claude, then /mcp (stitch should show as connected)."
else
  echo "WARNING: 'claude' CLI not found in PATH. Add the MCP server manually:" >&2
  echo "  claude mcp add stitch --transport http https://stitch.googleapis.com/mcp \\" >&2
  echo "    --header \"X-Goog-Api-Key: \$STITCH_API_KEY\" -s user" >&2
fi

echo "Done. The stitch-designer agent is now available in all Claude Code sessions."
