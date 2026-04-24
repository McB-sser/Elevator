package de.mcbesser.elevator;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ElevatorService {

    private static final long TELEPORT_COOLDOWN_MILLIS = 600L;
    private static final int PARTICLE_HORIZONTAL_RADIUS = 8;
    private static final int PARTICLE_VERTICAL_RADIUS = 6;
    private static final long PLATE_VALIDITY_CACHE_MILLIS = 5000L;
    private static final Particle.DustOptions UP_ARROW_DUST =
        new Particle.DustOptions(Color.fromRGB(64, 255, 96), 0.9F);
    private static final Particle.DustOptions DOWN_ARROW_DUST =
        new Particle.DustOptions(Color.fromRGB(255, 64, 64), 0.9F);

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastUseByPlayer = new HashMap<>();
    private final Map<UUID, BossBar> bossBarsByPlayer = new HashMap<>();
    private final Map<PlateKey, CachedPlateValidity> plateValidityCache = new HashMap<>();

    public ElevatorService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void startParticleTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Set<PlateKey> shownPlates = new HashSet<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                World world = player.getWorld();
                Block center = player.getLocation().getBlock();

                for (int x = center.getX() - PARTICLE_HORIZONTAL_RADIUS; x <= center.getX() + PARTICLE_HORIZONTAL_RADIUS; x++) {
                    for (int y = center.getY() - PARTICLE_VERTICAL_RADIUS; y <= center.getY() + PARTICLE_VERTICAL_RADIUS; y++) {
                        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                            continue;
                        }

                        for (int z = center.getZ() - PARTICLE_HORIZONTAL_RADIUS; z <= center.getZ() + PARTICLE_HORIZONTAL_RADIUS; z++) {
                            Block block = world.getBlockAt(x, y, z);
                            if (!isValidElevatorPlateCached(block)) {
                                continue;
                            }

                            PlateKey key = new PlateKey(world.getUID(), x, y, z);
                            if (shownPlates.add(key)) {
                                spawnValidParticle(block);
                            }
                        }
                    }
                }
            }
        }, 20L, 20L);
    }

    public void startBossBarTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateBossBar(player);
            }
        }, 2L, 4L);
    }

    public void shutdown() {
        for (BossBar bossBar : bossBarsByPlayer.values()) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
        bossBarsByPlayer.clear();
        plateValidityCache.clear();
    }

    public boolean tryMove(Player player, Location sourceLocation, Direction direction) {
        if (!canUse(player)) {
            return false;
        }

        Block currentPlate = findPlateUnderLocation(sourceLocation);
        if (currentPlate == null || !isValidElevatorPlate(currentPlate)) {
            return false;
        }

        Block targetPlate = findNextFloor(currentPlate, direction);
        if (targetPlate == null) {
            return false;
        }

        float yaw = player.getLocation().getYaw();
        teleport(player, currentPlate, targetPlate);
        showTravelParticle(currentPlate, direction, yaw);
        lastUseByPlayer.put(player.getUniqueId(), System.currentTimeMillis());
        updateBossBar(player);
        return true;
    }

    private boolean canUse(Player player) {
        long now = System.currentTimeMillis();
        long lastUse = lastUseByPlayer.getOrDefault(player.getUniqueId(), 0L);
        return now - lastUse >= TELEPORT_COOLDOWN_MILLIS;
    }

    private Block findPlateUnderLocation(Location location) {
        Block[] candidates = new Block[] {
            location.getBlock(),
            location.clone().subtract(0.0D, 0.2D, 0.0D).getBlock(),
            location.clone().subtract(0.0D, 0.5D, 0.0D).getBlock(),
            location.clone().subtract(0.0D, 1.0D, 0.0D).getBlock(),
            location.getBlock().getRelative(BlockFace.DOWN)
        };

        for (Block candidate : candidates) {
            if (isPressurePlate(candidate.getType())) {
                return candidate;
            }
        }

        return null;
    }

    private boolean isValidElevatorPlate(Block block) {
        if (!isPressurePlate(block.getType())) {
            return false;
        }

        if (!hasStandingSpace(block)) {
            return false;
        }

        return hasMatchingFloor(block);
    }

    private boolean isValidElevatorPlateCached(Block block) {
        if (!isPressurePlate(block.getType())) {
            return false;
        }

        PlateKey key = new PlateKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        long now = System.currentTimeMillis();
        CachedPlateValidity cached = plateValidityCache.get(key);
        if (cached != null && now - cached.checkedAtMillis() <= PLATE_VALIDITY_CACHE_MILLIS) {
            return cached.valid();
        }

        boolean valid = isValidElevatorPlate(block);
        plateValidityCache.put(key, new CachedPlateValidity(valid, now));
        return valid;
    }

    private boolean hasMatchingFloor(Block originPlate) {
        World world = originPlate.getWorld();
        int x = originPlate.getX();
        int z = originPlate.getZ();

        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            if (y == originPlate.getY()) {
                continue;
            }

            Block candidate = world.getBlockAt(x, y, z);
            if (isPressurePlate(candidate.getType()) && hasStandingSpace(candidate)) {
                return true;
            }
        }

        return false;
    }

    private Block findNextFloor(Block originPlate, Direction direction) {
        List<Block> floors = getElevatorFloors(originPlate);
        for (int index = 0; index < floors.size(); index++) {
            if (!isSameBlock(floors.get(index), originPlate)) {
                continue;
            }

            if (direction == Direction.UP && index + 1 < floors.size()) {
                return floors.get(index + 1);
            }

            if (direction == Direction.DOWN && index - 1 >= 0) {
                return floors.get(index - 1);
            }

            return null;
        }

        return null;
    }

    private boolean hasStandingSpace(Block plate) {
        Block above = plate.getRelative(BlockFace.UP);
        Block twoAbove = above.getRelative(BlockFace.UP);
        return above.getType().isAir() && twoAbove.isPassable();
    }

    private boolean isPressurePlate(Material material) {
        return Tag.PRESSURE_PLATES.isTagged(material);
    }

    private List<Block> getElevatorFloors(Block plate) {
        World world = plate.getWorld();
        List<Block> floors = new ArrayList<>();

        for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
            Block candidate = world.getBlockAt(plate.getX(), y, plate.getZ());
            if (isPressurePlate(candidate.getType()) && hasStandingSpace(candidate)) {
                floors.add(candidate);
            }
        }

        floors.sort(Comparator.comparingInt(Block::getY));
        return floors;
    }

    private void updateBossBar(Player player) {
        Block plate = findPlateUnderLocation(player.getLocation());
        if (plate == null || !isValidElevatorPlate(plate)) {
            hideBossBar(player);
            return;
        }

        List<Block> floors = getElevatorFloors(plate);
        int floorIndex = getFloorIndex(floors, plate);
        if (floorIndex < 0) {
            hideBossBar(player);
            return;
        }

        BossBar bossBar = bossBarsByPlayer.computeIfAbsent(player.getUniqueId(), ignored -> {
            BossBar createdBar = Bukkit.createBossBar("", BarColor.YELLOW, BarStyle.SOLID);
            createdBar.addPlayer(player);
            return createdBar;
        });

        bossBar.setTitle(ChatColor.GOLD + "Fahrstuhl | Etage " + (floorIndex + 1) + " von " + floors.size());
        bossBar.setStyle(getBarStyle(floors.size()));
        bossBar.setProgress(Math.max(0.05D, (floorIndex + 1D) / floors.size()));
        bossBar.setVisible(true);
        if (!bossBar.getPlayers().contains(player)) {
            bossBar.addPlayer(player);
        }
    }

    private int getFloorIndex(List<Block> floors, Block currentPlate) {
        for (int index = 0; index < floors.size(); index++) {
            if (isSameBlock(floors.get(index), currentPlate)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isSameBlock(Block first, Block second) {
        return first.getWorld().equals(second.getWorld())
            && first.getX() == second.getX()
            && first.getY() == second.getY()
            && first.getZ() == second.getZ();
    }

    private void hideBossBar(Player player) {
        BossBar bossBar = bossBarsByPlayer.remove(player.getUniqueId());
        if (bossBar == null) {
            return;
        }

        bossBar.removePlayer(player);
        bossBar.setVisible(false);
    }

    private void teleport(Player player, Block originPlate, Block targetPlate) {
        Location targetLocation = targetPlate.getLocation().add(0.5D, 0.15D, 0.5D);
        targetLocation.setYaw(player.getLocation().getYaw());
        targetLocation.setPitch(player.getLocation().getPitch());

        player.teleport(targetLocation);
        targetPlate.getWorld().playSound(targetLocation, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9F, 1.25F);
        targetPlate.getWorld().spawnParticle(Particle.PORTAL, targetLocation.clone().add(0.0D, 0.6D, 0.0D), 16, 0.2D, 0.35D, 0.2D, 0.01D);
        originPlate.getWorld().spawnParticle(Particle.PORTAL, originPlate.getLocation().add(0.5D, 0.3D, 0.5D), 16, 0.2D, 0.35D, 0.2D, 0.01D);
    }

    private void showTravelParticle(Block originPlate, Direction direction, float yaw) {
        spawnArrow(originPlate.getLocation().add(0.5D, 1.6D, 0.5D), direction, yaw);
    }

    private void spawnValidParticle(Block plate) {
        Location location = plate.getLocation().add(0.5D, 0.2D, 0.5D);
        plate.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.07D, 0.04D, 0.07D, 0.0D);
    }

    private void spawnArrow(Location center, Direction direction, float yaw) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        Vector forward = getHorizontalForward(yaw);
        Vector right = new Vector(-forward.getZ(), 0.0D, forward.getX()).normalize();
        Particle.DustOptions dust = direction == Direction.UP ? UP_ARROW_DUST : DOWN_ARROW_DUST;
        if (direction == Direction.UP) {
            Location shaftStart = offset(center, right, 0.0D, -1.00D);
            Location shaftEnd = offset(center, right, 0.0D, 0.65D);
            Location leftWing = offset(center, right, -0.50D, 0.20D);
            Location rightWing = offset(center, right, 0.50D, 0.20D);

            drawParticleLine(world, shaftStart, shaftEnd, dust, 16);
            drawParticleLine(world, shaftEnd, leftWing, dust, 8);
            drawParticleLine(world, shaftEnd, rightWing, dust, 8);
            return;
        }

        Location shaftStart = offset(center, right, 0.0D, 0.65D);
        Location shaftEnd = offset(center, right, 0.0D, -1.00D);
        Location leftWing = offset(center, right, -0.50D, -0.55D);
        Location rightWing = offset(center, right, 0.50D, -0.55D);

        drawParticleLine(world, shaftStart, shaftEnd, dust, 16);
        drawParticleLine(world, shaftEnd, leftWing, dust, 8);
        drawParticleLine(world, shaftEnd, rightWing, dust, 8);
    }

    private void drawParticleLine(World world, Location from, Location to, Particle.DustOptions dust, int steps) {
        for (int step = 0; step <= steps; step++) {
            double progress = step / (double) steps;
            Location point = from.clone().add(
                (to.getX() - from.getX()) * progress,
                (to.getY() - from.getY()) * progress,
                (to.getZ() - from.getZ()) * progress
            );

            world.spawnParticle(Particle.DUST, point, 1, 0.0D, 0.0D, 0.0D, 0.0D, dust);
        }
    }

    private BarStyle getBarStyle(int floorCount) {
        if (floorCount == 6) {
            return BarStyle.SEGMENTED_6;
        }
        if (floorCount == 10) {
            return BarStyle.SEGMENTED_10;
        }
        if (floorCount == 12) {
            return BarStyle.SEGMENTED_12;
        }
        if (floorCount == 20) {
            return BarStyle.SEGMENTED_20;
        }
        return BarStyle.SOLID;
    }

    private Vector getHorizontalForward(float yaw) {
        double radians = Math.toRadians(yaw);
        return new Vector(-Math.sin(radians), 0.0D, Math.cos(radians)).normalize();
    }

    private Location offset(Location base, Vector right, double rightAmount, double yAmount) {
        return base.clone().add(right.clone().multiply(rightAmount)).add(0.0D, yAmount, 0.0D);
    }

    public enum Direction {
        UP,
        DOWN
    }

    private record PlateKey(UUID worldId, int x, int y, int z) {
    }

    private record CachedPlateValidity(boolean valid, long checkedAtMillis) {
    }
}
