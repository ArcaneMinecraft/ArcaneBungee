package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

public class ArcaneLogSender implements Listener {
    private final ArcaneBungee plugin;
    private final String ip;
    private final int port;


    ArcaneLogSender(ArcaneBungee plugin, String ip, int port) {
        this.plugin = plugin;
        this.ip = ip;
        this.port = port;
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        try {
            if (e.getTag().equalsIgnoreCase("BungeeCord")) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
                String forward = in.readUTF(); // "Forward"
                in.readUTF(); // recipients "ONLINE"
                String channel = in.readUTF(); // channel
                if(forward.equals("Forward") && (channel.equals("ChatAndLog") || channel.equals("Chat"))){
                    byte[] msgBytes = new byte[in.readShort()];
                    in.readFully(msgBytes);

                    DataInputStream is = new DataInputStream(new ByteArrayInputStream(msgBytes));
                    String msg = is.readUTF();
                    String name = is.readUTF();
                    String displayName = is.readUTF();
                    String uuid = is.readUTF();

                    // Log chat on bungeecord console
                    plugin.getLogger().info(
                            "[" + plugin.getProxy().getPlayer(name).getServer().getInfo().getName() + "] "
                                    + name + ": " + msg);

                    if (channel.equals("ChatAndLog"))
                        log(name, displayName, uuid, msg);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void log(CommandSender sender, String command, String[] args) {
        if (!(sender instanceof ProxiedPlayer))
            return;

        String msg = command;
        if (args.length != 0)
            msg += " " + String.join(" ", args);

        ProxiedPlayer p = (ProxiedPlayer) sender;
        log(p.getName(), p.getDisplayName(), p.getUniqueId().toString(), msg);
    }

    public void log(CommandSender sender, String msg) {
        if (!(sender instanceof ProxiedPlayer))
            return;

        ProxiedPlayer p = (ProxiedPlayer) sender;
        log(p.getName(), p.getDisplayName(), p.getUniqueId().toString(), msg);
    }

    public void log(String name, String displayName, String uniqueId, String msg) {
        plugin.getLogger().info("log executed");
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            Socket client;
            try {
                client = new Socket(ip, port);
                DataOutputStream ds = new DataOutputStream(client.getOutputStream());
                ds.writeUTF(msg);
                ds.writeUTF(name);
                ds.writeUTF(displayName);
                ds.writeUTF(uniqueId);
                ds.close();
                client.close();
            } catch (ConnectException e) {
                plugin.getLogger().warning("Cannot connect to the logging server on " + ip + ":" + port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
