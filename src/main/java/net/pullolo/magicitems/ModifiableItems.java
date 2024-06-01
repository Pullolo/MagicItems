package net.pullolo.magicitems;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static net.pullolo.magicitems.MagicItems.logWarning;

public class ModifiableItems {
    private static final ArrayList<Material> materials = new ArrayList<>();

    public ModifiableItems(List<String> items){
        if (!materials.isEmpty()){
            return;
        }
        for (String s : items){
            try {
                materials.add(Material.valueOf(s));
            } catch (Exception e){
                logWarning("Cannot convert " + s + " to Material!");
                continue;
            }
        }
    }

    public static boolean canBeConverted(ItemStack item){
        return materials.contains(item.getType());
    }

    public static boolean isArmor(ItemStack item){
        String i = item.getType().toString().toLowerCase();
        return i.contains("helmet") || i.contains("chestplate") || i.contains("leggings") || i.contains("boots");
    }
    public static boolean isRange(ItemStack item){
        return item.getType().toString().toLowerCase().contains("bow");
    }
}
