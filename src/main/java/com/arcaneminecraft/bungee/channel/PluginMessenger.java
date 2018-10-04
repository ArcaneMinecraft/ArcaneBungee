package com.arcaneminecraft.bungee.channel;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.ReturnRunnable;
import com.arcaneminecraft.bungee.SpyAlert;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;
import java.util.UUID;

public class PluginMessenger implements Listener {
    private final ArcaneBungee plugin;
    private final SpyAlert spy;
    private final String ip;
    private final int port;


    public PluginMessenger(ArcaneBungee plugin, SpyAlert spy) {
        this.plugin = plugin;
        this.spy = spy;

        this.ip = plugin.getConfig().getString("arcanelog.ip");
        this.port = plugin.getConfig().getInt("arcanelog.port");
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent e) {
        try {
            if (e.getTag().equalsIgnoreCase("BungeeCord")) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));

                if (!in.readUTF().equals("Forward")) // Should be "Forward"
                    return;

                in.readUTF(); // recipients "ONLINE"
                String subChannel = in.readUTF(); // channel
                if(subChannel.equals("ChatAndLog") || subChannel.equals("Chat")) {
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
                    if (!tag.isEmpty()) {
                        tag = ChatColor.translateAlternateColorCodes('&', tag);
                        for (BaseComponent bp : TextComponent.fromLegacyText(tag))
                            log.addExtra(bp);
                        log.addExtra(" ");
                    }
                    log.addExtra("<" + name + "> " + msg);
                    plugin.getProxy().getConsole().sendMessage(log);

                    DiscordConnection d = plugin.getDiscordConnection();
                    if (d != null)
                        d.chatToDiscord(displayName, uuid, msg);

                    if (subChannel.equals("ChatAndLog"))
                        coreprotect(name, displayName, uuid, msg);
                    return;
                }

                if (subChannel.equals("AFK")) {
                    byte[] msgBytes = new byte[in.readShort()];
                    in.readFully(msgBytes);

                    try (DataInputStream is = new DataInputStream(new ByteArrayInputStream(msgBytes))) {
                        String server = is.readUTF();
                        String name = is.readUTF();
                        String displayName = is.readUTF();
                        String uuid = is.readUTF();
                        boolean isAFK = is.readBoolean();

                        ProxiedPlayer p = plugin.getProxy().getPlayer(UUID.fromString(uuid));
                        if (isAFK)
                            plugin.getAfkList().add(p);
                        else
                            plugin.getAfkList().remove(p);
                    }
                    return;
                }
                return;
            }

            if (e.getTag().equalsIgnoreCase("arcaneserver:alert")) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(e.getData()));
                in.readUTF(); // server // TODO: Optimize
                String type = in.readUTF();
                in.readUTF(); // player
                String uuid = in.readUTF();
                String world = in.readUTF(); // world
                int[] loc = {in.readInt(), in.readInt(), in.readInt()}; // Location

                if (type.equals("XRay")) {
                    String material = in.readUTF();
                    spy.xRayAlert(UUID.fromString(uuid), material, loc, world);
                } else if (type.equals("Sign")) {
                    String[] lines = new String[]{in.readUTF(), in.readUTF(), in.readUTF(), in.readUTF()};
                    spy.signAlert(UUID.fromString(uuid), lines, loc, world);
                }

                //return;

            }
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Transferred over from ArcaneServer's PluginMessenger.chat() method
     */
    void chat(@SuppressWarnings("SameParameterValue") String origin, String name, String displayName, String uuid, String msg, String tag) {
        String channel = "Chat";

        ByteArrayOutputStream byteos = new ByteArrayOutputStream();
        try (DataOutputStream os = new DataOutputStream(byteos)) {

            os.writeUTF(origin);
            os.writeUTF(msg);
            os.writeUTF(name);
            os.writeUTF(displayName == null ? name : displayName);
            os.writeUTF(uuid == null ? "" : uuid);
            os.writeUTF(tag == null ? "" : tag);

            forwardChannelMessage(channel, byteos); // Subchannel
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void forwardChannelMessage(String channel, ByteArrayOutputStream byteArrayOutputStream) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(channel); // Subchannel

        out.writeShort(byteArrayOutputStream.toByteArray().length);
        out.write(byteArrayOutputStream.toByteArray());

        for (ServerInfo s : plugin.getProxy().getServers().values()) {
            s.sendData("BungeeCord", out.toByteArray(), false);
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

    private void toLog(@SuppressWarnings("SameParameterValue") String subChannel, @SuppressWarnings("SameParameterValue") ReturnRunnable<String> run, String... args) {

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
