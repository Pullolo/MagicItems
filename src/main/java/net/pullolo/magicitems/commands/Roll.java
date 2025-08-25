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
    private final int experienceCost;

    public Roll(ItemConverter converter, int experienceCost) {
        this.converter = converter;
        this.experienceCost = experienceCost;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!cmd.getName().equalsIgnoreCase("roll")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        Player p = (Player) sender;
        ItemStack item = p.getInventory().getItemInMainHand();

        if (!canBeConverted(item)) {
            p.sendMessage(ChatColor.RED + "Invalid item!");
            return true;
        }

        // Check experience
        if (p.getTotalExperience() < experienceCost) {
            p.sendMessage(ChatColor.RED + "You do not have enough experience to perform this roll!");
            return true;
        }

        // Deduct experience
        p.giveExp(-experienceCost);

        // Handle scroll roll (OPs only)
        if (args.length == 1 && args[0].equalsIgnoreCase("scroll")) {
            if (!p.isOp()) {
                return true; // silently ignore for non-OPs
            }

            do {
                convert(item, p, false);
            } while (!item.getItemMeta().getPersistentDataContainer().has(converter.getScrollKey()));
            p.sendMessage(ChatColor.GREEN + "Item re-converted!");
            return true;
        }

        // Default roll
        convert(item, p, true);
        return true;
    }

    private void convert(ItemStack item, Player p, boolean display) {
        ItemMeta meta = item.getItemMeta();

        if (!meta.getPersistentDataContainer().has(converter.getGeneralKey())) {
            meta.getPersistentDataContainer().set(converter.getGeneralKey(), PersistentDataType.STRING, "rolled");
            item.setItemMeta(meta);
            converter.convert(item, p.getName());
            p.sendMessage(ChatColor.GREEN + "Item converted!");
            return;
        }

        if (meta.getPersistentDataContainer().has(converter.getStatKey())) {
            meta.getPersistentDataContainer().remove(converter.getStatKey());
        }

        if (meta.getPersistentDataContainer().has(converter.getScrollKey())) {
            meta.getPersistentDataContainer().remove(converter.getScrollKey());
        }

        item.setItemMeta(meta);
        converter.convert(item, p.getName());

        if (display) {
            p.sendMessage(ChatColor.GREEN + "Item re-converted!");
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (!cmd.getName().equalsIgnoreCase("roll")) {
            return null;
        }

        if (!(sender instanceof Player)) {
            return null;
        }

        Player p = (Player) sender;

        if (args.length == 1) {
            ArrayList<String> completion = new ArrayList<>();
            if (p.isOp()) {
                addToCompletion("scroll", args[0], completion);
            }
            return completion;
        }

        return new ArrayList<>();
    }

    // Helper for partial matches in tab completion
    public void addToCompletion(String option, String currentArg, List<String> completion) {
        if (option.toLowerCase().startsWith(currentArg.toLowerCase())) {
            completion.add(option);
        }
    }
}
