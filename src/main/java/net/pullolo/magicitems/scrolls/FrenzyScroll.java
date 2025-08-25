package net.pullolo.magicitems.scrolls;

import net.pullolo.magicitems.utils.CooldownApi;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

public class FrenzyScroll extends Scroll {

    private double damage;
    private long startDelay;
    private long strikeInterval;
    private int numberOfStrikes;

    private double attackRange;
    private double attackCone;

    private double speedBuffRadius;
    private int speedBuffDuration;

    public FrenzyScroll() {
        type = "frenzy";
        name = "Frenzy";
        description.add("Perform multiple fast directional sweep attacks.");
        description.add("Each strike gets faster.");
        description.add("Grants nearby allies Speed I.");
        cooldown = 30;

        FileConfiguration config = Bukkit.getPluginManager().getPlugin("MagicItems").getConfig();

        damage = config.getDouble("frenzy_scroll.damage", 4.0);
        startDelay = config.getLong("frenzy_scroll.start_delay_ticks", 10);
        strikeInterval = config.getLong("frenzy_scroll.strike_interval_ticks", 8);
        numberOfStrikes = config.getInt("frenzy_scroll.number_of_strikes", 5);

        attackRange = config.getDouble("frenzy_scroll.attack_range", 3.0);
        attackCone = config.getDouble("frenzy_scroll.attack_cone", 0.5); // Dot product threshold ≈ 60 degrees cone

        speedBuffRadius = config.getDouble("frenzy_scroll.speed_buff_radius", 8.0);
        speedBuffDuration = config.getInt("frenzy_scroll.speed_buff_duration", 10);
    }

    @Override
    public void executeAbility(Player p) {
        if (CooldownApi.isOnCooldown("frenzy-scroll", p)) {
            return;
        }

        scheduleStrike(p, startDelay, strikeInterval, 0);

        // Speed buff to nearby allies
        for (Entity entity : p.getNearbyEntities(speedBuffRadius, speedBuffRadius, speedBuffRadius)) {
            if (entity instanceof Player target && !target.equals(p)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedBuffDuration * 20, 0));
                target.spawnParticle(Particle.CLOUD, target.getLocation().clone().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
            }
        }

        CooldownApi.addCooldown("frenzy-scroll", p, cooldown);

        // Play ding when cooldown ends
        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.isOnline()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
                }
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("MagicItems"), cooldown * 20L);
    }

    private void scheduleStrike(Player p, long delay, long currentInterval, int strikesDone) {
        if (strikesDone >= numberOfStrikes || !p.isOnline()) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) {
                    return;
                }

                Vector direction = p.getLocation().getDirection().normalize();
                Random random = new Random();

                // Spawn 5 sweep particles at varying heights for a nicer effect
                for (int i = 0; i < 5; i++) {
                    double yOffset = 1.0 + (random.nextDouble() * 0.6); // between 1.0 and 1.6 blocks high
                    Location particleLoc = p.getLocation().clone().add(0, yOffset, 0).add(direction.multiply(1.5));
                    p.getWorld().spawnParticle(Particle.SWEEP_ATTACK, particleLoc, 1);
                }

                // Play sweep attack sound, pitch increasing slightly per strike
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 1 + strikesDone * 0.1f);

                // Play trident return sound on each strike
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RETURN, 1, 1);

                // Damage entities in the directional cone
                for (Entity entity : p.getNearbyEntities(attackRange, attackRange, attackRange)) {
                    if (entity instanceof LivingEntity living && !living.equals(p)) {
                        Vector toEntity = entity.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                        double dot = direction.dot(toEntity);

                        if (dot > attackCone) { // inside cone
                            living.damage(damage, p);
                        }
                    }
                }

                // Check if this was the last strike
                if (strikesDone + 1 >= numberOfStrikes) {
                    // Delay the fire extinguish sound by 10 ticks (0.5 seconds)
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (p.isOnline()) {
                                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1, 1);
                            }
                        }
                    }.runTaskLater(Bukkit.getPluginManager().getPlugin("MagicItems"), 10L);
                } else {
                    // Schedule next strike with accelerated interval
                    long nextInterval = Math.max(1, (long) (currentInterval * 0.9));
                    scheduleStrike(p, nextInterval, nextInterval, strikesDone + 1);
                }
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("MagicItems"), delay);
    }
}
