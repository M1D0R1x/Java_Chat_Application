import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;

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
    private Font font1 = new Font("Segue UI", Font.ITALIC, 22); // Font for heading
    private Font font2 = new Font("Noto Emoji", Font.PLAIN, 18); // Font for messages and input

    // SQLite database connection string for Server-specific database
    private static final String DB_URL = "jdbc:sqlite:server_chat.db"; // Unique database file for Server

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
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {}

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
        JButton clearChatButton = new JButton("Clear Chat");
        clearChatButton.setFont(font2);
        clearChatButton.addActionListener(e -> clearChatHistory()); // Call clearChatHistory on button click
        bottomPanel.add(clearChatButton, BorderLayout.WEST);        // Add button to the left of input

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
                    messageArea.append("Client: " + msg + "\n");
                    saveMessage("Client", msg);
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