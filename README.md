# SimpleSkills Mod for Minecraft 1.21.4

SimpleSkills is a **server-side Fabric mod** that transforms your Minecraft experience with a dynamic RPG-style skill system. Master skills like `Woodcutting`, `Mining`, or `Slaying` by leveling up through gameplay. Unlock tools, armor, and weapons as you progress, giving you a sense of achievement with every milestone.

---

## Features

- 📜 **Skill Progression**: Earn XP in classic skills such as `Slaying`, `Woodcutting`, `Defense`, `Mining`, and more.
- ⚔ **Tool and Weapon Requirements**: Tools, weapons, and armor all require specific skill levels to unlock the tiers.
- 🎯 **Customizable Configurations**: Define skill requirements and XP values in JSON files for full control over progression.
- 🛠 **Dynamic Event Handling**: Hooks into player activities like mining, slaying mobs, and crafting.
- 📈 **Runescape-like Leveling**: Experience a nostalgic leveling curve, with increasing XP requirements for higher levels (max level: 99).

---

## Current Skills

- **Defense**: Levels up as players take damage in combat while wearing armor.
- **Excavating**: Levels up as players dig dirt, sand, or gravel with shovels.
- **Mining**: Levels up as players mine ores or stone with pickaxes.
- **Woodcutting**: Levels up as players chop logs with axes.
- **Slaying**: Levels up as players deal combat damage with swords or axes.

---

## Skill Restrictions

In SimpleSkills, players can only equip and use tools, weapons, and armor for which they meet the skill requirements. These requirements add challenge and depth to progression:
- **Armor**: Requires a specific `Defense` level to equip.
- **Weapons**: Requires a specific `Slaying` level.
- **Tools**: Requires a specific skill level in `Mining`, `Woodcutting`, or `Excavating`.

---

## Installation

1. **Download the Mod**: Grab the latest release from the [Releases Section]().
2. **Install Fabric**:
    - Make sure you have the **Fabric Loader** installed.
    - Follow the steps on the [official Fabric website]().

3. **Add the Mod**:
    - Place the `simpleskills` mod `.jar` file into your Minecraft Fabric server `mods` folder:


---

## Configuration

`SimpleSkills` offers robust customization options through JSON configuration files. These files are automatically generated after the first mod run if they don't already exist. Server admins or world owners can tailor skill requirements and features to fit their gameplay style.

| File Name                       | Purpose                                    |
|---------------------------------|--------------------------------------------|
| `simpleskills_tool_requirements.json` | Define skill requirements for tools.     |
| `simpleskills_weapon_requirements.json` | Define skill requirements for weapons.  |
| `simpleskills_armor_requirements.json` | Define skill requirements for armor.    |

Config filepath:
```plaintext
/minecraft-server/mods/simpleskills/
```

Example Entry in `simpleskills_tool_requirements.json`:
```json
{
  "minecraft:diamond_pickaxe": { "skill": "Mining", "level": 45 }
}
```

Edit these files to customize progression for tools, armor, and weapons.

---

## Commands

SimpleSkills provides a set of easy-to-use commands for server admins:

- **Add XP**: `/simpleskills xp add <player> <skill> <amount>`
- **Set Level**: `/simpleskills level set <player> <skill> <level>`
- **Query Skills**: `/simpleskills query <player> <skill>`

Commands require a `/level 2` (operator) permission level.

---

## Technical Overview

- **Fabric API Required**: SimpleSkills is built specifically for the Fabric Loader and API.
- **Database-Driven**: Skill data is saved in an SQLite database, ensuring efficient storage and processing.
- **Event-Driven Logic**: Hooks into Minecraft events to track player activities like mining, slaying, or crafting.
- **Mixin-Based Architecture**: Ensures smooth integration into the core Minecraft experience.

---