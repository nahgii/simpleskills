# Simpleskills Mod for Minecraft 1.21.4

Simpleskills is a **Fabric mod** that prolongs your Minecraft experience with a familiar RPG-style skilling system. Master skills such as `Woodcutting`, `Magic`, or `Slaying` by leveling up through gameplay. Unlock tools, armor, and weapons as you progress, giving you a sense of achievement with every milestone.

This mod is performance-friendly, **does not utilize server ticks or client ticks**, and is lightweight. Simpleskills works in both **multiplayer and singleplayer**. Installation is **not** necessary to join a server running simpleskills.

Direct download at: https://modrinth.com/mod/simpleskills

##  IMPORTANT! Elytra requires level 65 Magic (adjustable in armor_requirements.json) and WILL unequip automatically; don't die to fall damage.
---

## Features

- 📜 **Skill Progression**: Earn XP in skills including `Defense`, `Woodcutting`, `Mining`, `Excavating`, `Slaying`, and `Magic`.
- ⚔ **Tool, Weapon, and Armor Requirements**: Tiered system based on skill levels to equip items and blocks.
- 🔮 **Magic Skill**: Enhance potion effects and unlock utility blocks like the Anvil, Brewing Stand, and Enchanting Table at specific Magic levels.
- 🎮 **Customizable Requirements**: Edit JSON files to easily tweak skill requirements or progression for your server.
- 🔋 **Performance Optimization**: Doesn't use server or client ticks, ensuring minimal overhead.
- 💪 **Passive Abilities**: after unlocking everything at level 65, players gain bonuses until the maximum level of 99 per skill.
---

## Skill Requirements

Below are the **default skill requirements** for equipping armor, tools, weapons, and unlocking blocks. These requirements are fully customizable via JSON files to suit your server or singleplayer needs (details on file locations below).

### **Armor Requirements**
| Armor                      | Skill      | Level |
|----------------------------|------------|-------|
| Leather Armor              | Defense    | 0     |
| Golden Armor               | Defense    | 10    |
| Chainmail Armor            | Defense    | 13    |
| Turtle Shell Helmet        | Defense    | 15    |
| Iron Armor                 | Defense    | 20    |
| Diamond Armor              | Defense    | 45    |
| Netherite Armor            | Defense    | 65    |

---

### **Tool Requirements**
| Tool              | Skill         | Level |
|-------------------|---------------|-------|
| Wooden Pickaxe    | Mining        | 0     |
| Stone Pickaxe     | Mining        | 10    |
| Golden Pickaxe    | Mining        | 15    |
| Iron Pickaxe      | Mining        | 20    |
| Diamond Pickaxe   | Mining        | 45    |
| Netherite Pickaxe | Mining        | 65    |
|                   |               |       |
| Wooden Axe        | Woodcutting   | 0     |
| Stone Axe         | Woodcutting   | 10    |
| Golden Pickaxe    | Mining        | 15    |
| Iron Axe          | Woodcutting   | 20    |
| Diamond Axe       | Woodcutting   | 45    |
| Netherite Axe     | Woodcutting   | 65    |
|                   |               |       |
| Wooden Shovel     | Excavating    | 0     |
| Stone Shovel      | Excavating    | 10    |
| Golden Pickaxe    | Mining        | 15    |
| Iron Shovel       | Excavating    | 20    |
| Diamond Shovel    | Excavating    | 45    |
| Netherite Shovel  | Excavating    | 65    |

---

### **Weapon Requirements**
| Weapon                     | Skill     | Level |
|----------------------------|-----------|-------|
| Wooden Axe/Sword           | Slaying   | 0     |
| Stone Axe/Sword            | Slaying   | 10    |
| Golden Axe/Sword           | Slaying   | 15    |
| Iron Axe/Sword             | Slaying   | 20    |
| Diamond Axe/Sword          | Slaying   | 45    |
| Netherite Axe/Sword        | Slaying   | 65    |
| Bow                        | Slaying   | 12    |
| Mace                       | Slaying   | 35    |

---

### **Blocks and Magic Unlocks**
| Block                  | Skill   | Level |
|------------------------|---------|-------|
| Anvil                  | Magic   | 10    |
| Enchanting Table       | Magic   | 20    |
| Fortune 1-3 (anvil)    | Magic   | 25    |
| Protection 1-4 (anvil) | Magic   | 35    |
| Efficiency 1-5 (anvil) | Magic   | 45    |
| Mending (anvil)        | Magic   | 55    |
| Elytra                 | Magic   | 65    |

---

### Magic Skill: Potion Durations and Unlocks

