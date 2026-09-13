# Changelog

## [unreleased]

- **Fabric: fixed item loss when sorting several identical max-stack-1 items** (reported with iron
  axes / iron leggings / stone shovels: a whole chest of them collapsed to a single item). The
  `InventorySorter` mixin that caps each slot's stack limit at the item's own limit embedded a
  vanilla type in its injection target but was compiled with `remap = false`. Fabric production
  runtimes use intermediary names (`class_1799`), so the injector was never found there: the sorter
  kept using the container's 64-per-slot limit, merged e.g. five axes into one "stack of 5", and the
  container truncated it back to one on write-back — the rest were destroyed. The injector target is
  now remapped (`@At(remap = true)`, keeping the Core method itself unremapped), and the built
  refmap maps `ItemStack` to `class_1799` so the hook actually applies. Forge/NeoForge keep official
  class names and were unaffected.
- **Sort results are now validated before being written back.** Besides the pre-existing "no item
  lost" check (per-item totals unchanged), no result stack may exceed what the input legitimately
  allowed for that item; an oversized stack (exactly the corruption above, which a container would
  truncate) makes the sort keep the originals instead. A legitimate pre-existing oversized stack
  still passes.
- **New config `transferMainInventoryFirst` (default `true`).** When transferring container items
  into the player inventory, the mod now fills the 27-slot main inventory before the 9-slot hotbar,
  matching Sophisticated Core's own transfer. Set it to `false` for the old vanilla quick-move order
  (hotbar first). The choice travels with the transfer request, so client and server agree. Applies to
  all targets.
- **The whole mod is now inert on Sophisticated Backpacks / Sophisticated Storage screens.**
  Those screens keep their own sort/transfer handling (including the no-sort "ignored" slots and
  memory slots), so the sorter no longer triggers its own sort on them from any entry point:
  - Their main screens extend Core's `StorageScreenBase` and their settings screens extend
    `SettingsScreen`; all client entry points (sort key, sort/transfer buttons, the settings gear,
    the disable toggle, slot-highlight decoration and even the search-box tooltip hint) now bail
    out on both.
  - Server-side guard: `CoreUtils.executeSort` / `executeTransfer` skip any open
    `StorageContainerMenuBase` / `SettingsContainerMenu`, so even a mismatched or third-party
    client cannot make the sorter touch a Sophisticated container.
- **Fixed item loss ("swallowing" items) when sorting a high-stack inventory.** The generic sort
  feeds stacks through Core's `InventorySorter`, which places items back at the *vanilla* 64-per-slot
  limit; a high-stack item (from a stack upgrade) then needs many slots, and Core silently drops the
  overflow when there are not enough. The sort result is now verified against the per-item totals
  taken before the sort and is discarded when anything would be lost, so sorting can never destroy
  items. Applies to the shared backend and the 26.1 adapter.
- Fixed the sort/disable keys doing anything at all on a Sophisticated screen, which previously made
  the key open-sort behave like "sort the player inventory".

> Note: the two entries above and the first two entries of this release are the fixes for the
> "sorting swallows items / only one item left" reports. The root causes were distinct:
> 1. **Fabric only** — a mixin lost its obfuscation mapping in production, so max-stack-1 items were
>    merged into one invalid stack and truncated (iron axes / leggings / shovels all reduced to one).
> 2. **All targets** — the generic sort could not represent high-stack items (stack upgrades) or
>    items that needed more slots than were free, and the overflow was dropped.

## [1.1.0-hotfix]

- Forge 1.20.1: fixed a production startup crash. The forge mixin config now declares its
  `refmap`, so mixin targets (e.g. the `quickcraftSlots` accessor) resolve correctly on the
  SRG-named production runtime instead of failing with "No candidates were found".
- Per-screen client options now use a combined **screen class + title key** identifier:
  - Button visibility (disable toggle) and button offset records are stored per screen, so
    containers that share one screen class (chest, barrel, shulker box, trapped chest - all
    `ChestScreen`) no longer share button positions or the hide toggle with each other.
  - The button offset *render/drag* path now uses the same identifier (previously it used the
    screen class only).
  - Matching is backward compatible and loose: a stored entry matches when it equals the full
    `class@title` id, the bare title key (the old format) or the bare screen class, so existing
    config entries keep working.

## [1.1.0] - Container Settings

This release brings the new **Container Settings** system to all supported loaders and versions.
It was originally developed for NeoForge 1.21.1 and is now available on Forge 1.20.1, Fabric 1.20.1,
Fabric 1.21.1, NeoForge 1.21.1 and NeoForge 26.1, with the loader-neutral logic shared from the
common codebase.

### New: container settings
- Per-container settings screen for every usable container (chests, barrels, ...) and the player
  inventory, opened from a new settings button in the top-right button group (the group shifts left
  to fit it). On vanilla container screens the entry is also reachable directly.
- Settings are persisted **server-side** per container (world SavedData, keyed by dimension and
  position), so every container keeps its own memory / no-sort / item-display preferences
  independently of the client and of other containers.
- **Memory slots**: pick a remembered item for any slot; empty memorized slots show the remembered
  item as a translucent ghost in the regular container view, and the server refuses placements into
  the slot that do not match the memorized item.
- **Ignore sorting (no-sort)**: mark slots that sorting must never touch; those slots keep whatever
  they hold.
- **Slot highlights**: the regular container view draws the same color stripes and memory ghosts the
  settings screen uses, so you can see at a glance which slots are special.
- Sorting now respects these settings: no-sort slots stay in place, and memorized slots are emptied
  and refilled with their remembered items during sorting (the classic pre-26.1 rule; see the new
  client config `memorySlotSorting` on 26.1).

### Ports and quality
- The whole settings feature is ported to Forge 1.20.1, Fabric 1.20.1/1.21.1 and NeoForge 26.1,
  adapted to each loader's networking, events, item handlers and Sophisticated Core API.
- Forge 1.20.1: fixed the per-slot highlight stripes not rendering on vanilla container screens
  (wrong `fillGradient` argument order); entering or leaving the settings screen no longer resets
  the mouse cursor to the screen center (menu swaps are closed server-side only, mirroring
  NeoForge's `SophisticatedMenuProvider` behavior).
- NeoForge 26.1: the settings screen now shows the container's actual items (item snapshots travel
  with the menu-open data instead of relying on per-slot sync that 26.1 no longer performs for
  view-only slots); sorting no longer silently does nothing after marking ignore-sort slots; new
  client config `memorySlotSorting` (default `true`) restores the classic memorized-slot refill
  behavior, set it to `false` to use the 26.1 sorter's plain handling.
- Shared key/store/resolver logic moved into the common source set used by all 1.20.1/1.21.1
  targets; language entries synchronized across all targets.

### Notes
- Container contents shown in the settings screen are a snapshot taken when the screen opens.
- `memorySlotSorting` lives in the client config (`sophisticatedsorter-client.toml`) and only
  affects the NeoForge 26.1 target, where Sophisticated Core removed memory-slot handling from its
  own sorter.