package de.mcbesser.elevator;

import org.bukkit.plugin.java.JavaPlugin;

public final class ElevatorPlugin extends JavaPlugin {

    private ElevatorService elevatorService;

    @Override
    public void onEnable() {
        elevatorService = new ElevatorService(this);
        getServer().getPluginManager().registerEvents(new ElevatorListener(elevatorService), this);
        elevatorService.startParticleTask();
        elevatorService.startBossBarTask();
    }

    @Override
    public void onDisable() {
        if (elevatorService != null) {
            elevatorService.shutdown();
        }
    }
}
