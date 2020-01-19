package server;

import model.Board;
import model.Ship;
import model.Square;
import server.messages.BoardMessage;
import server.messages.MoveMessage;
import server.messages.MoveResponseMessage;
import server.messages.NotificationMessage;

import javax.management.Notification;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Game {

    private Player player1;
    private ArrayList<Player>  player1Watchers;
    private Player player2;
    private ArrayList<Player>  player2Watchers;
    private Player turn;
    private Boolean isPublic = true;

    private Timer placementTimer;
    private Timer turnTimer;

    public final static int TURN_TIMEOUT = 40000;
    public final static int PLACEMENT_TIMEOUT = 100000;

    private boolean gameStarted;

    public Game(Player player1, Player player2) {
        this.player1Watchers = new ArrayList<>();
        this.player2Watchers = new ArrayList<>();
        this.player1 = player1;
        this.player2 = player2;
        player1.setGame(this);
        player2.setGame(this);
        player1.writeNotification(NotificationMessage.OPPONENTS_NAME,
                player2.getPlayerName());
        player2.writeNotification(NotificationMessage.OPPONENTS_NAME,
                player1.getPlayerName());
        NotificationMessage placeShipsMessage = new NotificationMessage(
                NotificationMessage.PLACE_SHIPS);
        player1.writeObject(placeShipsMessage);
        player2.writeObject(placeShipsMessage);

        placementTimer = new Timer();
        placementTimer.schedule(new PlacementTimerTask(), PLACEMENT_TIMEOUT);
    }

    public Player getOpponent(Player self) {
        if (player1 == self) {
            return player2;
        }
        return player1;
    }
    public void cancelTimers(){
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        if (placementTimer != null) {
            placementTimer.cancel();
        }
    }

    public void killGame() {
        player1.setGame(null);
        player2.setGame(null);
    }

    public synchronized void setTurn(Player player) {
        turn = player;
        if (turnTimer != null) {
            turnTimer.cancel();
        }
        turnTimer = new Timer();
        turnTimer.schedule(new TurnTimerTask(), TURN_TIMEOUT);
        turn.writeNotification(NotificationMessage.YOUR_TURN);
        getOpponent(turn).writeNotification(NotificationMessage.OPPONENTS_TURN);
    }

    public void checkBoards() {
        if (player1.getBoard() != null && player2.getBoard() != null) {
            placementTimer.cancel();
            startGame();
        }
    }

    private void startGame() {
        gameStarted = true;
        if (new Random().nextInt(2) == 0) {
            setTurn(player1);
        } else {
            setTurn(player2);
        }
    }

    public synchronized void applyMove(MoveMessage move, Player player) {
        if (player != turn) {
            player.writeNotification(NotificationMessage.NOT_YOUR_TURN);
            return;
        }
        int x = move.getX();
        int y = move.getY();
        int max = Board.BOARD_DIMENSION;
        if (x < 0 || x >= max || y < 0 || y >= max) {
            player.writeNotification(NotificationMessage.INVALID_MOVE);
        } else {
            Player opponent = getOpponent(player);
            Square square = opponent.getBoard().getSquare(x, y);
            if (square.isGuessed()) {
                player.writeNotification(NotificationMessage.REPEATED_MOVE);
                return;
            }
            boolean hit = square.guess();
            Ship ship = square.getShip();
            MoveResponseMessage response;
            if (ship != null && ship.isSunk()) {
                response = new MoveResponseMessage(x, y, ship, true, false);
            } else {
                response = new MoveResponseMessage(x, y, null, hit, false);
            }
            player.writeObject(response);
            for (Player p : player1Watchers)
                p.writeObject(response);

            response.setOwnBoard(true);
            opponent.writeObject(response);
            for (Player p : player2Watchers)
                p.writeObject(response);

            if (opponent.getBoard().gameOver()) {
                turn.writeNotification(NotificationMessage.GAME_WIN);
                opponent.writeNotification(NotificationMessage.GAME_LOSE);
                turn = null;
            } else if (hit) {
                setTurn(player); // player gets another go if hit
            } else {
                setTurn(getOpponent(player));
            }
        }
    }

    private class PlacementTimerTask extends TimerTask {

        @Override
        public void run() {
            if (player1.getBoard() == null & player2.getBoard() == null) {
                NotificationMessage draw = new NotificationMessage(
                        NotificationMessage.TIMEOUT_DRAW);
                player1.writeObject(draw);
                player2.writeObject(draw);
                killGame();
            } else if (player1.getBoard() == null) {
                // Player1 failed to place ships in time
                player1.writeNotification(NotificationMessage.TIMEOUT_LOSE);
                player2.writeNotification(NotificationMessage.TIMEOUT_WIN);
                killGame();
            } else if (player2.getBoard() == null) {
                // Player2 failed to place ships in time
                player1.writeNotification(NotificationMessage.TIMEOUT_WIN);
                player2.writeNotification(NotificationMessage.TIMEOUT_LOSE);
                killGame();
            }
        }
    }

    private class TurnTimerTask extends TimerTask {

        @Override
        public void run() {
            if (turn != null) {
                turn.writeNotification(NotificationMessage.TIMEOUT_LOSE);
                getOpponent(turn).writeNotification(
                        NotificationMessage.TIMEOUT_WIN);
                killGame();
            }
        }

    }

    public synchronized void addPlayerWatcher(Player player, Player watcher) {
        if(isPublic) {
            if (player.hashCode() == player1.hashCode()) {
                if (!player1Watchers.contains(watcher)) {
                    player1Watchers.add(watcher);
                    watcher.writeNotification(NotificationMessage.FRIEND_OPPONENTS , player2.getPlayerName());
                }
            } else if (player.hashCode() == player2.hashCode()) {
                if (!player2Watchers.contains(watcher)) {
                    player2Watchers.add(watcher);
                    watcher.writeNotification(NotificationMessage.FRIEND_OPPONENTS , player1.getPlayerName());
                }
            }
        }
    }
    public synchronized void giveWatcherBoards(Player player, Player watcher) {
        if(isPublic) {
            if (player.hashCode() == player1.hashCode()) {
                if (!player1Watchers.contains(watcher)) {
                    BoardMessage message =  new BoardMessage(
                            player2.getBoard() == null ? new Board(true) : player2.getBoard(),
                            player1.getBoard() == null ? new Board(false) : player1.getBoard());
                    watcher.writeObject(message);
                }
            } else if (player.hashCode() == player2.hashCode()) {
                if (!player2Watchers.contains(watcher)) {
                    BoardMessage message =  new BoardMessage(
                            player1.getBoard() == null ? new Board(true) : player1.getBoard(),
                            player2.getBoard() == null ? new Board(false) : player2.getBoard());
                    watcher.writeObject(message);
                }
            }
        }
    }


        public synchronized void removePlayerWatcher(Player player, Player watcher) {
        if(isPublic) {
            if (player.hashCode() == player1.hashCode()) {
                player1Watchers.remove(watcher);
            } else if (player.hashCode() == player2.hashCode()) {
                player2Watchers.remove(watcher);
            }
        }
    }

    public synchronized boolean playerWatcherNameExists(Player player, String watcherName) {

        if(isPublic) {
            if (player.hashCode() == player1.hashCode()) {
                for (Player p : player1Watchers) {
                    if (watcherName.equals(p.getPlayerName())) {
                        return true;
                    }
                }
            } else if (player.hashCode() == player2.hashCode()) {
                for (Player p : player2Watchers) {
                    if (watcherName.equals(p.getPlayerName())) {
                        return true;
                    }
                }
            }
        }
        return false;

    }
}
