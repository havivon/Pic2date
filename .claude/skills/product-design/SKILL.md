---
name: product-design
description: >
  Full physical-product design pipeline: turn a short brief (Hebrew or English)
  into an engineering spec, detailed 2D technical drawings with dimensions
  (SVG orthographic views + title block), a parametric 3D model (OpenSCAD),
  exported STL, rendered 3D views, 3D-print preparation (orientation,
  supports, slicer settings) with a final print-ready 3MF/G-code file, and
  an interactive design dossier.
  Use whenever the user asks to design/plan a physical product, device,
  enclosure, bracket, furniture piece, toy, fixture, or accessory —
  e.g. "תכנן לי מוצר", "design a product", "I need drawings and dimensions",
  "מידות ותרשימים", "3D model of ...".
argument-hint: <תיאור המוצר / product brief>
---

# Physical Product Design Pipeline

You are acting as a senior industrial/mechanical designer. The user gives a
product brief; you deliver a complete, internally consistent design package.

**Language:** Always respond in the language of the user's brief (Hebrew brief
→ Hebrew explanations). Drawings, file names, and code stay in English;
dimension units are always **millimeters** unless the user says otherwise.

## Golden rule — single source of truth

Define ONE parameter table (name, value, unit, rationale) early, and derive
**everything** from it: the spec, every dimension line in the drawings, and
every variable in the OpenSCAD model. Never let a number appear in a drawing
that does not trace back to the parameter table. If you change a value, update
all three places.

## Workflow

### Step 1 — Requirements intake

Extract from the brief: function, target user, overall envelope (W×D×H),
material, manufacturing method, load/usage conditions, quantity, aesthetic
constraints, budget hints.

- If a **critical** parameter is missing (overall size, material, or
  manufacturing method) and the session is interactive, ask once with
  `AskUserQuestion` (bundle up to 4 questions in a single call).
- Otherwise choose sensible, clearly-stated defaults and continue. Never stall.

### Step 2 — Design specification

Produce a spec containing:

1. **Concept summary** — what it is, how it's used, key design decisions.
2. **Parameter table** — the single source of truth (see golden rule).
3. **Materials & finish** — specific material grades (e.g. PLA+, ABS, 6061-T6,
   Baltic birch 12mm, PP), finish, color.
4. **Manufacturing method** — FDM/SLA print, CNC, laser cut, sheet metal,
   injection molding — and apply that method's design rules
   (see `references/dfm-rules.md`).
5. **Tolerances** — general tolerance class + critical fits called out
   explicitly (e.g. press-fit ⌀6 +0.0/−0.05).
6. **BOM** — every part and off-the-shelf item (screws, inserts, magnets,
   bearings) with quantity and standard designation (e.g. M3×8 DIN 912).

### Step 3 — 2D technical drawings (SVG)

Create dimensioned engineering drawings as hand-written SVG. Follow
`references/drawing-guide.md` exactly for line weights, dimension-line
anatomy, hatching, and the title block.

Required views per part:
- Third-angle orthographic set: **front, top, right side**.
- An **isometric** pictorial view (no dimensions on it).
- **Section view(s)** wherever internal geometry exists (pockets, bores,
  wall thicknesses).
- Every functional dimension from the parameter table must appear exactly once
  on the most descriptive view. Include overall envelope dims on every part.

One SVG sheet per part plus one assembly sheet with a numbered balloon BOM.
Save the raw `.svg` files in the working directory.

### Step 4 — Parametric 3D model (OpenSCAD)

Write a `.scad` file per the conventions in `references/3d-guide.md`:
- All parameter-table values as named variables at the top, with unit comments.
- One `module` per part; a top-level assembly with an `exploded` toggle.
- `$fn = 96;` for production, fillets/chamfers where DFM requires.

**Export STL and renders:**
1. If `openscad` CLI exists — use it (headless commands in the guide).
2. If not, try `sudo apt-get install -y openscad` (or `pip install trimesh
   numpy manifold3d` and build the mesh in Python).
3. Last resort: generate the STL with the pure-Python writer pattern in
   `references/3d-guide.md` (no dependencies).

Render at least 4 PNG views (front-iso, back-iso, top, exploded) when a
renderer is available.

### Step 4b — 3D-print preparation & final print file

Whenever the manufacturing method is 3D printing (or the user asks for a
printable file), follow `references/print-prep.md` and produce:

1. **Print plan** — per part: build orientation (and why), support strategy,
   estimated bed footprint vs. common printer volumes (220×220×250 /
   256×256×256), and whether the part must be split (add alignment
   pins/joints if so).
2. **Slicer settings sheet** — material, nozzle/layer height, perimeters,
   infill % and pattern, supports on/off + angle, brim/raft, and any
   per-part overrides. Include it in the spec and the dossier.
3. **Final print file** — export a **3MF** (preferred: embeds orientation and
   units; via OpenSCAD `-o model.3mf` or trimesh) in the print orientation,
   plus the STL. If a slicer CLI is available or installable
   (`prusa-slicer`/`slic3r`), also slice to **G-code** with the settings
   sheet values and report estimated print time and filament use. If no
   slicer is available, say so and deliver the 3MF/STL as the final file —
   never guess G-code by hand.

Parts must pass the FDM rules in `references/dfm-rules.md` *in the chosen
orientation* (overhangs, bridging, layer-direction strength) — re-check after
picking orientation, not before.

### Step 5 — Interactive design dossier (Artifact)

Load the `artifact-design` skill, then build ONE artifact page containing:
- Concept summary + parameter table + BOM.
- The SVG drawings embedded inline (each inside an `overflow-x: auto` wrapper).
- The PNG renders embedded as data URIs (if produced).
- An inline, dependency-free 3D viewer of the STL if feasible (small
  hand-rolled WebGL/canvas renderer parsing the STL embedded as base64 —
  no CDN scripts; skip the viewer rather than break CSP).

### Step 6 — Deliver files

Send via `SendUserFile` (single call, `display` unset):
`spec` (in the artifact), all `.svg` sheets, the `.scad`, the `.stl`, the
print-oriented `.3mf` (and `.gcode` when sliced), and renders. Caption
listing what each file is for (e.g. "the 3MF is the final print-ready file /
the SCAD lets you tweak any dimension").

## Quality gates (check before delivering)

- [ ] Every drawing dimension matches the parameter table and the SCAD variables.
- [ ] Wall thicknesses, clearances, and fits obey the chosen process's DFM rules.
- [ ] Views are to a stated scale; title block filled (name, scale, units, date, rev A).
- [ ] STL opens as a valid, watertight mesh (verify: re-parse triangle count, or
      `trimesh.load(...).is_watertight` when available).
- [ ] Mating parts have explicit clearance (moving fit ≥0.3mm FDM; press fit per spec).
- [ ] For printed parts: part fits the target build volume in its print
      orientation, overhangs >45° are supported or designed away, and the
      final 3MF/G-code matches the slicer settings sheet.
- [ ] Final message: TL;DR of the design, key dimensions, and what each delivered file is.

## Iteration

On follow-up change requests ("make it 20mm wider", "switch to plywood"):
update the parameter table first, then regenerate drawings, SCAD, STL, and the
dossier — redeploy the artifact to the SAME url (same file path) and state
what changed as rev B, C… in the title block.
