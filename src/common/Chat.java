package common;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.*;

public class Chat  {

    private static final long serialVersionUID = 1L;
    private String name;

    private ObjectOutputStream out;
    private ObjectInputStream in;
    private ChatService chatService;
    private Socket socket;
    private ChatProtocol chatProtocol;
    private ChatProtocol.Status protocolStatus;

    private ChatSettings chatSettings;
    private DiffieHellman diffieHellman;

    private static PrivateKey priKey;
    private static PublicKey pubKey;

    public interface ChatService {
        void showMessage(String message);
        void showInfo(String message);
        void showImportant(String message);
        void showError(String message);
    }

    public Chat(String name, ChatSettings chatSettings, ChatService chatService) {
        this.name = name;
        this.chatSettings = chatSettings;
        this.chatService = chatService;
        this.chatProtocol = new ChatProtocol(chatSettings);
        this.protocolStatus = ChatProtocol.Status.OKAY;

        chatService.showInfo("Chat settings you've selected: " + chatSettings.getSettingsString());
    }

    // Generate a DSA key pair with key size = 1024
    private void generator() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
            keyGen.initialize(1024);
            KeyPair keyPair = keyGen.genKeyPair();
            priKey = keyPair.getPrivate();
            pubKey = keyPair.getPublic();
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Failed to generate key pair.");
        }
    }

    public static PrivateKey getPriKey() {
        return priKey;
    }

    public static PublicKey getPubKey() {
        return pubKey;
    }

    // Create input and output socket readers
    public void initIO(Socket socket) throws IOException {
        this.socket = socket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
    }

    // Reset protocol settings
    public void initProtocol() {
        diffieHellman = null;
        protocolStatus = ChatProtocol.Status.OKAY;
        chatProtocol.init();
    }

    // Send the first message in the protocol (Normally the client does this)
    public void startProtocol() throws IOException {
        out.writeObject(new Message(chatProtocol.startProtocol()));
    }

    public void stop() throws IOException {
        if (in != null) in.close();
        if (out != null) out.close();
        if (socket != null) socket.close();
    }

    // Start listening on the socket (infinite loop)
    public void listen() throws IOException, ClassNotFoundException {
        Message message;
        while(true) {
            message = (Message)in.readObject();
            if (message == null) {
                break;
            }
            receiveMessage(message);
        }
        stop();
    }

    // Handle the sending of a message
    public void sendMessage(Message message) throws IOException {
        if (out == null) return;

        if (chatSettings.isConfedentiality() && diffieHellman != null) {
            message = Auth.encryptMessage(message, diffieHellman);
        }
        if (chatSettings.isIntegrity() && Chat.priKey != null) {
            message = Auth.signMessageWithPrivateKey(message);
        }

        System.out.println("");
        out.writeObject(message);
    }

    public void sendMessage(String text) throws IOException {
        sendMessage(new Message(text));
    }

    // Handle the receiving of a message
    public void receiveMessage(Message message) throws IOException {

        // If the initial protocol has not succeeded, send the message to it
        if (protocolStatus != ChatProtocol.Status.SUCCEED) {
            ChatProtocol.ProtocolResult result = chatProtocol.nextMessage(message);
            protocolStatus = result.newStatus;

            if (chatSettings.isConfedentiality() &&
                    (chatProtocol.getState() == ChatProtocol.State.S_SETTINGS_SENT
                            || chatProtocol.getState() == ChatProtocol.State.C_GO_SENT)) {
                chatService.showInfo("Generating Diffie Hellman keypair...");
            }

            if (result.newMessage != null) {
                sendMessage(result.newMessage);
            }
            if (protocolStatus == ChatProtocol.Status.SUCCEED) {
                if (chatSettings.isConfedentiality()) {
                    chatService.showInfo("Your messages will be encrypted.");
                }
                if (chatSettings.isIntegrity()) {
                    chatService.showInfo("Your messages will be signed.");
                    generator();
                }
                chatService.showImportant("Handshake successful. Chat away!\n");
                diffieHellman = chatProtocol.getDiffieHellman();
            }

            if (protocolStatus == ChatProtocol.Status.REFUSE) {
                disconnect();
                return;
            }
        } else {
            if (chatSettings.isIntegrity()) {
                if (!Auth.verifyMessageWithPublicKey(message)) {
                    chatService.showError("Message is not verified");
                    return;
                }
            }
            if (chatSettings.isConfedentiality()) {
                message = Auth.decryptMessage(message, diffieHellman);
            }

            chatService.showMessage(message.getText());
        }
    }

    // Can the user send messages
    public boolean isConnected() {
        return  socket != null
                && socket.isConnected()
                && (protocolStatus == ChatProtocol.Status.SUCCEED);
    }

    // Disconnect because of a difference in the chat settings
    private void disconnect() {
        chatService.showError("Chat requirements not the same, closing connection");

        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}