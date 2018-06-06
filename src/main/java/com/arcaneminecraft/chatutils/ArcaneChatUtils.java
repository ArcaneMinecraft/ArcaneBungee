package com.arcaneminecraft.chatutils;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

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

public final class ArcaneChatUtils extends Plugin {
    private final String logIP = "127.0.0.1";
    private final int logPort = 25555;

    @Override
    public void onEnable() {
        StaffChat sc = new StaffChat(this); // also registers commands in the class
        this.getProxy().getPluginManager().registerCommand(this, sc.getChatListener());
        this.getProxy().getPluginManager().registerCommand(this, sc.getToggleListener());

        Tell tell = new Tell(this);
    }

    void logCommand (ProxiedPlayer p, String msg) {
        Socket client;
        try {
            client = new Socket(logIP, logPort);
            DataOutputStream ds = new DataOutputStream(client.getOutputStream());
            ds.writeUTF(msg);
            ds.writeUTF(p.getName());
            ds.writeUTF(p.getDisplayName());
            ds.writeUTF(p.getUniqueId().toString());
            ds.close();
            client.close();
            // TODO: Handle ConnectException separately.
        } catch (ConnectException e) {
            getLogger().warning("Cannot connect to the logging server on " + logIP + ":" + logPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