The **Magic skill** allows players to enhance their gameplay through positive potion effects and unlock useful vanilla blocks.
To increase **Magic skill** players must convert their Vanilla XP by right-clicking on an empty lectern with an empty hand. Players can also gain **Magic skill** XP through potion effect application.
- **Duration Increase**: The maximum duration of positive potion effects applied to the player is increased based on their Magic level starting from level 1. Players can drink multiple level I potions to extend durations in minutes equal to their Magic level.
- **Adjusted Trades**: Novice clerics can now offer Nether Wart and Blaze powder, and Master clerics can offer Dragon's Breath.
- **Affected Potions (Level I Only)**:
   - Potion of Fire Resistance
   - Potion of Strength
   - Potion of Swiftness
   - Potion of Night Vision
   - Potion of Invisibility
   - Potion of Water Breathing
   - Potion of Leaping

**Limits**:
- **Minimum Duration**: 1 minute (level 0).
- **Maximum Duration**: 60 minutes (level 60).

---

### **Passive Abilities**
| Attribute bonus | Skill       | MAX   |
|-----------------|-------------|-------|
| Attack Damage   | Slaying     | +33%  |
| Block Range     | Woodcutting | 7.0   |
| Max Health      | Defense     | 8 HP  |
| Breaking Speed  | Mining      | +28%  |
| Movement Speed  | Excavating  | +33%  |
| Fall Reduction  | Magic       | +100% |

**Details**
* Slaying: 33% more damage at level 99 (1% per level)
* Woodcutting: Increases block interaction range per level. Base level is 4.5.
* Defense: Gain 2 health (1 heart) every 10 levels starting from 66 to a total of 8 health (4 hearts) at level 99.
* Mining: Gain faster block breaking speed per level. At level 99 you can instantly mine stone with a Netherite Pickaxe with Efficiency V (similar to Haste II).
* Excavating: +1% movement speed per level (+33% at level 99).
* Magic: Gain fall damage reduction based on level: lvl 66-75: 25%, lvl 76-85: 50%, lvl 86-98: 75%, lvl 99: 100%.

## Installation

1. **Download the Mod**
2. **Install Fabric**:
   - Make sure you have the **Fabric Loader** installed.
   - Follow the steps on the [official Fabric website]().

3. **Add the Mod**:
   - Place the `simpleskills` mod `.jar` file into your Minecraft Fabric server or `.minecraft` `mods` folder.

---

## Customization

To customize skill requirements and XP progression in SimpleSkills, follow these steps:

1. **Launch the game or server**: Run the game or server at least once to allow the mod to generate the necessary JSON files.
2. **Close the game or server**: Exit the game or shutdown the server once the JSON files are generated.
3. **Locate the JSON files**: You will find the JSON files in the `/mods/simpleskills/` folder.
4. **Edit the JSON files**: Use a text editor to make changes to the files to customize the mod to your preferences.
5. **Start the game or server**: After editing the files, restart the game or server for the changes to take effect.

| File Name                               | Purpose                                                               |
|-----------------------------------------|-----------------------------------------------------------------------|
| `simpleskills_tool_requirements.json`   | Adjust skill requirements for tools.                                  |
| `simpleskills_weapon_requirements.json` | Adjust skill requirements for weapons.                                |
| `simpleskills_armor_requirements.json`  | Adjust skill requirements for armor and Elytra.                       |
| `simpleskills_magic_requirements.json`  | Adjust skill requirements for magic unlocks (e.g., Mending, Anvil...) |
| `base_xp.json`                          | Adjust XP multipliers for skills.                                     |

These values are **easy to edit** with a text editor, allowing you to set custom requirements that align perfectly with your server or gameplay preferences.

---
## Commands

SimpleSkills provides a set of easy-to-use commands for server admins:

- **Add XP**: `/simpleskills xp add <player> <skill> <amount>`
- **Set Level**: `/simpleskills level set <player> <skill> <level>`
- **Query Skills**: `/simpleskills query <player> <skill>`

Commands require a permission level of `/op` or `/level 2`.

---

## Performance and Uninstalling

### Performance-Friendly:
SimpleSkills is optimized and **does not utilize server ticks**, ensuring no performance issues even on large servers. For singleplayer use, client ticks are only utilized to update the scoreboard.

### Easy and Clean to Uninstall:
1. Remove the `.jar` file from your `mods` folder.
2. Delete the `simpleskills` configuration folder.
3. Manually check Cleric villager trades, as they do not reset upon uninstall.

---

## Technical Overview

- **Fabric API Required**: Lightweight and built specifically for Fabric.
- **Mixin Architecture**: Smooth integration with Minecraft with minimal performance impact.
- **SQLite Database**: Ensures efficient skill data storage and retrieval for every player.

---
#### AI Disclaimer: SimpleSkills has been primarily generated with openai-gpt-4o.