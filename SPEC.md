# Chrissy's Crochet App Specification (Clean-Room Extraction)

## Anchor Drift Note
- Code anchors (file + line numbers) were verified as of `2026-02-10`.
- Function/type names are canonical anchors if line numbers drift in future edits.

## 1) App Overview
- The app is a square pixel-pattern editor for crochet planning with:
- Paint/drag painting on a grid, optional symmetry mirroring, replace-by-color workflow, undo/redo history, save/load/delete/export, and palette editing.
- Main implementation root is `MainActivity.onCreate` in `app/src/main/java/com/example/myapplication/MainActivity.kt`.

Primary code anchors:
- App state and orchestration: `MainActivity.onCreate` (`app/src/main/java/com/example/myapplication/MainActivity.kt:116`)
- Grid rendering: `PatternGrid` (`app/src/main/java/com/example/myapplication/PatternGrid.kt:19`)
- Persistence and export: `PatternStorage.kt` functions (`app/src/main/java/com/example/myapplication/PatternStorage.kt`)
- Palette tab name persistence: `PaletteTabPrefs.kt` (`app/src/main/java/com/example/myapplication/PaletteTabPrefs.kt`)
- Palette color parsing/editing helpers: `ColorPalette.kt` (`app/src/main/java/com/example/myapplication/ColorPalette.kt`)

## 2) Data Model and Semantics

### 2.1 Pattern Meaning
- A pattern is a flat list of `Color` values in row-major order.
- Index mapping:
- `row = index / gridSize`, `col = index % gridSize`.
- Grid is always square; expected cell count is `gridSize * gridSize`.

Code references:
- Row/col derivation for drawing/edit: `PatternGrid` (`app/src/main/java/com/example/myapplication/PatternGrid.kt:37`)
- Pattern list + size usage in painting: `onColorChange` (`app/src/main/java/com/example/myapplication/MainActivity.kt:747`)
- Raw load square derivation via sqrt: `deriveSquareSizeOrNull` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:95`)

### 2.2 Palette Index vs Stored Color
- Runtime selected paint color is derived from current tab's hex list and selected slot index (`selectedPaletteTab`, `selectedColorIndex`).
- Pattern cells store actual color values, not palette slot indices.
- Color input normalization contract:
- Accept `#RRGGBB` or `#AARRGGBB`.
- On parse, normalize `#RRGGBB` to canonical `#FFRRGGBB`.
- Persist/save/export using canonical `#AARRGGBB` tokens.
- Save format serializes each cell as canonical ARGB hex (`#AARRGGBB`), comma-separated.

Code references:
- Palette selection state: `MainActivity` (`app/src/main/java/com/example/myapplication/MainActivity.kt:126`)
- `selectedColor` derivation from hex: (`app/src/main/java/com/example/myapplication/MainActivity.kt:134`)
- Save serialization: `colorToHex` + `savePattern` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:43`, `app/src/main/java/com/example/myapplication/PatternStorage.kt:66`)

### 2.3 History
- History is an in-memory list of complete pattern snapshots.
- Max history depth is 50 snapshots.
- New edits truncate redo branch (`take(historyIndex + 1)`), append snapshot, then cap size.
- Reset operations (new/load/size change) replace history with one snapshot.

History truncation pseudocode:
```text
if reset:
  history = [newPattern]
  historyIndex = 0
else:
  branch = history.take(historyIndex + 1)
  appended = branch + [newPattern]
  history = appended.takeLast(50)
  historyIndex = history.lastIndex
