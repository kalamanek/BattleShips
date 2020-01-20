package server.messages;

import model.RoomListPlayer;

import java.io.Serializable;
import java.util.HashMap;

public class MatchRoomListMessage implements Serializable {

    private HashMap<String, RoomListPlayer> matchRoomList;

    public MatchRoomListMessage(HashMap<String, RoomListPlayer> matchRoomList) {
        this.matchRoomList = matchRoomList;
    }

    public HashMap<String, RoomListPlayer> getMatchRoomList() {
        return this.matchRoomList;
    }
    
}
