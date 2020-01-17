package model;

import server.messages.MatchRoomListMessage;
import server.messages.NotificationMessage;
import view.ClientView;
import view.InviteReceivedPane;
import view.InviteSentPane;
import view.MatchRoomView;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Properties;

public class MatchRoom extends Thread {

    private MatchRoomView matchRoomView;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile Client clientModel;
    private String key = "";
    private String ownName;
    private volatile NameState nameState;
    private HashMap<String, InviteReceivedPane> inviteDialogs;
    private InviteSentPane inviteSentPane;

    public MatchRoom(MatchRoomView matchRoomView) {
        this.matchRoomView = matchRoomView;

        boolean connected = false;

        while (!connected) {
            try {
                InputStream inputStream = new FileInputStream("config.properties");
                Properties properties = new Properties();
                properties.load(inputStream);
                String hostname = properties.getProperty("hostname");
                String portStr = properties.getProperty("port");
                if (hostname == null || portStr == null) {
                    matchRoomView.showConfigFileError();
                }
                int port = Integer.parseInt(portStr);
                Socket socket = new Socket(hostname, port);
                out = new ObjectOutputStream(new BufferedOutputStream(
                        socket.getOutputStream()));
                in = new ObjectInputStream(socket.getInputStream());
                out.flush();
                connected = true;
            } catch (FileNotFoundException e) {
                matchRoomView.showConfigFileError();
            } catch (IOException e) {
                int response = matchRoomView.showInitialConnectionError();
                if (response == 0) {
                    System.exit(-1);
                }
            }
        }

        inviteDialogs = new HashMap<>();

        start();
    }

    @Override
    public void run() {
        super.run();
        Object input;
        try {
            while ((input = in.readObject()) != null) {
                System.out.println(input);
                if (clientModel != null) {
                    clientModel.parseInput(input);
                } else {
                    parseInput(input);
                }
            }
            System.out.println("stopped");
        } catch (IOException e) {
            matchRoomView.showLostConnectionError();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void sendJoinFriend(String key, final String name) {
        try {
            out.writeObject(new String[]{"join", "join", key});
            out.flush();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    inviteSentPane = new InviteSentPane(name, MatchRoom.this);
                    inviteSentPane.showPane(matchRoomView);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendLogin(String name, String password) {
        this.nameState = NameState.WAITING;
        sendStringArray(new String[]{"login", name, password});
    }

    public void sendRegistration(String name, String password){
        this.nameState = NameState.WAITING;
        sendStringArray(new String[]{"register", name, password});
    }


    public void joinLobby() {
        sendStringArray(new String[]{"join", "start"});
    }

    public enum NameState {
        WAITING, ACCEPTED, INVALID, TAKEN
    }

    private void setNameState(NameState nameState) {
        synchronized (this) {
            this.nameState = nameState;
            this.notifyAll();
        }
    }

    public NameState getNameState() {
        return nameState;
    }

    private void parseInput(Object input) {
        if (input instanceof MatchRoomListMessage) {
            final HashMap<String, String> matchRoomList = ((MatchRoomListMessage) input)
                    .getMatchRoomList();
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    matchRoomView.updateMatchRoomList(matchRoomList);
                }
            });
        } else if (input instanceof NotificationMessage) {
            NotificationMessage n = (NotificationMessage) input;
            switch (n.getCode()) {
                case NotificationMessage.GAME_TOKEN:
                    if (n.getText().length == 1) {
                        key = n.getText()[0];
                    }
                    break;
                case NotificationMessage.OPPONENTS_NAME:
                    disposeAllPanes();
                    startGame(input);
                    break;
                case NotificationMessage.NAME_ACCEPTED:
                    setNameState(NameState.ACCEPTED);
                    break;
                case NotificationMessage.NAME_TAKEN:
                    setNameState(NameState.TAKEN);
                    break;
                case NotificationMessage.INVALID_LOGIN_NAME:
                case NotificationMessage.PASSWORD_IS_INVALID:
                    setNameState(NameState.INVALID);
                    break;
                case NotificationMessage.NEW_JOIN_GAME_REQUEST:
                    final InviteReceivedPane dialog = new InviteReceivedPane(
                            n.getText()[0], n.getText()[1], this);
                    System.out.println("request from " + n.getText()[0] + " " + n.getText()[1]);
                    inviteDialogs.put(n.getText()[0], dialog);
                    EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            dialog.showOptionPane(matchRoomView);
                        }
                    });
                    break;
                case NotificationMessage.JOIN_GAME_REQUEST_REJECTED:
                    System.out.println("Join request rejected");
                    if (inviteSentPane != null) {
                        inviteSentPane.dispose();
                    }
                    break;
                case NotificationMessage.JOIN_GAME_REQUEST_ACCEPTED:
                    System.out.println("Join request accepted");
                    break;
                case NotificationMessage.JOIN_GAME_REQUEST_CANCELLED:
                    System.out.println("cancelled");
                    InviteReceivedPane pane = inviteDialogs.get(n.getText()[0]);
                    if (pane != null) {
                        pane.dispose();
                    } else {
                        System.out.println("can't find " + n.getText()[0]);
                    }
            }
        }
    }

    private void startGame(Object firstInput) {
        matchRoomView.setVisible(false);
        ClientView clientView = new ClientView(this.out, this.in, this);
        clientModel = clientView.getModel();
        clientModel.parseInput(firstInput);
    }

    public String getKey() {
        return key;
    }

    public void reopen() {
        if (clientModel != null) {
            this.clientModel.getView().dispose();
            this.clientModel = null;
        }
        matchRoomView.setVisible(true);
        joinLobby();
    }

    public void sendStringArray(String[] array) {
        try {
            out.writeObject(array);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disposeAllPanes() {
        for (InviteReceivedPane pane : inviteDialogs.values()) {
            pane.dispose();
        }
        if (inviteSentPane != null) {
            inviteSentPane.dispose();
        }
    }

    public void setOwnName(String ownName) {
        this.ownName = ownName;
    }

    public String getOwnName() {
        return ownName;
    }
}
