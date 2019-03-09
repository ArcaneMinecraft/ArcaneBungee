package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.channel.DiscordBot;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.UUID;

/**
 * DiscordUserModule deals with all the things that interact with
 * Discord User accoutn and and Minecraft player accounts.
 * All fields with UUID uuid are for MC account, and
 * all fields with Long, long id are for Discord users.
 */
public class DiscordUserModule {
    private static final SecureRandom rnd = new SecureRandom();

    private HashMap<Long, UUID> discordToMinecraft = new HashMap<>();
    private HashMap<UUID, Long> minecraftToDiscord = new HashMap<>();

    private HashMap<Integer, UUID> mcToken = new HashMap<>();
    private HashMap<Integer, Long> dcToken = new HashMap<>();

    private MinecraftPlayerModule getMPModule() {
        return ArcaneBungee.getInstance().getMinecraftPlayerModule();
    }

    public void put(UUID uuid, long id) {
        Long oldId = minecraftToDiscord.put(uuid, id);
        if (oldId != null)
            discordToMinecraft.remove(oldId);

        discordToMinecraft.put(id, uuid);
    }

    private DiscordBot getDB() {
        return DiscordBot.getInstance();
    }

    public String getNickname(long id) {
        return getDB().getNickname(id);
    }

    public String getUserTag(long id) {
        return getDB().getUserTag(id);
    }

    public UUID getMinecraftUuid(long id) {
        return discordToMinecraft.get(id);
    }

    public long getDiscordId(UUID uuid) {
        Long id = minecraftToDiscord.get(uuid);
        if (id == null)
            return 0;
        return id;
    }

    public int linkToken(UUID uuid) {
        if (minecraftToDiscord.containsKey(uuid))
            return -1;
        int token = generateToken();
        mcToken.put(token, uuid);
        return token;
    }

    public int linkToken(long id) {
        if (discordToMinecraft.containsKey(id))
            return -1;
        int token = generateToken();
        dcToken.put(token, id);
        return token;
    }

    public boolean confirmLink(UUID uuid, long checkId, int token) {
        Long id = dcToken.remove(token);
        if (id == null || id != checkId)
            return false;

        put(uuid, id);
        return true;
    }

    public boolean confirmLink(long id, UUID checkUUID, int token) {
        UUID uuid = mcToken.remove(token);
        if (uuid == null || uuid != checkUUID)
            return false;

        put(uuid, id);
        return true;
    }

    public long unlink(UUID uuid) {
        Long id = minecraftToDiscord.remove(uuid);
        if (id == null)
            return 0;

        getDB().userUnlink(id);
        getMPModule().getPlayerData(uuid).setDiscord(0);
        discordToMinecraft.remove(id);
        return id;
    }

    public UUID unlink(long id) {
        UUID uuid = discordToMinecraft.remove(id);
        if (uuid == null)
            return null;

        getDB().userUnlink(id);
        getMPModule().getPlayerData(uuid).setDiscord(0);
        minecraftToDiscord.remove(uuid);
        return uuid;
    }

    private int generateToken() {
        int token = rnd.nextInt(999999);
        if (mcToken.containsKey(token) || dcToken.containsKey(token))
            return generateToken();
        return token;
    }
}
