# SVG Technical Drawing Guide

Conventions for producing dimensioned engineering drawings as hand-written
SVG. The goal: drawings that look like they came from a CAD drafting package,
not a diagram tool.

## Sheet setup

- Landscape sheet, `viewBox="0 0 1400 990"` (≈A3 proportions). One part per
  sheet; assembly gets its own sheet.
- White background rect, thin border frame inset 10 units, title block in the
  bottom-right corner.
- Scale: pick 1:1, 1:2, 2:1, 1:5… so the views fill ~70% of the sheet. State
  the scale in the title block. All dimension TEXT shows real millimeters
  regardless of drawing scale.
- Layout (third-angle projection): front view bottom-left; top view directly
  ABOVE front; right-side view directly RIGHT of front; isometric view in the
  top-right area. Views must be aligned — shared edges on shared axes.

## Line weights and styles

| Element | stroke | width | dash |
|---|---|---|---|
| Visible object lines | `#111` | 2.2 | solid |
| Hidden lines | `#111` | 1.1 | `6 3` |
| Center lines | `#111` | 0.9 | `14 3 3 3` |
| Dimension/extension lines | `#333` | 0.9 | solid |
| Section hatching | `#444` | 0.8 | solid, 45°, 6-unit pitch |
| Sheet frame / title block | `#111` | 1.5 / 0.9 | solid |

Use `stroke-linecap="round"` on object lines. No fills on object geometry
except section hatching regions (`fill="none"` everywhere else).

## Dimension line anatomy

Every dimension = extension lines + dimension line + arrowheads + text:

1. **Extension lines**: start 2 units off the object edge, extend 8 units past
   the dimension line.
2. **Dimension line**: between the extension lines, offset 24–40 units from
   the object (stack parallel dims 26 units apart, smallest closest).
3. **Arrowheads**: filled triangles, ~9 long × 3.5 half-width, tips touching
   the extension lines. Define once as a `<marker>` and reuse:
   ```svg
   <marker id="arr" viewBox="0 0 10 10" refX="9" refY="5"
           markerWidth="9" markerHeight="9" orient="auto-start-reverse">
     <path d="M0,1.5 L9,5 L0,8.5 z" fill="#333"/>
   </marker>
   <!-- usage: marker-start="url(#arr)" marker-end="url(#arr)" -->
   ```
4. **Text**: 15px, `font-family="ui-monospace, monospace"`, centered above a
   horizontal dim line (gap the line if space is tight); for vertical dims
   rotate −90° reading from the right. Real-world mm value only — no units
   suffix (units declared once in the title block).

Special callouts:
- Diameters: `⌀12`, radii: `R5` with a leader line + single arrowhead.
- Holes: `4× ⌀3.4 ⌵ ⌀6.5×3` style leader notes for patterns/counterbores.
- Critical tolerances inline: `20 ±0.1` or `⌀6 +0.0/−0.05`.
- Thread callouts: `M3×0.5 – 6H ↧8`.

## Section views

- Cutting plane on the parent view: thick dashed line, arrows showing view
  direction, letters `A—A`.
- The section view is titled `SECTION A—A (1:2)` and hatches only material
  actually cut. Different parts in an assembly section get different hatch
  angles (45° / 135°).

## Title block (bottom-right, ~360×110)

Rows: product name + part name | material + finish | scale, units (mm),
projection symbol "THIRD ANGLE", sheet n/N | date, revision (A, B…), and
"Drawn: Claude". Keep it a simple bordered grid of `<rect>`+`<text>`.

## General notes block

Above the title block, numbered notes, e.g.:
`1. ALL DIMENSIONS IN MM. 2. GENERAL TOLERANCE ±0.2 UNLESS NOTED.
3. BREAK ALL SHARP EDGES 0.5×45°. 4. MATERIAL: PLA+, 40% INFILL.`

## Sanity checks before saving each sheet

- Views aligned and to the same scale; isometric may use its own (noted) scale.
- No dimension duplicated across views; no functional dimension missing.
- Dimension text never overlaps geometry or other text — offset further out
  instead of shrinking the font.
- Sum-check chains: partial dims along an edge must sum to the overall dim.
