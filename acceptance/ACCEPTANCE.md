# Acceptance Checklist

## Core Editor
- [ ] App starts with a square `36x36` white pattern.
- [ ] Grid tap in Brush mode paints one cell with selected color.
- [ ] Drag in Brush mode paints traversed cells and does not repeatedly repaint the same cell many times in one continuous drag.
- [ ] Undo moves to previous snapshot; Redo moves forward; changing pattern after undo drops redo branch.
- [ ] History depth is capped (older entries eventually fall off).

## Grid Size and New
- [ ] Switching size to `21/25/29/36` resets to a white pattern of the selected size.
- [ ] New Pattern confirmation clears current pattern and filename.

## Mirror Tool
- [ ] Mirror options are only available when Brush tool is active.
- [ ] Vertical mirror paints mirrored column.
- [ ] Horizontal mirror paints mirrored row.
- [ ] Both mirror paints all quadrant mirrors, with correct odd-center handling.

## Replace Tool
- [ ] Replace mode does not paint directly on tap.
- [ ] "Pick Source" arms source pick; next valid tap captures source and disarms.
- [ ] "Apply Replace" asks confirmation and then replaces all exact source-color matches with selected color.

## Palette and Hex
- [ ] Palette sheet opens and closes from bottom navigation.
- [ ] Tab switching attempts to keep current selected hex if it exists in target tab; otherwise selected index becomes `0`.
- [ ] Rename tabs dialog enforces non-empty trimmed names and max length 14.
- [ ] Saved tab labels persist across app restarts.
- [ ] Edit Hex accepts `#RRGGBB` and `#AARRGGBB`, rejects invalid text.
- [ ] Valid edited color updates current tab slot.
- [ ] New valid edited color is appended into custom palette if not already present per current logic.

## Save/Load/Delete
- [ ] Save with blank filename opens naming dialog.
- [ ] Save with a colliding name creates `name (1)`, `name (2)`, etc., without overwriting.
- [ ] Saved patterns appear in load dialog with name and thumbnail where available.
- [ ] Load requires confirmation before replacing current content.
- [ ] Delete from saved list requires confirmation and removes both pattern text and thumbnail.

## Size Mismatch Load
- [ ] When loading mismatched size, mismatch dialog appears.
- [ ] "Switch to NxN and load" sets editor size to loaded size and loads raw content.
- [ ] "Center on current size" applies deterministic center pad/crop transform.
- [ ] Center transform uses floor-half-delta offset rule.

## Export
- [ ] Export creates JPG in Pictures with current filename or timestamp fallback.
- [ ] Export output is pattern-only bitmap (no toolbars/overlays).

## Known Current Bugs (Not Spec)
- Ghost/horizontal line artifacts outside intended grid bounds.
- Occasional background-region visual weirdness around workspace.
- Any clean-room rebuild should treat these as defects to avoid, not intended behavior.
