# Tower Bugs: Frog Defense

A grid-based tower-defense game in the Dungeon Dice Frogs universe.

## First playable prototype
The repository now contains a native Android prototype focused on proving the core game systems before final Dungeon Dice Frogs art is imported.

Implemented now:
- 16×10 grid battlefield
- Player-built wall maze
- Breadth-first shortest-path frog navigation
- Wall-placement validation that refuses any placement that blocks the final route to the Tower
- Validation against every currently active frog so a live enemy cannot be trapped by a new wall
- Free starting blocks plus increasingly expensive extra blocks purchased with Coins
- Bugs purchased with Coins and placed on wall tiles
- In-level bug upgrades for damage, range, health, and attack speed
- Five-wave level structure
- Skippable 10-second intermission between waves
- Bugs and blocks can still be purchased during active combat
- Tiered frog HP, armor/defense, speed, and Coin rewards
- Higher-tier frogs that can attack adjacent bugs
- Boss frog on the final wave
- Boss shortcut logic that can crack and destroy walls when doing so opens a meaningfully shorter route
- Procedural placeholder cracked-wall visuals
- Green → yellow → red health bars for frogs and bugs
- Tower HP and loss state
- Persistent Bug Bucks awarded for level completion
- Home-screen Bug Buck upgrades for starting Coins, free blocks, and bug damage
- Level progression saved on-device

## Currency model
**Coins** are temporary in-level currency. Defeated frogs award Coins, which are spent on blocks, bugs, and bug upgrades.

**Bug Bucks** are persistent progression currency. They are awarded for completing levels and spent from the home upgrade screen.

## Placeholder art
The first build intentionally uses simple generated shapes and labels for frogs, bugs, walls, spawn, and the Tower. These are placeholders for the final Dungeon Dice Frogs asset families, including colored frog tiers, boss frogs, defensive bugs, intact/cracked/destroyed blocks, and Tower states.

## Android build
The project targets Android API 36 and uses Java 17. A GitHub Actions workflow is included at `.github/workflows/build-apk.yml`.

To build from GitHub:
1. Open the repository's **Actions** tab.
2. Open **Build Debug APK**.
3. Choose **Run workflow** on the `main` branch.
4. When the run completes, open it and download the `tower-bugs-frog-defense-debug` artifact.
5. The artifact contains `app-debug.apk`.

The workflow can also build automatically on ordinary pushes to `main`.

Install-fix build: v0.1.1 uses a unique application ID and explicitly enables both V1 and V2 debug APK signing.
