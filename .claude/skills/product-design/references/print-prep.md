# 3D-Print Preparation Guide

How to turn a designed part into a final, print-ready file. Applies whenever
the manufacturing method is FDM/SLA printing or the user asks for a printable
file.

## 1. Build orientation

Pick orientation FIRST — it drives supports, strength, and surface quality:

- Largest flat face down when possible; no supports beats any support tuning.
- Layer direction = weakness plane. Orient so tensile/bending loads run
  IN-plane (a hook printed flat snaps; printed on its side it holds).
- Cosmetic faces up or vertical (top surfaces and side walls look best;
  faces touching supports look worst).
- Holes that need accuracy: axis vertical (round holes) beats horizontal
  (sagging ovals). If a horizontal hole is unavoidable, spec a teardrop or
  drill-after-print.
- State the chosen orientation and the reason in the print plan.

## 2. Splitting & build volume

Check the oriented bounding box against the target printer volume. Default
assumption when the user hasn't said: **220×220×250mm** (Ender-class); also
note fit for 256³ (Bambu/X1-class). If the part doesn't fit or a better
orientation requires it, split at a natural plane and add:

- Alignment: ⌀4×8 pins/sockets (0.2mm clearance) or a keyed lap joint.
- Joining: spec glue (PLA: cyanoacrylate; PETG/ABS: epoxy or solvent) or
  M3 screws + heat-set inserts for serviceable joints.

## 3. Supports & design-away

Prefer geometry changes over supports: 45° chamfers under overhangs,
bridging ≤ 25mm for flat ceilings, sacrificial ribs for tall thin walls.
When supports are unavoidable: tree/organic supports for organic shapes,
grid for flat overhangs; support interface distance 0.2mm; note which faces
will carry support scars.

## 4. Slicer settings sheet

Always include this table in the spec and dossier (defaults shown for a
structural PLA+ part, 0.4 nozzle — adjust per part and say why):

| Setting | Value |
|---|---|
| Material | PLA+ (PETG if outdoor/heat >50°C, TPU if flexible) |
| Layer height | 0.2 (0.28 draft / 0.12 fine detail) |
| Perimeters / top / bottom | 3 / 5 / 4 |
| Infill | 25% gyroid (40–60% for load-bearing, 15% for cosmetic) |
| Supports | none / tree @ >45°, interface 0.2 |
| Bed adhesion | none (brim 5mm if footprint < 30% of height) |
| Nozzle / bed temp | 210 / 60 (PLA+) · 240 / 80 (PETG) |
| Speed | 150mm/s outer walls 80 (or printer default) |
| Seam | rear / hidden edge |

Per-part overrides (e.g. "lid: 15% infill, no supports") listed under the
table.

## 5. Final file export

Deliver, in this order of preference:

1. **3MF** — the final file. Export the part ALREADY ROTATED into print
   orientation (Z = build direction), units mm:
   - OpenSCAD: apply the orientation `rotate()` at top level (guard with a
     `print_orientation = 1;` flag so the assembly view stays natural), then
     `openscad -D 'print_orientation=1' -o part.3mf part.scad`.
   - trimesh: `mesh.apply_transform(...); mesh.export("part.3mf")`.
   One 3MF per plate; small parts may share a plate with ≥5mm spacing.
2. **STL** — always included alongside, same orientation.
3. **G-code** — only via a real slicer CLI, never hand-written:
   - Check `prusa-slicer --help` or try `sudo apt-get install -y prusa-slicer`.
   - Slice: `prusa-slicer --export-gcode --load <profile.ini> part.3mf`
     writing an ini from the settings sheet (layer_height, perimeters,
     fill_density, support_material, temperatures, brim_width).
   - Parse the G-code footer for `estimated printing time` and
     `filament used` and report both.
   - If no slicer can be installed, state that plainly and deliver the 3MF
     as the final file — the settings sheet lets the user slice locally.

## 6. Pre-flight checks

- [ ] Oriented bounding box ≤ target build volume (print margins ≥5mm).
- [ ] Mesh watertight and manifold in the exported file (re-load and check).
- [ ] First layer: enough contact area, no floating islands at layer 1.
- [ ] Overhang audit in the FINAL orientation (>45° faces are supported,
      bridges ≤25mm).
- [ ] Clearances/holes compensated per FDM rules (`dfm-rules.md`).
- [ ] Settings sheet, 3MF orientation, and G-code (if any) all agree.
