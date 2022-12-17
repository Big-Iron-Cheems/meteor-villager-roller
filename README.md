
# Meteor Villager Roller

![checks](https://github.com/maxsupermanhd/meteor-villager-roller/actions/workflows/checks.yml/badge.svg)
![devbuild](https://github.com/maxsupermanhd/meteor-villager-roller/actions/workflows/devbuild.yml/badge.svg)

Addon that changes villager profession until desired trade found

We have [Discord server](https://discord.com/invite/DFsMKWJJPN)

## Versions

| Minecraft   | Meteor           | Supported          | Download |
| ----------- | ---------------- | ------------------ | -------- |
| 1.19.3      | 0.5.2-dev >1714  | Yes                | [1.3.5](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.3.5/villager-roller-1.3.5+mc1.19.3-rev.07401b8.jar) |
| 1.19.2      | 0.5.1-dev >1573  | No                 | [1.3.4](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.3.4/villager-roller-1.3.4+mc1.19.2-rev.f2c071c.jar) |
| 1.19.1      | 0.5.1-dev >1570  | No                 | [1.3.2](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.3.2/villager-roller-1.3.2+mc1.19.1-rev.bd5aa5e.jar) |
| 1.19        | 0.5.0-dev >=1563 | No                 | [1.3.1](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.3.1/villager-roller-1.3.1+mc1.19-build.34.jar) |
| 1.19        | 0.5.0-dev <1563  | No                 | [1.3](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.3/villager-roller-1.3+mc1.19-rev.b16e705.jar) |
| 1.18.2      | 0.4.9            | No                 | [1.3](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.3/villager-roller-1.3+mc1.18.2-rev.3d6f694.jar) |
| 1.18.1      | ?                | No                 | [1.2.1](https://github.com/maxsupermanhd/meteor-villager-roller/releases/download/1.2.1/villager-roller-1.2.1.jar) |

Download links for older versions are here only for archival reasons, do not use them.

If you want to roll villagers on server that is on different Minecraft version please use [viafabric](https://github.com/ViaVersion/ViaFabric).

## Solving use-case

Addon is targeted for lazy people that are playing on server and trying to find enchantment on librarians. No functionality about other trades/professions implemented and planned, you can pull request them if you really want to.

## How-to

1. Acquire villager (not nitwit)
2. Acquire *some* lecterns (32 will be good)
3. (Recommended, not required) Acquire axe, better quality - faster rolling
4. Confine yourself and a villager from step 1 in a space where no freely moving entities and blocks are present (except villager and you)
5. Block villager from wandering around (slabs, blocks, stairs, *not trapdoors* since those mess with pathfinding)
6. Place a lectern where villager can access it and ensure that hand-rolling of profession is possible
7. Configure module to your needs and enable it
8. Follow selection of block and villager from chat messages
9. Leave module running alone (switching focus from window will likely open pause menu and *pause* rolling if "Pause on screens" option is enabled, disable it if you know what you are doing)
10. Profit!

## Common issues

- It says "We got your villager" twice and stops \
  Addon is designed to run on servers. Singleplayer and plugins can interfere with how merchant screens behave, nothing that can be done about it.
- I can not interact with villager anymore \
  See above, screen desync can be fixed by rejoining server. Nothing that can be done about it.
- It does not place lecterns back \
  It does place lecterns back, if it failed it means that block is impossible to place because player, villager, other entity or block is already occupying that block. Rolling process will hang because of this but will resume after manual block placement.
- It closes chat/overlay/screens while rolling \
  Yes it does because there is no way to not to, when interaction with villager is requested server will force client to open merchant screen (will replace any screen that is present) and opening that screen is required to get list of trades from the villager. Toggle "Pause on screens" will prevent new interactions if any screen is open (to aboid force closing it), after you done what you wanted you can manually interact with villager to resume rolling.
- It does not collect lecterns and fails to place them when they run out \
  Searching, pathfinding and moving to specific item on the ground is difficult and messy, there always a chance that all lecterns will fly away from player and it will end up with empty hands. You can optimize it by having hopper-dropper setup or flowing water if you really want to. Feel free to implement non destructive moving towards dropped item if you want.
- It always rolls to best level of enchantment \
  Update Villager Roller to 1.2

## Copying and credit

Integrating Villager Roller into other clients without modifying underlying featureset/codebase is allowed only with prior agreement or credit inside module settings/description. (Example: `Villager Roller by FlexCoral`)

## License

GPL-3.0, see LICENSE in project root
