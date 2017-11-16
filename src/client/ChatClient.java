package client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

import common.Auth;
import common.Chat;
import common.ChatSettings;
import common.ChatWindow;

import javax.swing.*;

public class ChatClient {
    private Socket socket;
    private Chat chat;
    private ChatWindow chatWindow;
    private String serverName;
    private int serverPort;
    private ChatSettings chatSettings;

    private boolean waitingForPassword = false;

    private static final int port = 8888;
    private static final String name = "localhost";

    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;

        openWindow();

        chatWindow.showImportant("This is the chat client.");
        chatWindow.showImportant("Please select the chat settings from the right ->");
        chatWindow.chatInput.addActionListener(e -> {
            String text = chatWindow.chatInput.getText();

            // Do not run on listener thread
            SwingWorker sw = new SwingWorker() {
                @Override
                protected Object doInBackground() throws Exception {
                    if (waitingForPassword) {
                        chatWindow.clearInput();
                        if (Auth.userLogin("client",text)) {
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
            chatWindow.showMessage("client", message);
            chatWindow.clearInput();

            try {
                chat.sendMessage(message);
            } catch (IOException e) {
                chatWindow.showWarning("Error sending message");
            }
        }
    }

    public void start() {
        chat = new Chat("client", chatSettings, new Chat.ChatService() {
            @Override
            public void showMessage(String message) {
                chatWindow.showMessage("server", message);
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

    public void startConnecting() {
        chatWindow.showInfo("Establishing connection...");

        try {
            socket = new Socket(serverName, serverPort);
            chat.initIO(socket);

            chatWindow.showInfo("Connected to server, initiating protocol.");
        } catch (UnknownHostException e) {
            System.out.println("Host unknown: " + e.getMessage());
        } catch (IOException e) {
            System.out.println(e);
        }

        try {
            chat.initProtocol();
            chat.startProtocol();
            chat.listen();
        } catch (IOException e) {
            chatWindow.showError("Disconnected");
            chat = null;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void openWindow() {
        chatWindow = new ChatWindow("Client");

        JFrame frame = new JFrame("Client Chat");
        frame.setResizable(false);
        frame.setContentPane(chatWindow.view);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String args[]) throws IOException {
        ChatClient client = new ChatClient(name, port);
    }
}
