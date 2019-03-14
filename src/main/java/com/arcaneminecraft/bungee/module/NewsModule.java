package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.storage.SQLDatabase;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class NewsModule {
    private Entry latest;

    public synchronized void newNews(UUID author, String content) {
        SQLDatabase.getInstance().addNews(author, content);
        setLatest(new Entry(author, Timestamp.from(Instant.now()), content));
    }

    public void setLatest(Entry news) {
        latest = news;
    }

    public Entry getLatest() {
        return latest;
    }



    public static final class Entry {
        private final UUID author;
        private final Timestamp time;
        private final String contents;

        public Entry(UUID author, Timestamp time, String contents) {
            this.author = author;
            this.time = time;
            this.contents = contents;
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
}
