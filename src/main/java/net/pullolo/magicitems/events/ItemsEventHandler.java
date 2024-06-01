package net.pullolo.magicitems.events;

import net.pullolo.magicitems.items.ItemConverter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.pullolo.magicitems.MagicItems.*;
import static net.pullolo.magicitems.ModifiableItems.canBeConverted;

public class ItemsEventHandler implements Listener {

    private final ItemConverter converter;
    private final NamespacedKey generalKey = new NamespacedKey(magicItems, "magic-items");
    private final List<EquipmentSlot> armorSlots = new ArrayList<>();

    public ItemsEventHandler(ItemConverter converter){
        armorSlots.add(EquipmentSlot.HEAD);
        armorSlots.add(EquipmentSlot.CHEST);
        armorSlots.add(EquipmentSlot.LEGS);
        armorSlots.add(EquipmentSlot.FEET);
        this.converter = converter;
    }

    @EventHandler
    public void onContainerOpened(InventoryOpenEvent event){
        Player p = (Player) event.getPlayer();
        for (ItemStack item : event.getInventory().getContents()){
            if (item == null) continue;
            if (!canBeConverted(item)){
                continue;
            }
            if (item.getItemMeta().getPersistentDataContainer().has(generalKey, PersistentDataType.STRING)){
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(generalKey, PersistentDataType.STRING, "rolled");
            item.setItemMeta(meta);
            if (new Random().nextInt(4)==0) converter.convert(item, p.getName());
        }
    }

    @EventHandler
    public void onItemCrafted(CraftItemEvent event){
        ItemStack item = event.getInventory().getResult();
        if (item == null) return;
        if (!canBeConverted(item)){
            return;
        }
        if (item.getItemMeta().getPersistentDataContainer().has(generalKey, PersistentDataType.STRING)){
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(generalKey, PersistentDataType.STRING, "rolled");
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onMobKill(EntityDeathEvent event){
        if (event.getEntity() instanceof Player){
            return;
        }
        for (ItemStack item : event.getDrops()){
            if (!canBeConverted(item)){
                continue;
            }
            if (item.getItemMeta().getPersistentDataContainer().has(generalKey, PersistentDataType.STRING)){
                continue;
            }
            ItemMeta meta = item.getItemMeta();
            meta.getPersistentDataContainer().set(generalKey, PersistentDataType.STRING, "rolled");
            item.setItemMeta(meta);
            try {
                if (event.getDamageSource().getCausingEntity() instanceof Player){
                    if (new Random().nextInt(4)==0) converter.convert(item, event.getDamageSource().getCausingEntity().getName());
                }
            } catch (Exception e){
                logWarning(e.getMessage());
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event){
        ItemStack item = event.getBow();
        if (item == null) return;
        if (!item.hasItemMeta()){
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(generalKey)){
            return;
        }
        if (!item.getItemMeta().getPersistentDataContainer().has(converter.getStatKey())){
            return;
        }
        event.getProjectile().getPersistentDataContainer().set(converter.getStatKey(), PersistentDataType.DOUBLE, item.getItemMeta().getPersistentDataContainer().get(converter.getStatKey(), PersistentDataType.DOUBLE));
    }

    @EventHandler
    public void onEntityDamagedByEntityEvent(EntityDamageByEntityEvent event){
        if (event.getDamager() instanceof Arrow){
            Arrow arrow = (Arrow) event.getDamager();
            if (!arrow.getPersistentDataContainer().has(converter.getStatKey())){
                return;
            }
            event.setDamage(event.getFinalDamage()+arrow.getPersistentDataContainer().get(converter.getStatKey(), PersistentDataType.DOUBLE));
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity)){
            return;
        }
        LivingEntity e = (LivingEntity) event.getDamager();
        if (e.getEquipment()==null){
            return;
        }
        ItemStack item = e.getEquipment().getItem(EquipmentSlot.HAND);
        if (item.getItemMeta()==null) return;
        if (!item.getItemMeta().getPersistentDataContainer().has(converter.getStatKey())) return;
        event.setDamage(event.getFinalDamage() + item.getItemMeta().getPersistentDataContainer().get(converter.getStatKey(), PersistentDataType.DOUBLE));
    }

    @EventHandler
    public void onDamaged(EntityDamageEvent event){
        if (event.getCause().equals(EntityDamageEvent.DamageCause.VOID) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.FALL) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.DROWNING) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.FALLING_BLOCK) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.FLY_INTO_WALL) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.CUSTOM) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.CRAMMING) ||
                event.getCause().equals(EntityDamageEvent.DamageCause.KILL)
        ){
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)){
            return;
        }
        LivingEntity e = (LivingEntity) event.getEntity();
        if (e.getEquipment()==null){
            return;
        }
        double def = 0.0;
        for (EquipmentSlot slot : armorSlots){
            ItemStack item = e.getEquipment().getItem(slot);
            if (item.getItemMeta()==null) continue;
            if (!item.getItemMeta().getPersistentDataContainer().has(converter.getStatKey())) continue;
            def += item.getItemMeta().getPersistentDataContainer().get(converter.getStatKey(), PersistentDataType.DOUBLE);
        }
        if (def<=0.0) return;
        event.setDamage(event.getFinalDamage()-(event.getFinalDamage()*(def/100)));
    }
}
