package com.legendary.weapons;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class WeaponManager implements Listener {
    private final LegendaryWeapons plugin;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Double> fallDamageMultipliers = new HashMap<>();
    private final Map<UUID, Map<Location, BlockData>> originalBlocksMap = new HashMap<>();

    private final Map<UUID, Location> stunnedPlayers = new HashMap<>();
    private final Map<UUID, Float> originalYaws = new HashMap<>();
    private final Map<UUID, Float> originalPitches = new HashMap<>();

    public WeaponManager(LegendaryWeapons plugin) {
        this.plugin = plugin;
    }

    public static final String SEISMIC_MACE_NAME = "§6Seismic Mace";
    public static final String SHADOW_BLADE_NAME = "§5Shadow Blade";
    public static final String LIGHT_SENTINEL_NAME = "§eLight Sentinel";
    public static final String INFERNO_AXE_NAME = "§cInferno Axe";

    private static final int SEISMIC_MACE_MODEL = 1001;
    private static final int SHADOW_BLADE_MODEL = 1002;
    private static final int LIGHT_SENTINEL_MODEL = 1003;
    private static final int INFERNO_AXE_MODEL = 1004;

    public ItemStack createSeismicMace() {
        ItemStack club = new ItemStack(Material.MACE);
        ItemMeta meta = club.getItemMeta();
        meta.setDisplayName(SEISMIC_MACE_NAME);
        meta.setLore(Arrays.asList(
                "§7§oThe earth trembles as this weapon strikes,",
                "§7§oinflicting havoc upon all who stand in its way.",
                "§7§lGround Slam: Right-click to leap into the air and",
                "§7§llance the ground with the fury of the earth.",
                "§7Cooldown: 45 seconds",
                "§7Miss Penalty: Failing to strike increases fall damage",
                "§cGround Slam Damage: Damage scales with fall distance (capped at 5 hearts)",
                "§cSmash Attack: 0.5 hearts"
        ));
        meta.setUnbreakable(true);
        meta.setCustomModelData(SEISMIC_MACE_MODEL);
        club.setItemMeta(meta);
        return club;
    }

    public ItemStack createShadowBlade() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(SHADOW_BLADE_NAME);
        meta.setLore(Arrays.asList(
                "§7§oA weapon forged in darkness, it moves with silence",
                "§7§oland strikes with unseen force.",
                "§7§lPassive: Chance to apply Darkness on hit",
                "§7§lBlinding Strike: Right-click to unleash a devastating",
                "§7§oattack that blinds your enemies (30s cooldown)",
                "§7§lShadow Domain: Shift-right-click to enter the realm of",
                "§7§odarkness, confounding all who enter (120s cooldown)"
        ));
        meta.setUnbreakable(true);
        meta.setCustomModelData(SHADOW_BLADE_MODEL);
        sword.setItemMeta(meta);
        return sword;
    }

    public ItemStack createLightSentinel() {
        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(LIGHT_SENTINEL_NAME);
        meta.setLore(Arrays.asList(
                "§7§oA beacon of hope and light, its blade shines brighter",
                "§7§owith each strike, illuminating the darkest of foes.",
                "§7§lPassive: Speed boost on hit",
                "§7§lLight Beam: Right-click to unleash a piercing beam of",
                "§7§owhite-hot light (20s cooldown)",
                "§7§lBuff Aura: Shift-right-click to bathe allies in a",
                "§7§ohealing aura of brilliance (90s cooldown)"
        ));
        meta.setUnbreakable(true);
        meta.setCustomModelData(LIGHT_SENTINEL_MODEL);
        sword.setItemMeta(meta);
        return sword;
    }

    public ItemStack createInfernoAxe() {
        ItemStack axe = new ItemStack(Material.NETHERITE_AXE);
        ItemMeta meta = axe.getItemMeta();
        meta.setDisplayName(INFERNO_AXE_NAME);
        meta.setLore(Arrays.asList(
                "§7§oWielding the fury of fire, this axe ignites the air",
                "§7§oaround it, scorching the very earth with each swing.",
                "§7§lPassive: Fire resistance while holding",
                "§7§lRing of Fire: Right-click to summon a ring of blazing flames",
                "§7§othat engulfs your enemies (60s cooldown)",
                "§7§lFire Wave: Shift-right-click to release a wave of fire",
                "§7§o(75s cooldown) that deals 3 hearts of damage"
        ));
        meta.setUnbreakable(true);
        meta.setCustomModelData(INFERNO_AXE_MODEL);
        axe.setItemMeta(meta);
        return axe;
    }

    private boolean isOnCooldown(Player player, String ability, long cooldownSeconds) {
        UUID playerId = player.getUniqueId();
        String key = playerId + ability;
        if (cooldowns.containsKey(key)) {
            long secondsLeft = (cooldowns.get(key) - System.currentTimeMillis()) / 1000;
            if (secondsLeft > 0) {
                player.sendMessage("§c" + ability.replace("_", " ").toUpperCase() + " on cooldown for " + secondsLeft + " more seconds!");
                return true;
            }
        }
        cooldowns.put(key, System.currentTimeMillis() + (cooldownSeconds * 1000));
        return false;
    }

    @EventHandler
    public void onSeismicMaceUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
                && item.getItemMeta().getDisplayName().equals(SEISMIC_MACE_NAME)) {

            if (isOnCooldown(player, "seismic_slam", 45)) return;

            World world = player.getWorld();
            Location loc = player.getLocation();

            world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 1.5f, 0.8f);
            world.spawnParticle(Particle.DUST, loc, 30, 1, 0.1, 1,
                    new Particle.DustOptions(Color.fromRGB(255, 140, 0), 2.0f));

            Vector direction = player.getLocation().getDirection().multiply(2.0).setY(0.9);
            player.setVelocity(direction);

            new BukkitRunnable() {
                boolean hasLeftGround = false;
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }
                    if (!hasLeftGround) {
                        if (!player.isOnGround()) {
                            hasLeftGround = true;
                        }
                        return;
                    }

                    if (!player.isOnGround()) {
                        world.spawnParticle(Particle.DUST, player.getLocation(), 5, 0.3, 0.3, 0.3,
                                new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.5f));
                    }

                    if (player.isOnGround()) {
                        groundSlam(player);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        }
    }

    @EventHandler
    public void onSeismicMaceAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getDisplayName().equals(SEISMIC_MACE_NAME)) {

            event.setDamage(2.0);
        }
    }

    @EventHandler
    public void onMaceSmashAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getType() == Material.MACE && item.getItemMeta().getDisplayName().equals(SEISMIC_MACE_NAME)) {

            double fallDistance = player.getFallDistance();
            if (fallDistance > 0) {

                double damage = Math.min(10.0, fallDistance * 2.5); // 2.5 damage per block
                damage = Math.min(damage, 10.0);  // Ensure it does not exceed 5 hearts (10 damage)
                event.setDamage(damage);

                player.setFallDistance(0);
            } else {

                event.setDamage(1.0);
            }
        }
    }

    private void groundSlam(Player player) {
        Location loc = player.getLocation();
        World world = loc.getWorld();

        world.playSound(loc, Sound.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 2.0f, 0.7f);
        world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 0.6f);
        world.playSound(loc, Sound.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 2.0f, 0.5f);

        spawnFallingBlocksEffect(loc, 15, Material.STONE, 20);
        spawnGhostItemsEffect(loc);

        for (int ring = 1; ring <= 3; ring++) {
            final int ringSize = ring;
            new BukkitRunnable() {
                int step = 0;
                @Override
                public void run() {
                    if (step >= 25) {
                        cancel();
                        return;
                    }
                    for (int i = 0; i < 24; i++) {
                        double angle = 2 * Math.PI * i / 24;
                        double distance = ringSize * (step / 25.0) * 4;
                        double x = Math.cos(angle) * distance;
                        double z = Math.sin(angle) * distance;
                        Location particleLoc = loc.clone().add(x, 0.1, z);
                        world.spawnParticle(Particle.DUST, particleLoc, 1,
                                new Particle.DustOptions(Color.fromRGB(255, 140 + ringSize * 20, 0), 2.0f));
                        world.spawnParticle(Particle.BLOCK, particleLoc, 2, Material.STONE.createBlockData());
                    }
                    step++;
                }
            }.runTaskTimer(plugin, ring * 2, 1);
        }

        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= 12) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 16; i++) {
                    double y = step * 0.6;
                    double angle = 2 * Math.PI * i / 16;
                    double radius = 0.8 + (step * 0.1);
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = loc.clone().add(x, y, z);
                    world.spawnParticle(Particle.DUST, particleLoc, 1,
                            new Particle.DustOptions(Color.fromRGB(255, 200, 50), 1.5f));
                    world.spawnParticle(Particle.LAVA, particleLoc, 1);
                }
                step++;
            }
        }.runTaskTimer(plugin, 0, 2);

        boolean hit = false;
        for (Entity entity : world.getNearbyEntities(loc, 3.5, 2.5, 3.5)) {
            if (entity instanceof Player && entity != player) {
                Player target = (Player) entity;

                double damage = Math.min(8.0, target.getHealth());
                applyTrueDamage(target, damage, player);

                stunPlayer(target, 3); // 3 seconds

                world.spawnParticle(Particle.DUST, target.getEyeLocation(), 20,
                        new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f));
                world.spawnParticle(Particle.CRIT, target.getLocation(), 15, 0.5, 1, 0.5);
                target.playEffect(EntityEffect.HURT);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);
                hit = true;
            }
        }

        if (!hit) {
            fallDamageMultipliers.put(player.getUniqueId(), 2.5);
            new BukkitRunnable() {
                @Override
                public void run() {
                    fallDamageMultipliers.remove(player.getUniqueId());
                }
            }.runTaskLater(plugin, 100);
            player.sendMessage("§cMiss penalty applied! Increased fall damage for 5 seconds");
        }
    }

    private void applyTrueDamage(LivingEntity target, double damage, Entity source) {

        double originalHealth = target.getHealth();
        double damageToDeal = Math.min(damage, originalHealth);

        target.setHealth(originalHealth - damageToDeal);

        target.playEffect(EntityEffect.HURT);
        if (target instanceof Player) {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
        } else {
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, 1.0f, 1.0f);
        }
    }

    private void stunPlayer(Player player, int seconds) {
        UUID uuid = player.getUniqueId();
        stunnedPlayers.put(uuid, player.getLocation());
        originalYaws.put(uuid, player.getLocation().getYaw());
        originalPitches.put(uuid, player.getLocation().getPitch());

        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, seconds * 20, 0, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, seconds * 20, 255, false, false));

        new BukkitRunnable() {
            @Override
            public void run() {
                stunnedPlayers.remove(uuid);
                originalYaws.remove(uuid);
                originalPitches.remove(uuid);

                player.removePotionEffect(PotionEffectType.BLINDNESS);
                player.removePotionEffect(PotionEffectType.MINING_FATIGUE);

                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation(), 20, 0.5, 1, 0.5);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
            }
        }.runTaskLater(plugin, seconds * 20);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (stunnedPlayers.containsKey(player.getUniqueId())) {

            Location original = stunnedPlayers.get(player.getUniqueId());
            Location to = event.getTo();

            to.setX(original.getX());
            to.setY(original.getY());
            to.setZ(original.getZ());

            to.setYaw(originalYaws.get(player.getUniqueId()));
            to.setPitch(originalPitches.get(player.getUniqueId()));

            event.setTo(to);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        stunnedPlayers.remove(uuid);
        originalYaws.remove(uuid);
        originalPitches.remove(uuid);
    }

    @EventHandler
    public void onShadowBladeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getDisplayName().equals(SHADOW_BLADE_NAME)) {
            if (Math.random() < 0.15) {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 120, 0));
                    World world = target.getWorld();
                    Location loc = target.getLocation();
                    world.spawnParticle(Particle.DUST, target.getEyeLocation(), 25, 0.5, 0.5, 0.5,
                            new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f));
                    world.spawnParticle(Particle.SQUID_INK, loc, 10, 0.5, 1, 0.5);
                    world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.8f, 1.2f);
                    world.playSound(loc, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.6f);
                    spawnDarkParticleRain(loc);
                }
            }
        }
    }

    @EventHandler
    public void onShadowBladeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals(SHADOW_BLADE_NAME)) {
            Location loc = player.getLocation();
            World world = loc.getWorld();

            if (player.isSneaking()) {
                if (isOnCooldown(player, "shadow_domain", 120)) return;
                world.playSound(loc, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.PLAYERS, 2.0f, 0.8f);
                world.playSound(loc, Sound.AMBIENT_CAVE, SoundCategory.PLAYERS, 1.5f, 0.5f);
                createShadowDomain(player, loc);
            } else {
                if (isOnCooldown(player, "blinding_strike", 30)) return;
                world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 0.8f);
                world.playSound(loc, Sound.ENTITY_VEX_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.6f);

                new BukkitRunnable() {
                    double radius = 0;
                    @Override
                    public void run() {
                        if (radius > 12) {
                            cancel();
                            return;
                        }
                        for (int i = 0; i < 32; i++) {
                            double angle = 2 * Math.PI * i / 32;
                            double x = Math.cos(angle) * radius;
                            double z = Math.sin(angle) * radius;
                            Location particleLoc = loc.clone().add(x, 0.5, z);
                            world.spawnParticle(Particle.DUST, particleLoc, 1,
                                    new Particle.DustOptions(Color.fromRGB(0, 0, 0), 2.0f));
                        }
                        radius += 0.8;
                    }
                }.runTaskTimer(plugin, 0, 1);

                for (Entity entity : world.getNearbyEntities(loc, 12, 6, 12)) {
                    if (entity instanceof Player && entity != player) {
                        Player target = (Player) entity;
                        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 120, 0));
                        world.spawnParticle(Particle.DUST, target.getEyeLocation(), 20, 0.3, 0.3, 0.3,
                                new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f));
                        world.spawnParticle(Particle.SQUID_INK, target.getLocation(), 8, 0.5, 1, 0.5);
                        world.playSound(target.getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);
                    }
                }
                world.spawnParticle(Particle.DUST, player.getEyeLocation(), 50, 1, 1, 1,
                        new Particle.DustOptions(Color.fromRGB(75, 0, 130), 1.5f));
            }
        }
    }

    @EventHandler
    public void onLightSentinelHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getDisplayName().equals(LIGHT_SENTINEL_NAME)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 120, 0));
            World world = player.getWorld();
            Location loc = player.getLocation();
            world.spawnParticle(Particle.DUST, loc, 20, 0.5, 0.5, 0.5,
                    new Particle.DustOptions(Color.fromRGB(255, 255, 150), 1.5f));
            world.spawnParticle(Particle.END_ROD, loc, 8, 0.3, 0.5, 0.3);
            world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.5f);
            world.playSound(loc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.8f);
            spawnLightBurstEffect(loc);
        }
    }

    @EventHandler
    public void onLightSentinelUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals(LIGHT_SENTINEL_NAME)) {
            Location loc = player.getLocation();
            World world = loc.getWorld();

            if (player.isSneaking()) {
                if (isOnCooldown(player, "buff_aura", 90)) return;
                world.playSound(loc, Sound.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 1.5f, 1.2f);
                world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.5f);
                spawnLightPillarEffect(loc);

                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 400, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 400, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 400, 0));
                player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 400, 0));

                new BukkitRunnable() {
                    int duration = 0;
                    @Override
                    public void run() {
                        if (duration >= 400 || !player.isOnline()) {
                            cancel();
                            return;
                        }
                        Location currentLoc = player.getLocation();
                        for (int ring = 0; ring < 3; ring++) {
                            double height = ring * 0.7;
                            double radius = 2.5 - (ring * 0.3);
                            for (int i = 0; i < 20; i++) {
                                double angle = 2 * Math.PI * i / 20 + (duration * 0.08) + (ring * Math.PI / 3);
                                double x = Math.cos(angle) * radius;
                                double z = Math.sin(angle) * radius;
                                Location particleLoc = currentLoc.clone().add(x, height + 0.5, z);
                                Color color = ring == 0 ? Color.fromRGB(255, 255, 200) :
                                        ring == 1 ? Color.fromRGB(200, 255, 255) :
                                                Color.fromRGB(255, 200, 255);
                                world.spawnParticle(Particle.DUST, particleLoc, 1,
                                        new Particle.DustOptions(color, 1.8f));
                            }
                        }
                        duration += 5;
                    }
                }.runTaskTimer(plugin, 0, 5);
            } else {
                if (isOnCooldown(player, "light_beam", 20)) return;
                world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 1.2f, 1.5f);
                world.playSound(loc, Sound.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0f, 2.0f);
                createEnhancedLightBeam(player);
            }
        }
    }

    @EventHandler
    public void onInfernoAxeHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getDisplayName().equals(INFERNO_AXE_NAME)) {
            if (Math.random() < 0.20) {
                if (event.getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) event.getEntity();
                    target.setFireTicks(100);
                    World world = target.getWorld();
                    Location loc = target.getLocation();
                    world.spawnParticle(Particle.FLAME, loc, 15, 0.5, 1, 0.5);
                    world.spawnParticle(Particle.LAVA, loc, 8, 0.3, 0.5, 0.3);
                    world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.2f);
                    world.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.8f, 0.6f);
                    spawnFireBurstEffect(loc);
                }
            }
        }
    }

    @EventHandler
    public void onInfernoAxeUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        String displayName = item.getItemMeta().getDisplayName();
        if (displayName.equals(INFERNO_AXE_NAME)) {
            Location loc = player.getLocation();
            World world = loc.getWorld();

            if (player.isSneaking()) {
                if (isOnCooldown(player, "fire_wave", 75)) return;
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_SHOOT, SoundCategory.PLAYERS, 2.0f, 0.8f);
                world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.5f, 0.6f);
                createFireWave(player, loc);
            } else {
                if (isOnCooldown(player, "ring_of_fire", 60)) return;
                world.playSound(loc, Sound.ENTITY_BLAZE_AMBIENT, SoundCategory.PLAYERS, 1.5f, 0.8f);
                world.playSound(loc, Sound.BLOCK_FIRE_AMBIENT, SoundCategory.PLAYERS, 2.0f, 0.5f);
                spawnFireRainEffect(loc);
                createRingOfFire(player, loc);
            }
        }
    }

    private void spawnFallingBlocksEffect(Location center, int count, Material material, int durationTicks) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int spawned = 0;
            @Override
            public void run() {
                if (spawned >= count) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 3 && spawned < count; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * 8;
                    double y = center.getY() + 5 + Math.random() * 3;
                    double z = center.getZ() + (Math.random() - 0.5) * 8;
                    Location spawnLoc = new Location(world, x, y, z);
                    FallingBlock fallingBlock = world.spawnFallingBlock(spawnLoc, material.createBlockData());
                    fallingBlock.setDropItem(false);
                    fallingBlock.setHurtEntities(false);
                    fallingBlock.setMetadata("EffectBlock", new FixedMetadataValue(plugin, true));
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (fallingBlock.isValid()) {
                                fallingBlock.remove();
                            }
                        }
                    }.runTaskLater(plugin, durationTicks);
                    spawned++;
                }
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    @EventHandler
    public void onFallingBlockLand(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock) {
            FallingBlock fallingBlock = (FallingBlock) event.getEntity();
            if (fallingBlock.hasMetadata("EffectBlock")) {
                event.setCancelled(true);
                fallingBlock.remove();
            }
        }
    }

    private void spawnGhostItemsEffect(Location center) {
        World world = center.getWorld();
        Material[] items = {Material.IRON_SWORD, Material.DIAMOND, Material.GOLD_INGOT,
                Material.EMERALD, Material.IRON_INGOT, Material.COAL};
        new BukkitRunnable() {
            int spawned = 0;
            @Override
            public void run() {
                if (spawned >= 12) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 2; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * 6;
                    double y = center.getY() + 4 + Math.random() * 2;
                    double z = center.getZ() + (Math.random() - 0.5) * 6;
                    Location spawnLoc = new Location(world, x, y, z);
                    Material randomItem = items[(int)(Math.random() * items.length)];
                    Item ghostItem = world.dropItem(spawnLoc, new ItemStack(randomItem));
                    ghostItem.setVelocity(new Vector((Math.random() - 0.5) * 0.3, -0.1, (Math.random() - 0.5) * 0.3));
                    ghostItem.setPickupDelay(Integer.MAX_VALUE);
                    ghostItem.setGlowing(true);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (ghostItem.isValid()) {
                                world.spawnParticle(Particle.POOF, ghostItem.getLocation(), 5);
                                ghostItem.remove();
                            }
                        }
                    }.runTaskLater(plugin, 60);
                    spawned++;
                }
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    private void spawnDarkParticleRain(Location center) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 60) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 8; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * 4;
                    double y = center.getY() + 3 + Math.random() * 2;
                    double z = center.getZ() + (Math.random() - 0.5) * 4;
                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.DUST, particleLoc, 1, 0, -0.5, 0,
                            new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.5f));
                    world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, -0.3, 0);
                }
                duration += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void spawnLightBurstEffect(Location center) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= 15) {
                    cancel();
                    return;
                }
                double radius = step * 0.3;
                for (int i = 0; i < 16; i++) {
                    double angle = 2 * Math.PI * i / 16;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = center.clone().add(x, 0.5, z);
                    world.spawnParticle(Particle.DUST, particleLoc, 1,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 200), 1.8f));
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0, 0, 0);
                }
                step++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void spawnLightPillarEffect(Location center) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int height = 0;
            @Override
            public void run() {
                if (height >= 20) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 8; i++) {
                    double angle = 2 * Math.PI * i / 8 + (height * 0.2);
                    double radius = 0.8;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = center.clone().add(x, height * 0.3, z);
                    world.spawnParticle(Particle.DUST, particleLoc, 1,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 150), 2.0f));
                    world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0, 0.1, 0, 0);
                }
                height++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void createEnhancedLightBeam(Player player) {
        World world = player.getWorld();
        Location startLoc = player.getEyeLocation().clone();
        Vector direction = startLoc.getDirection();
        final double[] angle = {0};

        new BukkitRunnable() {
            double distance = 0;
            @Override
            public void run() {
                if (distance > 30) {
                    cancel();
                    return;
                }
                Location point = startLoc.clone().add(direction.clone().multiply(distance));
                world.spawnParticle(Particle.DUST, point, 3, 0.1, 0.1, 0.1,
                        new Particle.DustOptions(Color.fromRGB(255, 255, 200), 2.0f));
                world.spawnParticle(Particle.END_ROD, point, 2, 0.1, 0.1, 0.1, 0);

                Vector forward = direction.clone().normalize();
                Vector right = new Vector(-forward.getZ(), 0, forward.getX()).normalize();
                Vector up = right.getCrossProduct(forward).normalize();

                for (int i = 0; i < 4; i++) {
                    double spiralAngle = angle[0] + (i * Math.PI / 2);
                    double xOffset = Math.cos(spiralAngle) * 1.2;
                    double yOffset = Math.sin(spiralAngle) * 1.2;
                    Vector offset = right.clone().multiply(xOffset).add(up.clone().multiply(yOffset));
                    Location spiralLoc = point.clone().add(offset);
                    world.spawnParticle(Particle.DUST, spiralLoc, 1,
                            new Particle.DustOptions(Color.fromRGB(255, 255, 100), 1.5f));
                    world.spawnParticle(Particle.FIREWORK, spiralLoc, 1, 0, 0, 0, 0);
                }
                angle[0] += 0.4;

                for (Entity entity : world.getNearbyEntities(point, 1.0, 1.0, 1.0)) {
                    if (entity instanceof LivingEntity && entity != player) {

                        applyTrueDamage((LivingEntity) entity, 6, player);
                        entity.setFireTicks(60);
                        world.spawnParticle(Particle.EXPLOSION, point, 1);
                        world.spawnParticle(Particle.DUST, point, 20, 0.5, 0.5, 0.5,
                                new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2.0f));
                        world.playSound(point, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.5f);
                        cancel();
                        return;
                    }
                }
                distance += 0.8;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void spawnFireBurstEffect(Location center) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int step = 0;
            @Override
            public void run() {
                if (step >= 12) {
                    cancel();
                    return;
                }
                double radius = step * 0.4;
                for (int i = 0; i < 12; i++) {
                    double angle = 2 * Math.PI * i / 12;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location particleLoc = center.clone().add(x, 0.5, z);
                    world.spawnParticle(Particle.FLAME, particleLoc, 2, 0.1, 0.1, 0.1);
                    world.spawnParticle(Particle.LAVA, particleLoc, 1);
                    if (step > 6) {
                        world.spawnParticle(Particle.LARGE_SMOKE, particleLoc, 1, 0, 0.2, 0);
                    }
                }
                step++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void spawnFireRainEffect(Location center) {
        World world = center.getWorld();
        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 80) {
                    cancel();
                    return;
                }
                for (int i = 0; i < 6; i++) {
                    double x = center.getX() + (Math.random() - 0.5) * 8;
                    double y = center.getY() + 4 + Math.random() * 2;
                    double z = center.getZ() + (Math.random() - 0.5) * 8;
                    Location particleLoc = new Location(world, x, y, z);
                    world.spawnParticle(Particle.FLAME, particleLoc, 1, 0, -0.5, 0, 0.1);
                    world.spawnParticle(Particle.DRIPPING_LAVA, particleLoc, 1, 0, -0.3, 0);
                }
                duration += 2;
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void createShadowDomain(Player player, Location center) {
        World world = player.getWorld();
        int radius = 12; // Increased from 10 to 12
        int height = 10; // Increased height
        Set<UUID> affectedPlayers = new HashSet<>();

        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 300) {
                    cancel();
                    return;
                }

                for (double y = 0; y <= height; y += 0.4) {
                    double radiusY = radius * Math.cos((Math.PI * y) / (height * 2));
                    for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 20) {
                        double x = center.getX() + radiusY * Math.cos(angle);
                        double z = center.getZ() + radiusY * Math.sin(angle);
                        Location particleLoc = new Location(world, x, center.getY() + y, z);
                        Color color = duration % 30 < 10 ? Color.fromRGB(0, 0, 0) :
                                duration % 30 < 20 ? Color.fromRGB(20, 0, 40) :
                                        Color.fromRGB(40, 0, 60);
                        world.spawnParticle(Particle.DUST, particleLoc, 1,
                                new Particle.DustOptions(color, 1.8f));
                        if (Math.random() < 0.1) {
                            world.spawnParticle(Particle.SQUID_INK, particleLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                if (duration % 8 == 0) {
                    for (double r = 0; r <= radius; r += 0.6) {
                        for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 20) {
                            double x = center.getX() + r * Math.cos(angle);
                            double z = center.getZ() + r * Math.sin(angle);
                            Location floorLoc = new Location(world, x, center.getY(), z);
                            world.spawnParticle(Particle.DUST, floorLoc, 1,
                                    new Particle.DustOptions(Color.fromRGB(10, 0, 20), 1.2f));
                            world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, floorLoc, 1, 0, 0.1, 0, 0);
                        }
                    }
                }

                for (Entity entity : world.getNearbyEntities(center, radius, height, radius)) {
                    if (entity instanceof Player && entity != player) {
                        Player target = (Player) entity;

                        if (!affectedPlayers.contains(target.getUniqueId())) {
                            target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 160, 0));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 160, 1));
                            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1));
                            affectedPlayers.add(target.getUniqueId());
                        }

                        double distance = target.getLocation().distance(center);
                        if (distance > radius - 1.5) {
                            Vector direction = center.toVector().subtract(target.getLocation().toVector()).normalize();
                            target.setVelocity(direction.multiply(0.8));
                            world.spawnParticle(Particle.DUST, target.getLocation(), 15, 0.5, 0.5, 0.5,
                                    new Particle.DustOptions(Color.fromRGB(80, 0, 120), 1.5f));
                            world.playSound(target.getLocation(), Sound.BLOCK_GLASS_HIT, SoundCategory.PLAYERS, 1.0f, 0.5f);
                        }

                        if (duration % 10 == 0) {
                            world.spawnParticle(Particle.DUST, target.getLocation(), 12, 0.3, 1, 0.3,
                                    new Particle.DustOptions(Color.fromRGB(30, 0, 50), 1.2f));
                            world.spawnParticle(Particle.SQUID_INK, target.getLocation(), 3, 0.3, 0.5, 0.3);
                        }
                    }
                }

                if (duration % 40 == 0) {
                    world.playSound(center, Sound.ENTITY_PHANTOM_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.6f);
                }
                duration += 5;
            }
        }.runTaskTimer(plugin, 0, 5);
    }

    private void createRingOfFire(Player player, Location center) {
        World world = center.getWorld();
        int radius = 5;
        Map<Location, BlockData> originalBlocks = new HashMap<>();
        Set<UUID> burningEntities = new HashSet<>(); // Track entities to apply fire chance

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + y * y + z * z <= radius * radius) {
                        Location blockLoc = center.clone().add(x, y, z);
                        Block block = world.getBlockAt(blockLoc);
                        if (block.getType() == Material.WATER) {
                            originalBlocks.put(blockLoc.clone(), block.getBlockData().clone());
                            block.setType(Material.AIR);
                        } else if (block.getType() != Material.AIR) {
                            Material newMaterial = getNetherVariant(block.getType());
                            if (newMaterial != null) {
                                originalBlocks.put(blockLoc.clone(), block.getBlockData().clone());
                                block.setType(newMaterial);
                            }
                        }
                    }
                }
            }
        }
        originalBlocksMap.put(player.getUniqueId(), originalBlocks);

        new BukkitRunnable() {
            int duration = 0;
            @Override
            public void run() {
                if (duration >= 200 || !player.isOnline()) {
                    restoreBlocks(player.getUniqueId());
                    cancel();
                    return;
                }

                for (double r = radius - 0.5; r <= radius + 0.5; r += 0.2) {
                    for (double angle = 0; angle < 2 * Math.PI; angle += Math.PI / 40) {
                        double x = center.getX() + r * Math.cos(angle);
                        double z = center.getZ() + r * Math.sin(angle);
                        Location particleLoc = new Location(world, x, center.getY() + 0.1, z);
                        world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.RED, 1.0f));
                        if (Math.random() < 0.5) {
                            world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.fromRGB(255, 165, 0), 1.0f));
                        }
                        if (Math.random() < 0.2) {
                            world.spawnParticle(Particle.DUST, particleLoc, 1, new Particle.DustOptions(Color.YELLOW, 1.0f));
                        }
                    }
                }

                if (duration % 20 == 0) {
                    for (Entity entity : world.getNearbyEntities(center, radius, 2, radius)) {
                        if (entity instanceof LivingEntity && entity != player) {
                            LivingEntity target = (LivingEntity) entity;

                            if (Math.random() < 0.2) {
                                target.setFireTicks(100);
                                world.spawnParticle(Particle.FLAME, target.getLocation(), 10, 0.5, 0.5, 0.5);
                                world.playSound(target.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.8f, 1.2f);
                            }
                        }
                    }
                }

                duration += 5;
            }
        }.runTaskTimer(plugin, 0, 5);

        new BukkitRunnable() {
            @Override
            public void run() {
                restoreBlocks(player.getUniqueId());
            }
        }.runTaskLater(plugin, 200);
    }

    private Material getNetherVariant(Material original) {
        switch (original) {
            case STONE:
                return Material.NETHERRACK;
            case DIRT:
            case GRASS_BLOCK:
                return Material.CRIMSON_NYLIUM;
            case SAND:
                return Material.SOUL_SAND;
            case GRAVEL:
                return Material.SOUL_SOIL;
            case OAK_LOG:
            case BIRCH_LOG:
            case SPRUCE_LOG:
            case JUNGLE_LOG:
            case ACACIA_LOG:
            case DARK_OAK_LOG:
                return Material.CRIMSON_STEM;
            default:
                return null;
        }
    }

    private void createFireWave(Player player, Location start) {
        World world = start.getWorld();
        Vector direction = player.getEyeLocation().getDirection().normalize();
        Location origin = player.getEyeLocation().add(0, -0.5, 0); // Center on player's face
        Set<UUID> damagedEntities = new HashSet<>(); // Track entities already damaged

        new BukkitRunnable() {
            double progress = 0;
            @Override
            public void run() {
                if (progress > 15) {
                    cancel();
                    return;
                }

                Vector forward = direction.clone().multiply(progress);
                Location center = origin.clone().add(forward);

                double wallWidth = 3.0;
                double wallHeight = 3.0;
                int particlesPerLayer = 12;

                for (double h = -wallHeight/2; h <= wallHeight/2; h += 0.5) {
                    for (int i = 0; i < particlesPerLayer; i++) {

                        double angle = Math.toRadians(180.0 * i / (particlesPerLayer - 1));
                        double xOffset = Math.cos(angle) * wallWidth;
                        double zOffset = Math.sin(angle) * wallWidth;

                        Vector horizontal = new Vector(-direction.getZ(), 0, direction.getX()).normalize();
                        Vector vertical = new Vector(0, 1, 0);

                        Vector offset = horizontal.clone().multiply(xOffset)
                                .add(vertical.clone().multiply(h));
                        Location point = center.clone().add(offset);

                        if (Math.random() < 0.7) {

                            world.spawnParticle(Particle.DUST, point, 1,
                                    new Particle.DustOptions(Color.RED, 1.5f));
                        } else if (Math.random() < 0.85) {

                            world.spawnParticle(Particle.DUST, point, 1,
                                    new Particle.DustOptions(Color.fromRGB(255, 140, 0), 1.5f));
                        } else {

                            world.spawnParticle(Particle.DUST   , point, 1,
                                    new Particle.DustOptions(Color.YELLOW, 1.5f));
                        }

                        world.spawnParticle(Particle.FLAME, point, 1);

                        for (Entity entity : world.getNearbyEntities(point, 0.8, 0.8, 0.8)) {
                            if (entity instanceof LivingEntity && entity != player) {
                                LivingEntity target = (LivingEntity) entity;

                                if (!damagedEntities.contains(target.getUniqueId())) {

                                    double damage = Math.min(6.0, target.getHealth());
                                    applyTrueDamage(target, damage, player);
                                    target.setFireTicks(60);
                                    damagedEntities.add(target.getUniqueId());

                                    Vector kb = direction.clone().multiply(1.2).setY(0.5);
                                    target.setVelocity(target.getVelocity().add(kb));
                                }
                            }
                        }
                    }
                }

                progress += 0.6;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    private void restoreBlocks(UUID playerId) {
        Map<Location, BlockData> originalBlocks = originalBlocksMap.get(playerId);
        if (originalBlocks != null) {
            for (Map.Entry<Location, BlockData> entry : originalBlocks.entrySet()) {
                Location loc = entry.getKey();
                BlockData originalData = entry.getValue();
                if (loc.getWorld() != null) {
                    loc.getWorld().getBlockAt(loc).setBlockData(originalData);
                }
            }
            originalBlocksMap.remove(playerId);
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        new BukkitRunnable() {
            @Override
            public void run() {
                ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
                if (newItem != null && newItem.hasItemMeta() &&
                        newItem.getItemMeta().getDisplayName().equals(INFERNO_AXE_NAME)) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false));
                }
            }
        }.runTaskLater(plugin, 1);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (fallDamageMultipliers.containsKey(player.getUniqueId())) {
                event.setDamage(event.getDamage() * fallDamageMultipliers.get(player.getUniqueId()));
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation(), 20, 0.5, 0.1, 0.5,
                        new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.8f));
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, SoundCategory.PLAYERS, 1.0f, 0.8f);
            }
        }
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            if (fallDamageMultipliers.containsKey(player.getUniqueId())) {
                fallDamageMultipliers.remove(player.getUniqueId());
                player.sendMessage("§aFall damage penalty cleared!");
                World world = player.getWorld();
                world.spawnParticle(Particle.DUST, player.getLocation(), 15, 0.5, 0.5, 0.5,
                        new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f));
                world.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0f, 1.5f);
            }
        }
    }
}
