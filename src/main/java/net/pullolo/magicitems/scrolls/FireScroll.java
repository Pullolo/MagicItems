package net.pullolo.magicitems.scrolls;

import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class FireScroll extends Scroll{

    public FireScroll(){
        type = "fire";
        name = "Fiery Ring";
        description.add("Unleash a small ring of fire around you");
        description.add("dealing damage and setting everyone on fire.");
        cooldown=20;
    }

    @Override
    public void executeAbility(Player p) {
        if (CooldownApi.isOnCooldown("fire-scroll", p)){
            return;
        }
        p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().clone().add(0, 1, 0), 120, 3, 0.5, 3, 0.01);
        p.getWorld().playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1, 0.7f);
        for (Entity e: p.getLocation().getNearbyEntities(4, 2, 4)){
            if (e.equals(p)) continue;
            if (!(e instanceof LivingEntity)) continue;
            e.setFireTicks(65);
            ((LivingEntity) e).damage(4, p);
        }
        CooldownApi.addCooldown("fire-scroll", p, cooldown);
    }
}
