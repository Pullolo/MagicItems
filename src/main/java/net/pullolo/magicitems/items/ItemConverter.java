package net.pullolo.magicitems.items;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.pullolo.magicitems.scrolls.Scroll;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static net.pullolo.magicitems.MagicItems.*;
import static net.pullolo.magicitems.ModifiableItems.isArmor;
import static net.pullolo.magicitems.ModifiableItems.isRange;
import static net.pullolo.magicitems.utils.Utils.prettify;

public class ItemConverter {
    private final FileConfiguration config;
    private final NamespacedKey statKey;
    private final NamespacedKey qualityKey;
    private final NamespacedKey generalKey;
    private final NamespacedKey scrollKey;
    private List<String> melees;
    private List<String> range;
    private List<String> armor;
    private List<String> swords;
    private List<String> axes;

    public ItemConverter(FileConfiguration config, JavaPlugin plugin){
        this.config = config;
        statKey = new NamespacedKey(plugin, "magic-items-stat");
        qualityKey = new NamespacedKey(plugin, "magic-items-quality");
        generalKey = new NamespacedKey(plugin, "magic-items");
        scrollKey = new NamespacedKey(plugin, "magic-items-scrolls");
        loadNames();
    }

    public void convert(ItemStack item, String player){
        if (item.getAmount()>1) {
            logWarning("Plugin tried to convert more than 1 item, check config.yml!");
            return;
        }
        double quality = new Random().nextDouble()*100;
        boolean mythical = new Random().nextInt(10)==0;
        boolean scroll = new Random().nextInt(100)<5;
        setName(item, quality, mythical, scroll);
        setStats(item, player, quality, mythical, scroll);
    }

