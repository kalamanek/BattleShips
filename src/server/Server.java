package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class Server {
    private static HashMap<String, String> users;

    /**
     * Constructs a server that listens on a port.
     *
     * @param port the port to listen on
     */
    public Server(int port) {
        try {
            ServerSocket serverSocket = new ServerSocket(port);

            MatchRoom matchRoom = new MatchRoom();

            while (true) {
                new Player(serverSocket.accept(), matchRoom).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        users = new HashMap<String, String>();
        users.put("a","a");
        users.put("b","b");
        users.put("c","c");

        int port = 8900;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }
        new Server(port);
    }

    public static boolean checkUser(String login, String password)  {
        if(users.containsKey(login))
            return users.get(login).equals(password);
        else
            return false;
    }

}
