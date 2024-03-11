package com.alfred.weight;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
@Config(name = "player-weight")
public class WeightConfig implements ConfigData {
    @ConfigEntry.Gui.EnumHandler
    public DisplayType displayType = DisplayType.ICON;
    @ConfigEntry.Gui.EnumHandler
    @Comment("Useful if the server this mod is on is vanilla")
    public WarningType weightWarningType = WarningType.NONE;
    public float globalDefaultWeight = 1.0f;
    public float defaultMaxWeight = 300.0f;
    public boolean weightModifiersAreMultiplicative = true;
    public boolean affectsCreativeModePlayers = false;
    public final List<WeightTuple> modifiers = Arrays.asList(
            new WeightTuple("Air", 0.0f),
            new WeightTuple("Netherite", 1.6f),
            new WeightTuple("Gold", 20.0f),
            new WeightTuple("Diamond|Emerald|Ruby|Sapphire|Gem", 1.2f, MatchType.REGEX),
            new WeightTuple("Golden (Pickaxe|Axe|Shovel|Hoe|Sword)", 0.1f, MatchType.REGEX), // Undo the massive weight modifier that Gold would apply to these items
            new WeightTuple("Iron|Steel|Copper|Amethyst|Titanium|Silver|Bronze", 1.15f, MatchType.REGEX),
            new WeightTuple("Aluminum", 0.95f),
            new WeightTuple("Tungsten", 1.5f),
            new WeightTuple("Basalt", 1.15f),
            new WeightTuple("Netherrack", 0.75f),
            new WeightTuple("Blackstone", 1.1f),
            new WeightTuple("Stone", 1.3f),
            new WeightTuple("Dirt", 0.95f),
            new WeightTuple("Anvil", 1.35f),
            new WeightTuple("Block", 1.25f),
            new WeightTuple("Wood", 0.9f),
            new WeightTuple("Plank|Fence|Gate", 0.8f, MatchType.REGEX),
            new WeightTuple("Leaf|Leaves|Feather|Stick|Arrow|Dust|Wire|Sculk|Egg|Paper|Dollar|Fabric|Cloth|Foam|Nugget", 0.1f, MatchType.REGEX),
            new WeightTuple("Table", 1.2f),
            new WeightTuple("Glass|Ice", 0.8f, MatchType.REGEX),
            new WeightTuple("minecraft:swords", 1.1f, MatchType.TAG),
            new WeightTuple("minecraft:pickaxes", 1.25f, MatchType.TAG),
            new WeightTuple("minecraft:axes", 1.25f, MatchType.TAG),
            new WeightTuple("minecraft:shovels", 1.05f, MatchType.TAG),
            new WeightTuple("minecraft:hoes", 0.95f, MatchType.TAG),
            new WeightTuple("Metal", 1.25f),
            new WeightTuple("Ingot", 0.9f),
            new WeightTuple("Raw", 0.9f),
            new WeightTuple("Flint", 0.4f),
            new WeightTuple("Ore|Granite|Diorite|Andesite", 1.225f, MatchType.REGEX),
            new WeightTuple("Obsidian|Bedrock|Deepslate|Lead", 1.4f, MatchType.REGEX), // Changing the (ore) lead's weight affects the leash lead too :/
            new WeightTuple("Wool", 0.25f),
            new WeightTuple("Apple|Cooked|Cake|Stew|Fiber|Carbon|Chicken|Beef|Pork|Fish|Salmon|Sushi", 0.5f, MatchType.REGEX),
            new WeightTuple("Fungus|Mushroom|Plant|Rubber|Redstone", 0.6f, MatchType.REGEX),
            new WeightTuple("Boat", 1.3f),
            new WeightTuple("Totem", 0.8f)
    );
    // Can't think of a good name, gonna leave it as 'WeightPunishment(s)' for now
    public final List<WeightPunishment> weightPunishments = Arrays.asList(
            new WeightPunishment(PunishmentType.SPEED, 0.9f, 0.8f, true),
            new WeightPunishment(PunishmentType.SPEED, 0.5f, 1.25f, true),
            new WeightPunishment(PunishmentType.EXHAUSTION_PER_TICK, 0.02f, 1.25f, true),
            new WeightPunishment(PunishmentType.DAMAGE_PER_SECOND, 1.0f, 2f, true),
            new WeightPunishment(PunishmentType.KILL_MOUNT, null, 2f, null)
    );

    public static WeightConfig getInstance() {
        return AutoConfig.getConfigHolder(WeightConfig.class).getConfig();
    }
    public static void save() {
        AutoConfig.getConfigHolder(WeightConfig.class).save();
    }
    public static void load() {
        AutoConfig.getConfigHolder(WeightConfig.class).load();
    }

    public static class WeightTuple implements ConfigData {
        public String text;
        public float modifier;
        public MatchType type;

        WeightTuple() {
            this("", 1.0f, MatchType.PLAIN);
        }
        WeightTuple(String text, float modifier) {
            this(text, modifier, MatchType.PLAIN);
        }
        WeightTuple(String text, float modifier, MatchType type) {
            this.text = text;
            this.modifier = modifier;
            this.type = type;
        }
    }

    public static class WeightPunishment {
        //@ConfigEntry.Gui.EnumHandler // this is broken for complex lists in Cloth Config
        public PunishmentType type;
        public Float value;
        @Comment("This is a percentage of the player's max weight in which the punishment will take effect")
        public Float begin;
        public Boolean scaleWithWeight;

        WeightPunishment() {
            this(PunishmentType.SPEED, 1.0f, 0.0f, false);
        }
        WeightPunishment(PunishmentType type, Float value, Float begin, Boolean scaleWithWeight) {
            this.type = type;
            this.value = value;
            this.begin = begin;
            this.scaleWithWeight = scaleWithWeight;
        }
    }

    public enum PunishmentType {
        SPEED,
        KILL_MOUNT,
        PREVENT_MOUNT,
        EXHAUSTION_PER_TICK,
        EXHAUSTION_PER_SECOND,
        DAMAGE_PER_SECOND,
        DAMAGE_PER_SECOND_MOUNT
    }

    public enum DisplayType {
        NONE,
        NUMBERS,
        ICON
    }

    public enum WarningType {
        NONE,
        CENTERED_MESSAGE,
        CHAT_MESSAGE
    }

    public enum MatchType {
        PLAIN, // plain text matching (literal matching)
        REGEX, // regular expressions
        ITEM, // specific item id
        TAG // a tag id
    }
}
