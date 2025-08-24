package net.pullolo.magicitems.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.pullolo.magicitems.MagicItems;
import net.pullolo.magicitems.ModifiableItems;
import net.pullolo.magicitems.items.ItemConverter;
import net.pullolo.magicitems.scrolls.Scroll;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ItemsEventHandler implements Listener {
    private final ItemConverter converter;
    private final List<EquipmentSlot> armorSlots = new ArrayList<>();

    public ItemsEventHandler(ItemConverter converter) {
        this.armorSlots.add(EquipmentSlot.HEAD);
        this.armorSlots.add(EquipmentSlot.CHEST);
        this.armorSlots.add(EquipmentSlot.LEGS);
        this.armorSlots.add(EquipmentSlot.FEET);
        this.converter = converter;
    }

    @EventHandler
    public void onContainerOpened(InventoryOpenEvent event) {
        Player p = (Player) event.getPlayer();
        for (ItemStack item : event.getInventory().getContents()) {
            if (item != null && ModifiableItems.canBeConverted(item)) {
                if (!item.getItemMeta().getPersistentDataContainer().has(this.converter.getGeneralKey(), PersistentDataType.STRING)) {
                    ItemMeta meta = item.getItemMeta();
                    meta.getPersistentDataContainer().set(this.converter.getGeneralKey(), PersistentDataType.STRING, "rolled");
                    item.setItemMeta(meta);
                    if ((new Random()).nextInt(4) == 0) this.converter.convert(item, p.getName());
                }
            }
        }
    }

    @EventHandler
    public void onItemCrafted(CraftItemEvent event) {
        ItemStack item = event.getInventory().getResult();
        if (item == null) return;
        if (!ModifiableItems.canBeConverted(item)) return;
        if (item.getItemMeta().getPersistentDataContainer().has(this.converter.getGeneralKey(), PersistentDataType.STRING)) return;

        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(this.converter.getGeneralKey(), PersistentDataType.STRING, "rolled");
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;

        for (ItemStack item : event.getDrops()) {
            if (!ModifiableItems.canBeConverted(item)) continue;
            if (item.getItemMeta().getPersistentDataContainer().has(this.converter.getGeneralKey(), PersistentDataType.STRING)) continue;

            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(this.converter.getGeneralKey(), PersistentDataType.STRING, "rolled");
            item.setItemMeta(meta);

            try {
                if (event.getDamageSource().getCausingEntity() instanceof Player &&
                        (new Random()).nextInt(4) == 0) {
                    this.converter.convert(item, event.getDamageSource().getCausingEntity().getName());
                }
            } catch (Exception e) {
                MagicItems.logWarning(e.getMessage());
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        ItemStack item = event.getBow();
        if (item == null) return;
        if (!item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(this.converter.getGeneralKey())) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(this.converter.getStatKey())) return;

        event.getProjectile().getPersistentDataContainer().set(this.converter.getStatKey(),
                PersistentDataType.DOUBLE,
                item.getItemMeta().getPersistentDataContainer().get(this.converter.getStatKey(), PersistentDataType.DOUBLE));
    }

    @EventHandler
    public void onEntityDamagedByEntityEvent(EntityDamageByEntityEvent event) {
        // Handle arrow damage (unchanged)
        if (event.getDamager() instanceof Arrow arrow) {
            if (!arrow.getPersistentDataContainer().has(this.converter.getStatKey())) return;
            event.setDamage(event.getFinalDamage() +
                    arrow.getPersistentDataContainer().get(this.converter.getStatKey(), PersistentDataType.DOUBLE));
            return;
        }

        // Handle melee attacks with quadratic scaling
        if (!(event.getDamager() instanceof Player player)) return;
        if (player.getEquipment() == null) return;

        ItemStack item = player.getEquipment().getItem(EquipmentSlot.HAND);
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(this.converter.getStatKey())) return;

        double rawBonus = item.getItemMeta().getPersistentDataContainer()
                .get(this.converter.getStatKey(), PersistentDataType.DOUBLE);

        float cooldown = player.getAttackCooldown(); // Between 0.0 and 1.0

        // Quadratic scaling like vanilla: scaled = 0.2 + cooldown^2 * 0.8
        double scale = 0.2 + Math.pow(cooldown, 2) * 0.8;
        double scaledBonus = rawBonus * scale;

        event.setDamage(event.getFinalDamage() + scaledBonus);
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event) {
        if (event.getCause().equals(EntityDamageEvent.DamageCause.VOID) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.FALL) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.DROWNING) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.FALLING_BLOCK) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.FLY_INTO_WALL) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.CUSTOM) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.CRAMMING) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.KILL)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity e)) return;

        // Only apply damage modification if noDamageTicks is zero (cooldown finished)
        if (e.getNoDamageTicks() > 0) {
            return;
        }

        if (e.getEquipment() == null) return;

        double def = 0.0D;
        for (EquipmentSlot slot : this.armorSlots) {
            ItemStack item = e.getEquipment().getItem(slot);
            if (item.getItemMeta() == null) continue;
            if (!item.getItemMeta().getPersistentDataContainer().has(this.converter.getStatKey())) continue;
            def += item.getItemMeta().getPersistentDataContainer().get(this.converter.getStatKey(), PersistentDataType.DOUBLE);
        }

        if (def <= 0.0D) return;
        event.setDamage(event.getFinalDamage() - event.getFinalDamage() * def / 100.0D);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getItem() == null || event.getItem().getItemMeta() == null) return;

        ItemStack item = event.getItem();
        if (!item.getItemMeta().getPersistentDataContainer().has(this.converter.getScrollKey())) return;

        if (ModifiableItems.isRange(item)) {
            if (!event.getAction().equals(Action.LEFT_CLICK_AIR) && !event.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
        } else {
            if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !event.getAction().equals(Action.RIGHT_CLICK_AIR)) return;
        }

        Player p = event.getPlayer();
        Scroll s = Scroll.getScroll(item.getItemMeta().getPersistentDataContainer().get(this.converter.getScrollKey(), PersistentDataType.STRING));
        s.executeAbility(p);
    }
}
