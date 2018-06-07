package com.arcaneminecraft.bungee;

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
                String channel = in.readUTF(); // channel we delivered
                if(channel.equals("ChatAndLog")){
                    byte[] msgBytes = new byte[in.readShort()];
                    in.readFully(msgBytes);

                    DataInputStream is = new DataInputStream(new ByteArrayInputStream(msgBytes));
                    String msg = is.readUTF();
                    // in.readUTF() ordering: Name, DisplayName, UUID
                    logCommand(is.readUTF(), is.readUTF(), is.readUTF(), msg);
                }
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    void logCommand (ProxiedPlayer p, String msg) {
        logCommand(p.getName(), p.getDisplayName(), p.getUniqueId().toString(), msg);
    }

    void logCommand (String name, String displayName, String uniqueId, String msg) {
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
