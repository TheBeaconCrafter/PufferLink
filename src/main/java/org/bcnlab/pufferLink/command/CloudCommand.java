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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudCommand extends Command implements TabExecutor {
    private final PufferClient client;
    private final PufferLink plugin;

    public CloudCommand(PufferClient client, PufferLink plugin) {
        super("cloud", "pufferlink.use", "puffer"); // command, permission, aliases
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponent(plugin.getPrefix() + ChatColor.RED + "PufferLink Version " + ChatColor.GOLD + plugin.getVersionNumber() + ChatColor.RED + " by ItsBeacon"));
            sender.sendMessage(new TextComponent(plugin.getPrefix() + "§7Usage: /cloud <list|status|console|start|stop|restart>"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "list":
                client.listServers(serverList -> {
                    if (serverList.isEmpty()) {
                        sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cNo servers found."));
                        return;
                    }

                    sender.sendMessage(new TextComponent(plugin.getPrefix() + "§aAvailable Servers:"));
                    serverList.forEach(server -> {
                        String id = server.get("id").getAsString();
                        String name = server.get("name").getAsString();
                        TextComponent comp = new TextComponent("§7→ §e" + name + " §f(ID: " + id + ")");
                        comp.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id));
                        comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new Text("§7Click to copy")));
                        sender.sendMessage(comp);
                    });
                });
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
                break;

            default:
                sender.sendMessage(new TextComponent(plugin.getPrefix() + "§cUnknown subcommand. Use list, status or console."));
                break;
        }
    }

    // TAB EXECUTOR
    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();

        List<String> subcommands = Arrays.asList("list", "status", "start", "stop", "restart", "console");

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
