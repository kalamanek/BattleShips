package view;

import model.MatchRoom;
import model.RoomListPlayer;
import model.RoomPlayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MatchRoomView extends JFrame {

    private DefaultListModel<RoomPlayer> playersListModel = new DefaultListModel<RoomPlayer>();
    private MatchRoom matchRoom;
    private HashMap<String, RoomListPlayer> matchRoomList;
    private JList<RoomPlayer> playersList;
    private JButton sendInvite;
    private JLabel playersNumber;

    public MatchRoomView() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JPanel mainPanel = new JPanel(new BorderLayout(10, 5));
        setTitle("Battleships");
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        playersList = new JList<>();
        playersList.setModel(playersListModel);
        playersList.setCellRenderer(new PlayerListRenderer());
        playersList.addMouseListener(new PlayersListMouseAdapter());
        playersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sendInvite = new JButton("Send invite");
        sendInvite.setEnabled(false);
        sendInvite.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                RoomPlayer player = playersList.getSelectedValue();
                matchRoom.sendJoinFriend(player.getKey(), player.getName());
            }
        });


        playersList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                sendInvite.setEnabled(true);
            }
        });

        playersNumber = new JLabel("Players in room: " + playersListModel.getSize());
        playersNumber.setHorizontalAlignment(JLabel.CENTER);

        mainPanel.add(playersNumber, BorderLayout.NORTH);
        mainPanel.add(new JScrollPane(playersList), BorderLayout.CENTER);
        mainPanel.add(sendInvite, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);
        setVisible(true);
        pack();

        this.matchRoom = new MatchRoom(this);
        askForLoginOrRegister();
        matchRoom.joinLobby();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private class PlayersListMouseAdapter extends MouseAdapter {

        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2) {
                return;
            }

            RoomPlayer player = playersList.getSelectedValue();

            if (player != null) {
                matchRoom.sendJoinFriend(player.getKey(), player.getName());
            }
        }

    }

    private void askForLoginOrRegister() {
        String[] options = new String[]{"Login", "Register"};
        int response = JOptionPane.showOptionDialog(null, "Hello in BattleShips, to start choose option:", "BattleShips",
                JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (response == 0) {
            askForLoginAndPassword();
        } else if (response == 1) {
            registerUser();
        } else {
            System.exit(-1);
        }
    }

    private void registerUser() {
        String message = "Please choose a nickname.";
        while (true) {
            JTextField name = new JTextField();
            JTextField password = new JPasswordField();
            JTextField confirmPassword = new JPasswordField();
            JButton selectAvatarFileButton = new JButton("Select file");
            JLabel avatar = new JLabel();
            JFileChooser avatarFileChooser = new JFileChooser(System.getProperty("user.dir"));
            Object[] mes = {
                    message,
                    "Username:", name,
                    "Password:", password,
                    "Confirm Password:", confirmPassword,
                    "Avatar: (optional)", avatar,
                    selectAvatarFileButton
            };


            //Setting default avatar
            try {
                ImageIcon image = new ImageIcon(ImageIO.read(new File("resources/avatar/avatar.png")));
                avatar.setIcon(image);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, "Some files have been deleted",
                        "Fatal error", JOptionPane.ERROR_MESSAGE);
                System.exit(-1);
            }

            selectAvatarFileButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int returnValue = avatarFileChooser.showDialog(null, "Select");
                    if (returnValue == JFileChooser.APPROVE_OPTION) {
                        File selectedFile = avatarFileChooser.getSelectedFile();

                        try {
                            ImageIcon avatarImage = new ImageIcon(ImageIO.read(new File(selectedFile.getAbsolutePath())));
                            avatar.setIcon(avatarImage);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });
            //UIManager.put("OptionPane.minimumSize", new Dimension(500, 500));
            int option = JOptionPane.showConfirmDialog(null, mes, "Register", JOptionPane.OK_CANCEL_OPTION);

            if (name == null || password == null || option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                System.exit(-1);
            }

            byte[] avatarByteArray = {};

            if (avatar.getIcon() != null) {
                Icon icon = avatar.getIcon();
                BufferedImage image = new BufferedImage(icon.getIconWidth(),
                        icon.getIconHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics g = image.createGraphics();
                icon.paintIcon(null, g, 0, 0);
                g.dispose();

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try {
                    ImageIO.write(image, "png", bos);
                    avatarByteArray = bos.toByteArray();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (password.getText().equals(confirmPassword.getText())) {
                String imageString = Base64.getEncoder().encodeToString(avatarByteArray);

                this.matchRoom.sendRegistration(name.getText(), password.getText(), imageString);
                synchronized (matchRoom) {
                    try {
                        if (matchRoom.getNameState() == MatchRoom.NameState.WAITING) {
                            matchRoom.wait();
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                    MatchRoom.NameState state = matchRoom.getNameState();
                    if (state == MatchRoom.NameState.ACCEPTED) {
                        matchRoom.setOwnName(name.getText());
                        break;
                    } else if (state == MatchRoom.NameState.INVALID) {
                        message = "You must choose a valid login name.";
                    } else if (state == MatchRoom.NameState.TAKEN) {
                        message = "This nickname already exists, please try again.";
                    }
                }
            }
        }
    }


    private void askForLoginAndPassword() {
        String message = "Please choose a nickname.";
        while (true) {
            JTextField name = new JTextField();
            JTextField password = new JPasswordField();
            Object[] mes = {
                    message,
                    "Username:", name,
                    "Password:", password
            };

            int option = JOptionPane.showConfirmDialog(null, mes, "Login", JOptionPane.OK_CANCEL_OPTION);

            if (name == null || password == null || option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
                System.exit(-1);
            }
            this.matchRoom.sendLogin(name.getText(), password.getText());
            synchronized (matchRoom) {
                try {
                    if (matchRoom.getNameState() == MatchRoom.NameState.WAITING) {
                        matchRoom.wait();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            MatchRoom.NameState state = matchRoom.getNameState();
            if (state == MatchRoom.NameState.ACCEPTED) {
                matchRoom.setOwnName(name.getText());
                break;
            } else if (state == MatchRoom.NameState.INVALID) {
                message = "You must choose a valid login.";
            } else if (state == MatchRoom.NameState.TAKEN) {
                message = "This login already exists, please try again.";
            }
        }
    }

    public boolean playerNameExists(String name) {
        for (Map.Entry<String, RoomListPlayer> entry : matchRoomList.entrySet()) {
            if (entry.getValue().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void updateMatchRoomList(
            HashMap<String, RoomListPlayer> matchRoomList) {
        this.matchRoomList = matchRoomList;
        this.playersListModel.clear();
        for (Map.Entry<String, RoomListPlayer> entry : matchRoomList.entrySet()) {
            String key = entry.getKey();
            if (!key.equals(matchRoom.getKey())) {
                String name = entry.getValue().getName();
                String avatar = entry.getValue().getImage();
                RoomPlayer player = new RoomPlayer(key, name, avatar);
                this.playersListModel.addElement(player);
            }
        }
        if (playersList.isSelectionEmpty()) {
            sendInvite.setEnabled(false);
        }
        playersNumber.setText("Players in room: " + playersListModel.getSize());
    }

    public static void main(String[] args) {
        new MatchRoomView();
    }


    public void showConfigFileError() {
        String message = "Make sure you have a config.properties file\n" +
                "in the current working directory containing:\n\n" +
                "hostname=<hostname/ip>\n" +
                "port=<port>";
        JOptionPane.showMessageDialog(this,
                message, "Can't find a valid config.properties",
                JOptionPane.ERROR_MESSAGE);
        System.exit(-1);
    }

    public int showInitialConnectionError() {
        String message = "Could not connect to server, did you set the " +
                "correct hostname and port in config.properties?";
        String[] options = {"Quit", "Retry"};
        return JOptionPane.showOptionDialog(this, message,
                "Could not connect to server", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.ERROR_MESSAGE, null, options, options[1]);
    }

    public void showLostConnectionError() {
        JOptionPane.showMessageDialog(this,
                "Lost connection to server.", "Connection Error",
                JOptionPane.ERROR_MESSAGE);
        System.exit(-1);
    }
}
