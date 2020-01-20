package model;

public class RoomPlayer {
    private String key;
    private String name;
    private String avatar;

    public RoomPlayer(String key, String name, String avatar) {
        this.key = key;
        this.name = name;
        this.avatar = avatar;
    }

    public String toString() {
        return this.name;
    }

    public String getKey() {
        return this.key;
    }

    public String getName() {
        return this.name;
    }

    public String getAvatar() {
        return avatar;
    }
}
