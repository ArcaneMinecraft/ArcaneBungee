package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.module.data.NewsEntry;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import com.google.common.collect.ImmutableList;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NewsModule {
    private NewsEntry latest;
    private List<NewsEntry> allNews = new ArrayList<>();

    public void add(NewsEntry news) {
        allNews.add(news.getId(), news);
        if (latest == null || latest.getId() < news.getId()) {
            latest = news;
        }
    }

    public synchronized void newNews(UUID author, String content) {
        SQLDatabase.getInstance().addNews(author, content);
        add(new NewsEntry(allNews.size(), author, Timestamp.from(Instant.now()), content));
    }

    public NewsEntry get(int id) {
        return allNews.get(id);
    }

    public NewsEntry getLatest() {
        return latest;
    }

    public ImmutableList<NewsEntry> getAllNews() {
        return ImmutableList.copyOf(allNews);
    }
}
