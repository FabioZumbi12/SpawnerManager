package br.net.fabiozumbi12.spawnermanager;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SpawnerListener implements Listener {

    private SpawnerManager plugin;

    SpawnerListener(SpawnerManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        ItemStack hand = event.getItemInHand();

        if (block.getType().equals(Material.SPAWNER) && block.getState() instanceof CreatureSpawner) {

            if (!event.getPlayer().hasPermission("spawnermanager.place")) {
                event.setCancelled(true);
                return;
            }

            EntityType entity;
            if (hand.hasItemMeta() && hand.getItemMeta().hasLore()) {
                List<String> lore = hand.getItemMeta().getLore();
                Optional<EntityType> oEntity = Arrays.stream(EntityType.values()).filter(e -> e.name().equals(ChatColor.stripColor(lore.get(0)))).findFirst();
                entity = oEntity.orElse(EntityType.PIG);
            } else {
                entity = EntityType.PIG;
            }

            CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();
            creatureSpawner.setSpawnedType(entity);
            creatureSpawner.update(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (block.getType().equals(Material.SPAWNER) && block.getState() instanceof CreatureSpawner) {
            event.setDropItems(false);
            String type = ((CreatureSpawner) block.getState()).getSpawnedType().name();

            // Check if can break
            if (!player.hasPermission("spawnermanager.break." + type.toLowerCase()) && !player.hasPermission("spawnermanager.break.all")) {
                event.setCancelled(true);
                return;
            }

            // Check if can drop exp
            if (!player.hasPermission("spawnermanager.break.experience")) {
                event.setExpToDrop(0);
            }

            // Check if can drop the spawner
            if (!player.hasPermission("spawnermanager.break.drop")) return;

            if (validTool(player.getInventory().getItemInMainHand()) || validTool(player.getInventory().getItemInOffHand())) {
                ItemStack item = new ItemStack(Material.SPAWNER, 1);
                plugin.setItemSpawnwer(item, type, player.getName());

                // Check if should put on inventory
                if (player.hasPermission("spawnermanager.break.drop.inventory")){
                    HashMap<Integer, ItemStack> items =player.getInventory().addItem(item);
                    if (!items.isEmpty()) {
                        items.values().forEach(v->{
                            player.getWorld().dropItemNaturally(player.getLocation(), v);
                            player.sendMessage(plugin.getLang("nospaceinventory", true)
                                    .replace("{spawner}",item.getAmount() + "x " + plugin.capSpawnerName(type)));
                        });
                    }
                    return;
                }
                player.getWorld().dropItemNaturally(block.getLocation(), item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(InventoryClickEvent event) {
        if (event.getView().getType() == InventoryType.ANVIL) {
            if (event.getInventory().getItem(2) != null && event.getInventory().getItem(2).getType().equals(Material.SPAWNER))
                event.getInventory().setItem(2, new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChangeEgg(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block != null && block.getType().equals(Material.SPAWNER)) {
            ItemStack egg = null;
            if (player.getInventory().getItemInMainHand().getType().name().endsWith("_EGG"))
                egg = player.getInventory().getItemInMainHand();
            if (player.getInventory().getItemInOffHand().getType().name().endsWith("_EGG"))
                egg = player.getInventory().getItemInOffHand();
            if (egg != null) {
                String entity = egg.getType().name().split("_")[0];

                if (((CreatureSpawner) block.getState()).getSpawnedType().name().equals(entity)) {
                    player.sendMessage(plugin.getLang("alreadytype", true).replace("{type}", SpawnerManager.get().capSpawnerName(entity)));
                    event.setCancelled(true);
                    return;
                }
                if (!player.hasPermission("spawnermanager.change." + entity.toLowerCase()) && !player.hasPermission("spawnermanager.change.all")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean validTool(ItemStack itemStack) {
        if (itemStack == null) return false;

        List<String> tools = plugin.getConfig().getStringList("config.allowedTools");
        if (tools.stream().noneMatch(t -> itemStack.getType().name().equalsIgnoreCase(t)))
            return false;

        List<String> enchants = plugin.getConfig().getStringList("config.allowedEnchants");
        for (String e : enchants) {
            String[] split = e.split(":");
            if (split.length == 2) {
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(split[0].toLowerCase()));
                if (ench == null)
                    continue;
                if (itemStack.containsEnchantment(ench) &&
                        itemStack.getEnchantmentLevel(ench) == Integer.parseInt(split[1])) {
                    return true;
                }
            }
        }
        return false;
    }
}