    private void setStats(ItemStack item, String player, double quality, boolean mythical, boolean scroll) {
        ItemMeta meta = item.getItemMeta();
        Random r = new Random();
        double value = mythical ? r.nextDouble()*10+10 : r.nextDouble()*10;
        String valueStr = String.valueOf(Math.ceil(value*10)/10);

        List<TextComponent> lore = new ArrayList<>();
        lore.add(
                Component.text("Quality: ").color(TextColor.fromHexString("#aaaaaa")).decoration(TextDecoration.ITALIC, false).append(
                        Component.text((Math.ceil(quality*10)/10)).color(getQualityColor(quality)).decoration(TextDecoration.ITALIC, false)
                )
        );
        if(isArmor(item)){
            lore.add(
                    Component.text("Additional Defence: ").color(TextColor.fromHexString("#aaaaaa")).decoration(TextDecoration.ITALIC, false).append(
                            Component.text(valueStr).color(TextColor.fromHexString("#5555FF")).decoration(TextDecoration.ITALIC, false)
                    ).append(
                            Component.text(" (" + getQualityBonus(quality) + ")").color(getQualityColor(quality)).decoration(TextDecoration.ITALIC, false)
                    )
            );
        } else {
            lore.add(
                    Component.text("Additional Damage: ").color(TextColor.fromHexString("#aaaaaa")).decoration(TextDecoration.ITALIC, false).append(
                            Component.text(valueStr).color(TextColor.fromHexString("#00AA00")).decoration(TextDecoration.ITALIC, false)
                    ).append(
                            Component.text(" (" + getQualityBonus(quality) + ")").color(getQualityColor(quality)).decoration(TextDecoration.ITALIC, false)
                    )
            );
        }
        lore.add(Component.text(""));

        if (!isArmor(item) && !isRange(item) && scroll){
            Scroll s = Scroll.getScroll(Scroll.getAllScrolls().get(r.nextInt(Scroll.getAllScrolls().size())));
            meta.getPersistentDataContainer().set(scrollKey, PersistentDataType.STRING, s.getType());
            lore.add(Component.text("Ancient Scroll: " + s.getName()).color(TextColor.fromHexString("#FFAA00")).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" RIGHT CLICK").color(TextColor.fromHexString("#FFFF55")).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false)));
            for (String str: s.getDescription()){
                lore.add(Component.text(str).color(TextColor.fromHexString("#AAAAAA")).decoration(TextDecoration.ITALIC, false));
            }
            lore.add(Component.text("Cooldown: ").color(TextColor.fromHexString("#555555")).decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(s.getCooldown()+"s").color(TextColor.fromHexString("#55FF55")).decoration(TextDecoration.ITALIC, false)));
            lore.add(Component.text(""));
        }

        lore.add(
                Component.text("Obtained by ").color(TextColor.fromHexString("#606060")).decoration(TextDecoration.ITALIC, false).append(
                        Component.text((player)).color(TextColor.fromHexString("#00ff5c")).decoration(TextDecoration.ITALIC, false)
                )
        );
        //Already set properly just get it and apply it, there is no need for quality calculations
        meta.getPersistentDataContainer().set(statKey, PersistentDataType.DOUBLE, value+getQualityBonus(quality));
        meta.getPersistentDataContainer().set(qualityKey, PersistentDataType.DOUBLE, quality);
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    private TextColor getQualityColor(double quality){
        if(quality>90){
            return TextColor.fromHexString("#ff28b1");
        }
        if(quality>60){
            return TextColor.fromHexString("#00ff5c");
        }
        if(quality>35){
            return TextColor.fromHexString("#ff6900");
        }
        return TextColor.fromHexString("#a30006");
    }

    private double getQualityBonus(double quality){
        if(quality>90){
            return 4;
        }
        if(quality>60){
            return 2;
        }
        if(quality>35){
            return 0.0;
        }
        return -2;
    }

    private String getQualityName(double quality){
        if(quality>90){
            return "Perfect ";
        }
        if(quality>60){
            return "Undamaged ";
        }
        if(quality>35){
            return "Damaged ";
        }
        return "Highly Damaged ";
    }

    private void setName(ItemStack item, double q, boolean mythical, boolean scroll){
        Random r = new Random();
        ItemMeta meta = item.getItemMeta();
        String name = getQualityName(q) + (mythical ? "Mythical ": "");
        if(isArmor(item)){
            try {
                name += armor.get(r.nextInt(armor.size())) + " " + item.getType().toString().toLowerCase().split("_")[1];
            } catch (Exception e){
                logWarning(e.getMessage());
            }
        } else if (isRange(item)){
            try {
                name += range.get(r.nextInt(range.size())) + " " + item.getType().toString().toLowerCase();
            } catch (Exception e){
                logWarning(e.getMessage());
            }
        } else {
            try {
                if(item.getType().toString().toLowerCase().contains("sword")){
                    name += melees.get(r.nextInt(melees.size())) + " " + swords.get(r.nextInt(swords.size()));
                } else if (item.getType().toString().toLowerCase().contains("_axe")){
                    name += melees.get(r.nextInt(melees.size())) + " " + axes.get(r.nextInt(axes.size()));
                } else {
                    try {
                        name += melees.get(r.nextInt(melees.size())) + " " + item.getType().toString().toLowerCase().split("_")[1];
                    } catch (Exception e){
                        name += melees.get(r.nextInt(melees.size())) + " " + item.getType().toString().toLowerCase();
                    }
                }
            } catch (Exception e){
                logWarning(e.getMessage());
            }
        }
        name += (scroll ? " ✧": "");
        TextColor color = mythical ? TextColor.fromHexString("#d600ff") : TextColor.fromHexString("#55FFFF");
        meta.displayName(Component.text(prettify(name)).color(color).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
    }
    private void loadNames(){
        try {
            melees = config.getStringList("names-list-melee");
        } catch (Exception e){
            logWarning("Couldn't load item's names, check config.yml!");
        }
        try {
            range = config.getStringList("names-list-range");
        } catch (Exception e){
            logWarning("Couldn't load item's names, check config.yml!");
        }
        try {
            armor = config.getStringList("names-list-armor");
        } catch (Exception e){
            logWarning("Couldn't load item's names, check config.yml!");
        }
        try {
            swords = config.getStringList("sword-names");
        } catch (Exception e){
            logWarning("Couldn't load item's names, check config.yml!");
        }
        try {
            axes = config.getStringList("axe-names");
        } catch (Exception e){
            logWarning("Couldn't load item's names, check config.yml!");
        }
        logInfo("All names loaded!");
        logInfo("If any warnings showed up, you could try to delete the config.yml file so new one could generate.");
    }

    public NamespacedKey getStatKey(){
        return statKey;
    }

    public NamespacedKey getGeneralKey() {
        return generalKey;
    }

    public NamespacedKey getScrollKey() {
        return scrollKey;
    }
}
