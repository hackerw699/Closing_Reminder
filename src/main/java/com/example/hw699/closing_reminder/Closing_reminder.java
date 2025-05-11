package com.example.hw699.closing_reminder;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;

public class Closing_reminder extends JavaPlugin {

    @Override
    public void onEnable() {
        createPluginFolder();

        getCommand("shutdown").setExecutor(new ShutdownCommand(this));
        saveDefaultConfig();
        getLogger().info("ClosingReminder enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ClosingReminder disabled!");
    }

    private void createPluginFolder() {
        File pluginFolder = new File(getDataFolder(), "");
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs(); 
            getLogger().info("Plugin folder created at: " + pluginFolder.getAbsolutePath());
        }
    }
}
