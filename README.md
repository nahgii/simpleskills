# simpleskills Mod for Minecraft 1.21.4

simpleskills is a **Fabric mod** introducing a familiar RPG-style skilling system to Minecraft, enhancing gameplay with meaningful progression. Master skills like `Woodcutting`, `Magic`, `Slaying`, and more. Unlock tools, armor, weapons, and perks as you level up, creating a rewarding sense of achievement.

This lightweight, performance-friendly mod works seamlessly in both **multiplayer and singleplayer**. Players do not need to install the mod when joining a server running Simpleskills.

---

**Direct download: [Modrinth](https://modrinth.com/mod/simpleskills)**

---

## Patch Notes (v1.1.0)
**Classes & Perks**:
* 7 classes, 22 perks

**New HUD Options**:
* Enhanced HUD toggling mechanics with `/simpleskills togglehud`. Now compatible across server-client boundaries.

**Ironman Mode**
* Add more challenge by losing all skills on death with this optional feature.

**Commands**:
* Many commands, see #Commands section

**Overall Adjustments**:
* XP Scaling rework (you might have to delete your old database, but you can set your skills to appropriate levels with commands. if cheats are disabled on SP world -> open to LAN, enable cheats)
* XP now uses the exact same formula as OSRS
* Table for XP _(helpful for customizing base XP and unlock levels)_: https://docs.google.com/spreadsheets/d/1X0bJeM7FzuCeVRRfzInzpgxQvYY5CBWjMu23QnihAkg/edit?usp=sharing
* Base XP (base_xp.json) is configurable and aims for 60k XP/H when the skill is being **focused** on
* magic_requirements.json allows you to limit exact enchantments (ID, level of enchant) allowed for anvils by magic level
* Adjusted Cleric trades:  Nether Wart more max uses,  Blaze Powder less max uses,  Dragon Breath costs more and has fewer uses

---

## Features
- **🖥️ FULLY SERVERSIDE**: simpleskills does not require server players to download the mod nor Fabric.
- **📜 Skill Progression**: Level up skills like `Defense`, `Woodcutting`, `Mining`, `Excavating`, `Slaying`, `Farming`, and `Magic`.
- **⚔ Tiered Equipment System**: Unlock tools, armor, and weapons based on skill levels.
- **🎮Highly Customizable**: Edit JSON files to tweak progression, requirements, and enabled features.
- **🔮 Magic Skill Enhancements**: Improve potion durations, enchantments, and unlock utility blocks like the Anvil and Enchanting Table.
- **🔋 Performance Optimization**: Doesn't use server or client ticks, ensuring minimal overhead.
- **📊 HUD Toggle**: Enable or disable HUD in singleplayer (default key: `TAB`) or multiplayer (`/simpleskills togglehud`).
- **🌎 World-Specific Stats**: Skills are saved per world, not globally.
- **💀 Ironman Mode**: Reset skills upon death for a hardcore experience.`(OPTIONAL)`
- **💪 Unique Classes and Perks**: Choose from distinct classes with gameplay-altering perks. `(OPTIONAL)`
- **🧹Clean Uninstallation**: simpleskills performs unintrusive modifications that get reset whenever you log out.*

#### * Cleric Villager trades must be manually reset if the 'villager_trades' is set to TRUE in config.json

---

## Skill Requirements

Below are the **default skill requirements** for equipping armor, tools, weapons, and unlocking blocks. Requirements are fully customizable via JSON files.

### **Armor Requirements**
| Armor                      | Skill      | Level |
|----------------------------|------------|-------|
| Leather Armor              | Defense    | 0     |
| Golden Armor               | Defense    | 15    |
| Chainmail Armor            | Defense    | 15    |
| Turtle Shell Helmet        | Defense    | 35    |
| Iron Armor                 | Defense    | 35    |
| Diamond Armor              | Defense    | 60    |
| Netherite Armor            | Defense    | 75    |

---

### **Tool Requirements**
| Tool              | Skill         | Level |
|-------------------|---------------|-------|
| Wooden Pickaxe    | Mining        | 0     |
| Stone Pickaxe     | Mining        | 10    |
| Golden Pickaxe    | Mining        | 15    |
| Iron Pickaxe      | Mining        | 30    |
| Diamond Pickaxe   | Mining        | 60    |
| Netherite Pickaxe | Mining        | 75    |

---

### **Weapon Requirements**
| Weapon              | Skill     | Level |
|---------------------|-----------|-------|
| Wooden Axe/Sword    | Slaying   | 0     |
| Stone Axe/Sword     | Slaying   | 10    |
| Golden Axe/Sword    | Slaying   | 15    |
| Iron Axe/Sword      | Slaying   | 30    |
| Diamond Axe/Sword   | Slaying   | 60    |
| Netherite Axe/Sword | Slaying   | 75    |
| Crossbow            | Slaying   | 5     |
| Bow                 | Slaying   | 20    |
| Mace                | Slaying   | 35    |

---

### **Magic Skill**
Duration of Level I potions increases by 1 minute per Magic level (up to 60 minutes).

Affected Potions:

*   Potion of Fire Resistance
*   Potion of Strength
*   Potion of Swiftness
*   Potion of Night Vision
*   Potion of Invisibility
*   Potion of Water Breathing
*   Potion of Leaping

Gain Magic XP by:
- Enchanting items.
- Applying potion effects.

| Block                  | Skill   | Level |
|------------------------|---------|-------|
| Anvil                  | Magic   | 10    |
| Enchanting Table       | Magic   | 20    |
| Fortune III (anvil)    | Magic   | 35    |
| Protection IV (anvil)  | Magic   | 50    |
| Efficiency V (anvil)   | Magic   | 55    |
| Mending (anvil)        | Magic   | 60    |
| Elytra                 | Magic   | 65    |


---

## Passive Attributes

Each skill offers unique passive bonuses at higher levels:

| Attribute Bonus      | Skill       | Per Level      | Max Bonus        |
|----------------------|-------------|----------------|------------------|
| **Attack Damage**    | Slaying     | +1%            | +25%             |
| **Block Range**      | Woodcutting | +0.1 block/LVL | 7.0 blocks       |
| **Max Health**       | Defense     | +2HP /8LVLS    | +8 HP (4 hearts) |
| **Breaking Speed**   | Mining      | +1.1%          | +28.6%           |
| **Movement Speed**   | Excavating  | +1%            | +25%             |
| **Knockback Resist** | Farming     | +1%            | +25%             |
| **Fall Reduction**   | Magic       | -1%            | -25%             |

---

## Classes and Perks
`CLASSES AND PERKS CAN BE DISABLED IN THE CONFIG.JSON ! (Enabled by default)`

Reset your Class to Peasant by right clicking a Cauldron with a Feather. This will reset your primary skill.

### Peasant (None)
- Default class
### Knight (Defense): Right click a Smithing Table with an empty Book.
- **Hamstring**: Apply Slowness for 4 seconds with melee hits.
- **Heavy Bolts**: +45% crossbow damage.
- **Patronage**: Better deals with villagers.
- **Rigid Arsenal**: Cannot use tridents or bows.

### Rogue (Slaying): Right click a Grindstone with an empty Book.
- **Stealth**: Invisibility and faster sneaking speed.
- **Poison Strike**: Apply Wither II for 4 seconds while invisible.
- **Flash Powder**: Breaks all aggro of mobs within the radius. 12sec cooldown, 10 block radius, 1 Glowstone Dust per use.
- **Slim Physique**: Max health reduced by 40%.

### Farmhand (Farming): Right click a Composter with an empty Book.
- **Fortitude**: Minimum hunger of 7.
- **Rustic Temperament**: -30% Magic XP gain.

### Lumberjack (Woodcutting): Right click a Fletching Table with an empty Book.
- **Strong Arms:** Attack speed is increased to 5 (base = 4). (e.g., iron axe: 0.9 base -> 1.35 with perk).
- **Salvaged Bark:** Gain 2 planks when you strip a log.
- **Brute:** Swords deal 75% less damage, but Axes can be used for Slaying based on Woodcutting skill.

### Miner (Mining): Right click a Blast Furnace with an empty Book.
- **Safety Lamp:** night vision when a torch/lantern is equipped in the off-hand.
- **Blasting Expert:** 20% less damage from explosions (stacks with Blast Protection IV).
- **Vertigo:** 20% more fall damage.


### Nomad (Excavation): Right click a Loom with an empty Book.
- **Scavenger**: Food saturation decays 25% slower, and you are immune to hunger effects.
- **Bottomless Bundle**: Right-click with a Bundle to access your Ender Chest inventory.
- **Outsider**: Villages view Nomads with suspicion, refusing to trade.

### Wizard (Magic): Right click a Bookshelf with an empty Book.
- **Incantation:** cast spells based on your equipped wand:
- 1. Stick: grants regeneration to targeted player or self. (20sec cooldown)
- 2. Blaze Rod: casts a fireball that deals damage to entities, but not blocks. (25sec cooldown)
- 3. Breeze Rod: toggles levitation, allowing you to move in the air horizontally, but not vertically. (2sec cooldown)
- **Frail Body:** -10% XP gain to all skills except Magic.
- **I Put on My Robe and Wizard Hat:** can only wear Leather Armor, Elytra, and Turtle Shell.

---
## Adjusted Cleric Trades
To make early-game more tolerable with enchantments locked behind Magic levels, Potions play a larger role.

Clerics may now offer: 
* 1x Blaze Powder for 3x Emeralds
* 1x Nether Wart for 6x Emeralds
* 1x Dragon Breath for 8x Emeralds

### You can disable adjusted Cleric trades in config.json !

---

## Installation (server or client)
Players who wish to play on your server that is running simpleskills can join with a vanilla client.

1. **Download the Mod**
2. **Install Fabric API**
3. **Place the mod** in the `mods` folder (client:`C:\Users\Username\AppData\Roaming\.minecraft\mods`).

---

## Uninstallation

1. **Delete `simpleskills.jar` & `simpleskills` folder from `mods`**
2. **Delete `simpleskills.db` from the world data folder `C:\Users\Username\AppData\Roaming\.minecraft\saves\World_Name\data`**

---

## Commands

- `/simpleskills togglehud // toggles the tab menu (MULTIPLAYER) HUD on or off` 
- For singleplayer the keybind to enable or disable HUD is TAB by default, can be rebound in
  `Options -> Controls -> Key Binds... -> simpleskills` at the bottom of the menu.


- `/simpleskills ironman enable` // Enables Ironman mode


- `/simpleskills reset <username>` // Resets all your skill levels and class


- `/simpleskills addxp <target> <skill> <amount>` // Add XP to a player skill _[OP only]_


- `/simpleskills setlevel <target> <skill> <amount>` // Sets the level of a player skill _[OP only]_


- `/simpleskills getlevel <target> <skill/total>` // Gets the level of a target's skill or total level

#### CLASSES:
- `/simpleskills class revoke <target>` // Remove the class (also resets primary skill to level 0) _[OP can target other players]_
- `/simpleskills class set <target> <class>` // Sets a target's class _[OP only]_
- `/simpleskills class get<target>` // Gets a target's class
- `/simpleskills class perks <class>` // Lists the perks a class has
- `/simpleskills class list` // Lists all classes
- `/simpleskills perkinfo <perkname>` // NOT IMPLEMENTED

---

## Performance
- Lightweight: **No server ticks**.
- Only the HUD is **client-tick-based in Singleplayer.**
- Skills saved per world in `saves/<WorldName>/data/simpleskills.db`.

---

### Customization

Edit JSON files generated in `/mods/simpleskills/` to:
- Adjust XP scaling (`base_xp.json`).
- Customize skill requirements (`tool_requirements.json`, `armor_requirements.json`, etc.).

---

#### AI Disclaimer: simpleskills utilizes code that has been generated with openai-gpt-4o, claude.ai, and mistral.ai.
#### Disclaimer: simpleskills ("Software") is provided as-is without any warranty. We are not responsible for any data loss or damage. Back up your worlds before using mods. By using this Software, you accept these terms.