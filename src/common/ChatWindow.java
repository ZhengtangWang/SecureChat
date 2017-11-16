package common;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static common.ChatWindow.StatusLevel.*;

public class ChatWindow {
    public enum StatusLevel {
        INFO,
        WARN,
        ERROR,
        SUCCESS,
        IMPORTANT
    }

    public JTextField chatInput;
    private JTextPane chatTextArea;
    public JPanel view;
    public JCheckBox confidentialityCheck;
    public JCheckBox integrityCheck;
    public JCheckBox authenticationCheck;
    public JButton okayButton;
    private JScrollPane scollPane;

    private StyledDocument doc;

    // Status
    private Style infoStyle;
    private Style warnStyle;
    private Style errorStyle;
    private Style successStyle;
    private Style importantStyle;

    // Messages
    private Style messageStyle;
    private Style clientStyle;
    private Style serverStyle;

    private String title;
    private String placeholder = "Type here...";

    public ChatWindow(String title) {
        this.title = title;

        chatInput.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                maybeHidePlaceholder();
            }

            @Override
            public void focusLost(FocusEvent e) {
                showPlaceholder();
            }
        });
        showPlaceholder();

        scollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                e.getAdjustable().setValue(e.getAdjustable().getMaximum());
            }
        });
        scollPane.setBorder(BorderFactory.createEmptyBorder());

        doc = chatTextArea.getStyledDocument();

        // Styles
        infoStyle = chatTextArea.addStyle("Info Style", null);
        StyleConstants.setForeground(infoStyle, Color.GRAY);

        warnStyle = chatTextArea.addStyle("Warn Style", null);
        StyleConstants.setForeground(warnStyle, Color.ORANGE);

        errorStyle = chatTextArea.addStyle("Error Style", null);
        StyleConstants.setForeground(errorStyle, Color.RED);

        successStyle = chatTextArea.addStyle("Success Style", null);
        StyleConstants.setForeground(successStyle, Color.GREEN);

        importantStyle = chatTextArea.addStyle("Important Style", null);
        StyleConstants.setForeground(importantStyle, Color.BLACK);
        StyleConstants.setFontSize(importantStyle, 16);

        messageStyle = chatTextArea.addStyle("Message Style", null);
        StyleConstants.setForeground(messageStyle, Color.BLACK);

        clientStyle= chatTextArea.addStyle("Client Style", null);
        StyleConstants.setForeground(clientStyle, Color.MAGENTA);

        serverStyle = chatTextArea.addStyle("Server Style", null);
        StyleConstants.setForeground(serverStyle, Color.BLUE);
    }

    public void showPlaceholder() {
        if (chatInput.getText().isEmpty()) {
            chatInput.setForeground(Color.GRAY);
            chatInput.setText(placeholder);
        }
    }

    public void maybeHidePlaceholder() {
        if (chatInput.getText().equals(placeholder)) {
            chatInput.setText("");
            chatInput.setForeground(Color.BLACK);
        }
    }

    public void showStatus(String message, StatusLevel level) {
        Style style = null;
        switch (level) {
            case INFO:
                style = infoStyle;
                break;
            case WARN:
                style = warnStyle;
                break;
            case ERROR:
                style = errorStyle;
                break;
            case SUCCESS:
                style = successStyle;
                break;
            case IMPORTANT:
                style = importantStyle;
                break;
        }

        appendMessage(message, style, true);
    }

    public void showInfo(String message) {
        showStatus(message, INFO);
    }

    public void showWarning(String message) {
        showStatus(message, WARN);
    }

    public void showError(String message) {
        showStatus(message, ERROR);
    }

    public void showSuccess(String message) {
        showStatus(message, SUCCESS);
    }

    public void showImportant(String message) {
        showStatus(message, IMPORTANT);
    }

    public void showMessage(String from, String message) {
        Style nameStyle = serverStyle;
        if (from.compareTo("client") == 0) {
            nameStyle = clientStyle;
        }

        appendMessage("[ " + from + " ] ", nameStyle, true);
        appendMessage(message, messageStyle, false);
    }

    public void clearInput() {
        chatInput.setText("");
    }

    public void lockChecks() {
        confidentialityCheck.setEnabled(false);
        integrityCheck.setEnabled(false);
        authenticationCheck.setEnabled(false);
        okayButton.setVisible(false);
    }

    private void appendMessage(String message, Style style, boolean newline) {
        try {
            doc.insertString(doc.getLength(), newline ? "\n" + message : message, style);
            JScrollBar vertical = scollPane.getVerticalScrollBar();
            vertical.setValue( vertical.getMaximum() );
        } catch (BadLocationException e) {
        }
    }
}
