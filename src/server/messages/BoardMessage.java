
package server.messages;

import model.Board;

import java.io.Serializable;


public class BoardMessage implements Serializable {

    private Board friendBoard;
    private Board enemyBoard;

    public BoardMessage(Board friendBoard , Board enemyBoard ) {
        this.friendBoard = friendBoard;
        this.enemyBoard = enemyBoard;
    }

    public Board getFriendBoard() {
        return friendBoard;
    }
    public Board getEnemyBoard() {
        return enemyBoard;
    }

}
