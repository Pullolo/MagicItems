package net.pullolo.magicitems.scrolls;

import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

import static net.pullolo.magicitems.MagicItems.magicItems;
import static net.pullolo.magicitems.MagicItems.particleApi;
import static net.pullolo.magicitems.utils.Utils.rotateVector;

public class ShriekScroll extends Scroll {

    private double damageDirectHit;
    private double damageExplosion;
    private int distance;
    private double speed;

    public ShriekScroll() {
        type = "shriek";
        name = "Shriek Missile";
        description.add("Shoot a powerful shriek missile in the");
        description.add("direction you are looking at.");
        cooldown = 16;

        var plugin = magicItems; // assuming magicItems is your plugin instance
        var config = plugin.getConfig();

        damageDirectHit = config.getDouble("shriek_scroll.damage_direct_hit", 18.0);
        damageExplosion = config.getDouble("shriek_scroll.damage_explosion", 16.0);
        distance = config.getInt("shriek_scroll.distance", 20);
        speed = config.getDouble("shriek_scroll.speed", 1.0);
    }

    @Override
    public void executeAbility(Player p) {
        if (CooldownApi.isOnCooldown("shriek-scroll", p)) {
            return;
        }
        magicMissile(p);
        CooldownApi.addCooldown("shriek-scroll", p, cooldown);
    }

    private void magicMissile(Player p) {
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1, 2);
        ArmorStand as = p.getWorld().spawn(p.getLocation().add(0, 1.5, 0), ArmorStand.class, en -> {
            en.setVisible(false);
            en.setGravity(false);
            en.setSmall(true);
            en.setMarker(true);
        });

        Location dest = p.getLocation().add(rotateVector(p.getLocation().getDirection(), 0).multiply(distance));
        Vector v = dest.subtract(p.getLocation().clone()).toVector();

        Color[] colors = new Color[3];
        colors[0] = Color.fromRGB(255, 59, 232);
        colors[1] = Color.fromRGB(0, 255, 217);
        colors[2] = Color.fromRGB(240, 214, 255);

        new BukkitRunnable() {
            final Random r = new Random();
            int i = 1;

            @Override
            public void run() {
                if (p == null) {
                    as.remove();
                    cancel();
                }

                particleApi.spawnColoredParticles(as.getLocation(), colors[r.nextInt(colors.length)], 1, 3, 0.01, 0.01, 0.01);
                if (r.nextBoolean())
                    particleApi.spawnParticles(as.getLocation(), Particle.GLOW, r.nextInt(10) + 1, 0.01, 0.01, 0.01, 0.01);
                as.teleport(as.getLocation().add(v.normalize().multiply(speed)));

                for (Entity entity : as.getLocation().getChunk().getEntities()) {
                    if (!as.isDead()) {
                        if (entity instanceof ArmorStand) {
                            continue;
                        }
                        if (as.getLocation().distanceSquared(entity.getLocation()) <= 3.8) {
                            if (!entity.equals(p)) {
                                if (entity instanceof LivingEntity) {
                                    ((LivingEntity) entity).damage(damageDirectHit, p);
                                    ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0));
                                    spellExplode(p, as.getLocation().clone());
                                    as.remove();
                                    cancel();
                                }
                            }
                        }
                    }
                }

                boolean isLiquid = as.getLocation().clone().getBlock().isLiquid();
                if (!as.getLocation().clone().getBlock().isPassable() || isLiquid) {
                    if (!as.isDead()) {
                        spellExplode(p, as.getLocation().clone());
                        as.remove();
                        cancel();
                    }
                }

                if (i > distance) {
                    if (!as.isDead()) {
                        spellExplode(p, as.getLocation().clone());
                        as.remove();
                        cancel();
                    }
                }
                i++;
            }
        }.runTaskTimer(magicItems, 0, 1);
    }

    private void spellExplode(Player p, Location l) {
        particleApi.spawnParticles(l, Particle.GUST, 1, 0, 0, 0, 1);
        particleApi.spawnParticles(l, Particle.GLOW, 100, 1, 1, 1, 10);
        particleApi.spawnParticles(l, Particle.FIREWORK, 100, 0.1, 0.1, 0.1, 0.4);
        l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1, 0.6f);
        l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1, 0.9f);
        l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, 1, 1);
        l.getWorld().playSound(l, Sound.ENTITY_WARDEN_SONIC_BOOM, 1, 2);
        for (Entity e : l.getWorld().getNearbyEntities(l, 2, 2, 2)) {
            if (e instanceof LivingEntity) {
                ((LivingEntity) e).damage(damageExplosion, p);
            }
        }
    }
}
