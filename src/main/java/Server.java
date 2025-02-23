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
    private JTextField messageInput = new JTextField();
    private Font font1 = new Font("Segue UI", Font.ITALIC, 22); // Font for heading
    private Font font2 = new Font("Roboto", Font.PLAIN, 18);    // Font for messages

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
            public void keyTyped(KeyEvent e) {} // Not used

            @Override
            public void keyPressed(KeyEvent e) {} // Not used

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 10) { // Enter key presse
                    String contentToSend = messageInput.getText();
                    messageArea.append("Me : " + contentToSend + "\n");
                    if (contentToSend.equals("exit")) {
                        messageInput.setEnabled(false);
                    }
                    out.println(contentToSend);
                    out.flush();                // Ensure message is sent immediately
                    messageInput.setText("");   // Clear input field
                    messageInput.requestFocus(); // Refocus on input field

                    // Save the sent message to Server-specific database
                    saveMessage("Me", contentToSend);
                }
            }
        });
    }

    // Method to create and configure the GUI
    private void createGUI() {
        this.setTitle("Server");
        this.setSize(500, 600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Configure component styles
        heading.setFont(font1);
        messageArea.setFont(font2);
        messageInput.setFont(font2);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        messageArea.setEditable(false);

        // Set layout and add components to frame
        this.setLayout(new BorderLayout());
        this.add(heading, BorderLayout.NORTH);
        JScrollPane jScrollPane = new JScrollPane(messageArea);
        this.add(jScrollPane, BorderLayout.CENTER);
        this.add(messageInput, BorderLayout.SOUTH);
        this.setVisible(true);

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
    }

    // Method to load chat history from Server-specific database (without timestamps)
    private void loadChatHistory() {
        try (Connection conn = DriverManager.getConnection(DB_URL); // Connect to Server-specific database
             Statement stmt = conn.createStatement();              // Create statement
             ResultSet rs = stmt.executeQuery("SELECT sender, message FROM messages WHERE sender IN ('Me', 'Client') ORDER BY id")) { // Query only Server-relevant messages
            while (rs.next()) { // Iterate through result set
                // Append each message with sender (no timestamp) to message area
                messageArea.append(rs.getString("sender") + " : " + rs.getString("message") + "\n");
            }
        } catch (SQLException e) {
            System.err.println("Error loading chat history: " + e.getMessage());
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