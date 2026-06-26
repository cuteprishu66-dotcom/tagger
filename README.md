# Portal Tier Tagger

Fabric mod for Minecraft 1.21.1 that displays Portal Tiers rankings above
nearby players' nametags, with custom font/emoji icons per gamemode.

## Building

This project uses Fabric Loom. A Gradle wrapper properties file is included,
but the wrapper jar/scripts are not (binary files aren't included here) —
either:

- Run `gradle wrapper` once with a local Gradle 8.8 install to generate
  `gradlew` / `gradlew.bat` / `gradle/wrapper/gradle-wrapper.jar`, then use
  `./gradlew build` from then on, or
- Open the project directly in IntelliJ IDEA with the Fabric/Loom setup and
  let it sync, or
- Use your own Gradle installation: `gradle build`.

Build output (the mod jar) will be in `build/libs/`.

## Project layout

```
src/main/java/net/portaltiertagger/
  PortalTierTagger.java          - mod entrypoint, event registration
  config/ModConfig.java          - Cloth Config / AutoConfig config schema
  cache/RankingCache.java        - LRU + TTL cache of fetched player tiers
  network/RankingScraper.java    - fetches tier data from the Portal API
  network/RankingEntry.java      - per-player tier data + highest/lowest logic
  render/PlayerNameRenderHelper.java - draws the tier tag above player heads
  command/TierTaggerCommand.java - /tiertagger client commands
  keybind/ModKeybinds.java       - toggle / refresh / open-config keybinds

src/main/resources/
  fabric.mod.json
  portal_tier_tagger.mixins.json
  assets/portal_tier_tagger/...  - custom font, emoji textures, lang file
```

## Recent fix: nametag rendering

The nametag tier renderer was registered on `WorldRenderEvents.LAST`, which
fires *after* the shared entity vertex buffer for the frame has already been
flushed to the GPU — so the tier text was queued into a buffer that never
got drawn. It now registers on `WorldRenderEvents.AFTER_ENTITIES`, which runs
before that flush. See the comment in `PortalTierTagger.onInitializeClient()`
for details.
