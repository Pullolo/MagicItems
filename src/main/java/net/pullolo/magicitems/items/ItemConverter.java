package net.pullolo.magicitems.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.pullolo.magicitems.MagicItems;
import net.pullolo.magicitems.ModifiableItems;
import net.pullolo.magicitems.scrolls.Scroll;
import net.pullolo.magicitems.utils.Utils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemConverter {

    private final FileConfiguration config;
    private final NamespacedKey statKey;
    private final NamespacedKey qualityKey;
    private final NamespacedKey generalKey;
    private final NamespacedKey scrollKey;
    private final NamespacedKey modifierUUIDKey;

    public ItemConverter(FileConfiguration config, JavaPlugin plugin) {
        this.config = config;
        this.statKey = new NamespacedKey(plugin, "magic-items-stat");
        this.qualityKey = new NamespacedKey(plugin, "magic-items-quality");
        this.generalKey = new NamespacedKey(plugin, "magic-items");
        this.scrollKey = new NamespacedKey(plugin, "magic-items-scrolls");
        this.modifierUUIDKey = new NamespacedKey(plugin, "magic-items-modifier-uuid");
        loadNames();
    }

    public void convert(ItemStack item, String player) {
        if (item.getAmount() > 1) {
            MagicItems.logWarning("Plugin tried to convert more than 1 item, check config.yml!");
            return;
        }

        double quality = new Random().nextDouble() * 100.0;
        boolean mythical = quality > 60.0 && new Random().nextInt(10) == 0;
        boolean scroll = new Random().nextInt(100) < 7;

        setName(item, quality, mythical, scroll);
        setStats(item, player, quality, mythical, scroll);
    }

    private void setStats(ItemStack item, String player, double quality, boolean mythical, boolean scroll) {
        ItemMeta meta = item.getItemMeta();
        Random r = new Random();

        // === Reset stats and lore (for armor only) ===
        if (ModifiableItems.isArmor(item)) {
            // Remove all attribute modifiers
            if (meta.hasAttributeModifiers()) {
                var modifiers = meta.getAttributeModifiers();
                if (modifiers != null) {
                    for (Attribute attribute : modifiers.keySet()) {
                        for (AttributeModifier modifier : modifiers.get(attribute)) {
                            meta.removeAttributeModifier(attribute, modifier);
                        }
                    }
                }
            }

            // Clear lore
            meta.lore(null);

            // Remove relevant persistent data
            meta.getPersistentDataContainer().remove(statKey);
            meta.getPersistentDataContainer().remove(qualityKey);
            meta.getPersistentDataContainer().remove(scrollKey);

            item.setItemMeta(meta); // Re-apply cleaned meta
            meta = item.getItemMeta(); // Refresh meta reference
        }

        // === Continue with custom logic ===
        double value = mythical ? (r.nextDouble() * 10.0 + 10.0) : (r.nextDouble() * 10.0);
        String valueStr = String.valueOf(Math.ceil(value * 10.0) / 10.0);

        double armorToughness = 0;
        double knockbackResistance = 0;

        // === Generate or retrieve unique UUID for this item ===
        String uuidString = meta.getPersistentDataContainer().get(modifierUUIDKey, PersistentDataType.STRING);
        UUID itemUUID;
        if (uuidString == null) {
            itemUUID = UUID.randomUUID();
            meta.getPersistentDataContainer().set(modifierUUIDKey, PersistentDataType.STRING, itemUUID.toString());
        } else {
            itemUUID = UUID.fromString(uuidString);
        }

        // === Handle armor-specific modifiers ===
        if (ModifiableItems.isArmor(item)) {
            armorToughness = Math.ceil(r.nextDouble() * 5.0 * 10.0) / 10.0;
            knockbackResistance = Math.ceil(r.nextDouble() * 0.25 * 100.0) / 100.0;

            double baseArmor = getBaseArmorValue(item.getType());
            if (baseArmor > 0) {
                meta.addAttributeModifier(Attribute.GENERIC_ARMOR,
                        new AttributeModifier(itemUUID, "magicitems-base-armor", baseArmor, AttributeModifier.Operation.ADD_NUMBER, getArmorSlot(item.getType())));
            }

            meta.addAttributeModifier(Attribute.GENERIC_ARMOR_TOUGHNESS,
                    new AttributeModifier(itemUUID, "magicitems-armor-toughness", armorToughness, AttributeModifier.Operation.ADD_NUMBER, getArmorSlot(item.getType())));

            meta.addAttributeModifier(Attribute.GENERIC_KNOCKBACK_RESISTANCE,
                    new AttributeModifier(itemUUID, "magicitems-knockback-resistance", knockbackResistance, AttributeModifier.Operation.ADD_NUMBER, getArmorSlot(item.getType())));
        }

        // === Lore logic ===
        List<Component> existingLore = meta.hasLore() ? meta.lore() : new ArrayList<>();
        List<Component> newLore = new ArrayList<>();
        boolean skip = false;

        for (Component line : existingLore) {
            if (line instanceof TextComponent textLine) {
                String text = textLine.content().toLowerCase();

                if (text.contains("ancient scroll:")) {
                    skip = true;
                    continue;
                }
                if (text.contains("cooldown:")) {
                    skip = false;
                    continue;
                }
                if (skip || text.contains("quality:")
                        || text.contains("additional damage:")
                        || text.contains("additional defence:")
                        || text.contains("damage:")
                        || text.contains("defence:")
                        || text.contains("armor toughness:")
                        || text.contains("toughness:")
                        || text.contains("knockback resistance:")
                        || text.contains("bonus")
                        || text.contains("obtained by")) {
                    continue;
                }
            }
            newLore.add(line);
        }

        // === Add Quality Info ===
        newLore.add(Component.text("Quality: ").color(TextColor.fromHexString("#aaaaaa")).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(Math.ceil(quality * 10.0) / 10.0)
                        .color(getQualityColor(quality)).decoration(TextDecoration.ITALIC, false)));

        // === Bonus Section ===
        newLore.add(Component.text("Bonus")
                .color(TextColor.fromHexString("#AAAAAA"))
                .decoration(TextDecoration.UNDERLINED, true)
                .decoration(TextDecoration.ITALIC, false));

        if (ModifiableItems.isArmor(item)) {
            newLore.add(Component.text("Defence: ")
                    .color(TextColor.fromHexString("#AAAAAA"))
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(valueStr)
                            .color(TextColor.fromHexString("#5555FF"))
                            .decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(" (" + getQualityBonus(quality) + ")")
                            .color(getQualityColor(quality))
                            .decoration(TextDecoration.ITALIC, false)));

            newLore.add(Component.text("Toughness: ")
                    .color(TextColor.fromHexString("#AAAAAA"))
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(armorToughness))
                            .color(TextColor.fromHexString("#55AAFF"))
                            .decoration(TextDecoration.ITALIC, false)));

            newLore.add(Component.text("Knockback Resistance: ")
                    .color(TextColor.fromHexString("#AAAAAA"))
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(String.valueOf(knockbackResistance))
                            .color(TextColor.fromHexString("#55AA55"))
                            .decoration(TextDecoration.ITALIC, false)));
        } else {
            newLore.add(Component.text("Damage: ")
                    .color(TextColor.fromHexString("#AAAAAA"))
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(valueStr)
                            .color(TextColor.fromHexString("#00AA00"))
                            .decoration(TextDecoration.ITALIC, false))
                    .append(Component.text(" (" + getQualityBonus(quality) + ")")
                            .color(getQualityColor(quality))
                            .decoration(TextDecoration.ITALIC, false)));
        }

        // === Scroll logic ===
        if (!ModifiableItems.isArmor(item) && scroll) {
            Scroll s;
            if (ModifiableItems.isRange(item)) {
                s = Scroll.getScroll(Scroll.getAllRangeScrolls().get(r.nextInt(Scroll.getAllRangeScrolls().size())));
            } else {
                s = Scroll.getScroll(Scroll.getAllMeleeScrolls().get(r.nextInt(Scroll.getAllMeleeScrolls().size())));
            }

            meta.getPersistentDataContainer().set(this.scrollKey, PersistentDataType.STRING, s.getType());

            newLore.add(Component.text("Ancient Scroll: " + s.getName()).color(TextColor.fromHexString("#FFAA00")).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(ModifiableItems.isRange(item) ? " LEFT CLICK" : " RIGHT CLICK")
                            .color(TextColor.fromHexString("#FFFF55")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false)));

            for (String str : s.getDescription()) {
                newLore.add(Component.text(str).color(TextColor.fromHexString("#AAAAAA")).decoration(TextDecoration.ITALIC, false));
            }

            newLore.add(Component.text("Cooldown: ").color(TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(s.getCooldown() + "s").color(TextColor.fromHexString("#55FF55")).decoration(TextDecoration.ITALIC, false)));
        } else {
            meta.getPersistentDataContainer().remove(this.scrollKey);
        }

        // === Player info ===
        newLore.add(Component.text("Obtained by ").color(TextColor.fromHexString("#606060")).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(player).color(TextColor.fromHexString("#00ff5c")).decoration(TextDecoration.ITALIC, false)));

        // === Set final stats ===
        meta.getPersistentDataContainer().set(this.statKey, PersistentDataType.DOUBLE, value + getQualityBonus(quality));
        meta.getPersistentDataContainer().set(this.qualityKey, PersistentDataType.DOUBLE, quality);
        meta.lore(newLore);
        item.setItemMeta(meta);
    }

    private void removeModifier(ItemMeta meta, Attribute attribute, UUID uuid) {
        if (meta.getAttributeModifiers(attribute) != null) {
            for (AttributeModifier mod : meta.getAttributeModifiers(attribute)) {
                if (mod.getUniqueId().equals(uuid)) {
                    meta.removeAttributeModifier(attribute, mod);
                    break;
                }
            }
        }
    }

    private double getBaseArmorValue(Material material) {
        return switch (material) {
            case LEATHER_HELMET -> 1;
            case LEATHER_CHESTPLATE -> 3;
            case LEATHER_LEGGINGS -> 2;
            case LEATHER_BOOTS -> 1;
            case IRON_HELMET -> 2;
            case IRON_CHESTPLATE -> 6;
            case IRON_LEGGINGS -> 5;
            case IRON_BOOTS -> 2;
            case DIAMOND_HELMET, NETHERITE_HELMET -> 3;
            case DIAMOND_CHESTPLATE, NETHERITE_CHESTPLATE -> 8;
            case DIAMOND_LEGGINGS, NETHERITE_LEGGINGS -> 6;
            case DIAMOND_BOOTS, NETHERITE_BOOTS -> 3;
            case GOLDEN_HELMET -> 2;
            case GOLDEN_CHESTPLATE -> 5;
            case GOLDEN_LEGGINGS -> 3;
            case GOLDEN_BOOTS -> 1;
            case CHAINMAIL_HELMET -> 2;
            case CHAINMAIL_CHESTPLATE -> 5;
            case CHAINMAIL_LEGGINGS -> 4;
            case CHAINMAIL_BOOTS -> 1;
            default -> 0;
        };
    }

    private void setName(ItemStack item, double q, boolean mythical, boolean scroll) {
        String baseName;
        ItemMeta meta = item.getItemMeta();

        if (meta.hasDisplayName()) {
            baseName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(meta.displayName()));
            baseName = baseName
                    .replaceFirst("^Perfect ", "")
                    .replaceFirst("^Undamaged ", "")
                    .replaceFirst("^Damaged ", "")
                    .replaceFirst("^Highly Damaged ", "")
                    .replaceFirst("^Mythical ", "")
                    .replace(" ✧", "")
                    .trim();
        } else {
            baseName = item.getType().toString();
        }

        String name = getQualityName(q) + (mythical ? "Mythical " : "") + baseName + (scroll ? " ✧" : "");
        TextColor color = mythical ? TextColor.fromHexString("#d600ff") : TextColor.fromHexString("#55FFFF");

        meta.displayName(Component.text(Utils.prettify(name)).color(color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
    }

    private TextColor getQualityColor(double quality) {
        if (quality > 90.0) return TextColor.fromHexString("#ff28b1");
        if (quality > 60.0) return TextColor.fromHexString("#00ff5c");
        if (quality > 35.0) return TextColor.fromHexString("#ff6900");
        return TextColor.fromHexString("#a30006");
    }

    private double getQualityBonus(double quality) {
        if (quality > 90.0) return 4.0;
        if (quality > 60.0) return 2.0;
        if (quality > 35.0) return 0.0;
        return -2.0;
    }

    private String getQualityName(double quality) {
        if (quality > 90.0) return "Perfect ";
        if (quality > 60.0) return "Undamaged ";
        if (quality > 35.0) return "Damaged ";
        return "Highly Damaged ";
    }

    private void loadNames() {
        try {
            config.getStringList("names-list-melee");
            config.getStringList("names-list-range");
            config.getStringList("names-list-armor");
            config.getStringList("sword-names");
            config.getStringList("axe-names");
        } catch (Exception e) {
            MagicItems.logWarning("Couldn't load item's names, check config.yml!");
        }

        MagicItems.logInfo("All names loaded!");
        MagicItems.logInfo("If any warnings showed up, you could try to delete the config.yml file so a new one could generate.");
    }

    private org.bukkit.inventory.EquipmentSlot getArmorSlot(Material mat) {
        return switch (mat) {
            case LEATHER_HELMET, IRON_HELMET, DIAMOND_HELMET, GOLDEN_HELMET,
                 NETHERITE_HELMET, CHAINMAIL_HELMET -> org.bukkit.inventory.EquipmentSlot.HEAD;
            case LEATHER_CHESTPLATE, IRON_CHESTPLATE, DIAMOND_CHESTPLATE, GOLDEN_CHESTPLATE,
                 NETHERITE_CHESTPLATE, CHAINMAIL_CHESTPLATE -> org.bukkit.inventory.EquipmentSlot.CHEST;
            case LEATHER_LEGGINGS, IRON_LEGGINGS, DIAMOND_LEGGINGS, GOLDEN_LEGGINGS,
                 NETHERITE_LEGGINGS, CHAINMAIL_LEGGINGS -> org.bukkit.inventory.EquipmentSlot.LEGS;
            case LEATHER_BOOTS, IRON_BOOTS, DIAMOND_BOOTS, GOLDEN_BOOTS,
                 NETHERITE_BOOTS, CHAINMAIL_BOOTS -> org.bukkit.inventory.EquipmentSlot.FEET;
            default -> org.bukkit.inventory.EquipmentSlot.CHEST; // fallback (shouldn't happen)
        };
    }

    public NamespacedKey getStatKey() {
        return this.statKey;
    }

    public NamespacedKey getGeneralKey() {
        return this.generalKey;
    }

    public NamespacedKey getScrollKey() {
        return this.scrollKey;
    }
}
