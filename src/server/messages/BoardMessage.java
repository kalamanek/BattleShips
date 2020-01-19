
package server.messages;

import model.Board;

import java.io.Serializable;


public class BoardMessage implements Serializable {

    private Board board;

    public BoardMessage(Board board ) {
        this.board = board;
    }

    public Board getBoard() {
        return board;
    }

}
