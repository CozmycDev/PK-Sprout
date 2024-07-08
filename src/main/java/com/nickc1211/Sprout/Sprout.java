package com.nickc1211.Sprout;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.PlantAbility;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.TempBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Sprout extends PlantAbility implements AddonAbility {

    private boolean continueThroughEntities;
    private long cooldown;
    private double damage;
    private String pathPlant;
    private long pathRevertTime;
    private double range;
    private String snarePlantBottom;
    private String snarePlantTop;
    private long snareTime;
    private String sound;
    private static PotionEffectType POTION;

    private Set<Entity> affectedEntities;
    private TempBlock vine;
    private Location origin;
    private Location currentLocation;
    private Vector direction;

    public Sprout(Player player) {
        super(player);

        if (this.bPlayer.canPlantbend() && !this.bPlayer.isOnCooldown(this) && this.player.isSneaking()) {
            if (isOnFertileGround(player)) {
                initializeFields();
                this.bPlayer.addCooldown(this);
                start();
            }
        }
    }

    @Override
    public String getAuthor() {
        return "NickC1211, Cozmyc";
    }

    @Override
    public long getCooldown() {
        return this.cooldown;
    }

    @Override
    public String getDescription() {
        return "This plant ability allows waterbenders to ensnare their opponents at range. This move requires that you and your target be standing on connected fertile ground (dirt or grass blocks).";
    }

    @Override
    public String getInstructions() {
        return "Hold Shift and Left Click to shoot, and move cursor side to side to aim.";
    }

    @Override
    public Location getLocation() {
        return this.currentLocation;
    }

    @Override
    public String getName() {
        return "Sprout";
    }

    @Override
    public String getVersion() {
        return "V5.3.1";
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return true;
    }

    @Override
    public void load() {
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new SproutListener(), ProjectKorra.plugin);

        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.ContinueThroughEntities", true);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.Cooldown", 5000);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.Damage", 3);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.PathRevertTime", 1100);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.PathPlant", "SHORT_GRASS");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.Range", 20);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.SnarePlant.Top", "LARGE_FERN");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.SnarePlant.Bottom", "LARGE_FERN");
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.SnareTime", 4000);
        ConfigManager.defaultConfig.get().addDefault("ExtraAbilities.NickC1211.Sprout.Sound", "BLOCK_GRASS_BREAK");

        if (isBukkitVersionHigher(getCurrentMinecraftVersion(), "1.20.4")) {
            POTION = getPotionEffectTypeByName("SLOWNESS");
        } else {
            POTION = getPotionEffectTypeByName("SLOW");
        }

        ConfigManager.defaultConfig.save();
    }

    @Override
    public void progress() {
        if (shouldRemoveAbility()) {
            if (this.affectedEntities != null) {
                this.affectedEntities.clear();
            }
            remove();
        } else {
            updateCurrentLocation();
            createVinePath();
            handleEntitiesAroundCurrentLocation();
        }
    }

    @Override
    public void stop() {}

    private void applyDamageAndPotionEffect(LivingEntity entity) {
        DamageHandler.damageEntity(entity, this.damage, this);
        entity.addPotionEffect(new PotionEffect(POTION, (int) (20 * (this.snareTime/1000)), 3));

        Bukkit.getScheduler().runTaskLater(ProjectKorra.plugin, () -> {
            final Vector vec = new Vector();
            entity.setVelocity(vec);
            Location loc = this.currentLocation.clone();
            loc.setYaw(entity.getLocation().getYaw());
            loc.setPitch(entity.getLocation().getPitch());
            entity.teleport(loc);
        }, 1L);
    }

    private void applyEffectsToEntity(LivingEntity entity) {
        Block block = getGroundBlock();
        if (block == null) return;

        applySnareBlockEffect();
        applyDamageAndPotionEffect(entity);
        if (!this.continueThroughEntities) this.range = 0.5D;
    }

    private void applySnareBlockEffect() {
        Block block = verifyVineGroundBlock();
        if (block == null || block.isLiquid()) return;

        block = block.getRelative(BlockFace.UP);

        if (!block.isEmpty() && !block.isPassable()) {
            block = block.getRelative(BlockFace.UP);
            if (!block.isEmpty() && !block.isPassable()) {
                return;
            }
        }

        Material snareTopMaterial = getValidSnareMaterial(this.snarePlantTop);
        Material snareBottomMaterial  = getValidSnareMaterial(this.snarePlantBottom);

        TempBlock snareTop = new TempBlock(block.getRelative(BlockFace.UP), snareTopMaterial);
        TempBlock snareBottom = new TempBlock(block, snareBottomMaterial);

        snareTop.setRevertTime(this.snareTime);
        snareBottom.setRevertTime(this.snareTime);
    }

    private void createVinePath() {
        Block block = verifyVineGroundBlock();
        if (block == null) return;

        this.currentLocation.setY(block.getY() + 1);

        if (!block.isEmpty() && !block.isPassable()) {
            block = block.getRelative(BlockFace.UP);
            if (!block.isEmpty() && !block.isPassable()) {
                return;
            }
        }

        this.vine = new TempBlock(block, Material.valueOf(pathPlant));
        this.currentLocation.getWorld().playSound(block.getLocation(), Sound.valueOf(this.sound), 1.0F, 1.0F);
        this.vine.setRevertTime(this.pathRevertTime);
    }

    private Block findFertileBlockBelow(Block block) {
        while (!isFertileBlock(block)) {
            block = block.getRelative(BlockFace.DOWN);
            if (this.player.getLocation().getBlockY() - block.getY() > 5) {
                return null;
            }
        }
        return block;
    }

    public String getCurrentMinecraftVersion() {
        String version = Bukkit.getBukkitVersion();
        if (version.contains("-")) {
            version = version.split("-")[0];
        }
        return version;
    }

    private Block getGroundBlock() {
        Block topBlock = GeneralMethods.getTopBlock(this.currentLocation, 3);
        if (isFertileBlock(topBlock)) {
            return topBlock;
        } else {
            return findFertileBlockBelow(topBlock);
        }
    }

    private PotionEffectType getPotionEffectTypeByName(String name) {
        for (PotionEffectType type : PotionEffectType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }

    private Material getValidSnareMaterial(String snarePlant) {
        Material snare;
        try {
            snare = Material.valueOf(snarePlant);
            if (!snare.isBlock()) {
                snare = Material.LARGE_FERN;
            }
        } catch (IllegalArgumentException e) {
            snare = Material.LARGE_FERN;
        }
        return snare;
    }

    private void handleEntitiesAroundCurrentLocation() {
        List<Entity> entities = GeneralMethods.getEntitiesAroundPoint(this.currentLocation, 1.0);
        for (Entity entity : entities) {
            if (entity instanceof LivingEntity && entity.getEntityId() != this.player.getEntityId() && !this.affectedEntities.contains(entity)) {
                applyEffectsToEntity((LivingEntity) entity);
                this.affectedEntities.add(entity);
            }
        }
    }

    private void initializeFields() {
        this.continueThroughEntities = ConfigManager.defaultConfig.get().getBoolean("ExtraAbilities.NickC1211.Sprout.ContinueThroughEntities");
        this.cooldown = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.NickC1211.Sprout.Cooldown");
        this.damage = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.NickC1211.Sprout.Damage");
        this.pathRevertTime = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.NickC1211.Sprout.PathRevertTime");
        this.pathPlant = ConfigManager.defaultConfig.get().getString("ExtraAbilities.NickC1211.Sprout.PathPlant");
        this.range = ConfigManager.defaultConfig.get().getDouble("ExtraAbilities.NickC1211.Sprout.Range");
        this.snarePlantTop = ConfigManager.defaultConfig.get().getString("ExtraAbilities.NickC1211.Sprout.SnarePlant.Top");
        this.snarePlantBottom = ConfigManager.defaultConfig.get().getString("ExtraAbilities.NickC1211.Sprout.SnarePlant.Bottom");
        this.snareTime = ConfigManager.defaultConfig.get().getLong("ExtraAbilities.NickC1211.Sprout.SnareTime");
        this.sound = ConfigManager.defaultConfig.get().getString("ExtraAbilities.NickC1211.Sprout.Sound");

        this.origin = this.player.getLocation().clone();
        this.currentLocation = this.origin.clone();
        this.direction = this.player.getLocation().getDirection().setY(0);

        this.affectedEntities = new HashSet<>();
    }

    private boolean isFertileMaterial(Material material) {
        return material == Material.DIRT
                || material == Material.GRASS_BLOCK;
    }

    private boolean isFertileBlock(Block block) {
        return isFertileMaterial(block.getType());
    }

    private boolean isOnFertileGround(Player player) {
        Material groundType = player.getLocation().clone().add(0.0, -1.0, 0.0).getBlock().getType();
        return isFertileMaterial(groundType);
    }

    public boolean isBukkitVersionHigher(String version, String compareTo) {
        String[] versionParts = version.split("\\.");
        String[] compareToParts = compareTo.split("\\.");

        for (int i = 0; i < Math.max(versionParts.length, compareToParts.length); i++) {
            int vPart = i < versionParts.length ? Integer.parseInt(versionParts[i]) : 0;
            int cPart = i < compareToParts.length ? Integer.parseInt(compareToParts[i]) : 0;

            if (vPart > cPart) {
                return true;
            } else if (vPart < cPart) {
                return false;
            }
        }
        return false;
    }

    private boolean shouldRemoveAbility() {
        return !this.bPlayer.canBendIgnoreBindsCooldowns(this)
                || !bPlayer.getBoundAbilityName().equalsIgnoreCase("Sprout")
                || this.origin.distance(this.currentLocation) > this.range
                || !this.player.isSneaking()
                || this.currentLocation.getBlock().getType().isSolid()
                || verifyVineGroundBlock() == null;
    }

    private void updateCurrentLocation() {
        this.direction = this.player.getLocation().getDirection().setY(0);
        this.currentLocation.add(this.direction).clone();
        updateCurrentLocationYCoordinate();
    }

    private void updateCurrentLocationYCoordinate() {
        Block groundBlock = getGroundBlock();
        if (groundBlock != null) {
            this.currentLocation.setY(groundBlock.getLocation().getY() + 1.0);
        }
    }

    private Block verifyVineGroundBlock() {
        Block block = getGroundBlock();
        if (block == null || block.isLiquid()) return null;
        return block;
    }
}
