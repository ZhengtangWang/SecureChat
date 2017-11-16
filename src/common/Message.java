package common;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;

public class Message implements Serializable {

    private byte[] bytes;
    private byte[] encodedParams;
    private String signature;

    public Message(String text) {
        this.bytes = text.getBytes();
    }

    public Message(byte[] bytes) {
        this.bytes = bytes;
    }

    public String getText() {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getEncodedParams() {
        return encodedParams;
    }

    public void setEncodedParams(byte[] encodedParams) {
        this.encodedParams = encodedParams;
    }

    public String getSignature() { return signature; }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
