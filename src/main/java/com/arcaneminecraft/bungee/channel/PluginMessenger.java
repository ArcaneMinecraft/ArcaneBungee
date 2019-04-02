package com.arcaneminecraft.bungee.channel;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.SpyAlert;
import com.arcaneminecraft.bungee.module.MessengerModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
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

// TODO: Look into moving parts of this into the API. Reason: Shared code between ArcaneServer
public class PluginMessenger implements Listener {
    private final ArcaneBungee plugin;
    private final SpyAlert spy;
    private final String ip;
    private final int port;

    private final MessengerModule module = ArcaneBungee.getInstance().getMessengerModule();
    private MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();

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
                    ProxyServer.getInstance().getConsole().sendMessage(log);

                    UUID u;
                    if (uuid.isEmpty()) {
                        u = null;
                    } else {
                        try {
                            u = UUID.fromString(uuid);
                        } catch (IllegalArgumentException ex) {
                            u = null;
                        }
                    }

                    module.chatToDiscord(displayName, u, msg);

                    if (subChannel.equals("ChatAndLog"))
                        coreprotect(name, displayName, uuid, msg);
                    return;
                }

                if (subChannel.equals("AFK")) {
                    byte[] msgBytes = new byte[in.readShort()];
                    in.readFully(msgBytes);

                    try (DataInputStream is = new DataInputStream(new ByteArrayInputStream(msgBytes))) {
                        is.readUTF(); // Server
                        is.readUTF(); // Name
                        is.readUTF(); // DisplayName
                        String uuid = is.readUTF();
                        boolean isAFK = is.readBoolean();

                        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(UUID.fromString(uuid));
                        if (isAFK)
                            mpModule.setAFK(p);
                        else
                            mpModule.unsetAFK(p);
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
                String world = in.readUTF();
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
    public void chat(String origin, String name, String displayName, String uuid, String msg, String tag) {
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

        for (ServerInfo s : ProxyServer.getInstance().getServers().values()) {
            s.sendData("BungeeCord", out.toByteArray(), false);
        }
    }

    public void coreprotect(ProxiedPlayer p, String msg) {
        coreprotect(p.getName(), p.getDisplayName(), p.getUniqueId().toString(), msg);
    }

    private void coreprotect(String name, String displayName, String uuid, String msg) {
        ProxyServer.getInstance().getScheduler().runAsync(plugin, () -> {
            try (Socket client = new Socket(ip, port)) {
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());
                dos.writeUTF("LogCoreProtect");

                dos.writeUTF(name);
                dos.writeUTF(displayName);
                dos.writeUTF(uuid);
                dos.writeUTF(msg);

                dos.flush();

            } catch (ConnectException e) {
                plugin.getLogger().warning("Cannot connect to the logging server on " + ip + ":" + port);
            } catch (IOException e) {
                plugin.getLogger().warning("Socket connection closed before response.");
                e.printStackTrace();
            }

        });
    }

}
