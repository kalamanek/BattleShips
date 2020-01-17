package server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import server.messages.MatchRoomListMessage;
import server.messages.NotificationMessage;

public class MatchRoom {

    private final String ALPHABET = "abcdefghijklmnopqrstuvwxyz";
    private HashMap<String, Player> waitingPlayerList;
    private ArrayList<Player> connectedPlayers;

    public MatchRoom() {
        this.waitingPlayerList = new HashMap<String, Player>();
        this.connectedPlayers = new ArrayList<>();
    }

    public void parse(Player player, String[] args) {
        if (args.length < 2 || player.getPlayerName().equals("")) {
            return;
        }
        String option = args[1];
        switch (option) {
        case "start":
            player.leaveGame();
            joinWaitingList(player);
            break;
        case "join":
            player.leaveGame();
            if (args.length == 3) {
                player.leaveGame();
                joinRequest(player, args[2]);
            }
            break;
        case "accept":
            player.leaveGame();
            if (args.length == 3) {
                acceptRequest(player, args[2]);
            }
            break;
        case "reject":
            if (args.length == 3) {
                rejectRequest(player, args[2]);
            }
        case "cancel":
            if (args.length == 2) {
                cancelRequest(player);
            }
        }
    }

    private synchronized void joinWaitingList(Player player) {
        waitingPlayerList.put(player.getOwnKey(), player);
        player.writeNotification(NotificationMessage.GAME_TOKEN,
                player.getOwnKey());
        sendMatchRoomList();
    }

    public synchronized void assignKey(Player player) {
        StringBuilder keyBuilder = new StringBuilder();
        Random random = new Random();
        int length = ALPHABET.length();
        for (int i = 0; i < 10; ++i) {
            keyBuilder.append(ALPHABET.charAt(random.nextInt(length)));
        }
        String key = keyBuilder.toString();
        player.setOwnKey(key);
    }

    private synchronized void joinRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (player == opponent) {
            player.writeNotification(NotificationMessage.CANNOT_PLAY_YOURSELF);
        } else if (opponent != null) {
            opponent.sendRequest(player);
        }
    }

    private synchronized void acceptRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (opponent != null &&
                opponent.getRequestedGameKey().equals(player.getOwnKey())) {
            waitingPlayerList.remove(key);
            waitingPlayerList.values().remove(player);
            opponent.requestAccepted(player);
            new Game(opponent, player);
            sendMatchRoomList();
            player.rejectAll();
            opponent.rejectAll();
        }
    }

    private synchronized void rejectRequest(Player player, String key) {
        Player opponent = waitingPlayerList.get(key);
        if (opponent != null &&
                opponent.getRequestedGameKey().equals(player.getOwnKey())) {
            opponent.requestRejected(player);
        }
    }

    private synchronized void cancelRequest(Player player) {
        Player opponent = waitingPlayerList.get(player.getRequestedGameKey());
        player.setRequestedGameKey(null);
        if (opponent != null) {
            opponent.writeNotification(
                    NotificationMessage.JOIN_GAME_REQUEST_CANCELLED,
                    player.getOwnKey());
        }
    }

    public synchronized void removeWaitingPlayer(Player player) {
        waitingPlayerList.values().remove(player);
        sendMatchRoomList();
    }

    public boolean playerNameExists(String name) {
        for (Player player : connectedPlayers) {
            if (name.equals(player.getPlayerName())) {
                return true;
            }
        }
        return false;
    }

    public synchronized void sendMatchRoomList() {
        HashMap<String, String> matchRoomList = new HashMap<String, String>();
        for (Map.Entry<String, Player> entry : waitingPlayerList.entrySet()) {
            String key = entry.getKey();
            Player player = entry.getValue();
            matchRoomList.put(key, player.getPlayerName());
        }
        MatchRoomListMessage message = new MatchRoomListMessage(matchRoomList);
        for (Map.Entry<String, Player> entry : waitingPlayerList.entrySet()) {
            Player player = entry.getValue();
            player.writeObject(message);
        }
    }

    public void addPlayer(Player player) {
        if (!connectedPlayers.contains(player)) {
            connectedPlayers.add(player);
        }
    }

    public void removePlayer(Player player) {
        connectedPlayers.remove(player);
    }

}
