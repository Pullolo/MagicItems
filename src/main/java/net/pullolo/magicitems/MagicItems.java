package net.pullolo.magicitems;

import net.pullolo.magicitems.events.ItemsEventHandler;
import net.pullolo.magicitems.items.ItemConverter;
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
        getServer().getPluginManager().registerEvents(new ItemsEventHandler(new ItemConverter(config, this)), this);
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
