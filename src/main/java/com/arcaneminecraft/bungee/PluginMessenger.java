package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
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

public class PluginMessenger implements Listener {
    private final ArcaneBungee plugin;
    private final SpyAlert spy;
    private final String ip;
    private final int port;


    PluginMessenger(ArcaneBungee plugin, SpyAlert spy) {
        this.plugin = plugin;
        this.spy = spy;

        this.ip = plugin.getConfig().getString("arcanelog.ip");
        this.port = plugin.getConfig().getInt("arcanelog.port");
    }

    @SuppressWarnings("unused") // because ArcaneAlert portion is still under development. TODO: Remove when ArcaneAlert is implemented
    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        try {
            if (e.getTag().equalsIgnoreCase("BungeeCord")) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

                if (!in.readUTF().equals("Forward")) // Should be "Forward"
                    return;

                in.readUTF(); // recipients "ONLINE"
                String channel = in.readUTF(); // channel
                if(channel.equals("ChatAndLog") || channel.equals("Chat")){
                    byte[] msgBytes = new byte[in.readShort()];
                    in.readFully(msgBytes);

                    DataInputStream is = new DataInputStream(new ByteArrayInputStream(msgBytes));
                    String server = is.readUTF(); // server
                    String msg = is.readUTF();
                    String name = is.readUTF();
                    String displayName = is.readUTF();
                    String uuid = is.readUTF();
                    String tag = is.readUTF();

                    // Log chat on bungeecord console
                    TextComponent log = new TextComponent(server + ": ");
                    if (!tag.isEmpty())
                        log.addExtra(" " + tag);
                    log.addExtra("<" + name + "> " + msg);
                    plugin.getProxy().getConsole().sendMessage(log);

                    if (channel.equals("ChatAndLog"))
                        coreprotect(name, displayName, uuid, msg);
                }
                return;
            }

            if (e.getTag().equalsIgnoreCase("ArcaneAlert")) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
                String server = in.readUTF();
                String type = in.readUTF();
                String player = in.readUTF();
                String uuid = in.readUTF();
                String world = in.readUTF();
                int[] loc = {in.readInt(), in.readInt(), in.readInt()}; // Location

                if (type.equals("XRay")) {
                    String material = in.readUTF();
                    spy.xRayAlert(player, material, loc);
                } else if (type.equals("Sign")) {
                    String[] lines = new String[]{in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF()};
                    spy.signAlert(player, lines, loc);
                }

                //return;

            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void coreprotect(CommandSender sender, String command, String[] args) {
        if (!(sender instanceof ProxiedPlayer))
            return;

        String msg = command;
        if (args.length != 0)
            msg += " " + String.join(" ", args);

        ProxiedPlayer p = (ProxiedPlayer) sender;
        coreprotect(p.getName(), p.getDisplayName(), p.getUniqueId().toString(), msg);
    }

    public void coreprotect(CommandSender sender, String msg) {
        if (!(sender instanceof ProxiedPlayer))
            return;

        ProxiedPlayer p = (ProxiedPlayer) sender;
        coreprotect(p.getName(), p.getDisplayName(), p.getUniqueId().toString(), msg);
    }

    public void coreprotect(String name, String displayName, String uuid, String msg) {
        toLog("LogCoreProtect", null, name, displayName, uuid, msg);
    }

    private void toLog(String subChannel, ReturnRunnable<String> run, String... args) {

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            String response = null;

            try (Socket client = new Socket(ip, port)) {
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                dos.writeUTF(subChannel);

                for (String s : args)
                    dos.writeUTF(s);

                dos.flush();

                if (run != null) {
                    DataInputStream dis = new DataInputStream(client.getInputStream());
                    response = dis.readUTF();
                }
            } catch (ConnectException e) {
                plugin.getLogger().warning("Cannot connect to the logging server on " + ip + ":" + port);
            } catch (IOException e) {
                plugin.getLogger().warning("Socket connection closed before response.");
                e.printStackTrace();
            }

            if (run != null)
                run.run(response);
        });
    }

}
