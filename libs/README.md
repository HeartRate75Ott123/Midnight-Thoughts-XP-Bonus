# `libs/` — local build dependencies

This folder is intentionally **empty in the repository**.

`build.gradle` resolves the Midnight Thoughts jar this addon compiles against from here:

```
libs/midnightthoughts-1.4.2+neoforge.1.21.1.jar
```

Midnight Thoughts is a third party mod and is **not redistributed** with this repository, so you have to
download it yourself and drop it in. Without it the build fails with missing symbols in the sources under
`com/plumejade/midnightthoughtsxpgift/mixin`, which reference the `mt.*` classes.

Steps:

1. Download **Midnight Thoughts 1.4.2+ for NeoForge 1.21.1** (the same jar you play with).
2. Copy it into this folder using exactly the name above.
3. Run `./gradlew build` from the repository root.

If you want to build against a different Midnight Thoughts version, change `midnightthoughts_version` in
`gradle.properties` and rename the file to match, or edit the path in `build.gradle` directly.

`*.jar` inside this folder is git ignored, so the dependency can never be committed by accident.
