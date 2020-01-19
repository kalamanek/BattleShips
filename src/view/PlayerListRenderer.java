package view;

import model.RoomPlayer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class PlayerListRenderer extends JLabel implements ListCellRenderer<RoomPlayer> {

    PlayerListRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends RoomPlayer> list, RoomPlayer value, int index, boolean isSelected, boolean cellHasFocus) {


        try {
            ImageIcon avatar = new ImageIcon(ImageIO.read(new File("resources/avatar/avatar.png")));
            setIcon(avatar);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Some files have been deleted",
                    "Fatal error", JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }

        setText(value.getName());

        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}
