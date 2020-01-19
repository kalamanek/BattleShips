package model;

public class RoomPlayer {
    private String key;
    private String name;

    public RoomPlayer(String key, String name) {
        this.key = key;
        this.name = name;
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

}
