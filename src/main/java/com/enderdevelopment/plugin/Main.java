package com.enderdevelopment.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public final class Main extends JavaPlugin implements Listener {
    private static final String MENU_TITLE = "§0✦ Grimório de Anti-Magia ✦";
    private static final List<Ability> ABILITIES = List.of(
        new Ability("black_aerial", "Voo da Asa Negra", Material.FEATHER, "Mobilidade aérea da união demoníaca."),
        new Ability("black_slash", "Corte Anti-Magia", Material.IRON_SWORD, "Golpe que dissipa efeitos mágicos próximos."),
        new Ability("black_divider", "Divisor Negro", Material.NETHERITE_SWORD, "Um amplo corte de energia anti-mágica."),
        new Ability("black_meteorite", "Meteoro Negro", Material.OBSIDIAN, "Investida poderosa contra inimigos próximos."),
        new Ability("ki_sense", "Percepção de Ki", Material.ENDER_EYE, "Revela criaturas nas proximidades."),
        new Ability("demon_dweller", "Espada do Habitante", Material.IRON_AXE, "Dispara uma onda de energia anti-mágica."),
        new Ability("demon_destroyer", "Espada Destruidora", Material.NETHERITE_AXE, "Remove efeitos negativos de você."),
        new Ability("black_guard", "Defesa Negra", Material.SHIELD, "Ativa resistência por alguns instantes.")
    );
    private final Map<UUID, Set<String>> equipped = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> transformed = new ConcurrentHashMap<>();
    private NamespacedKey abilityKey;
    private NamespacedKey grimoireKey;

    @Override
    public void onEnable() {
        abilityKey = new NamespacedKey(this, "ability");
        grimoireKey = new NamespacedKey(this, "grimoire");
        Bukkit.getPluginManager().registerEvents(this, this);
        if (getCommand("grimorio") != null) {
            getCommand("grimorio").setExecutor((sender, command, label, args) -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be used in game.");
                    return true;
                }
                openMenu(player);
                return true;
            });
        }
        for (Player player : Bukkit.getOnlinePlayers()) giveGrimoire(player);
        getLogger().info("Anti-magic grimoire enabled.");
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) restorePlayer(player);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTask(this, () -> giveGrimoire(event.getPlayer()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        equipped.remove(event.getPlayer().getUniqueId());
        transformed.remove(event.getPlayer().getUniqueId());
    }

    private ItemStack markedItem(Material material, String title, NamedTextColor color, String marker) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(Component.text(title, color));
        NamespacedKey key = marker.equals("asta_grimoire") ? grimoireKey : abilityKey;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, marker);
        item.setItemMeta(meta);
        return item;
    }

    private void giveGrimoire(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta != null && "asta_grimoire".equals(meta.getPersistentDataContainer().get(grimoireKey, PersistentDataType.STRING))) return;
        }
        player.getInventory().addItem(markedItem(Material.WRITTEN_BOOK, "Grimório de Anti-Magia", NamedTextColor.DARK_PURPLE, "asta_grimoire"));
        equipped.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
    }

    private void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 27, Component.text(MENU_TITLE, NamedTextColor.DARK_PURPLE));
        menu.setItem(4, markedItem(Material.BOOK, "Grimório de Asta", NamedTextColor.GOLD, "menu_info"));
        Set<String> selected = equipped.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19};
        for (int i = 0; i < ABILITIES.size(); i++) {
            Ability ability = ABILITIES.get(i);
            ItemStack item = markedItem(ability.material(), (selected.contains(ability.id()) ? "✓ " : "＋ ") + ability.name(), selected.contains(ability.id()) ? NamedTextColor.GREEN : NamedTextColor.GRAY, ability.id());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.lore(List.of(Component.text(ability.description(), NamedTextColor.GRAY), Component.text("Clique para equipar ou desequipar", NamedTextColor.YELLOW)));
                item.setItemMeta(meta);
            }
            menu.setItem(slots[i], item);
        }
        boolean active = transformed.getOrDefault(player.getUniqueId(), false);
        menu.setItem(22, markedItem(active ? Material.NETHERITE_CHESTPLATE : Material.NETHER_STAR, active ? "Desativar União Demoníaca" : "Ativar União Demoníaca", NamedTextColor.RED, "demon_union"));
        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || !event.getView().title().equals(Component.text(MENU_TITLE, NamedTextColor.DARK_PURPLE))) return;
        event.setCancelled(true);
        ItemStack item = event.getCurrentItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String id = meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        if ("demon_union".equals(id)) {
            player.closeInventory();
            if (transformed.getOrDefault(player.getUniqueId(), false)) endTransformation(player); else beginTransformation(player);
            return;
        }
        if (id == null || ABILITIES.stream().noneMatch(ability -> ability.id().equals(id))) return;
        Set<String> set = equipped.computeIfAbsent(player.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());
        if (set.contains(id)) {
            set.remove(id);
            removeHotbarAbility(player, id);
            player.sendMessage(Component.text("Habilidade removida da hotbar.", NamedTextColor.GRAY));
        } else if (set.size() >= 8) {
            player.sendMessage(Component.text("Você já equipou o máximo de oito habilidades.", NamedTextColor.RED));
        } else {
            set.add(id);
            addHotbarAbility(player, ABILITIES.stream().filter(ability -> ability.id().equals(id)).findFirst().orElseThrow());
            player.sendMessage(Component.text("Habilidade equipada na hotbar.", NamedTextColor.GREEN));
        }
        openMenu(player);
    }

    private void addHotbarAbility(Player player, Ability ability) {
        for (int slot = 0; slot < 9; slot++) if (isAbilityItem(player.getInventory().getItem(slot), ability.id())) return;
        for (int slot = 0; slot < 9; slot++) if (player.getInventory().getItem(slot) == null) {
            player.getInventory().setItem(slot, markedItem(ability.material(), ability.name(), NamedTextColor.AQUA, ability.id()));
            return;
        }
        player.sendMessage(Component.text("Hotbar cheia; a habilidade foi equipada, mas não há espaço livre.", NamedTextColor.YELLOW));
    }

    private void removeHotbarAbility(Player player, String id) {
        for (int slot = 0; slot < 9; slot++) if (isAbilityItem(player.getInventory().getItem(slot), id)) player.getInventory().setItem(slot, null);
    }

    private boolean isAbilityItem(ItemStack item, String id) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && id.equals(meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        String grimoire = meta.getPersistentDataContainer().get(grimoireKey, PersistentDataType.STRING);
        String id = meta.getPersistentDataContainer().get(abilityKey, PersistentDataType.STRING);
        if ("asta_grimoire".equals(grimoire)) {
            event.setCancelled(true);
            openMenu(player);
        } else if (id != null && equipped.getOrDefault(player.getUniqueId(), Set.of()).contains(id)) {
            event.setCancelled(true);
            activate(player, id);
        }
    }

    private void activate(Player player, String id) {
        switch (id) {
            case "black_aerial" -> {
                player.setVelocity(player.getLocation().getDirection().multiply(1.3).setY(0.8));
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation(), 12, 0.6, 0.8, 0.6);
            }
            case "black_slash", "black_divider", "black_meteorite", "demon_dweller" -> {
                int damage = id.equals("black_divider") || id.equals("black_meteorite") ? 10 : 6;
                double radius = id.equals("black_divider") ? 5 : 3.5;
                int hits = 0;
                for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
                    if (!(entity instanceof LivingEntity target) || target == player) continue;
                    if (target.getLocation().distanceSquared(player.getLocation()) > radius * radius) continue;
                    target.damage(damage, player);
                    target.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0, 1, 0), 1);
                    hits++;
                }
                if (hits == 0 && id.equals("demon_dweller")) player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(player.getLocation().getDirection().multiply(2)), 8);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1, 0.6f);
            }
            case "ki_sense" -> {
                for (Entity entity : player.getNearbyEntities(16, 16, 16)) if (entity instanceof LivingEntity target && target != player) target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 100, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 200, 0));
            }
            case "demon_destroyer" -> {
                for (PotionEffect effect : new ArrayList<>(player.getActivePotionEffects())) player.removePotionEffect(effect.getType());
                player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.7, 1, 0.7);
            }
            case "black_guard" -> player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 1));
            default -> { return; }
        }
        player.setCooldown(itemMaterial(id), 12);
    }

    private Material itemMaterial(String id) {
        return ABILITIES.stream().filter(ability -> ability.id().equals(id)).map(Ability::material).findFirst().orElse(Material.AIR);
    }

    private void beginTransformation(Player player) {
        UUID uuid = player.getUniqueId();
        if (transformed.getOrDefault(uuid, false)) return;
        transformed.put(uuid, false);
        player.sendMessage(Component.text("A União Demoníaca começa a se formar...", NamedTextColor.DARK_RED));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.2f);
        for (int stage = 1; stage <= 4; stage++) {
            final int current = stage;
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (!player.isOnline()) return;
                player.getWorld().spawnParticle(Particle.SOUL, player.getLocation().add(0, 1, 0), 35, 0.7, 1, 0.7, 0.03);
                player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, current * 0.45, 0), 15, 0.45, 0.25, 0.45, 0.01);
                player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1, 0.7f + current * 0.1f);
                player.sendMessage(Component.text(switch (current) {
                    case 1 -> "A energia negra envolve suas pernas...";
                    case 2 -> "A armadura demoníaca cobre seu torso...";
                    case 3 -> "A armadura se fecha e as asas surgem...";
                    default -> "União Demoníaca completa!";
                }, NamedTextColor.DARK_RED));
                if (current == 4) {
                    transformed.put(uuid, true);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 12000, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 12000, 0));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 12000, 1));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 12000, 0));
                    player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation().add(0, 1, 0), 80, 1, 1, 1, 0.04);
                    if (!equipped.getOrDefault(uuid, Set.of()).contains("black_aerial")) {
                        equipped.computeIfAbsent(uuid, ignored -> ConcurrentHashMap.newKeySet()).add("black_aerial");
                        addHotbarAbility(player, ABILITIES.getFirst());
                    }
                }
            }, current * 20L);
        }
    }

    private void endTransformation(Player player) {
        transformed.put(player.getUniqueId(), false);
        player.removePotionEffect(PotionEffectType.STRENGTH);
        player.removePotionEffect(PotionEffectType.RESISTANCE);
        player.removePotionEffect(PotionEffectType.SPEED);
        player.removePotionEffect(PotionEffectType.SLOW_FALLING);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 0.7, 1, 0.7);
        player.sendMessage(Component.text("A União Demoníaca foi desativada.", NamedTextColor.GRAY));
    }

    private void restorePlayer(Player player) {
        if (transformed.getOrDefault(player.getUniqueId(), false)) endTransformation(player);
    }

    private record Ability(String id, String name, Material material, String description) {}
}
