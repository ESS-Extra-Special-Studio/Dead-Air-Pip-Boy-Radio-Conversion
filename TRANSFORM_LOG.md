# Pip-Boy display transform log — ESA canonical

**Status:** transforms still being player-tuned (TP orientation / LCD / STAT doll).  
This file is the seed for **Extra Special Anchor (ESA)** defaults.

**ESA product vision (docs only — not this pass):** Kerbal-style in-game gizmos — pick model → pick wrist/anchor → set FP / FP+gun / TP per context → Save Profile → reuse (e.g. right wrist). Full write-up: [`ESS_FRAMEWORK.md` § ESA](../ExtraSpecialCore/ESS_FRAMEWORK.md).

Mod: `dead_air_pip_boy_radio_conversion`  
Model: `geo/item/pip_radio.geo.json` (sleeve **+Y**, screen west **−X**, root bone Y=90)  
Code: `PipWristRender`, `PipLcdRects`, `PipOffhandArmHandler`, `PipRadioItemRenderer`

---

## Why Blockbench alone fails (ESA design constraint)

| Space | What it is |
|-------|------------|
| Blockbench Display | Static preview on a fake arm |
| Vanilla FP runtime | Predictable off-hand item + walk bob |
| TACZ FP runtime | **Overrides left-arm animation**; support arm = `lefthand_pos` / `HumanoidArm.LEFT` (RIGHT is grip) |

ESA must attach to the **final PoseStack parent** for each POI, not Display-tab numbers.

---

## POI registry (LOCKED)

### 1. `fp_empty_left` — first person, no gun

| Field | Value |
|-------|--------|
| **Status** | LOCKED perfect |
| **When** | Off-hand Pip, main hand not a gun mod item |
| **Parent** | Vanilla off-hand item path (`FIRST_PERSON_LEFT_HAND`) + injected left arm |
| **Draw** | Locked display JSON + `PipRadioItemRenderer` rolls; walk-bob **undone** for this hand only |
| **LCD** | `pip_lcd_<skin>`, `paintRotateDegrees = -90` |

**Display JSON `firstperson_lefthand`:** rotation `[-8, 80, -18]`, translation `[-2.6, 3.25, 1.25]`, scale `0.7`  

**Renderer extras:** YP `−90°`, ZP `180°`, peek Y `+0.12`

**Compat notes:** `PipOffhandArmHandler.undoWalkBob` — right hand keeps vanilla sway.

---

### 2. `fp_gun_tacz_left` — first person + TACZ (or gun mod)

| Field | Value |
|-------|--------|
| **Status** | LOCKED perfect |
| **When** | Off-hand Pip + main hand gun (`tacz` / `cgm` / `tac` / soft `IGun`) |
| **Parent** | TACZ support/reload arm after `lefthand_pos` — **LEFT only** (`GUN_FP_SUPPORT_ARM`) |
| **Draw** | Cancel off-hand item; `PipWristRender` after arm mixin; `ItemDisplayContext.NONE` |
| **LCD** | `pip_lcd_wrist_<skin>`, `paintRotateDegrees = 90` (CCW; was −270 / upside-down on cuff) |

**Arm-local translate (after parent, before ZP flip):**

```
OUTWARD     =  0.40     // + = screen-left toward support wrist
ALONG_ARM   =  0.55     // + = toward hand; − = toward elbow (hand sticks out)
THICKNESS   = -0.01     // + = down to floor; − = up to ceiling
FLIP        =  ZP 180°  // after translate (YP/XP 180 were wrong)
SCALE       =  0.80
```

**Gun FP axis dictionary (confirmed in-game — do not invert):**

| You say | Constant change |
|---------|-----------------|
| left (screen) | increase `OUTWARD` |
| right (screen) | decrease `OUTWARD` |
| toward hand / away from player | increase `ALONG_ARM` |
| up the arm / toward elbow / hand sticks out | decrease `ALONG_ARM` |
| **down toward floor** | **increase `THICKNESS`** |
| **up toward ceiling** | **decrease `THICKNESS`** |

Proof: more-negative `THICKNESS` moved the Pip **up**, not down.

**TACZ animation compat (required for ESA):**

1. Detect gun (namespace + soft `IGun`).
2. **Cancel** vanilla/GeckoLib off-hand item draw for Pip.
3. Hook **after** TACZ draws the LEFT FP arm (`TaczRenderHelperMixin` / `PlayerRenderer.renderLeftHand` RETURN).
4. Parent Pip to that PoseStack (already includes TACZ arm anim).
5. Apply POI offsets in **arm-local** space; do **not** use Display JSON.
6. Never parent to RIGHT in gun FP — that is the grip hand.
7. Optional raise-when-GUI-open is a separate blend on this POI (`applyRaiseIfOpen`).