```

Code references:
- `history`, `historyIndex`, `pattern`: (`app/src/main/java/com/example/myapplication/MainActivity.kt:137`)
- Update behavior: `updatePattern` (`app/src/main/java/com/example/myapplication/MainActivity.kt:197`)

## 3) Screen and UI Behavior

### 3.1 Main Editor Screen
- Contains top action bar, size selector row, central grid workspace, replace/mirror overlays, bottom navigation, optional splash overlay.
- Workspace supports pan/zoom gestures and pointer-to-cell mapping.
- Grid sizes exposed in UI: `21`, `25`, `29`, `36`.

Code references:
- Top-level structure: `Surface` + `Scaffold` and inner `Column` (`app/src/main/java/com/example/myapplication/MainActivity.kt:655`)
- Size buttons: (`app/src/main/java/com/example/myapplication/MainActivity.kt:733`)
- Grid workspace gesture/pointer mapping: (`app/src/main/java/com/example/myapplication/MainActivity.kt:812`)

### 3.2 Palette Bottom Sheet
- Opened via bottom navigation "Palette".
- Shows tab chips (4 tabs), rename tabs action, edit-hex action, and color swatches.
- Selecting a tab preserves current hex if present in target tab, else falls back to index `0`.

Code references:
- Sheet visibility and content: (`app/src/main/java/com/example/myapplication/MainActivity.kt:1077`)
- Tab switch and selected-index carryover logic: (`app/src/main/java/com/example/myapplication/MainActivity.kt:1123`)
- Palette grid component: `ColorPalette` (`app/src/main/java/com/example/myapplication/ColorPalette.kt:103`)

### 3.3 Saved Patterns Dialog
- Opened by top-bar load icon.
- Lists saved patterns with thumbnail + name.
- Card click starts load confirmation flow.
- Per-card close icon opens delete confirmation flow.

Code references:
- Dialog content and list: (`app/src/main/java/com/example/myapplication/MainActivity.kt:562`)
- Thumbnail source is `"{name}_thumb.png"`: (`app/src/main/java/com/example/myapplication/PatternStorage.kt:203`)

## 4) User Flows

### 4.1 Paint (Tap)
- In brush mode, tap inside mapped cell writes selected color (plus symmetry effects).
- In replace mode with "pick source" armed, tap captures source color from tapped cell and disarms pick.

Code references:
- Tap mapping and behavior: (`app/src/main/java/com/example/myapplication/MainActivity.kt:866`)

### 4.2 Drag Painting
- In brush mode, drag paints traversed cells.
- Deduplicates repeated writes to same cell during one drag (`lastIndex`).

Code references:
- Drag gesture handling: (`app/src/main/java/com/example/myapplication/MainActivity.kt:831`)

### 4.3 Replace Tool
- Toggle Replace from bottom nav.
- "Pick Source" arms source-pick mode; next tap picks source color.
- "Apply Replace" asks confirmation, then replaces all exact matches of source with current selected target color.

Replace state machine:

| Current mode | Source state | Event | Next mode | Next source state |
| --- | --- | --- | --- | --- |
| Brush | None | Switch to Replace | Replace | Unarmed |
| Replace | Unarmed | Pick Source | Replace | Armed |
| Replace | Armed | Tap valid cell | Replace | Source selected |
| Replace | Source selected | Apply Replace OK | Replace | Source selected |
| Replace | Any | Switch away from Replace | Brush | Cleared |
| Replace | Any | Undo | Replace | Cleared |
| Replace | Any | Redo | Replace | Cleared |
| Replace | Any | New pattern | Replace or Brush (unchanged mode choice) | Cleared |
| Replace | Any | Load pattern (success, mismatch dialog path, or error path) | Replace or Brush (unchanged mode choice) | Cleared |
| Replace | Any | Change grid size | Replace or Brush (unchanged mode choice) | Cleared |

Code references:
- Tool mode state and toggle: (`app/src/main/java/com/example/myapplication/MainActivity.kt:1027`)
- Replace panel status/actions: (`app/src/main/java/com/example/myapplication/MainActivity.kt:947`)
- Replace application logic: `applyReplace` (`app/src/main/java/com/example/myapplication/MainActivity.kt:212`)

### 4.4 Mirror Tool
- Available only in brush mode.
- Modes:
- `Off` (`NONE`), `H` (`HORIZONTAL`), `V` (`VERTICAL`), `Both` (`QUADRANT`).
- Odd-grid explicit rules for size `N` with `c = floor(N/2)`:
- `HORIZONTAL`: if `row == c`, do not mirror; single write only.
- `VERTICAL`: if `col == c`, do not mirror; single write only.
- `QUADRANT`: if `row == c` or `col == c`, do not mirror across that centerline; write only unique cells (no duplicates).

Concrete examples for `N=21` (`c=10`):
- `VERTICAL`, paint `(row=4,col=3)` -> affected cells: `(4,3)`, `(4,17)`.
- `VERTICAL`, paint `(row=4,col=10)` -> affected cells: `(4,10)` only.
- `QUADRANT`, paint `(row=10,col=6)` -> affected cells: `(10,6)`, `(10,14)` only.
- `QUADRANT`, paint `(row=3,col=5)` -> affected cells: `(3,5)`, `(3,15)`, `(17,5)`, `(17,15)`.

Code references:
- Mirror options UI: (`app/src/main/java/com/example/myapplication/MainActivity.kt:985`)
- Symmetry write rules: (`app/src/main/java/com/example/myapplication/MainActivity.kt:764`)

### 4.5 Palette Tabs + Rename + Edit Hex
- Four tab labels default to `Pastel`, `Metallic`, `Bright`, `Custom`.
- Rename dialog enforces non-blank trimmed names, max length 14, persisted in SharedPreferences.
- Edit Hex accepts `#RRGGBB` or `#AARRGGBB`, normalizes to uppercase with leading `#`.
- Invalid input blocks confirm.
- On valid edit:
- Updates current tab cell.
- If edited color is not already in custom palette (or current custom list in custom tab case), appends to custom list.

