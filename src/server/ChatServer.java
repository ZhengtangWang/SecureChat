package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

import common.Auth;
import common.Chat;
import common.ChatSettings;
import common.ChatWindow;

import javax.swing.*;

public class ChatServer {

    private ServerSocket server;
    private Chat chat;
    private ChatWindow chatWindow;
    private ChatSettings chatSettings;

    private boolean waitingForPassword = false;

    private static final int port = 8888;

    public ChatServer(int port) {
        openWindow();

        chatWindow.showImportant("This is the chat server.");
        chatWindow.showImportant("Please select the chat settings from the right ->");

        chatWindow.chatInput.addActionListener(e -> {
            String text = chatWindow.chatInput.getText();

            // Do not run on listener thread
            SwingWorker sw = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    if (waitingForPassword) {
                        chatWindow.clearInput();
                        if (Auth.userLogin("server",text)) {
                            chatWindow.showSuccess("Passwords match!");
                            waitingForPassword = false;
                            startConnecting();
                        } else {
                            chatWindow.showWarning("Password incorrect...");
                        }
                    } else {
                        sendMessage(text);
                    }
                    return null;
                }
            };
            sw.execute();
        });

        chatWindow.okayButton.addActionListener(e -> {
            chatWindow.lockChecks();

            // Do not run on event listener thread
            SwingWorker sw = new SwingWorker() {
                public Object doInBackground(){
                    chatSettings = new ChatSettings(
                            chatWindow.confidentialityCheck.isSelected(),
                            chatWindow.integrityCheck.isSelected(),
                            chatWindow.authenticationCheck.isSelected()
                    );
                    start();
                    return null;
                }
            };
            sw.execute();
        });
    }

    public void sendMessage(String message) {
        if (chat != null && chat.isConnected()) {
            chatWindow.showMessage("server", message);
            chatWindow.clearInput();

            try {
                chat.sendMessage(message);
            } catch (IOException e) {
                chatWindow.showWarning("Error sending message");
            }
        }
    }

    public void start() {
        chat = new Chat("server", chatSettings, new Chat.ChatService() {
            @Override
            public void showMessage(String message) {
                chatWindow.showMessage("client", message);
            }

            @Override
            public void showInfo(String message) {
                chatWindow.showInfo(message);
            }

            @Override
            public void showImportant(String message) {
                chatWindow.showImportant(message);
            }

            @Override
            public void showError(String message) {
                chatWindow.showError(message);
            }
        });

        startPasswordCheck();
    }

    public void startPasswordCheck() {
        if (chatSettings.isAuthentication()) {
            chatWindow.showImportant("\nPlease enter the password");
            waitingForPassword = true;
        } else {
            startConnecting();
        }
    }

    public void startConnecting()  {
        chatWindow.showInfo("Binding to port " + port);
        try {
            server = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(e);
        }

        // If client disconnects we just start accepting connections again
        while (true) {
            try {
                Socket socket = waitForClient();
                chat.initIO(socket);
            } catch (IOException e) {
                chatWindow.showError("Error connecting to client");
                continue;
            }

            try {
                chat.initProtocol();
                chat.listen();
                chatWindow.showWarning("Client disconnected\n");
            } catch(IOException e) { } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Socket waitForClient() throws IOException {
        chatWindow.showInfo("Waiting for client...");
        Socket socket = server.accept();
        chatWindow.showInfo("Connected to a client!");
        return socket;
    }

    public void openWindow() {
        chatWindow = new ChatWindow(("Server"));

        JFrame frame = new JFrame("Server Chat");
        frame.setResizable(false);
        frame.setContentPane(chatWindow.view);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] argv) {
        ChatServer server = new ChatServer(port);
    }
}