# Midnight Thoughts: XP Bonus

> [!IMPORTANT]
> **Before building you have to supply the dependency yourself.** This repository ships no third party mod
> jars — download **Midnight Thoughts 1.4.2+ (NeoForge 1.21.1)** and place it at
> `libs/midnightthoughts-1.4.2+neoforge.1.21.1.jar`. See [Building](#building).

An addon for [Midnight Thoughts](https://www.curseforge.com/minecraft/mc-mods/midnight-thoughts) that pays out a
small amount of experience for the day that just ended, shows it on the Midnight Thoughts morning summary
panel and writes it to the chat.

* **Mod id:** `midnight_thoughts_xp_gift`
* **Author:** Plume Jade
* **Minecraft / loader:** 1.21.1 / NeoForge 21.1.238
* **Mod logo:** `src/main/resources/midnight_thoughts_xp_gift.png` (declared via `logoFile` in
  `META-INF/neoforge.mods.toml`)
* **Optional dependency:** Midnight Thoughts `1.4.2+` — without it this mod stays completely inert

## Features

### Daily experience rewards

The rewards are settled at the exact moment Midnight Thoughts settles its own "since last sleep" statistics,
i.e. when a player sleeps through the night and wakes up in the morning. The data covers everything that
happened since the previous sleep.

| Reward | Rule (default) | Config key |
| --- | --- | --- |
| Travel reward (旅途奖励) | every 64 blocks travelled → 10 XP | `blocks_per_distance_exp`, `distance_exp` |
| Hunt reward (猎魔奖励) | every 10 monsters killed → 30 XP | `kills_per_hunt_exp`, `hunt_exp` |
| Early bird reward (早起奖励) | a flat bonus for having slept | `base_bonus` (16) |

* The travelled distance counts **every** way of moving (walking, sprinting, swimming, flying, riding, being
  pushed around, ...); it is accumulated from the player's actual position each tick.
* The amount of slain monsters is read from the vanilla `minecraft.mob_kills` statistic, so it counts every
  non-player entity the player killed (hostile monsters as well as animals) — the same counter vanilla uses for
  the *Monster Hunter* advancement.
* The settled experience is granted to the player with `Player#giveExperiencePoints`.

### Morning summary panel entry

Midnight Thoughts' morning summary panel gets one extra entry:

* Icon: the addon's own experience orb sprite
  (`midnight_thoughts_xp_gift:textures/gui/experience_orb.png`, a single 16x16 sprite). The vanilla
  `minecraft:textures/entity/experience_orb.png` is deliberately **not** used: it is a 64x64 sprite sheet holding
  sixteen 16x16 animation frames, so blitting it as one icon would sample the whole sheet instead of one orb.
* Label: `经验奖励` (`XP Bonus` in English)
* Value: the total experience that was granted for the night
* Style: identical to the vanilla statistic badges (theme aware badge background and text colour, same roll up
  animation), drawn as a full width row directly below the six statistic badges

The entry is added with a Mixin into `mt.client.ui.summary.PlayerRowRenderer`, so it also appears next to the
name of any other player on the panel.

### Chat summary

After waking up the player receives a chat message:

```
早安！这是你昨日的冒险总结：
旅途奖励 → X 点经验
猎魔奖励 → X 点经验
早起奖励 → X 点经验
——————————
共获得 X 点经验！
```

The title is green (`§a`), the reward lines are gray (`§7`) and the separator plus the total are aqua (`§b`),
matching the specification. All lines are translatable
(`midnight_thoughts_xp_gift.message.*`) with Chinese fallbacks, so a language file can reword them.

## Configuring the thresholds with a data pack

Put a JSON file into `data/<your_namespace>/midnight_thoughts_xp_gift/`, for example
`data/mypack/midnight_thoughts_xp_gift/rewards.json`:

```json
{
  "blocks_per_distance_exp": 64,
  "distance_exp": 10,
  "kills_per_hunt_exp": 10,
  "hunt_exp": 30,
  "base_bonus": 16
}
```

Every field is optional — missing fields keep the default shown above. The file shipped by the mod lives at
`data/midnight_thoughts_xp_gift/midnight_thoughts_xp_gift/rewards.json`, so a data pack with a higher priority
simply overrides the values it wants to change. Data packs are applied on world load and on `/reload`.

## Building

> **You must provide the dependency yourself.**
> This repository does **not** ship any third party mod jar. Before you can build, download
> **Midnight Thoughts 1.4.2+ for NeoForge 1.21.1** yourself and put it into the `libs/` folder:
>
> ```
> libs/midnightthoughts-1.4.2+neoforge.1.21.1.jar
> ```
>
> The exact file name matters — `build.gradle` resolves it through
> `libs/midnightthoughts-${midnightthoughts_version}.jar` (override `midnightthoughts_version` in
> `gradle.properties` if you use a different build). If the file is missing, the build fails with a
> missing symbol error in the Mixin sources that reference `mt.*`. See
> [`libs/README.md`](libs/README.md) for the details, and note that GitHub Actions is disabled for the
> same reason.

Once `libs/midnightthoughts-1.4.2+neoforge.1.21.1.jar` is in place:

```bash
./gradlew build
```

The resulting jar is written to `build/libs/midnight_thoughts_xp_gift-<version>.jar`.

That jar in `libs/` is only used to compile against (and to run the game in the development environment); it is
**not** bundled into the built jar, which is why `libs/*.jar` is git ignored. The addon declares Midnight
Thoughts as an `optional` mod dependency and guards its Mixins with `required: false`, so it can be installed
with or without Midnight Thoughts present.

## Development notes

* `./gradlew runClient` / `./gradlew runServer` start a development client/server.
* The two Mixin hooks are:
  * `mt.server.DailyStatsManager#showDailySummary` — settles the rewards while the night's deltas are still
    available and before Midnight Thoughts resets its counters.
  * `mt.client.ui.summary.PlayerRowRenderer#render` — draws the extra panel entry.
* To see the panel entry, sleep through a night on a Midnight Thoughts enabled world.
