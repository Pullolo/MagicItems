package net.pullolo.magicitems.scrolls;

import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WindScroll extends Scroll {

    private double velocityMultiplier;

    public WindScroll() {
        type = "wind";
        name = "Cloud Boost";
        description.add("Get a small dash by bouncing off clouds.");
        cooldown = 8; // still hardcoded

        var plugin = Bukkit.getPluginManager().getPlugin("MagicItems"); // Replace if needed
        var config = plugin.getConfig();

        velocityMultiplier = config.getDouble("wind_scroll.velocity_multiplier", 1.5);
    }

    @Override
    public void executeAbility(Player p) {
        if (CooldownApi.isOnCooldown("wind-scroll", p)) {
            return;
        }
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().clone(), 20, 0.1, 0, 0.1, 0.1);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1, 1.2f);
        p.setVelocity(p.getLocation().getDirection().clone().normalize().add(new Vector(0, 0.5, 0)).normalize().multiply(velocityMultiplier));
        CooldownApi.addCooldown("wind-scroll", p, cooldown);
    }
}
