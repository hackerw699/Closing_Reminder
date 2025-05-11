package com.example.hw699.closing_reminder;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.TimeUnit;

public class ShutdownCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private BukkitTask shutdownTask = null;
    private long shutdownTicks = -1;
    private String kickMessage;

    public ShutdownCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig(); 
        loadConfig();
    }

    private void loadConfig() {
        kickMessage = plugin.getConfig().getString("shutdown_kick_message", "§cThe server is shutting down for operational hours.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("shutdown")) {

            if (args.length == 1 && args[0].equalsIgnoreCase("cancel")) {
                return cancelShutdown(sender);
            }

            if (args.length == 0) {
                return initiateImmediateShutdown(sender);
            }

            if (args.length == 1) {
                return scheduleShutdown(sender, args[0]);
            }

            sender.sendMessage("§cUsage: /shutdown [HH.mm] or /shutdown cancel");
            return false;
        }

        if (command.getName().equalsIgnoreCase("cancelshutdown")) {
            return cancelShutdown(sender);
        }

        return false;
    }

    private boolean initiateImmediateShutdown(CommandSender sender) {
        sender.sendMessage("§aShutdown initiated immediately.");
        shutdownServer();
        return true;
    }

    private boolean cancelShutdown(CommandSender sender) {
        if (shutdownTask != null && !shutdownTask.isCancelled()) {
            shutdownTask.cancel();
            shutdownTask = null;
            shutdownTicks = -1;
            sender.sendMessage("§aScheduled shutdown has been cancelled.");
            plugin.getLogger().info("Shutdown task cancelled.");
        } else {
            sender.sendMessage("§cNo shutdown is currently scheduled or the scheduled shutdown has already occurred.");
        }
        return true;
    }

    private boolean scheduleShutdown(CommandSender sender, String timeInput) {
        if (shutdownTask != null && !shutdownTask.isCancelled()) {
            sender.sendMessage("§cA shutdown is already scheduled.");
            return true;
        }

        if (!timeInput.matches("\\d{1,2}\\.\\d{2}")) {
            sender.sendMessage("§cInvalid time format. Use HH.mm (e.g., 2.30).");
            return false;
        }

        String[] timeParts = timeInput.split("\\.");
        int hours = Integer.parseInt(timeParts[0]);
        int minutes = Integer.parseInt(timeParts[1]);

        if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
            sender.sendMessage("§cInvalid time range. Use up to 23.59.");
            return false;
        }

        long delayMillis = TimeUnit.HOURS.toMillis(hours) + TimeUnit.MINUTES.toMillis(minutes);
        long delayTicks = delayMillis / 50;

        this.shutdownTicks = delayTicks;

        broadcast("§cServer is shutting down in " + hours + " hour(s) and " + minutes + " minute(s)!");
        plugin.getLogger().info("[closing_reminder] Shutdown task scheduled. Delay ticks: " + delayTicks);

        scheduleCountdowns(delayTicks);
        shutdownTask = Bukkit.getScheduler().runTaskLater(plugin, this::shutdownServer, delayTicks);

        sender.sendMessage("§aShutdown scheduled in " + hours + " hour(s) and " + minutes + " minute(s).");
        return true;
    }

    private void scheduleCountdowns(long delayTicks) {
        long fiveMin = TimeUnit.MINUTES.toSeconds(5) * 20;
        long oneMin = TimeUnit.MINUTES.toSeconds(1) * 20;

        if (delayTicks > fiveMin) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> broadcast("§eServer will shut down in 5 minutes!"), delayTicks - fiveMin);
        }

        if (delayTicks > oneMin) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> broadcast("§eServer will shut down in 1 minute!"), delayTicks - oneMin);
        }

        for (int i = 10; i >= 1; i--) {
            final int sec = i;
            long tick = delayTicks - (sec * 20L);
            if (tick > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendTitle("§cServer closing in " + sec + "s", "", 0, 20, 0);
                        player.playSound(player.getLocation(), getCountdownSound(), getCountdownVolume(), getCountdownPitch());
                    }
                }, tick);
            }
        }
    }

    private void shutdownServer() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.kickPlayer(kickMessage);
        }
        shutdownTask = null;
        shutdownTicks = -1;
        Bukkit.shutdown();
    }

    private void broadcast(String msg) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(msg);
        }
    }

    private Sound getCountdownSound() {
        String name = plugin.getConfig().getString("sounds.countdown.sound", "block.note_block.pling");
        try {
            return Sound.valueOf(name.toUpperCase().replace('.', '_'));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound: " + name + ". Using default.");
            return Sound.BLOCK_NOTE_BLOCK_PLING;
        }
    }

    private float getCountdownVolume() {
        return (float) plugin.getConfig().getDouble("sounds.countdown.volume", 1.0);
    }

    private float getCountdownPitch() {
        return (float) plugin.getConfig().getDouble("sounds.countdown.pitch", 1.0);
    }
}
