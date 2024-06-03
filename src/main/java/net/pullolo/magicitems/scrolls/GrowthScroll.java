package net.pullolo.magicitems.scrolls;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class GrowthScroll extends Scroll{

    public GrowthScroll() {
        type = "growth";
        name = "Instant Heal";
        description.add("Grant yourself a small instant heal.");
        cooldown=15;
    }

    @Override
    public void executeAbility(Player p) {
        if (CooldownApi.isOnCooldown("growth-scroll", p)){
            return;
        }
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().clone().add(0, 1, 0), 5, 0.4, 0.1, 0.4, 0.1);
        double amount = p.getHealth()+5;
        try {
            p.setHealth(Math.min(amount, p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()));
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1.8f);
        } catch (Exception e){
            p.sendMessage(Component.text("You couldn't be healed!").color(TextColor.color(255, 0, 0)));
        }
        CooldownApi.addCooldown("growth-scroll", p, cooldown);
    }
}
