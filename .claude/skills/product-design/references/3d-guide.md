# 3D Modeling & Export Guide

## OpenSCAD conventions

```scad
// ==== PARAMETERS (single source of truth — mirrors the spec table) ====
box_w   = 120;   // mm — outer width
box_d   = 80;    // mm — outer depth
wall    = 2.4;   // mm — FDM: 3 perimeters @ 0.8
fillet  = 3;     // mm — outer edge fillet
$fn     = 96;    // production smoothness (use 32 while iterating)

exploded = 0;    // 0 = assembled, 1 = exploded view

module base() { /* ... */ }
module lid()  { /* ... */ }

// ---- assembly ----
base();
translate([0, 0, exploded ? 60 : 0]) lid();
```

- One `module` per part; parts must be individually exportable
  (`openscad -D 'exploded=0' -o part.stl` with only that module enabled, or
  use a `part = "base";` selector variable).
- Fillets: `minkowski()` with a sphere for outer shells, or `offset(r=...)` on
  2D profiles before `linear_extrude` (much faster).
- Model clearances explicitly: a lid that slips over a base is modeled with
  the real gap (e.g. 0.3mm/side for FDM), not line-to-line.

## Headless export (openscad CLI)

```bash
openscad -o model.stl model.scad
# PNG renders — camera: translate_x,y,z,rot_x,rot_y,rot_z,distance
openscad -o iso_front.png --imgsize 1400,1050 --viewall --autocenter \
         --camera 0,0,0,60,0,35,340 --colorscheme Tomorrow model.scad
openscad -o iso_back.png  --imgsize 1400,1050 --viewall --autocenter \
         --camera 0,0,0,60,0,215,340 --colorscheme Tomorrow model.scad
openscad -o top.png       --imgsize 1400,1050 --viewall --autocenter \
         --camera 0,0,0,0,0,0,340 --colorscheme Tomorrow model.scad
openscad -o exploded.png  --imgsize 1400,1050 --viewall --autocenter \
         --camera 0,0,0,60,0,35,420 -D 'exploded=1' --colorscheme Tomorrow model.scad
```

If `openscad` is missing: `sudo apt-get install -y openscad` (needs network).

## Fallback A — Python mesh (trimesh)

`pip install trimesh numpy manifold3d` then build with booleans:

```python
import trimesh
outer = trimesh.creation.box((120, 80, 40))
inner = trimesh.creation.box((120-4.8, 80-4.8, 40))
inner.apply_translation((0, 0, 2.4))
shell = outer.difference(inner)          # needs manifold3d
assert shell.is_watertight
shell.export("model.stl")
# renders (may need pyglet/offscreen; skip renders if headless GL fails)
```

`trimesh` can also render PNGs via `scene.save_image()`, but headless GL often
fails in containers — treat renders as optional here and rely on the SVG
isometric view instead.

## Fallback B — pure-Python binary STL writer (zero deps)

Works everywhere. Build triangles yourself (boxes, extrusions, revolves are
easy to triangulate) and write:

```python
import struct

def write_stl(path, tris):
    """tris: list of ((v0),(v1),(v2)) tuples, CCW when viewed from outside."""
    with open(path, "wb") as f:
        f.write(b"\0" * 80)
        f.write(struct.pack("<I", len(tris)))
        for a, b, c in tris:
            u = [b[i]-a[i] for i in range(3)]
            v = [c[i]-a[i] for i in range(3)]
            n = (u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0])
            l = max((n[0]**2+n[1]**2+n[2]**2) ** 0.5, 1e-12)
            f.write(struct.pack("<3f", *(x/l for x in n)))
            for p in (a, b, c):
                f.write(struct.pack("<3f", *p))
            f.write(b"\0\0")
```

Helper patterns: a box = 12 triangles; an extruded polygon = fan-triangulated
caps + side quads split in two; a cylinder = polygon with 64 segments.
Validate by re-reading the header count and checking min/max bounds match the
parameter table.

## Inline STL viewer in the artifact (optional)

CSP forbids CDN three.js. If you include a viewer, hand-roll it:
- Embed the binary STL as base64 in a `<script type="text/plain">` block;
  decode with `atob` + `DataView`.
- Render with raw WebGL: one vertex+fragment shader pair, flat shading from
  per-face normals (already in the STL), drag-to-orbit via two rotation
  angles, wheel-to-zoom. ~150 lines total.
- Degrade gracefully: wrap in try/catch and show the PNG renders (or the SVG
  isometric) if WebGL is unavailable. Never let the viewer break the page.
