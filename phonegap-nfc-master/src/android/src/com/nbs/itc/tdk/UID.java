package com.nbs.itc.tdk;

import java.util.Arrays;

public class UID extends BufCodec implements BufCodec.BufValidator {
    // Cascade Tag 0x88
    private final static byte CT = (byte) 0x88;

    private final static int BIT_SN0 = 0;
    private final static int BIT_SN1 = 1;
    private final static int BIT_SN2 = 2;
    private final static int BIT_SN3 = 4;
    private final static int BIT_SN4 = 5;
    private final static int BIT_SN5 = 6;
    private final static int BIT_SN6 = 7;
    private final static int BIT_BCC0 = 3;
    private final static int BIT_BCC1 = 8;
    public final static int LEN_UID = 9;

    public UID(byte[] theBuf) {
        super(theBuf);
    }

    @Override
    public void validateCheckSum() throws InvalidChecksumException {
        if(super.buf.length != LEN_UID) {
            throw new InvalidChecksumException("invalid length");
        }
        // BCC0
        if(bccChecksum(new byte[]{CT, buf[BIT_SN0], buf[BIT_SN1], buf[BIT_SN2]}) != buf[BIT_BCC0]) {
            throw new InvalidChecksumException("invalid BCC0");
        }
        // BCC1
        if((bccChecksum(Arrays.copyOfRange(buf, BIT_SN3, BIT_SN6 + 1))) != buf[BIT_BCC1]) {
            throw new InvalidChecksumException("invalid BCC1");
        }
    }
}
