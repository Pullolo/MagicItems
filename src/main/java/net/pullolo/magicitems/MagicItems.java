package net.pullolo.magicitems;

import net.pullolo.magicitems.commands.Roll;
import net.pullolo.magicitems.events.ItemsEventHandler;
import net.pullolo.magicitems.items.ItemConverter;
import net.pullolo.magicitems.scrolls.FireScroll;
import net.pullolo.magicitems.scrolls.Scroll;
import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public final class MagicItems extends JavaPlugin {
    private static final Logger logger = Logger.getLogger("Minecraft");
    private static final String prefix = "[MagicItems] ";
    public static JavaPlugin magicItems;
    public static FileConfiguration config;


    @Override
    public void onEnable() {
        // Plugin startup logic
        magicItems = this;
        saveDefaultConfig();
        config = getConfig();
        init();
        ItemConverter converter = new ItemConverter(config, this);
        Scroll.init();
        getServer().getPluginManager().registerEvents(new ItemsEventHandler(converter), this);
        registerCommand(new Roll(converter), "roll");
        createCooldowns();
        logInfo("Hello from magic items!");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        logInfo("Goodbye from magic items!");
    }

    private void init(){
        try {
            new ModifiableItems(config.getStringList("convertable"));
        } catch (Exception e){
            logWarning(e.getMessage());
        }
    }

    private void createCooldowns(){
        CooldownApi.createCooldown("fire-scroll", new FireScroll().getCooldown());
    }

    private void registerCommand(CommandExecutor cmd, String cmdName){
        if (cmd instanceof TabCompleter){
            getCommand(cmdName).setExecutor(cmd);
            getCommand(cmdName).setTabCompleter((TabCompleter) cmd);
        } else {
            throw new RuntimeException("Provided object is not a command executor and a tab completer at the same time!");
        }
    }

    public static Logger getLog(){
        return logger;
    }

    public static void logInfo(String s){
        logger.info(prefix + s);
    }

    public static void logWarning(String s){
        logger.warning(prefix + s);
    }
}
