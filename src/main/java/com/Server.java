package com;

import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;


// Server class extending JFrame to create a GUI-based chat server
public class Server extends JFrame {
    // Network-related variables
    ServerSocket server;           // ServerSocket to listen for client connections
    Socket socket;                 // Socket for the connected client
    BufferedReader br;             // To read messages from the client
    PrintWriter out;               // To send messages to the client

    // GUI components
    private JLabel heading = new JLabel("Server Area");
    private JTextArea messageArea = new JTextArea();
    private JTextArea messageInput = new JTextArea();
    final private Font font1 = new Font("Segue UI", Font.ITALIC, 22); // Font for heading
    final private Font font2 = new Font("Roboto UI", Font.PLAIN, 18); // Font for emojis
    private JButton emojiButton;
    private JButton sendFileButton;
    private JButton clearChatButton;
    private JDialog emojiDialog;


    // SQLite database connection string for Server-specific database
    private static final String DB_URL = "jdbc:sqlite:src/main/resources/server_chat.db"; // Unique database file for Server


    // Constructor: Initializes the server and sets up the connection
    public Server() {
        try {
            server = new ServerSocket(2103); // Create server socket on port 2103
            System.out.println("Server is ready to accept connection");
            System.out.println("Waiting...");
            socket = server.accept();        // Wait for and accept a client connection
            System.out.println("Connection Done");

            // Set up input and output streams for communication
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());

            // Initialize database, GUI, and start operations
            initializeDatabase();
            createGUI();
            handleEvents();
            startReading();
            loadChatHistory();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to initialize SQLite database and create the messages table for Server
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to Server-specific database
             Statement stmt = conn.createStatement()) {            // Create a statement for SQL execution
            // SQL to create a table if it doesn’t exist
            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +    // Unique ID for each message
                    "sender TEXT NOT NULL, " +                    // Sender of the message (e.g., "Me" or "Client")
                    "message TEXT NOT NULL, " +                   // The message content
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)"; // Auto-set timestamp (still stored but not displayed)
            stmt.execute(sql);                                    // Execute the SQL command
            System.out.println("Server database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    // Method to handle user input events (e.g., pressing Enter to send a message)
    private void handleEvents() {
        messageInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {} // not used

            @Override
            public void keyPressed(KeyEvent e) {} // not used

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String contentToSend = messageInput.getText().trim(); // Get and trim text from JTextArea
                    if (!contentToSend.isEmpty()) { // Only send non-empty messages
                        messageArea.append("Me : " + contentToSend + "\n"); // Display locally
                        if (contentToSend.equals("exit")) {
                            messageInput.setEnabled(false); // Disable input if "exit" is sent
                            emojiButton.setEnabled(false);
                            sendFileButton.setEnabled(false);
                            clearChatButton.setEnabled(false);
                        }
                        out.println(contentToSend); // Send message to client/server
                        out.flush();                // Ensure message is sent immediately
                        messageInput.setText("");   // Clear input field
                        messageInput.requestFocus(); // Refocus on input field

                        // Save the sent message to the database (assuming SQLite is still used)
                        saveMessage("Me", contentToSend);

                        if (emojiDialog != null && emojiDialog.isVisible()) {
                            emojiDialog.dispose();
                        }
                    }
                }
            }
        });
    }

    // Method to create and configure the GUI
    private void createGUI() {
        this.setTitle("Server");
        this.setSize(500, 600);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close app on window close

        // Get screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;
        int windowWidth = 600;
        int windowHeight = 700;

        // Adjust window width if screen is too narrow
        if (screenWidth < 1200) {
            windowWidth = screenWidth / 2; // Each window takes half the screen width
            windowHeight = (int) (screenHeight * 0.8); // Use 80% of screen height for better fit
        }

        this.setSize(windowWidth, windowHeight);

        // Position Client with a 50-pixel margin from the right edge, centered vertically
        int margin = 50; // Margin from the right edge
        int xPosition = screenWidth - windowWidth - margin; // Right edge with 50px margin
        int yPosition = (screenHeight - windowHeight) / 2; // Center vertically on screen
        this.setLocation(xPosition, yPosition);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Configure component styles
        heading.setFont(font1);
        messageArea.setFont(font2);
        messageInput.setFont(font2);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        messageArea.setEditable(false);       // Prevent editing of message area

        // Improve text wrapping for messageArea
        messageArea.setLineWrap(true);        // Enable line wrapping
        messageArea.setWrapStyleWord(true);   // Wrap at word boundaries for better readability
        messageArea.setColumns(40);           // Set initial column width (adjust for better fit, e.g., 40 chars)
        messageArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1)); // Add a light border for readability

        // Set layout and add components to frame
        this.setLayout(new BorderLayout());
        this.add(heading, BorderLayout.NORTH);
        JScrollPane jScrollPane = new JScrollPane(messageArea);
        this.add(jScrollPane, BorderLayout.CENTER);

        // Create a panel for input and clear button
        JPanel bottomPanel = new JPanel(new BorderLayout());
        this.add(bottomPanel, BorderLayout.SOUTH);

        // Use JTextArea for messageInput with dynamic sizing
        messageInput.setLineWrap(true);       // Enable line wrapping for input
        messageInput.setWrapStyleWord(true);  // Wrap at word boundaries
        messageInput.setRows(1);              // Initial height of 1 row
        messageInput.setColumns(30);          // Initial width in columns (adjust as needed)
        JScrollPane inputScrollPane = new JScrollPane(messageInput); // Add scroll pane for dynamic height
        inputScrollPane.setPreferredSize(new Dimension(400, 50)); // Initial preferred size (adjust as needed)

        // Add message input (with scroll pane) to the center of bottom panel
        bottomPanel.add(inputScrollPane, BorderLayout.CENTER);

        // Add Clear Chat button to the west (left) of bottom panel
        clearChatButton = new JButton("Clear Chat");
        clearChatButton.setFont(font2);
        clearChatButton.addActionListener(e -> clearChatHistory()); // Call clearChatHistory on button click
        bottomPanel.add(clearChatButton, BorderLayout.WEST);        // Add button to the left of input

        // Add Emoji button to the east (right) of bottom panel
        emojiButton = new JButton("Emoji");
        emojiButton.setFont(font2);
        emojiButton.addActionListener(e -> showEmojiPicker()); // Call showEmojiPicker on button click
        bottomPanel.add(emojiButton, BorderLayout.EAST);       // Add button to the right of input

        // Add Send File button below input (or beside, if space allows)
        sendFileButton = new JButton("Send File");
        sendFileButton.setFont(font2);
        sendFileButton.addActionListener(e -> sendFile());    // Call sendFile on button click
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // Use FlowLayout for multiple buttons
        buttonPanel.add(sendFileButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);       // Place below input and other buttons

        this.add(bottomPanel, BorderLayout.SOUTH);
        this.add(bottomPanel, BorderLayout.SOUTH);
        this.setVisible(true);                            // Show the window

        // Auto-scroll to bottom when new messages are added
        messageArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { scrollToBottom(); }
            @Override
            public void removeUpdate(DocumentEvent e) { scrollToBottom(); }
            @Override
            public void changedUpdate(DocumentEvent e) { scrollToBottom(); }

            // Helper method to scroll to the bottom of the message area
            public void scrollToBottom() {
                SwingUtilities.invokeLater(() -> {
                    jScrollPane.getVerticalScrollBar().setValue(jScrollPane.getVerticalScrollBar().getMaximum());
                });
            }
        });

        // Dynamic sizing for messageInput
        messageInput.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { adjustInputSize(); }
            @Override
            public void removeUpdate(DocumentEvent e) { adjustInputSize(); }
            @Override
            public void changedUpdate(DocumentEvent e) { adjustInputSize(); }

            private void adjustInputSize() {
                String text = messageInput.getText();
                int lines = (int) Math.ceil(text.length() / 40.0); // Estimating lines based on 40 chars per line
                if (lines > 1) {
                    messageInput.setRows(Math.min(lines, 3)); // Limit to 3 rows max for a more compact appearance
                } else {
                    messageInput.setRows(1); // Reset to 1 row if short
                }
                inputScrollPane.revalidate(); // Update the scroll pane size
                inputScrollPane.repaint();    // Redraw the component
            }
        });
    }

    // Method to load chat history from Server-specific database (without timestamps)
    private void loadChatHistory() {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to Server-specific database
             Statement stmt = conn.createStatement();              // Create statement
             ResultSet rs = stmt.executeQuery("SELECT sender, message FROM messages WHERE sender IN ('Me', 'Client') ORDER BY id")) { // Query only Server-relevant messages
            while (rs.next()) { // Iterate through result set
                // Append each message with sender to message area
                messageArea.append(rs.getString("sender") + " : " + rs.getString("message") + "\n");
            }
        } catch (SQLException e) {
            System.err.println("Error loading chat history: " + e.getMessage());
        }
    }

    // MEthod to clear chat history from Server-specific database and GUI
    private void clearChatHistory() {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to the respective database
             Statement stmt = conn.createStatement()) {            // Create a statement for SQL execution
            String sql = "DELETE FROM messages";                   // SQL to delete all rows from messages table
            stmt.execute(sql);                                     // Execute the deletion
            System.out.println("Chat history cleared successfully");
            messageArea.setText("");                               // Clear the GUI message area
        } catch (SQLException e) {
            System.err.println("Error clearing chat history: " + e.getMessage());
        }
    }

    // Method to save a message to Server-specific database
    private void saveMessage(String sender, String message) {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to Server-specific database
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO messages (sender, message) VALUES (?, ?)")) { // Prepare SQL insert
            pstmt.setString(1, sender);    // Set sender
            pstmt.setString(2, message);   // Set message content
            pstmt.executeUpdate();         // Execute the insert
        } catch (SQLException e) {
            System.err.println("Error saving message: " + e.getMessage());
        }
    }

    // Method to show an emoji picker dialog
    private void showEmojiPicker() {
        if (emojiDialog != null && emojiDialog.isVisible()) {
            emojiDialog.dispose(); // Close existing dialog if open
            return;
        }
        emojiDialog = new JDialog(this, "Emoji Picker", false); // Set to non-modal (false) to allow interaction with main window
        emojiDialog.setSize(300, 400);
        emojiDialog.setLocationRelativeTo(this); // Center relative to the parent window

        // Create a panel to hold emojis
        JPanel emojiPanel = new JPanel(new GridLayout(0, 4, 5, 5)); // Grid layout for emojis (4 columns for better fit)
        emojiDialog.add(new JScrollPane(emojiPanel)); // Add scroll pane for many emojis

        // Define reaction emoji categories (using emoji-java Emoji objects based on names/aliases)
        List<Emoji> reactionEmojis = new ArrayList<>();
        reactionEmojis.add(EmojiManager.getForAlias("smile")); // 😊
        reactionEmojis.add(EmojiManager.getForAlias("slight_smile")); // 🙂
        reactionEmojis.add(EmojiManager.getForAlias("blush")); // 😊
        reactionEmojis.add(EmojiManager.getForAlias("smirk")); // 😏
        reactionEmojis.add(EmojiManager.getForAlias("stuck_out_tongue")); // 😛
        reactionEmojis.add(EmojiManager.getForAlias("stuck_out_tongue_winking_eye")); // 😜
        reactionEmojis.add(EmojiManager.getForAlias("stuck_out_tongue_closed_eyes")); // 😝
        reactionEmojis.add(EmojiManager.getForAlias("cry")); // 😢
        reactionEmojis.add(EmojiManager.getForAlias("sob")); // 😭
        reactionEmojis.add(EmojiManager.getForAlias("disappointed_relieved")); // 😥
        reactionEmojis.add(EmojiManager.getForAlias("laughing")); // 😂
        reactionEmojis.add(EmojiManager.getForAlias("joy")); // 😄
        reactionEmojis.add(EmojiManager.getForAlias("rolling_on_the_floor_laughing")); // 🤣
        reactionEmojis.add(EmojiManager.getForAlias("heart")); // ❤️
        reactionEmojis.add(EmojiManager.getForAlias("heart_eyes")); // 😍
        reactionEmojis.add(EmojiManager.getForAlias("sparkling_heart")); // 💖
        reactionEmojis.add(EmojiManager.getForAlias("two_hearts")); // 💕
        reactionEmojis.add(EmojiManager.getForAlias("revolving_hearts")); // 💞
        reactionEmojis.add(EmojiManager.getForAlias("kissing_heart")); // 😘
        reactionEmojis.add(EmojiManager.getForAlias("kissing")); // 😗
        reactionEmojis.add(EmojiManager.getForAlias("kissing_smiling_eyes")); // 😙
        reactionEmojis.add(EmojiManager.getForAlias("hug")); // 🤗
        reactionEmojis.add(EmojiManager.getForAlias("unamused")); // 😒
        reactionEmojis.add(EmojiManager.getForAlias("raising_hand")); // 🙋
        reactionEmojis.add(EmojiManager.getForAlias("sleeping")); // 😴
        reactionEmojis.add(EmojiManager.getForAlias("sleepy")); // 😪
        reactionEmojis.add(EmojiManager.getForAlias("snowflake")); // ❄️
        reactionEmojis.add(EmojiManager.getForAlias("fire")); // 🔥
        reactionEmojis.add(EmojiManager.getForAlias("angry")); // 😠
        reactionEmojis.add(EmojiManager.getForAlias("rage")); // 😡
        reactionEmojis.add(EmojiManager.getForAlias("clap")); // 👏
        reactionEmojis.add(EmojiManager.getForAlias("dove")); // 🕊️

        // Add emojis to the panel with multiple selection (allowing duplicates, no toggle)
        for (Emoji emoji : reactionEmojis) {
            if (emoji != null) { // Ensure emoji exists
                JButton emojiButton = new JButton(emoji.getUnicode()); // Use Unicode representation as button label
                emojiButton.setFont(font2); // Use the color emoji font (e.g., Noto Color Emoji)
                String emojiUnicode = emoji.getUnicode();
                emojiButton.addActionListener(e -> {
                    String currentText = messageInput.getText();
                    // Allow adding the same emoji multiple times (no removal unless explicitly handled)
                    messageInput.setText(currentText + emojiUnicode); // Append emoji to input, allowing duplicates
                    messageInput.requestFocus(); // Refocus on input field
                });
                emojiPanel.add(emojiButton);
            } else {
                System.out.println("Null emoji found for alias: ");
            }
        }

        // Ensure the dialog stays open until the user closes it (via "X")
        emojiDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Close only when "X" is clicked
        emojiDialog.setVisible(true); // Show the dialog, and it stays open until closed manually
    }

    // Method to send a file to the client
    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setAcceptAllFileFilterUsed(false);

        // Define supported file types (images, PDFs, documents, archives, text, excluding videos)
        fileChooser.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                if (f.isDirectory()) return true;
                String name = f.getName().toLowerCase();
                return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || // Images
                        name.endsWith(".pdf") || // PDFs
                        name.endsWith(".doc") || name.endsWith(".docx") || // Word documents
                        name.endsWith(".txt") || // Text files
                        name.endsWith(".zip") || name.endsWith(".rar"); // Archives
            }

            @Override
            public String getDescription() {
                return "Supported Files (*.png, *.jpg, *.jpeg, *.pdf, *.doc, *.docx, *.txt, *.zip, *.rar)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                byte[] fileBytes = Files.readAllBytes(selectedFile.toPath());
                String base64File = Base64.getEncoder().encodeToString(fileBytes);
                String fileType = getFileType(selectedFile.getName());
                String message = "FILE:" + fileType + ":" + base64File;
                out.println(message); // Send to server
                out.flush();         // Ensure message is sent immediately
                messageArea.append("Me: Sent " + fileType + " file: " + selectedFile.getName() + "\n"); // Indicate in GUI with icon
                saveMessage("Me", "Sent " + fileType + " file: " + selectedFile.getName()); // Save to database (text description)
            } catch (IOException e) {
                System.err.println("Error sending file: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Failed to send file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // Method to determine the file type based on the file extension
    private String getFileType(String filename) {
        String extension = filename.toLowerCase().substring(filename.lastIndexOf(".") + 1);
        switch (extension) {
            case "png":
            case "jpg":
            case "jpeg":
                return "image";
            case "pdf":
                return "pdf";
            case "doc":
            case "docx":
                return "document";
            case "txt":
                return "text";
            case "zip":
            case "rar":
                return "archive";
            default:
                return "file";
        }
    }

    // Method to save a file to the local disk
    private void saveFileLocally(String fileType, byte[] fileBytes) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setSelectedFile(new File("received_" + fileType + "_" + System.currentTimeMillis() + "." + getExtension(fileType)));
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File outputFile = fileChooser.getSelectedFile();
            try {
                Files.write(outputFile.toPath(), fileBytes);
                System.out.println("File saved successfully: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Error saving file: " + e.getMessage());
            }
        }
    }

    // Method to get the file extension based on the file type
    private String getExtension(String fileType) {
        switch (fileType.toLowerCase()) {
            case "image": return "png";
            case "pdf": return "pdf";
            case "document": return "docx";
            case "text": return "txt";
            case "archive": return "zip";
            default: return "bin";
        }
    }

    // Method to start reading messages from the client in a separate thread
    public void startReading() {
        Runnable r1 = () -> {
            System.out.println("Reader started...");
            try {
                while (true) {
                    String msg = br.readLine();
                    if (msg.equals("exit")) {
                        messageArea.append("Client: " + msg + "\n");
                        saveMessage("Client", msg); // Save exit message in Server-specific database
                        System.out.println("Client terminated the chat");
                        JOptionPane.showMessageDialog(this, "Client Terminated the chat");
                        messageInput.setEnabled(false); // Disable input
                        socket.close();                 // Close connection
                        out.flush();                    // Flush output
                        break;
                    }
                    if (msg.startsWith("FILE:")) { // Handle file message
                        String[] parts = msg.split(":", 3); // Split into "FILE", type, and base64 data
                        if (parts.length == 3) {
                            String fileType = parts[1];
                            String base64File = parts[2];
                            try {
                                byte[] fileBytes = Base64.getDecoder().decode(base64File);
                                saveFileLocally(fileType, fileBytes); // Save file to local disk
                                messageArea.append("Client: Sent " + fileType + " file\n"); // Indicate in GUI with icon
                                saveMessage("Client", "Sent " + fileType + " file"); // Save to database (text description)
                                JOptionPane.showMessageDialog(this, "Received " + fileType + " file", "File Received", JOptionPane.INFORMATION_MESSAGE);
                            } catch (Exception e) {
                                System.err.println("Error decoding file: " + e.getMessage());
                                messageArea.append("Client: Error receiving file\n"); // Add computer icon for Server
                            }
                        }
                    } else {
                        messageArea.append("Client: " + msg + "\n");
                        saveMessage("Client", msg);
                    }
                }
            } catch (Exception e) {
                System.out.println("Connection closed");
            }
        };
        new Thread(r1).start();
    }

    // Main method to start the server
    public static void main(String[] args) {
        System.out.println("This is server..going to start server");
        new Server();
    }
}