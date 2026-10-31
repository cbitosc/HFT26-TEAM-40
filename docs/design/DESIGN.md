# Lynq UI Design — Apple Minimalist & Obsidian Knowledge Graph

Status: Redesigned and verified on real device at 390×844.

## 1. North Star: "Apple Simplicity, Obsidian Mesh, Zero Noise"

Lynq is an offline-first emergency mesh communication tool. The interface is refined, quiet, and minimalist—built according to Apple iOS Human Interface Guidelines and Oil-UI principles. No noisy borders, no uppercase monospace shouting, no unnecessary boxes. Every element is deliberate and legible under stress.

**Core Philosophy (from Oil-UI):**
- **One Protagonist, One Primary Action per Screen**:
  - *Network (Dashboard)*: Protagonist is the live Obsidian Knowledge Graph & quick Emergency SOS trigger
  - *Messages*: Protagonist is the Apple iMessage-style conversation list and clean thread
  - *Emergency*: Protagonist is the Apple Satellite Emergency SOS trigger and category selector
  - *Mesh Explorer*: Protagonist is the full-screen interactive Obsidian Knowledge Graph (pan, pinch-to-zoom, tap to inspect)
  - *Settings*: Protagonist is the Apple Inset Grouped list (Keystore identity, radio parameters, security)
- **Radical Subtraction**: Only essential information remains. Hairline dividers (0.5dp), subtle 10% white borders, and pure OLED black surfaces.

---

## 2. The Obsidian Knowledge Graph

Inspired by the iconic **Obsidian Graph View**:
- **Canvas Ambiance**: Pure deep space radial gradient (`#14171F` to `#000000`) with ambient cosmic micro-dust.
- **Organic Physics Drift**: Floating nodes gently drifting with natural harmonic motion.
- **Delicate Gravitational Filaments**: Thin translucent threads (1.2dp) connecting nodes.
- **Interactive Gestures**: Pinch-to-zoom (0.6x to 2.8x) and drag-to-pan across the mesh. Tap to inspect nodes.
- **Live SOS Neural Chain**: When an emergency SOS is active, the entire path `You → Aarav → Kabir → Priya` illuminates with a glowing crimson beam (`#FF453A`) and active traveling white energy packets.
- **Floating Note Typography**: Clean SF Pro style labels floating beside each node with high-contrast pips.

---

## 3. Design Tokens (`docs/design/mockups/tokens.css`)

Target Viewport: **390px × 844px** (Standard modern mobile viewport)

| Token | Hex / Value | Semantic Role |
|---|---|---|
| `--bg` | `#000000` | Pure Apple OLED Black |
| `--surface` | `#1C1C1E` | Apple Secondary System Background |
| `--surface-elevated` | `#2C2C2E` | Apple Tertiary System Background |
| `--surface-muted` | `#3A3A3C` | Apple Quaternary Fill |
| `--border` | `rgba(255, 255, 255, 0.10)` | Hairline subtle separator (0.5dp) |
| `--border-subtle` | `rgba(255, 255, 255, 0.05)` | Faint divider line |
| `--text-primary` | `#FFFFFF` | 100% white crisp primary text |
| `--text-secondary` | `#8E8E93` | Apple System Gray secondary labels |
| `--text-muted` | `#636366` | Apple System Gray 2 subtle metadata |
| `--blue` | `#0A84FF` | Apple System Blue (active links, selection) |
| `--green` | `#30D158` | Apple System Green (online, delivered) |
| `--amber` | `#FF9F0A` | Apple System Orange (pending, warning) |
| `--red` | `#FF453A` | Apple System Red (Emergency SOS) |
| `--cyan` | `#32ADE6` | Apple System Cyan |

---

## 4. Typography Hierarchy (Apple SF Style)

- **Large Title**: 32-34sp Bold (`AppleLargeTitle`)
- **Title 2**: 22sp Bold (`AppleTitle2`)
- **Headline**: 17sp SemiBold (`AppleHeadline`)
- **Body**: 16-17sp Regular (`AppleBody`)
- **Subhead**: 15sp Regular (`AppleSubhead`)
- **Footnote**: 13sp Regular (`AppleFootnote`)
- **Caption**: 11sp Medium (`AppleCaption`)
- **Tech Code**: Crisp JetBrains Mono reserved *strictly* for cryptographic hashes, fingerprints, and GPS coordinates.

---

## 5. Clean Transitions & Apple Feel

- **Screen Transitions**: Apple fluid horizontal slide + fade (`FastOutSlowInEasing`, 280ms).
- **Haptic Feedback & Spring Physics**: Interactive buttons scale subtly on press.
- **Tab Bar**: Frosted glass bottom navigation bar (`#121214` with hairline border, 22dp vector glyphs, active blue tint).