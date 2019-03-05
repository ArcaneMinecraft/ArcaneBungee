package com.arcaneminecraft.bungee.storage.sql;

import com.arcaneminecraft.bungee.ArcaneBungee;

import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class ReportDatabase {
    private final ArcaneBungee plugin;
    private HashMap<Integer, Report> reports;
    private int last;

    public ReportDatabase(ArcaneBungee plugin) {
        this.plugin = plugin;
        // TODO: get all reports from a database
        // TODO: modify later on to get and store open reports only?
        // Do we even need to store all the reports? We might change them on future web gui
    }

    public Report getReport(int id) {
        Report ret = reports.get(id);
        if (ret == null) {
            // pull from database
        }
        return ret;
    }

    public boolean updatePriority(int id, Priority priority) {
        if (priority == null)
            return false;

        Report rep = getReport(id);
        if (rep == null || rep.closed)
            return false;

        rep.priority = priority;
        rep.last = new Date();
        // TODO: Update on SQL server

        return true;
    }

    public boolean updateClosed(int id, boolean closed) {
        Report rep = getReport(id);
        if (rep == null)
            return false;

        rep.closed = closed;
        rep.last = new Date();
        // TODO: Update on SQL server

        return true;
    }

    public void newReport(UUID name, String server, String world, int x, int y, int z, String body) {
        int id = last++;
        reports.put(id, new Report(id, name, new Location(server, world, x, y, z), body));
        // TODO: Notify Discord
        // TODO: Notify admins with permission
    }

    private class Report {
        private final int id;
        private final Date open;
        private final UUID user;
        private final Location location;
        private final String body;
        private Date last;
        private Priority priority;
        private boolean closed;

        private Report(int id, UUID user, Location location, String body) {
            this.id = id;
            this.open = new Date();
            this.last = this.open;
            this.user = user;
            this.location = location;
            this.body = body;
            this.priority = Priority.NORMAL;
            this.closed = false;
        }

        private Report(int id, UUID user, Location location, String body, Date open, Date last, Priority priority, boolean closed) {
            this.id = id;
            this.open = open;
            this.last = last;
            this.user = user;
            this.location = location;
            this.body = body;
            this.priority = priority;
            this.closed = closed;
        }
    }

    public enum Priority {
        LOW(0), NORMAL(1), HIGH(2);
        private final int n;

        Priority(int n) {
            this.n = n;
        }
        public int getValue() {
            return n;
        }
    }

    public class Location {
        private final String server;
        private final String world;
        private final int x;
        private final int y;
        private final int z;

        private Location(String server, String world, int x, int y, int z) {
            this.server = server;
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
