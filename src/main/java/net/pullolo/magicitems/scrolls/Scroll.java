package net.pullolo.magicitems.scrolls;

import org.bukkit.entity.Player;
import org.joml.AxisAngle4f;

import java.util.ArrayList;

public abstract class Scroll {
    private static final ArrayList<String> meleeScrolls = new ArrayList<>();
    private static final ArrayList<String> rangeScrolls = new ArrayList<>();
    protected String name;
    protected String type;
    protected ArrayList<String> description = new ArrayList<>();
    protected int cooldown;



    public abstract void executeAbility(Player p);

    public static Scroll getScroll(String type){
        switch (type){
            case "fire":
                return new FireScroll();
            case "wind":
                return new WindScroll();
            case "lightning":
                return new LightningScroll();
            case "growth":
                return new GrowthScroll();
            case "shriek":
                return new ShriekScroll();
            case "frenzy":
                return new FrenzyScroll();
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
        meleeScrolls.add("fire");
        meleeScrolls.add("wind");
        meleeScrolls.add("lightning");
        meleeScrolls.add("growth");
        meleeScrolls.add("frenzy");

        rangeScrolls.add("shriek");
    }

    public static ArrayList<String> getAllMeleeScrolls(){
        return meleeScrolls;
    }

    public static ArrayList<String> getAllRangeScrolls() {
        return rangeScrolls;
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
