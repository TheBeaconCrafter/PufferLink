package org.bcnlab.pufferLink.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bcnlab.pufferLink.PufferLink;
import org.bcnlab.pufferLink.api.PufferClient;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class CloudCommand extends Command implements TabExecutor {
    private final PufferClient client;
    private final PufferLink plugin;
    private final Map<String, CachedStatus> statusCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION = 10000; // 10 seconds cache

    private static class CachedStatus {
        final String statusIndicator;
        final long timestamp;
        final boolean isRunning;

        CachedStatus(String indicator, boolean running) {
            this.statusIndicator = indicator;
            this.timestamp = System.currentTimeMillis();
            this.isRunning = running;
        }

        boolean isValid() {
            return System.currentTimeMillis() - timestamp < CACHE_DURATION;
        }
    }

    public CloudCommand(PufferClient client, PufferLink plugin) {
        super("cloud", "pufferlink.use", "puffer"); // command, permission, aliases
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "PufferLink Version " + ChatColor.GOLD + plugin.getVersionNumber() + ChatColor.RED + " by ItsBeacon"));
            sender.sendMessage(new TextComponent(plugin.getPrefix() + "§7Usage: /cloud <list|up|status|console|start|stop|restart>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                client.listServers(serverList -> processServerList(sender, serverList, false));
                break;

            case "status":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cUsage: /cloud status <id>"));
                    return;
                }

                client.getServerStatus(args[1], status -> {
                    if (status == null) {
                        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cServer not found or error occurred."));
                        return;
                    }
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + "§aServer Status for §e" + args[1] + "§a:"));
                    sender.sendMessage(new TextComponent("§7Running: §f" + status.get("running").getAsBoolean()));
                    sender.sendMessage(new TextComponent("§7Installing: §f" + status.get("installing").getAsBoolean()));
                });
                break;

            case "console":
                if (args.length < 3) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cUsage: /cloud console <id> <command>"));
                    return;
                }
                String id = args[1];
                String command = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                client.sendConsoleCommand(id, command, success -> {
                    if (success) sender.sendMessage(new TextComponent(plugin.getPrefix() + "§aCommand sent to console."));
                    else sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cFailed to send command."));
                });
                break;

            case "start":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cUsage: /cloud start <id>"));
                    return;
                }

                client.startServer(args[1], success -> {
                    if (success) {
                        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§aServer starting..."));
                    } else {
                        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cFailed to start server."));
                    }
                });
                break;

            case "stop":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cUsage: /cloud stop <id>"));
                    return;
                }

                client.stopServer(args[1], success -> {
                    if (success) {
                        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§aServer stopping..."));
                    } else {
                        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cFailed to stop server."));
                    }
                });
                break;

            case "restart":
                if (args.length < 2) {
                    sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cUsage: /cloud restart <id>"));
                    return;
                }                client.restartServer(args[1], 
                    success -> {}, // We'll handle messages through the messageCallback
                    message -> sender.sendMessage(new TextComponent(plugin.getPrefix() + message)));
                break;            case "up":
                client.listServers(serverList -> processServerList(sender, serverList, true));
                break;

            default:
                sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cUnknown subcommand. Use list, up, status, console, start, stop, or restart"));
                break;
        }
    }    private void getServerWithStatus(JsonObject server, Consumer<TextComponent> callback) {
        String id = server.get("id").getAsString();
        String name = server.get("name").getAsString();
        
        client.getServerStatus(id, status -> {
            String statusIndicator = "§cD"; // Default to Down (red)
            if (status != null) {
                boolean isRunning = status.get("running").getAsBoolean();
                boolean isInstalling = status.get("installing").getAsBoolean();
                if (isInstalling) {
                    statusIndicator = "§eI"; // Yellow for Installing
                } else if (isRunning) {
                    statusIndicator = "§aU"; // Green for Up
                }
            }
            
            TextComponent comp = new TextComponent("§7→ " + statusIndicator + " §e" + name + " §f(ID: " + id + ")");
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text("§7Click to copy")));
            callback.accept(comp);
        });
    }    private void processServerList(CommandSender sender, List<JsonObject> serverList, boolean onlyRunning) {
        if (serverList.isEmpty()) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cNo servers found."));
            return;
        }

        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§aAvailable Servers:"));
        
        // Create batches of 5 servers
        List<List<JsonObject>> batches = new ArrayList<>();
        for (int i = 0; i < serverList.size(); i += 5) {
            batches.add(serverList.subList(i, Math.min(i + 5, serverList.size())));
        }

        final int[] processed = {0};
        final int[] shown = {0};
        final int total = serverList.size();

        // Process each batch
        processBatch(sender, batches, 0, processed, shown, total, onlyRunning);
    }

    private void processBatch(CommandSender sender, List<List<JsonObject>> batches, int batchIndex,
                            int[] processed, int[] shown, int total, boolean onlyRunning) {
        if (batchIndex >= batches.size()) {
            if (onlyRunning && shown[0] == 0) {
                sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cNo running servers found."));
            }
            return;
        }

        List<JsonObject> batch = batches.get(batchIndex);
        CountDownLatch latch = new CountDownLatch(batch.size());

        // Process servers in current batch concurrently
        for (JsonObject server : batch) {
            String id = server.get("id").getAsString();
            CachedStatus cached = statusCache.get(id);

            if (cached != null && cached.isValid()) {
                // Use cached status
                processServerStatus(sender, server, cached.statusIndicator, cached.isRunning, 
                    processed, shown, total, onlyRunning);
                latch.countDown();
            } else {
                // Get fresh status
                getServerWithStatus(server, component -> {
                    String text = component.toLegacyText();
                    boolean isRunning = text.contains("§aU");
                    String indicator = isRunning ? "§aU" : (text.contains("§eI") ? "§eI" : "§cD");
                    
                    // Cache the result
                    statusCache.put(id, new CachedStatus(indicator, isRunning));
                    
                    processServerStatus(sender, server, indicator, isRunning,
                        processed, shown, total, onlyRunning);
                    latch.countDown();
                });
            }
        }

        // Schedule next batch processing
        plugin.getProxy().getScheduler().schedule(plugin, () -> {
            try {
                // Wait for current batch to complete (with timeout)
                latch.await(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Process next batch
            processBatch(sender, batches, batchIndex + 1, processed, shown, total, onlyRunning);
        }, 50, TimeUnit.MILLISECONDS);
    }

    private void processServerStatus(CommandSender sender, JsonObject server, String statusIndicator,
                                   boolean isRunning, int[] processed, int[] shown, 
                                   int total, boolean onlyRunning) {
        processed[0]++;
        if (!onlyRunning || isRunning) {
            String id = server.get("id").getAsString();
            String name = server.get("name").getAsString();
            
            TextComponent comp = new TextComponent("§7→ " + statusIndicator + " §e" + name + " §f(ID: " + id + ")");
            comp.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id));
            comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text("§7Click to copy")));
            
            sender.sendMessage(comp);
            shown[0]++;
        }
    }

    // TAB EXECUTOR
    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        List<String> subcommands = Arrays.asList("list", "up", "status", "start", "stop", "restart", "console");

        if (args.length == 1) {
            for (String subcommand : subcommands) {
                if (subcommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subcommand);
                }
            }
        }

        return completions;
    }
}
