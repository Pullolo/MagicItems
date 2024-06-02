package net.pullolo.magicitems.commands;

import net.pullolo.magicitems.items.ItemConverter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.pullolo.magicitems.ModifiableItems.canBeConverted;

public class Roll extends Command implements CommandExecutor, TabCompleter {

    private final ItemConverter converter;

    public Roll(ItemConverter converter){
        this.converter = converter;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!cmd.getName().equalsIgnoreCase("roll")){
            return false;
        }
        if (!(sender instanceof Player)){
            sender.sendMessage("Only players can use this command!");
            return true;
        }
        Player p = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!canBeConverted(item)){
            p.sendMessage(ChatColor.RED + "Invalid item!");
            return true;
        }
        ItemMeta meta = item.getItemMeta();
        if (!item.getItemMeta().getPersistentDataContainer().has(converter.getGeneralKey())){
            meta.getPersistentDataContainer().set(converter.getGeneralKey(), PersistentDataType.STRING, "rolled");
            item.setItemMeta(meta);
            converter.convert(item, p.getName());
            p.sendMessage(ChatColor.GREEN + "Item converted!");
            return true;
        }
        if (item.getItemMeta().getPersistentDataContainer().has(converter.getStatKey())){
            meta.getPersistentDataContainer().remove(converter.getStatKey());
        }
        if (item.getItemMeta().getPersistentDataContainer().has(converter.getScrollKey())){
            meta.getPersistentDataContainer().remove(converter.getScrollKey());
        }
        item.setItemMeta(meta);
        converter.convert(item, p.getName());
        p.sendMessage(ChatColor.GREEN + "Item re-converted!");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return new ArrayList<>();
    }
}
