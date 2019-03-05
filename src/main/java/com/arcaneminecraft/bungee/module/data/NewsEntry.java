package com.arcaneminecraft.bungee.module.data;

import java.sql.Timestamp;
import java.util.UUID;

public final class NewsEntry {
    private final int id;
    private final UUID author;
    private final Timestamp time;
    private final String contents;

    public NewsEntry(int id, UUID author, Timestamp time, String contents) {
        this.id = id;
        this.author = author;
        this.time = time;
        this.contents = contents;
    }

    public int getId() {
        return id;
    }

    public UUID getAuthor() {
        return author;
    }

    public Timestamp getTime() {
        return time;
    }

    public String getContent() {
        return contents;
    }
}
