package de.mcbesser.elevator;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ElevatorService {

    private static final long TELEPORT_COOLDOWN_MILLIS = 600L;
    private static final int PARTICLE_HORIZONTAL_RADIUS = 8;
    private static final int PARTICLE_VERTICAL_RADIUS = 6;
    private static final Particle.DustOptions ELEVATOR_ARROW_DUST =
        new Particle.DustOptions(Color.fromRGB(85, 220, 255), 1.1F);

    private final JavaPlugin plugin;
    private final Map<UUID, Long> lastUseByPlayer = new HashMap<>();

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
                            if (!isValidElevatorPlate(block)) {
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
        }, 20L, 10L);
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

        teleport(player, currentPlate, targetPlate);
        showTravelParticle(currentPlate, targetPlate, direction);
        lastUseByPlayer.put(player.getUniqueId(), System.currentTimeMillis());
        return true;
    }

    private boolean canUse(Player player) {
        long now = System.currentTimeMillis();
        long lastUse = lastUseByPlayer.getOrDefault(player.getUniqueId(), 0L);
        return now - lastUse >= TELEPORT_COOLDOWN_MILLIS;
    }

    private Block findPlateUnderLocation(Location location) {
        Block primary = location.clone().subtract(0.0D, 0.15D, 0.0D).getBlock();
        if (isPressurePlate(primary.getType())) {
            return primary;
        }

        Block fallback = location.clone().subtract(0.0D, 1.0D, 0.0D).getBlock();
        if (isPressurePlate(fallback.getType())) {
            return fallback;
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
        World world = originPlate.getWorld();
        int step = direction == Direction.UP ? 1 : -1;
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;

        for (int y = originPlate.getY() + step; y >= minY && y <= maxY; y += step) {
            Block candidate = world.getBlockAt(originPlate.getX(), y, originPlate.getZ());
            if (isPressurePlate(candidate.getType()) && hasStandingSpace(candidate)) {
                return candidate;
            }
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

    private void teleport(Player player, Block originPlate, Block targetPlate) {
        Location targetLocation = targetPlate.getLocation().add(0.5D, 0.15D, 0.5D);
        targetLocation.setYaw(player.getLocation().getYaw());
        targetLocation.setPitch(player.getLocation().getPitch());

        player.teleport(targetLocation);
        targetPlate.getWorld().spawnParticle(Particle.PORTAL, targetLocation.clone().add(0.0D, 0.6D, 0.0D), 16, 0.2D, 0.35D, 0.2D, 0.01D);
        originPlate.getWorld().spawnParticle(Particle.PORTAL, originPlate.getLocation().add(0.5D, 0.3D, 0.5D), 16, 0.2D, 0.35D, 0.2D, 0.01D);
    }

    private void showTravelParticle(Block originPlate, Block targetPlate, Direction direction) {
        if (direction == Direction.DOWN) {
            spawnArrow(originPlate.getLocation().add(0.5D, 0.15D, 0.5D), Direction.DOWN);
            return;
        }

        spawnArrow(targetPlate.getLocation().add(0.5D, 0.15D, 0.5D), Direction.UP);
    }

    private void spawnValidParticle(Block plate) {
        Location location = plate.getLocation().add(0.5D, 0.2D, 0.5D);
        plate.getWorld().spawnParticle(Particle.END_ROD, location, 1, 0.07D, 0.04D, 0.07D, 0.0D);
    }

    private void spawnArrow(Location center, Direction direction) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        double[][] points = direction == Direction.UP
            ? new double[][] {
                {0.0D, 0.25D, 0.0D}, {0.0D, 0.45D, 0.0D}, {0.0D, 0.65D, 0.0D},
                {-0.12D, 0.55D, 0.0D}, {0.12D, 0.55D, 0.0D}, {-0.08D, 0.72D, 0.0D}, {0.08D, 0.72D, 0.0D}
            }
            : new double[][] {
                {0.0D, 0.65D, 0.0D}, {0.0D, 0.45D, 0.0D}, {0.0D, 0.25D, 0.0D},
                {-0.12D, 0.35D, 0.0D}, {0.12D, 0.35D, 0.0D}, {-0.08D, 0.18D, 0.0D}, {0.08D, 0.18D, 0.0D}
            };

        for (double[] point : points) {
            world.spawnParticle(
                Particle.DUST,
                center.clone().add(point[0], point[1], point[2]),
                1,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                ELEVATOR_ARROW_DUST
            );
        }
    }

    public enum Direction {
        UP,
        DOWN
    }

    private record PlateKey(UUID worldId, int x, int y, int z) {
    }
}
