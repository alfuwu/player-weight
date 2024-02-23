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
            new WeightTuple("Gold", 20.0f),
            new WeightTuple("Diamond", 1.2f),
            new WeightTuple("Golden (Pickaxe|Axe|Shovel|Hoe|Sword)", 0.1f, true)
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
        public boolean regex;

        WeightTuple() {
            this("", 1.0f, false);
        }
        WeightTuple(String text, float modifier) {
            this(text, modifier, false);
        }
        WeightTuple(String text, float modifier, boolean regex) {
            this.text = text;
            this.modifier = modifier;
            this.regex = regex;
        }
    }

    public static class WeightPunishment {
        @ConfigEntry.Gui.EnumHandler
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
        NUMBERS,
        ICON
    }

    public enum WarningType {
        NONE,
        CENTERED_MESSAGE,
        CHAT_MESSAGE
    }
}