Code references:
- Defaults and validation rules: `PaletteTabPrefs.kt` (`app/src/main/java/com/example/myapplication/PaletteTabPrefs.kt:5`)
- Rename dialog: (`app/src/main/java/com/example/myapplication/MainActivity.kt:451`)
- Hex normalize/parse: `normalizeHexInput`, `hexToColor` (`app/src/main/java/com/example/myapplication/ColorPalette.kt:137`)
- Edit Hex dialog apply behavior: (`app/src/main/java/com/example/myapplication/MainActivity.kt:395`)

### 4.6 Save
- Save action:
- If filename is blank, prompts "Name Pattern".
- Otherwise saves immediately.
- Naming does not overwrite existing files; collisions become `"Name (n)"`.
- Also writes PNG thumbnail next to text file.

Code references:
- Top-bar save behavior: (`app/src/main/java/com/example/myapplication/MainActivity.kt:682`)
- Save dialog: (`app/src/main/java/com/example/myapplication/MainActivity.kt:512`)
- Non-overwrite naming: `nextAvailablePatternName` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:53`)
- Save IO and thumbnail creation: (`app/src/main/java/com/example/myapplication/PatternStorage.kt:66`)

### 4.7 Load
- Selecting a saved card requires confirmation ("Loading will replace current pattern.").
- If loaded pattern size equals current grid size: load directly.
- If size mismatch: second dialog offers:
- Switch current grid to loaded size and load exact raw colors.
- Center-transform raw pattern onto current grid (pad when current bigger, crop when current smaller), using white pad.
- Cancel.

Code references:
- Load confirmation dialog: (`app/src/main/java/com/example/myapplication/MainActivity.kt:289`)
- Raw load + mismatch branch: `loadPatternByName` (`app/src/main/java/com/example/myapplication/MainActivity.kt:269`)
- Mismatch dialog actions: (`app/src/main/java/com/example/myapplication/MainActivity.kt:316`)
- Center transform rules: `transformPatternCenter` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:137`)

### 4.8 Delete
- Delete action is per-saved-pattern in load dialog.
- Requires confirmation.
- Deletes both `name.txt` and `name_thumb.png` (missing file counts as success for each file).
- If deleted name matches current `filename`, clears current filename.

