package de.mcbesser.elevator;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public final class ElevatorListener implements Listener {

    private final ElevatorService elevatorService;

    public ElevatorListener(ElevatorService elevatorService) {
        this.elevatorService = elevatorService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        elevatorService.tryMove(event.getPlayer(), event.getPlayer().getLocation(), ElevatorService.Direction.DOWN);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        elevatorService.tryMove(event.getPlayer(), event.getFrom(), ElevatorService.Direction.UP);
    }
}
