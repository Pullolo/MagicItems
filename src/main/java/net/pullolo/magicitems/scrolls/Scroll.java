package net.pullolo.magicitems.scrolls;

import org.bukkit.entity.Player;

import java.util.ArrayList;

public abstract class Scroll {
    private static final ArrayList<String> scrolls = new ArrayList<>();
    protected String name;
    protected String type;
    protected ArrayList<String> description = new ArrayList<>();
    protected int cooldown;

    public abstract void executeAbility(Player p);

    public static Scroll getScroll(String type){
        switch (type){
            case "fire":
                return new FireScroll();
            default:
                return new Scroll() {
                    @Override
                    public void executeAbility(Player p) {
                        p.sendMessage("No ability found!");
                    }
                };
        }
    }

    public static void init(){
        scrolls.add("fire");
    }

    public static ArrayList<String> getAllScrolls(){
        return scrolls;
    }

    public String getName() {
        return name;
    }
    public String getType(){
        return type;
    }

    public ArrayList<String> getDescription() {
        return description;
    }

    public int getCooldown() {
        return cooldown;
    }
}
