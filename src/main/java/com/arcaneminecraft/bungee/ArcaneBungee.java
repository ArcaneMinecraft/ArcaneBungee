package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

/**
 * ArcaneChatUtilPlugin.java
 * Close-chat function for the Arcane Survival server.
 *
 * @author Morios (Mark Talrey)
 * @author SimonOrJ (Simon Chuu)
 * @version 3.0-SNAPSHOT
 */

public final class ArcaneBungee extends Plugin implements Listener {
    private final String logIP = "127.0.0.1";
    private final int logPort = 25555;

    @Override
    public void onEnable() {
        StaffChat sc = new StaffChat(this); // also registers commands in the class
        getProxy().getPluginManager().registerCommand(this, sc.getChatListener());
        getProxy().getPluginManager().registerCommand(this, sc.getToggleListener());
        getProxy().getPluginManager().registerListener(this, sc);

        Tell tell = new Tell(this);
        getProxy().getPluginManager().registerCommand(this, tell.getMessage());
        getProxy().getPluginManager().registerCommand(this, tell.getReply());

        getProxy().registerChannel("BungeeCoreProtectLogger");
    }

    void logCommand (ProxiedPlayer p, String msg) {
        logCommand(p.getName(), p.getDisplayName(), p.getUniqueId().toString(), msg);
    }

    void logCommand (String name, String displayName, String uniqueId, String msg) {
        getProxy().getScheduler().runAsync(this, () -> {
            Socket client;
            try {
                client = new Socket(logIP, logPort);
                DataOutputStream ds = new DataOutputStream(client.getOutputStream());
                ds.writeUTF(msg);
                ds.writeUTF(name);
                ds.writeUTF(displayName);
                ds.writeUTF(uniqueId);
                ds.close();
                client.close();
                // TODO: Handle ConnectException separately.
            } catch (ConnectException e) {
                getLogger().warning("Cannot connect to the logging server on " + logIP + ":" + logPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        if (e.getTag().equals("MessageToBungeeCord")) {
            // data ordering: "CoreProtectForward", Message, Name, DisplayName, UniqueID
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
            try {
                String subchannel = in.readUTF();
                if (subchannel.equals("CoreProtectForward")) {
                    String msg = in.readUTF();
                    logCommand(in.readUTF(), in.readUTF(), in.readUTF(), msg); // Name, DisplayName, UUID
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }
}
