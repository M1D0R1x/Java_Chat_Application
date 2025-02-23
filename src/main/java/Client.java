import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


// Client class extending JFrame to create a GUI-based chat client
public class Client extends JFrame {
    // Network-related variables
    Socket socket;  // Socket to connect to the server
    BufferedReader br;
    PrintWriter out;

    // GUI components
    private JLabel heading = new JLabel("Client Area");
    private JTextArea messageArea = new JTextArea();
    private JTextArea messageInput = new JTextArea();
    private Font font1 = new Font("Segue UI", Font.ITALIC, 22); // Font for heading
//    private Font font2 = new Font("Segoe UI Emoji", Font.PLAIN, 18);
    private Font font2 = new Font("Noto Color Emoji", Font.PLAIN, 18); // Font for messages and input
    // Font for messages and input
    // SQLite database connection string for Client-specific database
    private static final String DB_URL = "jdbc:sqlite:client_chat.db"; // Unique database file for Client

    // Constructor: Initializes the client and connects to the server
    public Client() {
        try {
            System.out.println("Sending Request to server");
            socket = new Socket(InetAddress.getLocalHost(), 2103);
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

    // Method to initialize SQLite database and create the messages table for Client
    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to Client-specific database
             Statement stmt = conn.createStatement()) {            // Create a statement for SQL execution
            // SQL to create a table if it doesn’t exist
            String sql = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +    // Unique ID for each message
                    "sender TEXT NOT NULL, " +                    // Sender of the message (e.g., "Me" or "Server")
                    "message TEXT NOT NULL, " +                   // The message content
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)"; // Auto-set timestamp (still stored but not displayed)
            stmt.execute(sql);                                    // Execute the SQL command
            System.out.println("Client database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    // Method to handle user input events (e.g., pressing Enter to send a message)
    private void handleEvents() {
        messageInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
//                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
//                    String currentText = messageInput.getText();
//                    if (!currentText.isEmpty()) {
//                        // Remove the last character or emoji (works for both text and Unicode emojis)
//                        String newText = currentText.substring(0, currentText.length() - 1);
//                        messageInput.setText(newText);
//                        messageInput.requestFocus(); // Refocus on input field
//                    }
//                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String contentToSend = messageInput.getText().trim(); // Get and trim text from JTextArea
                    if (!contentToSend.isEmpty()) { // Only send non-empty messages
                        messageArea.append("Me : " + contentToSend + "\n"); // Display locally
                        if (contentToSend.equals("exit")) {
                            messageInput.setEnabled(false); // Disable input if "exit" is sent
                        }
                        out.println(contentToSend); // Send message to client/server
                        out.flush();                // Ensure message is sent immediately
                        messageInput.setText("");   // Clear input field
                        messageInput.requestFocus(); // Refocus on input field

                        // Save the sent message to the database (assuming SQLite is still used)
                        saveMessage("Me", contentToSend);
                    }
                }
            }
        });
    }

    // Method to create and configure the GUI
    private void createGUI() {
        this.setTitle("Client");
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

        // Position Server with a 50-pixel margin from the left edge, centered vertically
        int margin = 50; // Margin from the left edge
        int xPosition = margin; // Left edge with 50px margin
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
        JButton clearChatButton = new JButton("Clear Chat");
        clearChatButton.setFont(font2);
        clearChatButton.addActionListener(e -> clearChatHistory()); // Call clearChatHistory on button click
        bottomPanel.add(clearChatButton, BorderLayout.WEST);        // Add button to the left of input

        // Add Emoji button to the east (right) of bottom panel
        JButton emojiButton = new JButton("Emoji");
        emojiButton.setFont(font2);
        emojiButton.addActionListener(e -> showEmojiPicker()); // Call showEmojiPicker on button click
        bottomPanel.add(emojiButton, BorderLayout.EAST);       // Add button to the right of input

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

    // Method to load chat history from Client-specific database (without timestamps)
    private void loadChatHistory() {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to Client-specific database
             Statement stmt = conn.createStatement();              // Create statement
             ResultSet rs = stmt.executeQuery("SELECT sender, message FROM messages WHERE sender IN ('Me', 'Server') ORDER BY id")) { // Query only Client-relevant messages
            while (rs.next()) { // Iterate through result set
                // Append each message with sender to message area
                messageArea.append(rs.getString("sender") + " : " + rs.getString("message") + "\n");
            }
        } catch (SQLException e) {
            System.err.println("Error loading chat history: " + e.getMessage());
        }
    }

    // Method to clear chat history from Client-specific database and GUI
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

    // Method to save a message to Client-specific database
    private void saveMessage(String sender, String message) {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to Client-specific database
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
        JDialog emojiDialog = new JDialog(this, "Emoji Picker", false); // Set to non-modal (false) to allow interaction with main window
        emojiDialog.setSize(300, 400);
        emojiDialog.setLocationRelativeTo(this); // Center relative to the parent window

        // Create a panel to hold emojis
        JPanel emojiPanel = new JPanel(new GridLayout(0, 4, 5, 5)); // Grid layout for emojis (4 columns for better fit)
        emojiDialog.add(new JScrollPane(emojiPanel)); // Add scroll pane for many emojis

        // Use a StringBuilder to accumulate all selected emojis (including duplicates)
        StringBuilder selectedEmojis = new StringBuilder(messageInput.getText()); // Start with current input text

        // Define reaction emoji categories (using emoji-java Emoji objects based on names/aliases)
        List<Emoji> reactionEmojis = new ArrayList<>();

        // Smiles (happy or positive faces, including blush, smirk, tongue out variants)
        reactionEmojis.add(EmojiManager.getForAlias("smile")); // 😊
        reactionEmojis.add(EmojiManager.getForAlias("slight_smile")); // 🙂
        reactionEmojis.add(EmojiManager.getForAlias("blush")); // 😊
        reactionEmojis.add(EmojiManager.getForAlias("smirk")); // 😏
        reactionEmojis.add(EmojiManager.getForAlias("stuck_out_tongue")); // 😛
        reactionEmojis.add(EmojiManager.getForAlias("stuck_out_tongue_winking_eye")); // 😜
        reactionEmojis.add(EmojiManager.getForAlias("stuck_out_tongue_closed_eyes")); // 😝
        reactionEmojis.add(EmojiManager.getForAlias("money_mouth_face")); // 🤑

        // Cries (sad or tearful faces, including watery eyes)
        reactionEmojis.add(EmojiManager.getForAlias("cry")); // 😢
        reactionEmojis.add(EmojiManager.getForAlias("sob")); // 😭
        reactionEmojis.add(EmojiManager.getForAlias("disappointed_relieved")); // 😥 (watery eyes/sad but relieved)

        // Laughs (laughing or joyful faces)
        reactionEmojis.add(EmojiManager.getForAlias("laughing")); // 😂
        reactionEmojis.add(EmojiManager.getForAlias("joy")); // 😄
        reactionEmojis.add(EmojiManager.getForAlias("rolling_on_the_floor_laughing")); // 🤣

        // Loves (hearts or affectionate symbols, including 100)
        reactionEmojis.add(EmojiManager.getForAlias("heart")); // ❤️
        reactionEmojis.add(EmojiManager.getForAlias("heart_eyes")); // 😍
        reactionEmojis.add(EmojiManager.getForAlias("sparkling_heart")); // 💖
        reactionEmojis.add(EmojiManager.getForAlias("two_hearts")); // 💕
        reactionEmojis.add(EmojiManager.getForAlias("revolving_hearts")); // 💞
        reactionEmojis.add(EmojiManager.getForAlias("growing_heart")); // 💗
        reactionEmojis.add(EmojiManager.getForAlias("hundred_points")); // 💯

        // Kisses (kissing faces or symbols)
        reactionEmojis.add(EmojiManager.getForAlias("kissing_heart")); // 😘
        reactionEmojis.add(EmojiManager.getForAlias("kissing")); // 😗
        reactionEmojis.add(EmojiManager.getForAlias("kissing_smiling_eyes")); // 😙

        // Hugs (hugging gestures or symbols)
        reactionEmojis.add(EmojiManager.getForAlias("hugging_face")); // 🤗
        reactionEmojis.add(EmojiManager.getForAlias("people_hugging")); // 🫂

        // Miscellaneous (side eye, skeptical or suspicious looks)
        reactionEmojis.add(EmojiManager.getForAlias("unamused")); // 😒 (side eye/skeptical)
        reactionEmojis.add(EmojiManager.getForAlias("raising_hand")); // 🤨 (side eye/raising eyebrow)

        // Status (OK, not OK)
        reactionEmojis.add(EmojiManager.getForAlias("thumbs_up")); // 👍 (OK)
        reactionEmojis.add(EmojiManager.getForAlias("thumbs_down")); // 👎 (not OK)

        // Feelings (sleepy, cold, hot, angry, very angry)
        reactionEmojis.add(EmojiManager.getForAlias("sleeping")); // 😴 (sleepy)
        reactionEmojis.add(EmojiManager.getForAlias("sleepy")); // 😪 (sleepy)
        reactionEmojis.add(EmojiManager.getForAlias("snowflake")); // ❄️ (cold)
        reactionEmojis.add(EmojiManager.getForAlias("fire")); // 🔥 (hot)
        reactionEmojis.add(EmojiManager.getForAlias("angry")); // 😠 (angry)
        reactionEmojis.add(EmojiManager.getForAlias("rage")); // 😡 (angry)
        reactionEmojis.add(EmojiManager.getForAlias("face_with_steam_from_nose")); // 😤 (very angry)

        // Peace (peace signs or symbols)
        reactionEmojis.add(EmojiManager.getForAlias("dove")); // 🕊️ (peace)

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
                System.out.println("Null emoji found for alias: " );
            }
        }

        // Ensure the dialog stays open until the user closes it (via "X")
        emojiDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); // Close only when "X" is clicked

        emojiDialog.setVisible(true); // Show the dialog, and it stays open until closed manually
    }

    // Method to start reading messages from the server in a separate thread
    public void startReading() {
        Runnable r1 = () -> {
            System.out.println("Reader started...");
            try {
                while (true) {
                    String msg = br.readLine();
                    if (msg.equals("exit")) {
                        messageArea.append("Server: " + msg + "\n");
                        saveMessage("Server", msg);
                        System.out.println("Server terminated the chat");
                        JOptionPane.showMessageDialog(this, "Server Terminated the chat");
                        messageInput.setEnabled(false);
                        socket.close();                 // Close connection
                        out.flush();                    // Flush output
                        break;
                    }
                    messageArea.append("Server: " + msg + "\n");
                    saveMessage("Server", msg);
                }
            } catch (Exception e) {
                System.out.println("Connection closed");
            }
        };
        new Thread(r1).start();
    }

    // Main method to start the client
    public static void main(String[] args) {
        System.out.println("This is client...");
        new Client();
    }
}