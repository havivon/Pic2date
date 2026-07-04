# Outlook Add-in Toolbar Icons

Fluent UI style toolbar icons for the Microsoft Outlook add-in.

## Icons

| File | Meaning |
|------|---------|
| `reply-forward.svg` | Reply + Forward — envelope with a two-way arrow (single left chevron = reply, right chevron = forward) |
| `reply-all-forward.svg` | Reply All + Forward — envelope with a two-way arrow (double left chevron = reply all, right chevron = forward) |
| `*-dark.svg` | Dark-theme variants (light neutral envelope for dark Outlook themes) |

## Design

- Flat, minimal vector style following Microsoft Fluent UI icon language
- Stroke-based, rounded caps and joins, no gradients or shadows
- Two visual elements only (envelope + arrow motif) for 16 px readability
- Transparent background, 32×32 viewBox, scales cleanly to any square size

## Colors

| Role | Light theme | Dark theme |
|------|-------------|------------|
| Envelope base | `#323130` | `#F3F2F1` |
| Arrow accent | `#5EA9FF` | `#5EA9FF` |

## PNG exports

`png/` contains raster exports at the sizes the Outlook add-in manifest
expects (16, 32, 80 px), e.g.:

```xml
<Icon>
  <bt:Image size="16" resid="Icon.16x16"/>
  <bt:Image size="32" resid="Icon.32x32"/>
  <bt:Image size="80" resid="Icon.80x80"/>
</Icon>
```

Prefer the SVGs wherever the host supports them; use the PNGs for the
manifest `IconUrl` / `HighResolutionIconUrl` resources.
