package net.pullolo.magicitems.scrolls;

import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class LightningScroll extends Scroll {

    private double distance;
    private double damage;

    public LightningScroll() {
        type = "lightning";
        name = "Lightning Strike";
        description.add("Makes a lightning strike a set distance in front of you.");
        cooldown = 25;

        // Load config values
        FileConfiguration config = Bukkit.getPluginManager().getPlugin("MagicItems").getConfig();
        distance = config.getDouble("lightning_scroll.distance", 4.0);
        damage = config.getDouble("lightning_scroll.damage", 6.0);
    }

    @Override
    public void executeAbility(Player p) {
        if (CooldownApi.isOnCooldown("lightning-scroll", p)) {
            return;
        }

        Location l = p.getLocation().clone().add(p.getLocation().getDirection().clone().normalize().multiply(distance));
        for (int i = 0; i < 20; i++) {
            if (l.getBlock().isPassable()) {
                l.add(0, -1, 0);
                continue;
            }
            break;
        }

        // Spawn lightning strike
        p.getWorld().spawn(l, LightningStrike.class);

        // Damage nearby entities at the strike location
        for (Entity e : l.getNearbyEntities(3, 3, 3)) { // radius 3 blocks (tweak if needed)
            if (e instanceof LivingEntity && !e.equals(p)) {
                ((LivingEntity) e).damage(damage, p);
            }
        }

        CooldownApi.addCooldown("lightning-scroll", p, cooldown);
    }
}
