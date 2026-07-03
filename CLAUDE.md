# Pic2date — Claude Code instructions

## Design work: always use the stitch-designer agent

For ANY design task in this project — UI/UX, screens, pages, components,
landing pages, mockups, redesigns, styling, themes — you MUST delegate to
the `stitch-designer` agent (defined in `.claude/agents/stitch-designer.md`).
Do not hand-craft designs directly in the main thread.

The agent uses the Google Stitch MCP server (`stitch`, configured in
`.mcp.json`) to generate advanced designs and then integrates the output
into this codebase.

### Setup requirement

The Stitch MCP server authenticates with the `STITCH_API_KEY` environment
variable. If it is missing, tell the user to:

1. Generate an API key at https://stitch.withgoogle.com (Settings → API keys).
2. Set `STITCH_API_KEY` in the environment (locally: shell profile; in
   Claude Code on the web: the environment's variables settings).

To make the agent available in every project (not only this repo), run:

```bash
./scripts/install-stitch-designer-global.sh
```
