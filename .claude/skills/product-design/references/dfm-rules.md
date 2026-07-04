# Design-for-Manufacturing Rules (quick reference)

Apply the block matching the chosen manufacturing method. Values are safe
defaults in mm; the spec may override with justification.

## FDM 3D printing (PLA/PETG/ABS)

- Min wall: 1.2 (3 perimeters @ 0.4 nozzle); prefer 2.0–2.4 structural.
- Min feature/pin: ⌀3; min hole: ⌀2 (drill after print if critical).
- Holes print undersized — add 0.2–0.3 to modeled diameter, or spec reaming.
- Overhangs >45° need support → prefer chamfers (45°) over fillets on the
  underside; design parts to print flat-side down.
- Clearances: sliding fit 0.3/side, snap/loose fit 0.5, press fit 0.05–0.1.
- Threads: ≥M6 printable; below that use heat-set inserts (spec the insert
  boss: ⌀ insert +0.1 hole, wall ≥2 around it).
- Layer direction = weakness plane: orient so loads run in-plane.

## SLA/resin

- Min wall 1.0; drain holes ≥3.5 for hollow parts; account for 0.1–0.3%
  shrink on tolerance-critical fits.

## CNC machining (aluminum/plastic)

- Internal corners get radii ≥ tool radius (spec R3+ for pockets); add relief
  or note "corner radius R__ permissible".
- Min wall 0.8 (metal) / 1.5 (plastic); thread depth ≤ 3×⌀.
- Deep pockets ≤ 4× tool diameter. Standard tolerance ±0.1; tighter costs.

## Laser-cut sheet (plywood/acrylic/steel)

- Design in 2D profiles at material thickness; slot-and-tab joints with
  kerf compensation (~0.15 wood, ~0.1 acrylic per side).
- Tab width ≥ material thickness; acrylic min feature 1.0, avoid sharp
  inside corners (stress cracks) — add R0.5+.

## Sheet metal (bent steel/alu)

- Uniform thickness; min bend radius = 1× thickness; min flange = 4× thickness.
- K-factor ~0.44 for flat-pattern length; holes ≥ 2× thickness from bends.
- Add corner reliefs at intersecting bends.

## Injection molding

- Uniform nominal wall 1.5–3.0; ribs ≤ 60% of wall, bosses cored.
- Draft ≥1° per side (2° on textured faces); no undercuts without noting
  side-action cost.
- Radius everything: inside ≥ 0.5× wall.

## Woodworking (furniture)

- Standard sheet thicknesses: 12/15/18/21 plywood, 18 MDF; design to stock.
- Joinery per load: dado/rabbet for shelves, dominos/dowels ⌀8 for frames,
  confirmat/cam-lock for flat-pack. Screw pilot holes: 70% of root ⌀.
- Solid wood moves ~±0.2%/across grain per season — never constrain wide
  panels rigidly (elongated holes / buttons for tabletops).

## Universal

- Fasteners from the standard series only (M3/M4/M5/M6; DIN 912/965/985).
- Every mating interface has an explicit modeled clearance — never
  line-to-line.
- Human factors: grips ⌀28–35, handle clearance ≥ 30 for fingers, edges
  contacting skin R2+, tip-over check for standing objects (CG inside ⅓ of
  base footprint).
