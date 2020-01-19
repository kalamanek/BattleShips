package model;

import server.Game;
import server.messages.*;
import view.ClientView;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Client extends Thread {

    private Board ownBoard;
    private Board opponentBoard;
    private ClientView view;

    private ObjectOutputStream out;
    private ObjectInputStream in;

    private String opponentName = "Player";

    public Client(ClientView clientView, Board ownBoard, Board opponentBoard,
            ObjectOutputStream out, ObjectInputStream in) {
        this.ownBoard = ownBoard;
        this.opponentBoard = opponentBoard;
        this.view = clientView;

        ownBoard.setClient(this);
        opponentBoard.setClient(this);

        this.out = out;
        this.in = in;
    }

    @Override
    public void run() {
        super.run();
        Object input;
        try {
            while ((input = in.readObject()) != null) {
                parseInput(input);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    public void parseInput(Object input) {
        if (input instanceof NotificationMessage) {
            NotificationMessage n = (NotificationMessage) input;
            switch (n.getCode()) {
            case NotificationMessage.OPPONENTS_NAME:
                // TODO: handle receiving opponents name
                //view.addChatMessage("Received opponent's name.");
                if (n.getText().length == 1) {
                    opponentName = n.getText()[0];
                    view.setTitle("Playing Battleships against " +
                            opponentName);
                }
                break;
            case NotificationMessage.FRIEND_OPPONENTS:
                if (n.getText().length == 1) {
                    opponentName = n.getText()[0];
                    view.setTitle("Watching friends game in Battleships against " +
                            opponentName);
                    getBoards();
                }
                break;
            case NotificationMessage.BOARD_ACCEPTED:
                view.setMessage("Board accepted. Waiting for opponent.");
                view.stopTimer();
                ownBoard.setBoatPositionLocked(true);
                break;
            case NotificationMessage.GAME_TOKEN:
                // TODO: handle receiving game token to share with friend
                view.addChatMessage("Received game token.");
                break;
            case NotificationMessage.GAME_NOT_FOUND:
                // TODO: handle joining a game that doesn't exist
                view.addChatMessage("Game not found.");
                break;
            case NotificationMessage.PLACE_SHIPS:
                // TODO: allow player to start positioning ships
                //view.addChatMessage("Can place ships now.");
                ownBoard.setBoatPositionLocked(false);
                break;
            case NotificationMessage.YOUR_TURN:
                view.stopTimer();
                view.setTimer(Game.TURN_TIMEOUT / 1000);
                view.setMessage("Your turn.");
                break;
            case NotificationMessage.OPPONENTS_TURN:
                view.stopTimer();
                view.setTimer(Game.TURN_TIMEOUT / 1000);
                view.addChatMessage("Opponent's turn.");
                view.setMessage("Opponent's turn.");
                break;
            case NotificationMessage.GAME_WIN:
                // TODO: inform player they have won the game
                view.setMessage("You won.");
                view.stopTimer();
                view.gameOverAction("You won!");
                break;
            case NotificationMessage.GAME_LOSE:
                // TODO: inform player they have lost the game
                view.setMessage("You lost.");
                view.stopTimer();
                view.gameOverAction("You lost!");
                break;
            case NotificationMessage.TIMEOUT_WIN:
                // TODO: inform of win due to opponent taking too long
                view.addChatMessage("Your opponent took to long, you win!");
                view.gameOverAction("Your opponent took to long, you win!");
                break;
            case NotificationMessage.TIMEOUT_LOSE:
                // TODO: inform of loss due to taking too long
                view.addChatMessage("You took too long, you lose!");
                view.gameOverAction("You took too long, you lose!");
                break;
            case NotificationMessage.TIMEOUT_DRAW:
                // TODO: inform that both took too long to place ships
                view.addChatMessage("Game ended a draw.");
                view.gameOverAction("Game ended a draw.");
                break;
            case NotificationMessage.NOT_YOUR_TURN:
                view.addChatMessage("It's not your turn!");
                break;
            case NotificationMessage.INVALID_BOARD:
                view.addChatMessage("Invalid board.");
                break;
            case NotificationMessage.NOT_IN_GAME:
                view.addChatMessage("You're not in a game.");
                break;
            case NotificationMessage.INVALID_MOVE:
                view.addChatMessage("Invalid move.");
                break;
            case NotificationMessage.REPEATED_MOVE:
                view.addChatMessage("You cannot repeat a move.");
                break;
            case NotificationMessage.OPPONENT_DISCONNECTED:
                view.addChatMessage("Opponent disconnected.");
                view.gameOverAction("You won!");
            }
        } else if (input instanceof MoveResponseMessage) {
            MoveResponseMessage move = (MoveResponseMessage) input;
            if (move.isOwnBoard()) {
                ownBoard.applyMove(move);
            } else {
                opponentBoard.applyMove(move);
            }
        } else if (input instanceof ChatMessage) {
            ChatMessage chatMessage = (ChatMessage) input;
            view.addChatMessage("<b>" + opponentName + ":</b> " + chatMessage.getMessage());
        }else if (input instanceof BoardMessage){
            BoardMessage boards = (BoardMessage) input;
            boards.getEnemyBoard().printBoard(true);
            boards.getFriendBoard().printBoard(true);
        }


    }

    public void sendBoard(Board board) throws IOException {
        out.reset();
        out.writeObject(board);
        out.flush();
    }
    public void getBoards(){

    }

    public ClientView getView() {
        return view;
    }

    public void sendChatMessage(String message) throws IOException {
        System.out.println(message);
        out.writeObject(new ChatMessage(message));
        out.flush();
    }

    public void sendMove(int x, int y) throws IOException {
        out.writeObject(new MoveMessage(x, y));
        out.flush();
    }

    public String getOpponentName() {
        return opponentName;
    }

}
