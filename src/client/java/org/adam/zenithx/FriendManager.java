package org.adam.zenithx;

import java.util.*;

public class FriendManager {

    private static FriendManager instance;

    private final Set<String>    friends       = new LinkedHashSet<>();
    private final List<String>   requests      = new ArrayList<>();
    private final List<String>   invites       = new ArrayList<>();
    private final Map<String, Long> lastSeen   = new HashMap<>();
    private final Set<String>    onlinePlayers = new HashSet<>();

    public static FriendManager getInstance() {
        if (instance == null) instance = new FriendManager();
        return instance;
    }

    public void addFriend(String name) {
        if (name == null || name.isEmpty()) return;
        friends.add(name);
    }

    public void removeFriend(String name) {
        if (name == null) return;
        friends.remove(name);
    }

    public boolean isFriend(String name) {
        return friends.contains(name);
    }

    public Set<String> getFriends() {
        return new LinkedHashSet<>(friends);
    }

    public void addPendingRequest(String from) {
        if (from == null) return;
        if (!requests.contains(from)) requests.add(from);
    }

    public List<String> getPendingRequests() {
        return new ArrayList<>(requests);
    }

    public void removePendingRequest(String from) {
        requests.remove(from);
    }

    public void addPendingInvite(String from) {
        if (from == null) return;
        if (!invites.contains(from)) invites.add(from);
    }

    public List<String> getPendingInvites() {
        return new ArrayList<>(invites);
    }

    public void removePendingInvite(String from) {
        invites.remove(from);
    }

    public boolean isOnline(String name) {
        return onlinePlayers.contains(name);
    }

    public Set<String> getOnlinePlayers() {
        return new HashSet<>(onlinePlayers);
    }

    public void setOnlinePlayers(Collection<String> players) {
        long now = System.currentTimeMillis();
        Set<String> newSet = new HashSet<>();
        if (players != null) newSet.addAll(players);
        for (String old : onlinePlayers) {
            if (!newSet.contains(old)) lastSeen.put(old, now);
        }
        onlinePlayers.clear();
        onlinePlayers.addAll(newSet);
    }

    public void setLastSeen(String name, long time) {
        if (name == null) return;
        lastSeen.put(name, time);
    }

    public String getLastSeenText(String name) {
        if (onlinePlayers.contains(name)) return "";
        long last = lastSeen.getOrDefault(name, 0L);
        if (last == 0) return "(Offline)";
        long diff    = System.currentTimeMillis() - last;
        long seconds = diff / 1000;
        if (seconds < 60)  return "(Offline " + seconds + "s ago)";
        long minutes = seconds / 60;
        if (minutes < 60)  return "(Offline " + minutes + "m ago)";
        long hours = minutes / 60;
        if (hours < 24)    return "(Offline " + hours   + "h ago)";
        long days = hours / 24;
        if (days < 7)      return "(Offline " + days    + "d ago)";
        long weeks = days / 7;
        if (weeks < 52)    return "(Offline " + weeks   + "w ago)";
        long years = weeks / 52;
        return "(Offline " + years + "y ago)";
    }

    public void clear() {
        friends.clear();
        requests.clear();
        invites.clear();
        onlinePlayers.clear();
        lastSeen.clear();
    }
}