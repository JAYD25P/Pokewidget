# Autonomous Battle Widget

This feature adds an optional two-Pokémon battle mode to the PokéWidget Android app.

## User-facing behaviour

- Enable **Autonomous battle** in the widget configuration screen.
- The Medium widget preset is sized for a 4-column by 3-row launcher cell footprint.
- Each battle randomly selects two different Pokémon from the closed roster below.
- The left and right slots can each be pinned to a favourite. Leaving a slot on Random allows that side to be selected automatically.
- Battles are simulated through successive attacks until one combatant reaches 0 HP. The final frame announces the winner and shows the fainted Pokémon.
- Existing single-Pokémon widgets remain the default unless battle mode is enabled.

## Allowed roster

| National Pokédex | Pokémon |
|---:|---|
| 25 | Pikachu |
| 6 | Charizard |
| 34 | Nidoking |
| 18 | Pidgeot |
| 123 | Scyther |
| 95 | Onix |
| 93 | Haunter |
| 150 | Mewtwo |
| 121 | Starmie |
| 2 | Ivysaur |

The battle engine contains a compact move list, attack stats, HP, accuracy checks, STAB and type effectiveness. It is a lightweight fan-made simulation, not a claim of exact cartridge battle mechanics.

## Sprite and widget architecture

- Uses the existing catalog's `veekun_emerald_animated` sprite set, which points to the original Emerald-era animated battle sprite files.
- Sprite files are fetched through the existing `SpriteSource` cache; they are not bundled into the APK by this feature.
- Both sprites, the HP HUD and battle log are rasterized into a bounded series of bitmaps.
- The sequence is handed to Android `RemoteViews` as child views in the existing `ViewFlipper`. The launcher advances those frames without a continuously running service or per-second alarm.
- The battle engine resolves the duel through fainting, then samples the full opening-to-result timeline into at most 24 display frames. This bounds bitmap transfer without dropping the final result.

## Asset and distribution notes

Pokémon names, characters and sprite art are intellectual property of their respective rights holders. Public availability of a sprite file does not grant a redistribution or commercial-use licence. This implementation fetches the sprite assets at runtime rather than embedding them. The upstream sprite mirror and its terms should be reviewed before distributing builds beyond personal testing. This is an unofficial fan project and is not affiliated with Nintendo, Creatures Inc. or Game Freak.

## Verification

Run the Android unit tests and debug APK build with JDK 17:

```bash
bash ./gradlew --no-daemon :app:testDebugUnitTest :app:assembleDebug
```

The GitHub Actions Android workflow is configured to watch `feature/**` branches. A successful remote run and on-device launcher verification are still required before treating the feature as release-ready.
