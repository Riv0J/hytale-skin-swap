# Skin Swap — CurseForge Upload Guide

## Metadata del proyecto

| Campo | Valor |
|---|---|
| **Nombre** | Skin Swap |
| **Versión** | 1.0.1 |
| **Autor** | Rivito |
| **Categoría** | Server-Side Mods / Utility |
| **Compatibilidad** | Hytale Server `2026.02.19-1a311a592` |
| **Archivo a subir** | `build/libs/SkinSwap-1.0.1.jar` |

---

## Descripción (para CurseForge)

> Lets authorized players clone, save, and apply custom player skin appearances.

### Features
- `/ss clone <name>` — Capture your (or another player's) current skin and save it as a named preset
- `/ss list` — View all saved skin presets
- `/ss swap <name>` — Apply a saved skin preset to your character
- `/ss delete <name>` — Remove a saved skin preset
- Each skin is stored as a Model asset (`SS_MODEL_<name>.json`) + skin data (`SS_SKIN_<name>.json`) under `exports/skins/<name>/`
- Fully permission-controlled — no access for regular players by default