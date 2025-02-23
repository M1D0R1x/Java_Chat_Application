import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.InetAddress;
import java.net.Socket;

//
//@veera
//

public class Client extends JFrame {
    //    variables
    Socket socket;
    BufferedReader br;
    PrintWriter out;
    FileWriter fileWriter;

    private JLabel heading = new JLabel("Client Area");
    private JTextArea messageArea = new JTextArea();
    private JTextField messageInput = new JTextField();
    private Font font1 = new Font("Segue UI", Font.ITALIC, 22);
    private Font font2 = new Font("Roboto", Font.PLAIN, 18);


    //    constructor
    public Client() {
        try {
            System.out.println("Sending Request to server");
//            socket = new Socket("192.168.77.87", 2103);
            socket = new Socket(InetAddress.getLocalHost(), 2103);
            System.out.println("Connection Done");
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            fileWriter = new FileWriter("client_history.txt", true); // append mode
            out = new PrintWriter(socket.getOutputStream());

            createGUI();
            handleEvents();
            startReading();
            loadChatHistory();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEvents() {

        messageInput.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {

            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 10) {
                    String contentToSend = messageInput.getText();
                    messageArea.append("Me : " + contentToSend + "\n");
                    if (contentToSend.equals("exit")) {
                        messageInput.setEnabled(false);
                    }
                    out.println(contentToSend);
                    out.flush();
                    messageInput.setText("");
                    messageInput.requestFocus();

                    try {
                        fileWriter.write("Me: " + contentToSend + "\n");
                        fileWriter.flush();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                }

            }
        });
    }

    private void createGUI() {
        this.setTitle("Client");
        this.setSize(500,600);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

//        component code
        heading.setFont(font1);
        messageArea.setFont(font2);
        messageInput.setFont(font2);
        heading.setHorizontalAlignment(SwingConstants.CENTER);
        heading.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        messageArea.setEditable(false);

//        frame layout
        this.setLayout(new BorderLayout());

//        adding components to frame
        this.add(heading, BorderLayout.NORTH);
        JScrollPane jScrollPane = new JScrollPane(messageArea);
        this.add(jScrollPane, BorderLayout.CENTER);
        this.add(messageInput, BorderLayout.SOUTH);
        this.setVisible(true);

//        to scroll jpane
        messageArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scrollToBottom();

            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scrollToBottom();

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scrollToBottom();

            }

            public void scrollToBottom() {
                SwingUtilities.invokeLater( ()-> {
                    jScrollPane.getVerticalScrollBar().setValue(jScrollPane.getVerticalScrollBar().getMaximum());
                });
            }
        });
    }

    private void loadChatHistory() {
        try (BufferedReader reader = new BufferedReader(new FileReader("client_history.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                messageArea.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public void startReading() {
//        thread-reading
        Runnable r1 = () -> {
            System.out.println("Reader started...");
            try {
                while (true) {
                    String msg = br.readLine();
                    if (msg.equals("exit")) {
                        messageArea.append("Server: " + msg + "\n");
                        try {
                            fileWriter.write("Server: " + msg + "\n");
                            fileWriter.flush();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Server terminated the chat");
                        JOptionPane.showMessageDialog(this, "Server Terminated the chat");
                        messageInput.setEnabled(false);
                        socket.close();
                        out.flush();
                        break;
                    }
                    messageArea.append("Server: " + msg + "\n");
                    try {
                        fileWriter.write("Server: " + msg + "\n");
                        fileWriter.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.out.println("Connection closed");
            }
        };
        new Thread(r1).start();
    }


    public static void main(String[] args) {
        System.out.println("This is client...");
        new Client();
    }
}