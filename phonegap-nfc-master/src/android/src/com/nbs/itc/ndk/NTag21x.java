package com.nbs.itc.ndk;

import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcF;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author samzuel zhou
 * @version 1.0.0
 * @date 2018-10-15
 *
 */

public class NTag21x {

    class InvalidPackException extends Exception {
        public InvalidPackException(String message) {
            super(message);
        }
    }

    class NotNTagException extends Exception {
        public NotNTagException() {
            throw new RuntimeException("Stub!");
        }

        public NotNTagException(String message) {
            super(message);
        }

        public NotNTagException(String message, Throwable cause) {
            throw new RuntimeException("Stub!");
        }

        public NotNTagException(Throwable cause) {
            throw new RuntimeException("Stub!");
        }
    }
    private static final byte TAG_MANUFACTURER_NXP = 0x04;

    private static final String TAG = NTag21x.class.getCanonicalName();

    protected NfcA nfcA;
    public Tag tag;
    private boolean debugMode = false;

    // User Memory (It's defined in the children)
    public int PAGE_USER_START;
    public int PAGE_USER_END;
    public int AUTH0_CONFIG_PAGE;
    public int ACCESS_CONFIG_PAGE;
    public int PWD_CONFIG_PAGE;
    public int PACK_CONFIG_PAGE;
    public int PAGE_CONFIG_0;
    public int PAGE_CONFIG_1;
    public int PAGE_STATIC_LOCK;
    public int PAGE_DYNAMIC_LOCK;

    // Available CMDs
    private static final byte READ = (byte) 0x30;
    private static final byte WRITE = (byte) 0xA2;
    private static final byte PWD_AUTH = (byte) 0x1B;
    private static final byte FAST_READ = (byte) 0x3A;
    private static final byte GET_VERSION = (byte)0x60;
    private static final byte READ_CNT = (byte)0x39;
    private static final byte READ_SIG = (byte)0x3C;



    public enum SigLockType {
        UNLOCK,
        LOCK,
        LOCK_PERMANENTLY
    }

    public final static byte[] TAG_VERSION_NTAG_213 = new byte[] {0x00, 0x04, 0x04, 0x02, 0x01, 0x00, 0x0f, 0x03};
    public final static byte[] TAG_VERSION_NTAG_213_TAG_TAMPER = new byte[] {0x00, 0x04, 0x04, 0x02, 0x03, 0x00, 0x0f, 0x03};
    public final static byte[] TAG_VERSION_NTAG_216 = new byte[] {0x00, 0x04, 0x04, 0x02, 0x01, 0x00, 0x13, 0x03};


    public NTag21x(Tag tag) {
        this.tag = tag;
        this.nfcA = NfcA.get(tag);
    }

    public void debugMode(boolean enable) {
        debugMode = enable;
    }


    public void connect() throws IOException, NotNTagException {
        if (!nfcA.isConnected()) {
            nfcA.connect();
        }
        byte[] uid = read(0);
        if (uid[0] != TAG_MANUFACTURER_NXP) {
            close();
            throw new NotNTagException(String.format("Tag manufacturer %x not recognized", uid[0]));
        }
    }

    public void close() throws IOException {
        if (nfcA.isConnected()) {
            nfcA.close();
        }
    }

    public void setTimeout(int millis) {
        nfcA.setTimeout(millis);
    }

    // write 4 bytes of data
    public void write(int address, byte[] data) throws Exception {
        if (data.length != 4) {
            throw new Exception("invalid data length");
        }
        nfcA.transceive(new byte[]{
                WRITE,
                (byte)address,
                data[0], data[1], data[2], data[3]
        });
    }


    public byte[] fastRead(int startPageAddress, int endPageAddress) throws IOException {
        return nfcA.transceive(new byte[]{
                FAST_READ,
                (byte)startPageAddress,
                (byte)endPageAddress
        });
    }

    // read 4 pages
    public byte[] read(int startPageAddress) throws IOException {
        return nfcA.transceive(new byte[]{
                READ,
                (byte)startPageAddress
        });
    }

    public void authenticatePwd(byte[] password, byte[] passwordAcknowledgement) throws InvalidPackException, IOException {
        byte[] response = nfcA.transceive(new byte[]{
                PWD_AUTH,
                password[0], password[1], password[2], password[3]
        });
        if(response[0] != passwordAcknowledgement[0] || response[1] != passwordAcknowledgement[1]) {
            throw new InvalidPackException("invalid pack");
        }
    }


    public byte[] getVersion() throws IOException {
        return nfcA.transceive(new byte[]{
                GET_VERSION
        });
    }

    public byte[] readCounter() throws IOException {
        return nfcA.transceive(new byte[]{
                READ_CNT,
                (byte)0x02
        });
    }

    public byte[] readSignature() throws IOException {
        return nfcA.transceive(new byte[]{
                READ_SIG,
                (byte)0x00
        });
    }


    public byte[] getConfigBytes() throws IOException {
        return fastRead(PAGE_CONFIG_0, PAGE_CONFIG_1);
    }

    public void enableCounter(boolean isEnabled) throws Exception {
        byte[] config1 = read(PAGE_CONFIG_1);
        byte enableFlag = isEnabled ? (byte)1 : (byte)0;
        if((config1[0] & 0x10) == enableFlag) {
            return;
        }
        if(isEnabled) {
            config1[0] |= (byte) 0x10;
        } else {
            config1[0] &= (byte) 0xEF;
        }
        write(PAGE_CONFIG_1, Arrays.copyOfRange(config1, 0, 4));
    }
}