---

### 3. `tp_left_wrist` — third person

| Field | Value |
|-------|--------|
| **Status** | LOCKED perfect |
| **When** | Off-hand Pip, third-person player render |
| **Parent** | Anatomical `leftArm` via `PipWristLayer` (`translateAndRotate`) |
| **Draw** | Palm `ItemInHandLayer` Pip cancelled; wrist layer only; `NONE` context |
| **LCD** | same wrist atlas as gun FP: `paintRotateDegrees = 90` |

**Arm-local (after `leftArm.translateAndRotate`):**

```
OUTWARD     =  0.045
ALONG_ARM   =  0.34
THICKNESS   =  0.010
WRIST_ROLL  =  +3.5° YP
SCALE       =  1.02
```

**TP axis dictionary:**

| You say | Constant change |
|---------|-----------------|
| outward from torso | increase `OUTWARD` |
| inward to torso | decrease `OUTWARD` |
| toward hand (down the arm) | increase `ALONG_ARM` |
| toward shoulder (up the arm) | decrease `ALONG_ARM` |
| **forward** (in front of body) | **decrease `THICKNESS`** (Z−) |
| **backward** | **increase `THICKNESS`** (Z+) |
| enlarge | increase `SCALE` |

---

## Live LCD paint (per-POI)

| POI | liveId pattern | degrees | Blit meaning |
|-----|----------------|---------|--------------|
| `fp_empty_left` | `dynamic/pip_lcd_<skin>` | **−90** | CW 90 — LOCKED |
| `fp_gun_tacz_left` | `dynamic/pip_lcd_wrist_<skin>` | **90** | CCW90 only — was −270; that painted LCD upside-down (2026-08-09 recheck) |
| `tp_left_wrist` | (same wrist liveId) | **90** | same |

**Lessons:** empty and wrist **must not share** one live texture/rotation. Idle-only flips do not fix listening HUD — change `paintRotateDegrees` on the wrist Spec. `−270` (CCW90+flip180) looked upright for early note-icon tests but reads upside-down for current TP/gun cuff.

Implemented in Dead Air `RadioLiveDisplay` (`0`, `±90`, `180`, `±270`).

---

## ESA import sketch

```json
{
  "modId": "dead_air_pip_boy_radio_conversion",
  "modelId": "pip_radio",
  "pois": [
    {
      "poiId": "fp_empty_left",
      "parent": "vanilla_offhand_fp",
      "hostAnim": "vanilla",
      "lcdPaintDegrees": -90,
      "notes": "locked JSON + rolls; undo walk bob on left only"
    },
    {
      "poiId": "fp_gun_tacz_left",
      "parent": "tacz_lefthand_pos",
      "hostAnim": "tacz",
      "humanoidArm": "LEFT",
      "translate": [0.40, 0.55, -0.01],
      "rotateDeg": [180, 0, 0],
      "rotateAxisOrder": "ZP_after_translate",
      "scale": 0.80,
      "lcdPaintDegrees": 90,
      "axisHints": {
        "thicknessPositive": "down_toward_floor",
        "alongPositive": "toward_hand"
      }
    },
    {
      "poiId": "tp_left_wrist",
      "parent": "player_model_leftArm",
      "hostAnim": "vanilla_tp",
      "translate": [0.045, 0.34, 0.010],
      "rotateDeg": [0, 3.5, 0],
      "scale": 1.02,
      "lcdPaintDegrees": 90,
      "axisHints": {
        "thicknessNegative": "forward",
        "alongPositive": "toward_hand"
      }
    }
  ]
}
```

---

## Large GUI + idle mini LCD (Fallout pass)

- **Shell:** `PipBoyScreen` — tabs `STAT | INV | MAP | RADIO | SPECIAL` via `EscTabBar.Style.falloutPip()`; default **STAT**.
- **STAT:** `PipBoySilhouetteRenderer` (green walk-in-place player) + HP / LEVEL+XP footer.
- **RADIO:** embeds Dead Air `WalkieTalkieTuningScreen` body (unchanged controls).
- **INV / MAP / SPECIAL:** Coming soon stubs.
- **Wrist LCD idle:** silhouette via `RadioLiveDisplay.setIdlePainter` (Pip liveIds only).
- **Wrist LCD listening:** existing `paintHud` (NOW PLAYING path). T2 walkie unchanged.
- **CRT helpers:** ESC `EscFalloutDraw` / `EscUiStyle.falloutPip()`.

---

## Version

Keep `mod_version=1.0.0` until user asks to bump.
