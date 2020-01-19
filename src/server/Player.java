package server;

import model.Board;
import server.messages.ChatMessage;
import server.messages.MoveMessage;
import server.messages.NotificationMessage;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Player extends Thread {

    private Socket socket;
    private MatchRoom matchRoom;
    private String login = "";
    private ObjectOutputStream out;
    private Game game;
    private Board board;
    private HashMap<String, Player> requestList;
    private String ownKey;
    private String requestedGameKey;
    private Timer inactivityTimer;

    public final static int INACTIVITY_TIMEOUT = 600000;

    public Player(Socket socket, MatchRoom matchRoom) {
        this.socket = socket;
        this.matchRoom = matchRoom;
        matchRoom.assignKey(this);
        matchRoom.addPlayer(this);
        this.requestList = new HashMap<>();
        System.out.println(socket.getRemoteSocketAddress().toString() +
                " connected with user key - " + ownKey);
    }

    @Override
    public void run() {
        super.run();
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(
                    socket.getOutputStream()));
            out.flush();
            ObjectInputStream in = new ObjectInputStream(
                    socket.getInputStream());

            Object input;

            while ((input = in.readObject()) != null) {
                this.refreshInavtivityTimer();

                if (input instanceof String[]) {
                    String[] array = (String[]) input;
                    for(String a : array )
                        System.out.println(a);

                    int length = array.length;

                    if (length > 0) {
                        String message = array[0];

                        switch (message) {
                            case "join":
                                matchRoom.parse(this, array);
                                break;
                            case "login":
                                if (length != 3 || array[1] == null ||
                                        array[1].equals("")) {
                                    writeNotification(NotificationMessage.INVALID_LOGIN_NAME);
                                } else if (matchRoom.playerNameExists(array[1])) {
                                    writeNotification(NotificationMessage.NAME_TAKEN);
                                }else if(array[2] == null ||
                                        array[2].equals("")){
                                    writeNotification(NotificationMessage.PASSWORD_IS_INVALID);

                                }else {
                                    if(Server.checkUser(array[1],array[2])) {
                                        login = array[1];
                                        writeNotification(NotificationMessage.NAME_ACCEPTED);
                                        matchRoom.sendMatchRoomList();
                                    }else{
                                        writeNotification(NotificationMessage.PASSWORD_IS_INVALID);
                                    }
                                }
                                break;
                            case "register":
                                if (length != 3 || array[1] == null ||
                                        array[1].equals("")) {
                                    writeNotification(NotificationMessage.INVALID_LOGIN_NAME);
                                }else if (Server.userExist(array[1])) {
                                    writeNotification(NotificationMessage.NAME_TAKEN);
                                }else{
                                    Server.addUser(array[1],array[2]);
                                    login = array[1];
                                    writeNotification(NotificationMessage.NAME_ACCEPTED);
                                    matchRoom.sendMatchRoomList();
                                }
                        }
                    }
                } else if (input instanceof Board) {
                    Board board = (Board) input;
                    if (Board.isValid(board) && game != null) {
                        writeNotification(NotificationMessage.BOARD_ACCEPTED);
                        this.board = board;
                        game.checkBoards();
                    } else if (game == null) {
                        writeNotification(NotificationMessage.NOT_IN_GAME);
                    } else {
                        writeNotification(NotificationMessage.INVALID_BOARD);
                    }
                } else if (input instanceof MoveMessage) {
                    if (game != null) {
                        game.applyMove((MoveMessage) input, this);
                    }
                } else if (input instanceof ChatMessage) {
                    if (game != null) {
                        Player opponent = game.getOpponent(this);
                        if (opponent != null) {
                            opponent.writeObject(input);
                        }
                    }
                }
            }
        } catch (java.net.SocketException e) {
            if (game != null) {
                leaveGame();
            } else {
                matchRoom.removeWaitingPlayer(this);
            }
            matchRoom.removePlayer(this);
            System.out.println(socket.getRemoteSocketAddress().toString() +
                    " socket closed");
        } catch (Exception e){
            e.printStackTrace();
            if (game != null) {
                leaveGame();
            } else {
                matchRoom.removeWaitingPlayer(this);
            }
            matchRoom.removePlayer(this);
        }
    }

    private synchronized void refreshInavtivityTimer(){

        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
        inactivityTimer = new Timer();
        inactivityTimer.schedule(new Player.InactivityTimerTask(), INACTIVITY_TIMEOUT);
    }

    private synchronized void destroySelf(){

        if (inactivityTimer != null) {
            inactivityTimer.cancel();
        }
        try {
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(game!=null) {
            game.cancelTimers();
        }
        System.out.println("for Inactivity destroying " + this.login + " session on thread " + Thread.currentThread().getId());
        Thread.currentThread().interrupt();
        //Thread.currentThread().stop();
        return;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public String getPlayerName() {
        return login;
    }

    public void writeMessage(String message) {
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeObject(Object object) {
        try {
            out.writeObject(object);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeNotification(int notificationMessage, String... text) {
        try {
            NotificationMessage nm = new NotificationMessage(
                    notificationMessage, text);
            out.writeObject(nm);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Board getBoard() {
        return this.board;
    }

    public synchronized void sendRequest(Player requester) {
        requestList.put(requester.getOwnKey(), requester);
        requester.requestedGameKey = this.ownKey;
        writeNotification(NotificationMessage.NEW_JOIN_GAME_REQUEST,
                requester.getOwnKey(), requester.getPlayerName());
    }

    public synchronized void requestAccepted(Player opponent) {
        opponent.requestList.remove(ownKey);
        requestedGameKey = null;
        writeNotification(NotificationMessage.JOIN_GAME_REQUEST_ACCEPTED);
    }

    public synchronized void requestRejected(Player opponent) {
        opponent.requestList.remove(ownKey);
        requestedGameKey = null;
        writeNotification(NotificationMessage.JOIN_GAME_REQUEST_REJECTED);
    }

    public void setOwnKey(String ownKey) {
        this.ownKey = ownKey;
    }

    public String getOwnKey() {
        return ownKey;
    }

    public void setRequestedGameKey(String key) {
        this.requestedGameKey = key;
    }

    public String getRequestedGameKey() {
        return requestedGameKey;
    }

    public void rejectAll() {
        for (Player p : requestList.values()) {
            p.requestRejected(this);
        }
    }
    public void leaveGame() {
        if (game != null) {
            Player opponent = game.getOpponent(this);
            opponent.writeNotification(NotificationMessage.OPPONENT_DISCONNECTED);
            game.killGame();
        }
    }

    private class InactivityTimerTask extends TimerTask {

        @Override
        public void run() {
            Player.this.writeNotification(NotificationMessage.PLAYER_INACIVITY);

            Player.this.destroySelf();
        }

    }

}
