package common;

public class ChatSettings {

    private boolean confedentiality;
    private boolean integrity;
    private boolean authentication;

    public ChatSettings(boolean confedentiality, boolean integrity, boolean authentication) {
        this.confedentiality = confedentiality;
        this.integrity = integrity;
        this.authentication = authentication;
    }

    public String getSettingsString() {
        String s = "";
        if (confedentiality) {
            s += "c";
        }
        if (integrity) {
            s += "i";
        }
        if (authentication) {
            s += "a";
        }
        return s;
    }

    public boolean isConfedentiality() {
        return confedentiality;
    }

    public boolean isIntegrity() {
        return integrity;
    }

    public boolean isAuthentication() {
        return authentication;
    }
}
