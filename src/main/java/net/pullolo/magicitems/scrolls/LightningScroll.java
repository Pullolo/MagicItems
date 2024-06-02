package net.pullolo.magicitems.scrolls;

import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class LightningScroll extends Scroll{

    public LightningScroll(){
        type = "lightning";
        name = "Lightning Strike";
        description.add("Makes a lightning strike 4 blocks in front of you.");
        cooldown=25;
    }

    @Override
    public void executeAbility(Player p) {
        if (CooldownApi.isOnCooldown("lightning-scroll", p)){
            return;
        }
        Location l = p.getLocation().clone().add(p.getLocation().getDirection().clone().normalize().multiply(4));
        for (int i = 0; i<20; i++){
            if (l.getBlock().isPassable()){
                l.add(0, -1, 0);
                continue;
            }
            break;
        }
        p.getWorld().spawn(l, LightningStrike.class);
        CooldownApi.addCooldown("lightning-scroll", p, cooldown);
    }
}
