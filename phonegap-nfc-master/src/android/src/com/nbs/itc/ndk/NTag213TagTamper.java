package com.nbs.itc.ndk;

import android.nfc.Tag;


import java.io.IOException;
import java.util.Arrays;

/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-10-15
 *
 */
public class NTag213TagTamper extends NTag21x {


    // commands
    private static byte READ_TT_STATUS = (byte)0xA4;
    private static final byte LOCK_SIG = (byte)0xAC;
    private static final byte WRITE_SIG = (byte) 0xA9;


    // page address
    private static byte PAGE_TT_MESSAGE = (byte) 0x2D;


    public NTag213TagTamper(Tag tag) {
        super(tag);
        super.PAGE_USER_START = (byte) 0x04;
        super.PAGE_USER_END   = (byte) 0x27;
        super.AUTH0_CONFIG_PAGE = (byte) 0x29;
        super.ACCESS_CONFIG_PAGE = (byte) 0x2A;
        super.PWD_CONFIG_PAGE = (byte) 0x2B;
        super.PACK_CONFIG_PAGE = (byte) 0x2C;
        super.PAGE_CONFIG_0 = (byte) 0x29;
        super.PAGE_CONFIG_1 = (byte) 0x2A;
    }

    public byte[] readTTStatus() throws IOException {
        return super.nfcA.transceive(new byte[] {
                READ_TT_STATUS,
                0x00
        });
    }

    public void writeCustomTTMessage(byte[] message) throws Exception {
        write(PAGE_TT_MESSAGE, message);
    }

    public void lockSignature(NTag213TagTamper.SigLockType type) throws IOException {
        nfcA.transceive(new byte[] {
                LOCK_SIG,
                (byte)type.ordinal()
        });
    }

    // write 32 byte signature
    public void writeSignature(byte[] signature) throws Exception {
        // write 00 ~ 07
        if (signature.length != 32) {
            throw new Exception("invalid signature length");
        }
        for(int i = 0; i < 32; i+=4) {
            nfcA.transceive(new byte[] {
                    WRITE_SIG,
                    (byte)(i / 4),
                    signature[i], signature[i+1], signature[i+2], signature[i+3]
            });
        }
    }
}
