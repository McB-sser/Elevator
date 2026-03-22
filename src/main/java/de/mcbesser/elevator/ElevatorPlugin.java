package de.mcbesser.elevator;

import org.bukkit.plugin.java.JavaPlugin;

public final class ElevatorPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        ElevatorService elevatorService = new ElevatorService(this);
        getServer().getPluginManager().registerEvents(new ElevatorListener(elevatorService), this);
        elevatorService.startParticleTask();
    }
}
