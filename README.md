# Sprout Ability for ProjectKorra

This is an addon ability for the [ProjectKorra](https://projectkorra.com/) plugin for Spigot Minecraft servers. The ability was originally created by NickC1211 and has been updated and fixed to work with later versions of ProjectKorra and Spigot.

## Description

**Sprout** is a plant ability that allows waterbenders to ensnare their opponents at range. This move requires that you and your target be standing on connected fertile ground (dirt or grass blocks).

### Features

- **Root Path**: Creates a path of plants as it travels along the ground.
- **Snare Effect**: Ensnares entities with a double plant to immobilize them.
- **Damage**: Applies damage to entities hit by the snare.
- **Potion Effect**: Applies a slowness effect to ensnared entities.

## Instructions

- **Activation**: Hold Shift and Left Click to shoot, and move the cursor side to side to aim.

## Installation

1. Download the `Sprout.jar` file.
2. Place the `Sprout.jar` file in the `./plugins/ProjectKorra/Abilities` directory.
3. Restart your server or reload the ProjectKorra plugin with `/b reload` to enable the ability.

## Compatibility

- **Minecraft Version**: Tested and working on MC 1.20.4. Includes updated potion name for 1.20.6+ support.
- **ProjectKorra Version**: Tested and working on PK 1.11.2 and 1.11.3. Might support earlier versions too.

## Configuration

The ability can be configured in the ProjectKorra `config.yml` file under `ExtraAbilities.NickC1211.Sprout`:

```yaml
ExtraAbilities:
  NickC1211:
    Sprout:
      ContinueThroughEntities: true
      Cooldown: 5000
      Damage: 3
      PathRevertTime: 1100
      PathPlant: "SHORT_GRASS"
      Range: 20
      SnarePlant:
        Top: "LARGE_FERN"
        Bottom: "LARGE_FERN"
      SnareTime: 4000
      Sound: "BLOCK_GRASS_BREAK"
