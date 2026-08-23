# Dead Air — Pip-Boy Radio Conversion

Optional companion mod for [Dead Air](https://www.curseforge.com/minecraft/mc-mods).

Adds **T1/T2 Pip-Boy radios** (five colours) **alongside** stock Dead Air radios — never replaces them.

**Requires Dead Air 2.0.6+ and Extra Special Core 1.2.3+ (hard dependencies).** PipBoyScreen is an ESC UI (`EscTabBar` + Dead Air radio panels).

## Features (1.0.0)

- **Off-hand equip** — right-click wears the Pip on the **left wrist**; main hand stays free for tools. (`F` also swaps to off-hand.)
- **Live wrist LCD** on both T1 and T2 Pip while worn (station / signal / time), corner HUD suppressed.
- **PipBoyScreen** on tune (N / right-click): framed Pip chrome, arm raises toward camera.
- **RADIO tab** = same Power / Ping / tune controls as stock (T2 also Link/Unlink).
- **Tab shell** for future windows: `SPECIAL` | `INV` (stubs — “Coming soon”).

## How to get one (alternate to T1.Radio — same ease)

| Path | Detail |
|------|--------|
| **Call Airdrop catalog** | Light Green T1 — **5 points** (same as T1.Radio) |
| **Craft** | Light Green T1: leather + redstone + Static Note (backup / extras) |
| **Tower structure crates** | Guaranteed Light Green T1 (with RadioTowers) |
| **Recolour** | Any Pip + dye |
| **T2 Link** | Same shaped craft as T2.Radio, with that colour's Pip T1 + Dimensional Relay |

Airdrop *delivery* crates only contain what you picked in the Call Airdrop GUI — no random bonus radios.

### Craft patterns

**T1 (Light Green):**
```
L R L
R N R
L R L
```
L = leather, R = redstone, N = Static Note

**T2 (any colour):** same as Dead Air T2.Radio (`GRG / IWI / III`) with W = that colour's Pip T1.

### Dye map

| Colour | Dye |
|--------|-----|
| Light Green | Lime |
| Dark Green | Green |
| Grey | Gray |
| Dark Blue | Blue |
| Dark Brown | Brown |

## Extension points (scaffold)

Companion mods can later fill PipBoyScreen tabs:

| Tab | Intended use |
|-----|----------------|
| **RADIO** | Built-in — Dead Air tuning (this mod) |
| **SPECIAL** | Skills / SPECIAL-style window (future skills companion — not started) |
| **INV** | Inventory / craft mirror (future — not started) |

Hook pattern: open `PipBoyScreen`, call `setTab(PipBoyTab.…)`, or register content via a future API. Stock Dead Air radios stay on `WalkieTalkieTuningScreen`.

## Branding

Post-collapse Pip-Boy-style radio unit — not an official Fallout product.
