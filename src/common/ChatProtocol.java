package common;

import static common.ChatProtocol.State.*;
import static common.ChatProtocol.Status.OKAY;
import static common.ChatProtocol.Status.REFUSE;
import static common.ChatProtocol.Status.SUCCEED;

public class ChatProtocol {

    private final String hello = "hello";
    private final String go = "go";
    private final String reject = "reject";

    DiffieHellman diffieHellman;

    // OKAY: continue with protocol
    // SUCCEED: protocol passed
    // REFUSE: protocol failed, reject connection
    public enum Status {
        OKAY,
        SUCCEED,
        REFUSE
    }

    enum State {
        INIT,
        C_HELLO_SENT,
        C_SETTINGS_SENT,
        C_GO_SENT,
        S_HELLO_SENT,
        S_SETTINGS_SENT,
        S_GO_SENT,
        C_PUBKEY_SENT,
        ACCEPT,
        REJECT
    }

    public class ProtocolResult {
        public Message newMessage;
        public Status newStatus;

        public ProtocolResult(String text, Status newStatus) {
            this(new Message(text), newStatus);
        }

        public ProtocolResult(byte[] bytes, Status newStatus) {
            this(new Message(bytes), newStatus);
        }

        public ProtocolResult(Message newMessage, Status newStatus) {
            this.newMessage = newMessage;
            this.newStatus = newStatus;
        }
    }

    private State state;
    private ChatSettings chatSettings;

    public ChatProtocol(ChatSettings chatSettings) {
        this.chatSettings = chatSettings;
        init();
    }

    public void init() {
        state = INIT;
    }

    public String startProtocol() {
        this.state = C_HELLO_SENT;
        return hello;
    }

    public State getState() {
        return state;
    }

    public DiffieHellman getDiffieHellman() {
        return diffieHellman;
    }

    public ProtocolResult nextMessage(Message message) {
        ProtocolResult result = refuseResult();
        State newState = state;

        String text = message.getText();

        if (message.equals(reject)) {
            newState = REJECT;
        }

        switch (state) {
            // New message initing protocol to the server
            // Send hello back
            case INIT:
                if (text.equals(hello)) {
                    result = new ProtocolResult(hello, OKAY);
                    newState = S_HELLO_SENT;
                    break;
                }

            // The client has sent hello, wait on hello back
            // Send chat settings
            case C_HELLO_SENT:
                if (text.equals(hello)) {
                    result = new ProtocolResult(chatSettings.getSettingsString(), OKAY);
                    newState = C_SETTINGS_SENT;
                    break;
                } else {
                    result = refuseResult();
                    newState = REJECT;
                }

            // The client has sent the chat settings, wait on them back
            // Send go
            case C_SETTINGS_SENT:
                if (text.equals(chatSettings.getSettingsString())) {
                    result = new ProtocolResult(go, OKAY);
                    newState = C_GO_SENT;
                    break;
                } else {
                    result = refuseResult();
                    newState = REJECT;
                }

            // The client is okay with handshake, wait on go from server
            // If go then protocol has completed successfully
            case C_GO_SENT:
                if (text.equals(go)) {
                    if (chatSettings.isConfedentiality()) {
                        try {
                            diffieHellman = DiffieHellman.generateFreshKey();
                            result = new ProtocolResult(diffieHellman.getEncodedPublicKey(), OKAY);
                            newState = C_PUBKEY_SENT;
                            break;
                        } catch (Exception e) {
                            result = refuseResult();
                            newState = REJECT;
                        }
                    } else {
                        result = successResult();
                        newState = ACCEPT;
                        break;
                    }
                }

            // The client has sent its public key, wait on one from the server
            case C_PUBKEY_SENT:
                try {
                    diffieHellman.setSenderPublicKey(message.getBytes());
                    diffieHellman.doDHPhase();
                    diffieHellman.generateSharedSecret();
                    result = successResult();
                    newState = ACCEPT;
                    break;
                } catch (Exception e) {
                    result = refuseResult();
                    newState = REJECT;
                }

            // The server is waiting on the chat settings
            // Send settings back
            case S_HELLO_SENT:
                if (text.equals(chatSettings.getSettingsString())) {
                    result = new ProtocolResult(chatSettings.getSettingsString(), OKAY);
                    newState = S_SETTINGS_SENT;
                    break;
                } else {
                    result = refuseResult();
                    newState = REJECT;
                }

            // The server is waiting for a go from the client
            // Send confirmation go back
            case S_SETTINGS_SENT:
                if (text.equals(go)) {
                    if (chatSettings.isConfedentiality()) {
                        result = new ProtocolResult(go, OKAY);
                        newState = S_GO_SENT;
                    } else {
                        result = new ProtocolResult(go, SUCCEED);
                        newState = ACCEPT;
                    }
                    break;
                }

            // The server has sent back go and is waiting for the clients public key
            // Send back its own public key
            case S_GO_SENT:
                try {
                    diffieHellman = DiffieHellman.generateFromPublic(message.getBytes());
                    diffieHellman.doDHPhase();
                    diffieHellman.generateSharedSecret();
                    result = new ProtocolResult(diffieHellman.getEncodedPublicKey(), SUCCEED);
                    newState = ACCEPT;
                } catch (Exception e) {
                    result = refuseResult();
                    newState = REJECT;
                }
                break;

            case REJECT:
                break;
            case ACCEPT:
                result = successResult();
                break;
        }

        if (result.newStatus == REFUSE) {
            newState = REJECT;
        }

        state = newState;
        return result;
    }

    private ProtocolResult successResult() {
        return new ProtocolResult((Message) null, SUCCEED);
    }

    private ProtocolResult refuseResult() {
        return new ProtocolResult(reject, REFUSE);
    }

}
