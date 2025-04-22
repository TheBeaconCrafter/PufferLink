package org.bcnlab.pufferLink;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import org.bcnlab.pufferLink.api.PufferClient;
import org.bcnlab.pufferLink.api.PufferSession;
import org.bcnlab.pufferLink.command.CloudCommand;
import org.bcnlab.pufferLink.monitor.ServerMonitor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class PufferLink extends Plugin {
    private Configuration config;
    private String pluginPrefix;
    private String versionNumber = "1.0";
    private Boolean enableMonitor = true;

    private ServerMonitor serverMonitor;

    @Override
    public void onEnable() {
        getLogger().info("PufferLink plugin has been enabled!");
        loadConfig();

        Configuration config = getConfig();
        String apiUrl = config.getString("api-url", "");
        String email = config.getString("email", "");
        String password = config.getString("password", "");
        pluginPrefix = ChatColor.translateAlternateColorCodes('&', config.getString("plugin-prefix", "&3Cloud &8» &r"));
        enableMonitor = config.getBoolean("enable-monitor", true);

        if (apiUrl.isEmpty() || email.isEmpty() || password.isEmpty()) {
            getLogger().warning("Missing config values! Please fill in 'api-url', 'email', and 'password'.");
            return;
        }

        PufferSession session = new PufferSession(email, password, apiUrl);
        if (!session.login()) {
            getLogger().severe("Login to PufferPanel failed!");
            return;
        }

        // Register commands
        PufferClient client = new PufferClient(apiUrl, session.getSessionCookie(), this);
        getProxy().getPluginManager().registerCommand(this, new CloudCommand(client, this));

        // Server Monitor
        if(enableMonitor) {
            this.serverMonitor = new ServerMonitor(this);
            this.serverMonitor.start();
            getLogger().info("Server monitor started.");
        }

        getLogger().info("Logged into PufferPanel!");
        client.printAllServers();
    }

    @Override
    public void onDisable() {
        if (serverMonitor != null) {
            serverMonitor.stop();
        }

        getLogger().info("PufferLink plugin has been disabled!");
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            try {
                getDataFolder().mkdirs();
                try (InputStream in = getResourceAsStream("config.yml")) {
                    Files.copy(in, configFile.toPath());
                }
                getLogger().info("Default config.yml created.");
            } catch (IOException e) {
                getLogger().severe("Failed to create config.yml: " + e.getMessage());
            }
        }

        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().severe("Failed to load config.yml: " + e.getMessage());
        }
    }

    public Configuration getConfigFile() {
        return config;
    }

    public String getPrefix() {
        return pluginPrefix;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    private Configuration getConfig() {
        try {
            return ConfigurationProvider.getProvider(YamlConfiguration.class)
                    .load(new File(getDataFolder(), "config.yml"));
        } catch (Exception e) {
            getLogger().severe("Failed to load config.yml: " + e.getMessage());
            return null;
        }
    }
}