Code references:
- Delete confirmation dialog: (`app/src/main/java/com/example/myapplication/MainActivity.kt:363`)
- Delete implementation: `deleteSavedPattern` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:208`)

### 4.9 Export JPG (Pattern-Only)
- Exports current pattern bitmap scaled by `10x` cell size.
- Exported content is only cell color blocks (no UI chrome, no gridline overlay).
- Name uses current filename or timestamp fallback `pattern_yyyyMMdd_HHmmss` (device local time, 24-hour).
- Export collision handling: if target filename already exists, append `_1`, `_2`, etc.
- Writes to Pictures via MediaStore on Android Q+, or direct Pictures file path on older versions.

Code references:
- Export trigger and timestamp fallback: (`app/src/main/java/com/example/myapplication/MainActivity.kt:243`)
- Bitmap generation (cell rectangles only): `createPatternBitmap` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:16`)
- JPG write path behavior: `exportPatternToJpg` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:219`)

## 5) Edge Cases and Deterministic Rules

### 5.1 Size Mismatch Load
- Deterministic options:
- Switch-size mode: set `gridSize = raw.size` then load exactly.
- Center mode: call `transformPatternCenter(raw, currentSize, Color.White)`.

Deterministic transform details:
- Pad offset for larger destination: `(dst - src) / 2` using integer floor.
- Crop start for smaller destination: `(src - dst) / 2` using integer floor.

Code references:
- Dialog action wiring: (`app/src/main/java/com/example/myapplication/MainActivity.kt:327`)
- Transform implementation and offset rule comments: (`app/src/main/java/com/example/myapplication/PatternStorage.kt:128`)

### 5.2 Naming Collisions
- Save never overwrites an existing `name.txt`.
- First collision becomes `name (1)`, then increments until unused.
- Empty trimmed requested name is returned unchanged by helper; caller-side UI blocks blank save from dialog.
- Export never overwrites an existing JPG.
- First export collision becomes `name_1.jpg`, then increments to `name_2.jpg`, etc.

Code references:
- Collision algorithm: `nextAvailablePatternName` (`app/src/main/java/com/example/myapplication/PatternStorage.kt:53`)
- Dialog-side blank-name guard: (`app/src/main/java/com/example/myapplication/MainActivity.kt:527`)

### 5.3 Delete Confirmation and Semantics
- Delete requires explicit confirmation.
- Missing files are treated as already-deleted for success calculation.

Code references:
- UI confirmation: (`app/src/main/java/com/example/myapplication/MainActivity.kt:363`)
- Boolean deletion logic: (`app/src/main/java/com/example/myapplication/PatternStorage.kt:214`)

### 5.4 Load Error Handling
- Error conditions:
- Corrupted/invalid hex tokens.
- Non-square color count.
- Empty pattern file.
- Read/IO failure.
- Required behavior on load error:
- Show an error message (exact string may be TBD).
- Close load flow dialogs.
- Keep current pattern unchanged.
- Keep history unchanged.
- Keep current grid size unchanged.

Code references:
- Error/null return points from raw loader: (`app/src/main/java/com/example/myapplication/PatternStorage.kt:105`)
- Caller-side load failure branch: (`app/src/main/java/com/example/myapplication/MainActivity.kt:271`)

### 5.5 Undo/Redo Scope
- Create history snapshots:
- Brush paint tap.
- Brush drag paint.
- Mirror paint write (all affected cells recorded as one snapshot).
- Replace operation (all affected cells recorded as one snapshot).
- Reset history:
- New pattern.
- Load pattern.
- Change grid size.
- Not recorded in history:
- Palette selection changes.
- Tool mode changes.
- Mirror mode toggles.
- Palette tab switching/rename.
- Edit hex in palette.

## 6) Storage Contracts (Human Summary)
- Pattern files:
- Directory: app internal `filesDir/patterns`.
- Main file: `<name>.txt`, content = comma-separated ARGB hex cells.
- Thumbnail: `<name>_thumb.png`.
- Palette tab names:
- SharedPreferences file name: `palette_tab_names`.
- Keys: `palette_tab_name_0` ... `palette_tab_name_3`.
- Values sanitized as trimmed non-empty strings, max length 14.
- Custom palette colors:
- Runtime list exists and can be appended during Edit Hex flow.
- Persistent storage for custom palette colors is not implemented in current code; see `contracts/custom_palette.schema.json` for future contract target.
- Custom palette cap: uncapped in v1 (no explicit max size in spec).

Code references:
- Pattern storage pathing: (`app/src/main/java/com/example/myapplication/PatternStorage.kt:68`)
- Palette pref file usage: (`app/src/main/java/com/example/myapplication/MainActivity.kt:167`)
- Key generation and sanitize: (`app/src/main/java/com/example/myapplication/PaletteTabPrefs.kt:10`)

## 7) UI String Appendix (Canonical Spec Text)

### 7.1 Delete Pattern Confirmation
- Title: `Delete Pattern`
- Body: `Delete this pattern?`
- Buttons: `Cancel` / `Delete`

### 7.2 Load Pattern Confirmation
- Title: `Load Pattern`
- Body: `Loading will replace the current pattern.`
- Buttons: `Cancel` / `OK`

### 7.3 Size Mismatch Load Dialog
- Title: `Pattern Size Mismatch`
- Body: `Loaded {src}x{src}. Current is {dst}x{dst}.`
- Buttons:
- `Cancel`
- `Switch Size` (switch editor size to loaded size and load)
- `Center And Load` (center pad/crop onto current size)

### 7.4 Replace Tool Text
- Confirmation title: `Apply Replace`
- Confirmation body: `Replace all matching colors in the pattern?`
- Confirmation buttons: `Cancel` / `OK`
- Status text (unarmed): `Pick a source color.`
- Status text (armed): `Tap a cell to pick source.`
- Status text (source selected): `Source set. Choose target and apply.`

### 7.5 New Pattern Confirmation
- Title: `New Pattern`
- Body: `This will clear the current pattern.`
- Buttons: `Cancel` / `OK`

### 7.6 Load Error Messages
- Corrupt/invalid hex: `Could not load pattern. Invalid color data.`
- Non-square color count: `Could not load pattern. Pattern is not square.`
- Empty file: `Could not load pattern. Pattern is empty.`
- Read failure: `Could not load pattern. Read failed.`
