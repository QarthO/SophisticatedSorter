# Sophisticated Sorter (精妙整理)

Sophisticated Sorter extends the container sorting (and related) features that come with the Sophisticated
series so they work on the player inventory and on every usable container.

## Main Features

### Sort Button Extension
The mod adds Sophisticated Core's sort buttons to most regular container screens. "Extension" here means
bringing the Sophisticated Core sorting entry point to more containers outside the Sophisticated series,
not adding a second, completely different sorting logic. Sorting still uses Sophisticated Core's four
sorting modes: by name, by mod, by count and by tags, with **by name** as the default mode.

The config option `pinyin` is enabled by default. When enabled, Pinyin sorting is used in Chinese-language
environments; when disabled, that processing is turned off.

One button performs the sort, the other cycles the sort mode. Players already used to the Sophisticated
series' sorting should not need to relearn anything.

### Player Inventory Sorting
The mod can sort the player's own inventory. When the current screen does not qualify as "sort the
container itself", the mod sorts the player inventory instead. So even on crafting tables, furnaces or
recipe-type screens you can still quickly sort the items on your character.

When the current screen does qualify, the mod sorts the container itself rather than the player inventory.
This way ordinary containers such as chests and barrels get the same sorting experience as the
Sophisticated series.

Even on screens that do not display the sort buttons, you can still sort with the hotkey (default: `R`).
The hotkey and the buttons share the same target logic: if the current screen suits sorting the
container, it sorts the container; otherwise it sorts the player inventory.

### Inactive on Sophisticated Backpacks / Sophisticated Storage
The whole mod is inert on Sophisticated Backpacks and Sophisticated Storage screens: the sort hotkey,
the sort buttons, the search box, quick transfer, the settings gear and the slot highlights all do
nothing there. Use the Sophisticated Core sorting (its button or its own hotkey) on those screens.

This matters because Sophisticated Storage supports oversized stacks from stack upgrades, which this
mod's ordinary-container sorting cannot handle safely. Pressing this mod's hotkey on such a screen now
does nothing at all, and can no longer "collapse to a vanilla stack" or swallow items.

### Search & Quick Transfer
Very common functionality; not much to say.

When transferring items into the player inventory, the 27-slot main inventory is filled before the
9-slot hotbar, matching Sophisticated Core. The config option `transferMainInventoryFirst`
(default `true`) switches this: set it to `false` to fill the hotbar first instead (the vanilla
quick-move order).

### Container Filter Rules
Whether the current container is sorted is controlled by two filters:

- `Filter1` (enabled by default): when enabled, only screens with more than `46` slots are treated as a
  sortable container; otherwise sorting falls back to the player inventory. 46 can be read as the
  player's 36 inventory slots plus 10 for a small container. Screens with few slots, such as crafting
  tables or small utility screens, therefore do not sort the container itself by default.
- `Filter2` (enabled by default): when enabled, if the current screen contains any slots that are
  clearly unsuitable for sorting, the screen is not treated as a sortable container. The most typical
  example is special slots such as crafting result slots. This filter exists to keep functional screens
  from accidentally sorting special slots.

On a regular container screen the sort buttons only appear when it satisfies the current filters (and is
not blacklisted). Screens that are not suited for sorting the container usually hide the buttons, but
the hotkey still sorts the player inventory there.

### Button Layout
Buttons can be repositioned by right-click-dragging them, and toggled hidden/shown with the hotkey
(default: `U`).

Button layout is isolated per container type: moving the buttons inside a chest does not affect barrels.

### Container Settings
- **Ignore sorting**: mark a slot range; slots inside that range are never sorted.
- **Slot memory**: assign a fixed item to a specific slot in a container — no other item can be placed
  there, and when sorting, the matching item is placed into that slot first.