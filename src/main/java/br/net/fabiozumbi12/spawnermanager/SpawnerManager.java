package br.net.fabiozumbi12.spawnermanager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public final class SpawnerManager extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static SpawnerManager plugin;
    private File config = new File(getDataFolder(), "config.yml");

    static SpawnerManager get() {
        return plugin;
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        plugin = this;

        if (!getDataFolder().exists())
            getDataFolder().mkdir();

        getConfig().set("config.allowedTools", getConfig().get("config.allowedTools", Collections.singletonList("DIAMOND_PICKAXE")));
        getConfig().set("config.allowedEnchants", getConfig().get("config.allowedEnchants", Collections.singletonList("SILK_TOUCH:1")));
        getConfig().set("config.setMinedByOnLore", getConfig().get("config.setMinedByOnLore", false));
        getConfig().set("config.logOnConsole", getConfig().get("config.logOnConsole", true));


        getConfig().set("lang.prefix", getConfig().get("lang.prefix", "&7[&8SpawnManager&7]&r "));
        getConfig().set("lang.reloaded", getConfig().get("lang.reloaded", "&aSpawnerManager reloaded with success!"));
        getConfig().set("lang.onlyplayers", getConfig().get("lang.onlyplayers", "&cOnly players can use this command"));
        getConfig().set("lang.setto", getConfig().get("lang.setto", "&2Spawner set to &6{type}"));
        getConfig().set("lang.invalid", getConfig().get("lang.invalid", "&cIncorrect spawner type or the item is not a spawner"));
        getConfig().set("lang.noonlineplayer", getConfig().get("lang.noonlineplayer", "&cNo online player by name &6{player}"));
        getConfig().set("lang.noentitytype", getConfig().get("lang.noentitytype", "&cNo online player by name &6{player}"));
        getConfig().set("lang.invalidstacksize", getConfig().get("lang.invalidstacksize", "&6{size} &cis not a valid stack size"));
        getConfig().set("lang.invalidnumber", getConfig().get("lang.invalidnumber", "&6{number} &cis not a valid number"));
        getConfig().set("lang.given", getConfig().get("lang.given", "&2Given &6{amount}x &2spawner(s) of &6{type} &2to player &6{player}"));
        getConfig().set("lang.alreadytype", getConfig().get("lang.alreadytype", "&cThis spawner is already the type of &6{type}"));
        getConfig().set("lang.spawnername", getConfig().get("lang.spawnername", "&2&lSpawner of &6&l{type}"));
        getConfig().set("lang.minedby", getConfig().get("lang.minedby", "&5> &aMined by {player}"));
        getConfig().set("lang.placed", getConfig().get("lang.placed", "&2A spawner of &6{type} &2was placed by &6{player} &2on &6{location}"));
        getConfig().set("lang.changed", getConfig().get("lang.changed", "&2The player &6{player} &2was changed a spawner type from &6{from} &2to &6{to} &2on &6{location}"));
        getConfig().set("lang.broken", getConfig().get("lang.broken", "&2A spawner of &6{type} &2was broken by &6{player} &2using &6{tool} &2on &6{location}"));
        getConfig().set("lang.nospaceinventory", getConfig().get("lang.nospaceinventory", "&cYou have no more space available in your inventory. We throw &6{spawner} &cin your position!"));
        getConfig().set("lang.notspawner", getConfig().get("lang.notspawner", "&cThis block is not a spawner"));
        getConfig().set("lang.setwild", getConfig().get("lang.setwild", "&2Spawner set &6Wild &2with success!"));
        getConfig().set("lang.notsetwild", getConfig().get("lang.notsetwild", "&cThis spawner is already a &6Wild &2spawner"));
        try {
            getConfig().save(config);
        } catch (IOException e) {
            e.printStackTrace();
        }
        getServer().getPluginManager().registerEvents(new SpawnerListener(this), this);
        getLogger().info("SpawnerManager loaded!");
    }

    String getLang(String key, boolean prefix) {
        return ChatColor.translateAlternateColorCodes('&', (prefix ? getConfig().get("lang.prefix") : "") + getConfig().getString("lang." + key, "Lang not found for " + key));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("SpawnerManager disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("spawnermanager.command.reload")) return false;

                sender.sendMessage(getLang("reloaded", true));
                reloadConfig();
                return true;
            }
        }

        if (args.length == 2 || args.length == 3) {

            if (args.length == 2 && args[0].equalsIgnoreCase("setwild") && sender instanceof Player) {
                if (!sender.hasPermission("spawnermanager.command.setwild")) return false;

                Player player = (Player) sender;
                Optional<Block> optSpawn = player.getLineOfSight(null, 15).stream().filter(e -> e.getType().equals(Material.SPAWNER)).findFirst();
                if (optSpawn.isPresent()) {
                    Block spawner = optSpawn.get();
                    if (spawner.hasMetadata("mined")) {
                        spawner.removeMetadata("mined", plugin);
                        sender.sendMessage(getLang("setwild", true));
                    } else {
                        sender.sendMessage(getLang("notsetwild", true));
                    }
                } else {
                    sender.sendMessage(getLang("notspawner", true));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("change")) {
                if (!sender.hasPermission("spawnermanager.command.change")) return false;

                Player player = null;

                if (args.length == 3) {
                    if (Bukkit.getPlayer(args[2]) != null)
                        player = Bukkit.getPlayer(args[2]);
                    else {
                        sender.sendMessage(getLang("noonlineplayer", true).replace("{player}", args[1]));
                        return false;
                    }
                }

                if (player == null) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(getLang("onlyplayers", true));
                        return false;
                    } else {
                        player = (Player) sender;
                    }
                }

                if (Arrays.stream(EntityType.values()).noneMatch(e -> e.name().equalsIgnoreCase(args[1])))
                    return false;

                String entity = args[1].toUpperCase();
                ItemStack hand = null;
                if (player.getInventory().getItemInMainHand().getType().equals(Material.SPAWNER))
                    hand = player.getInventory().getItemInMainHand();
                else if (player.getInventory().getItemInOffHand().getType().equals(Material.SPAWNER))
                    hand = player.getInventory().getItemInOffHand();

                if (hand != null) {
                    setItemSpawnwer(hand, entity, player.getName());
                    sender.sendMessage(getLang("setto", true).replace("{type}", hand.getAmount() + "x " + capSpawnerName(entity)));
                    return true;
                }

                if (player.getLineOfSight(null, 15).stream().anyMatch(e -> e.getType().equals(Material.SPAWNER))) {
                    Block spawner = player.getLineOfSight(null, 15).stream().filter(e -> e.getType().equals(Material.SPAWNER)).findFirst().get();
                    CreatureSpawner creatureSpawner = (CreatureSpawner) spawner.getState();
                    creatureSpawner.setSpawnedType(EntityType.valueOf(entity));
                    creatureSpawner.update(true);
                    return true;
                }

                sender.sendMessage(getLang("invalid", true));
                return false;
            }
        }

        if (args.length == 3 || args.length == 4) {

            if (args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("spawnermanager.command.give")) return false;

                if (Bukkit.getPlayer(args[1]) == null) {
                    sender.sendMessage(getLang("noonlineplayer", true).replace("{player}", args[1]));
                    return false;
                }

                if (Arrays.stream(EntityType.values()).noneMatch(e -> e.name().equalsIgnoreCase(args[2]))) {
                    sender.sendMessage(getLang("noentitytype", true).replace("{entity}", args[2]));
                    return false;
                }

                ItemStack item = new ItemStack(Material.SPAWNER, 1);

                if (args.length == 4) {
                    try {
                        int amount = Integer.parseInt(args[3]);
                        if (amount < 1 || amount > Material.SPAWNER.getMaxStackSize()) {
                            sender.sendMessage(getLang("invalidstacksize", true).replace("{size}", String.valueOf(amount)));
                            return false;
                        }

                        item = new ItemStack(Material.SPAWNER, amount);
                    } catch (Exception ignored) {
                        sender.sendMessage(getLang("invalidnumber", true).replace("{number}", args[3]));
                        return false;
                    }
                }

                Player player = Bukkit.getPlayer(args[1]);
                String entity = args[2].toUpperCase();

                setItemSpawnwer(item, entity, player.getName());

                player.getInventory().addItem(item);
                sender.sendMessage(getLang("given", true)
                        .replace("{amount}", String.valueOf(item.getAmount()))
                        .replace("{type}", capSpawnerName(entity))
                        .replace("{player}", player.getName()));
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> tab = new ArrayList<>();
        List<String> commands = Arrays.asList("reload", "change", "give");
        if (args.length == 1) {
            if (args[0].isEmpty())
                tab.addAll(commands.stream().filter(c -> sender.hasPermission("spawnermanager.command." + c)).collect(Collectors.toList()));
            else
                tab.addAll(commands.stream().filter(c -> c.startsWith(args[0]) && sender.hasPermission("spawnermanager.command." + c)).collect(Collectors.toList()));
        }
        if (args.length == 2) {
            if (args[0].startsWith("change")) {
                if (args[1].isEmpty())
                    tab.addAll(Arrays.stream(EntityType.values()).map(EntityType::name).collect(Collectors.toList()));
                else
                    tab.addAll(Arrays.stream(EntityType.values()).filter(c -> c.name().startsWith(args[1].toUpperCase())).map(EntityType::name).collect(Collectors.toList()));
            }
            if (args[0].startsWith("give")) {
                if (args[1].isEmpty())
                    tab.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                else
                    tab.addAll(Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().startsWith(args[1])).map(Player::getName).collect(Collectors.toList()));
            }
        }
        if (args.length == 3) {
            if (args[0].startsWith("give")) {
                if (args[2].isEmpty())
                    tab.addAll(Arrays.stream(EntityType.values()).map(EntityType::name).collect(Collectors.toList()));
                else
                    tab.addAll(Arrays.stream(EntityType.values()).filter(c -> c.name().startsWith(args[2].toUpperCase())).map(EntityType::name).collect(Collectors.toList()));
            }
            if (args[0].startsWith("change")) {
                if (args[2].isEmpty())
                    tab.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                else
                    tab.addAll(Bukkit.getOnlinePlayers().stream().filter(p -> p.getName().startsWith(args[2])).map(Player::getName).collect(Collectors.toList()));
            }
        }
        return tab;
    }

    String capSpawnerName(String name) {
        String[] split = name.split("_");
        StringBuilder finalName = new StringBuilder();
        for (String nm : split) {
            finalName.append(nm, 0, 1).append(nm.toLowerCase().substring(1)).append(" ");
        }
        return finalName.delete(finalName.length() - 1, finalName.length()).toString();
    }

    void setItemSpawnwer(ItemStack itemStack, String type, String player) {
        ItemMeta meta = itemStack.getItemMeta();
        meta.setLore(Collections.singletonList("§0" + type));
        if (getConfig().getBoolean("config.setMinedByOnLore"))
            meta.setLore(Arrays.asList("§0" + type, getLang("minedby", false).replace("{player}", player)));
        meta.setDisplayName(getLang("spawnername", false).replace("{type}", capSpawnerName(type)));
        itemStack.setItemMeta(meta);
    }
}
