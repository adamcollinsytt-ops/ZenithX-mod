package org.adam.zenithx;

import java.util.*;

public class FriendManager {

    private static FriendManager instance;

    private final Set<String> friends  = new LinkedHashSet<>();
    private final List<String> requests = new ArrayList<>();
    private final List<String> invites  = new ArrayList<>();

    /** Usernames currently online (received from ONLINE_LIST packet) */
    private final Set<String> onlinePlayers = new HashSet<>();

    public static FriendManager getInstance() {
        if (instance == null) instance = new FriendManager();
        return instance;
    }

    // ── Friends ───────────────────────────────────────────────────────────────
    public void addFriend(String name)    { friends.add(name); }
    public void removeFriend(String name) { friends.remove(name); }
    public boolean isFriend(String name)  { return friends.contains(name); }
    public Set<String> getFriends()       { return Collections.unmodifiableSet(friends); }

    // ── Requests ──────────────────────────────────────────────────────────────
    public void addPendingRequest(String from) {
        if (!requests.contains(from)) requests.add(from);
    }
    public List<String> getPendingRequests()      { return Collections.unmodifiableList(requests); }
    public void removePendingRequest(String from) { requests.remove(from); }

    // ── Invites ───────────────────────────────────────────────────────────────
    public void addPendingInvite(String from) {
        if (!invites.contains(from)) invites.add(from);
    }
    public List<String> getPendingInvites()      { return Collections.unmodifiableList(invites); }
    public void removePendingInvite(String from) { invites.remove(from); }

    // ── Online status (updated from server ONLINE_LIST) ───────────────────────
    public void setOnlinePlayers(Collection<String> players) {
        onlinePlayers.clear();
        for (String p : players) {
            onlinePlayers.add(p);
        }
    }

    public boolean isOnline(String name) {
        return onlinePlayers.contains(name);
    }

    public Set<String> getOnlinePlayers() {
        return Collections.unmodifiableSet(onlinePlayers);
    }

    // ── Reset ─────────────────────────────────────────────────────────────────
    public void clear() {
        friends.clear();
        requests.clear();
        invites.clear();
        onlinePlayers.clear();
    }
}