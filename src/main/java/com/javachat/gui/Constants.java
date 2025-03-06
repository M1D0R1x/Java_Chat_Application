package com.javachat.gui;

import javax.swing.*;
import java.awt.*;

public class Constants {
    public static JTextArea messageArea = new JTextArea();
    public static JTextArea messageInput = new JTextArea();
    public static JLabel serverHeading = new JLabel("Server Area");
    public static JLabel clientHeading = new JLabel("Client Area");

    public static JButton emojiButton;
    public static JButton sendFileButton;
    public static JButton clearChatButton;
    public static JDialog emojiDialog;

//    Font
    final public static Font font1 = new Font("Segue UI", Font.ITALIC, 22); // Font for heading
    final public static Font font2 = new Font("Roboto UI", Font.PLAIN, 18); // Font for emojis

//    Database locations
    public static final String SDB_URL = "jdbc:sqlite:src/main/resources/server_chat.db";
    public static final String CDB_URL = "jdbc:sqlite:src/main/resources/client_chat.db";

}
