package common;

import javax.crypto.KeyAgreement;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

public class DiffieHellman {

    private KeyPair keyPair;
    private KeyAgreement keyAgreement;
    private PublicKey senderPublicKey;
    private byte[] secret;
    private SecretKeySpec aesKey;

    public DiffieHellman(KeyPair keyPair, KeyAgreement keyAgreement) {
        this.keyPair = keyPair;
        this.keyAgreement = keyAgreement;
    }

    public byte[] getEncodedPublicKey() {
        return keyPair.getPublic().getEncoded();
    }

    public void setSenderPublicKey(byte[] senderPubKeyEnc) throws Exception {
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(senderPubKeyEnc);
        this.senderPublicKey = keyFactory.generatePublic(x509EncodedKeySpec);
    }

    public void doDHPhase() throws Exception {
        keyAgreement.doPhase(senderPublicKey, true);
    }

    public void generateSharedSecret() {
        byte[] sharedSecret = keyAgreement.generateSecret();
        this.secret = sharedSecret;
        this.aesKey = new SecretKeySpec(sharedSecret, 0, 16, "AES");
    }

    public byte[] getSecret() {
        return secret;
    }

    public SecretKeySpec getAesKey() {
        return aesKey;
    }

    public static DiffieHellman generateFreshKey() throws Exception {
        // Create DH key pair with 2048 bit key size
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("DH");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();

        // Create DH KeyAgreement object
        KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
        keyAgree.init(keyPair.getPrivate());

        // Encode public key
        byte[] pubKeyEnc = keyPair.getPublic().getEncoded();

        return new DiffieHellman(keyPair, keyAgree);
    }

    public static DiffieHellman generateFromPublic(byte[] senderPubKeyEnc) throws Exception {
        // Convert encoded public key into object
        KeyFactory keyFactory = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(senderPubKeyEnc);

        PublicKey senderPublicKey = keyFactory.generatePublic(x509EncodedKeySpec);

        // Get DH parameters associated with senders public key
        DHParameterSpec dhParamFromPublicKey = ((DHPublicKey)senderPublicKey).getParams();

        // Create key pair using DH parameters
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(dhParamFromPublicKey);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        // Create and init DH KeyAgreement object
        KeyAgreement keyAgree = KeyAgreement.getInstance("DH");
        keyAgree.init(keyPair.getPrivate());

        // Encode public key
        byte[] pubKeyEnc = keyPair.getPublic().getEncoded();

        DiffieHellman dh = new DiffieHellman(keyPair, keyAgree);
        dh.setSenderPublicKey(senderPubKeyEnc);
        return dh;
    }
}
