package org.bcnlab.pufferLink.monitor;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.bcnlab.pufferLink.PufferLink;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ServerMonitor {
    private final PufferLink plugin;
    private final Map<String, Boolean> serverStatus = new HashMap<>();
    private ScheduledTask task;
    private final String permission = "pufferlink.notify";

    public ServerMonitor(PufferLink plugin) {
        this.plugin = plugin;
    }

    public void start() {
        ProxyServer.getInstance().getServers().values().forEach(server -> {
            serverStatus.put(server.getName(), false); // assume offline initially
        });

        task = ProxyServer.getInstance().getScheduler().schedule(plugin, this::checkServers, 0, 10, TimeUnit.SECONDS);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void checkServers() {
        for (ServerInfo server : ProxyServer.getInstance().getServers().values()) {
            server.ping((result, error) -> {
                boolean isOnline = error == null && result != null;
                String name = server.getName();
                boolean wasOnline = serverStatus.getOrDefault(name, false);

                if (isOnline != wasOnline) {
                    serverStatus.put(name, isOnline);

                    String msg = plugin.getPrefix() +
                            ChatColor.GRAY + "[" +
                            (isOnline ? ChatColor.GREEN + "+" : ChatColor.RED + "-") +
                            ChatColor.GRAY + "] " +
                            ChatColor.RED + "Server " +
                            ChatColor.GOLD + name +
                            ChatColor.RED + (isOnline ? " has connected to" : " has disconnected from") +
                            " the proxy.";

                    TextComponent component = new TextComponent(msg);

                    ProxyServer.getInstance().getConsole().sendMessage(component);
                    ProxyServer.getInstance().getPlayers().stream()
                            .filter(p -> p.hasPermission(permission))
                            .forEach(p -> p.sendMessage(component));
                }
            });
        }
    }
}
