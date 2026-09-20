# Optional Sorter clients on Fabric 1.21.1

This fork is intended for the **server**. Players may use the unmodified upstream
SophisticatedSorter Fabric 1.21.1 **1.1.1** client, or omit Sorter entirely.
Other modpack requirements still apply: this does not make Sophisticated Core,
Backpacks, Storage, Cobblemon, or other content mods optional.

Install the fork JAR once in the server's `mods/` directory, replacing any upstream
Sorter JAR. Keep the server's Fabric API, Cloth Config and Sophisticated Core
requirements. The implementation is built against Fabric API 0.116.14+1.21.1,
including its internal registry-sync and common channel negotiation APIs.
Revalidate the connection tests when updating Fabric API.

## Behavior

- Stock upstream clients retain the existing packet IDs, codecs and settings menu.
- Clients without Sorter receive no Sorter settings menu registration or Sorter
  state packets. Their ordinary inventory interactions are not subject to Sorter's
  memory-slot filters, even on a container configured by another player.
- Other mods' registry entries and missing-entry checks remain intact.
- Only the Fabric 1.21.1 target is changed. Fork version:
  `1.1.1+optional-clients.1`.

## Connection implementation

Fabric normally exchanges play channels after its early registry synchronization.
The server performs the same standard `c:version` / `c:register` exchange before
registry sync, using Fabric's existing task keys and receivers. No custom client
handshake or fork client is needed. The normal later exchange remains harmless.
The two upstream receiving channels identify a Sorter-capable connection.

At server startup, Fabric's registry remapper moves the Sorter settings menu to
the end of the menu registry, preserving the order of all other menus and firing
Fabric's remap callbacks. This matters because omitting a middle entry would leave
a raw-ID hole that Fabric cannot restore on client disconnect. The per-connection
map omits only the final Sorter menu for clients without both receiving channels;
stock clients receive the complete server map with matching raw IDs. A client
without common channel negotiation is treated as lacking Sorter.

The one Fabric mixin replaces only server registry task scheduling. It does not
relax client registry validation, hide a namespace, or disable registry sync.
Packet sends are independently checked with `ServerPlayNetworking.canSend`.

## Build and verification

Use JDK 21:

```sh
cd targets/fabric-1.21.1
bash gradlew clean build --no-daemon --console=plain
```

The build runs five regression tests covering capability detection, exact stock
registry IDs, absent-client filtering, isolation between connections, and dense
menu IDs. The installable remapped JAR is in `build/libs/`.

`.github/workflows/build.yml` follows the counter project's workflow: Java 21,
Gradle cache, a clean build, a commit-named JAR and SHA-256 in the job summary, and
a directly downloadable artifact retained for 30 days.

A local dedicated-server protocol smoke test can be run with
`scripts/optional-client-probe.cjs`. It needs `minecraft-protocol@1.66.2` available
in Node's module search path and an offline test server bound to localhost:25579
with this fork, Fabric API, and Sophisticated Backpacks/Core. It does not alter
production authentication settings. The probe advertises stock or absent Sorter
channels, checks the actual registry maps, and completes configuration to play.
It is not a replacement for testing the real Minecraft client UI.

## Manual client acceptance

Use the same Cobbleverse pack as the server for each profile:

1. **No Sorter:** join, open chests/backpacks/storage, move and shift-click items,
   disconnect and rejoin. A chest with Sorter memory slots should still accept
   normal vanilla interactions for this player.
2. **Stock upstream 1.1.1:** join, sort player inventory and an ordinary chest,
   transfer stacks, open/edit/close the settings gear, verify ignored/memory slots,
   disconnect and rejoin. Core backpack/storage screens retain their own controls.
3. Join both profiles simultaneously and check that a stock client's settings
   updates do not send Sorter packets or apply Sorter slot restrictions to the
   profile without Sorter.

Full client UI acceptance is pending; server startup and protocol smoke tests
alone do not establish that all Cobbleverse containers behave correctly.
