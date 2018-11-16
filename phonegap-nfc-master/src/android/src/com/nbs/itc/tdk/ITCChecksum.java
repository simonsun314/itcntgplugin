package com.nbs.itc.tdk;


import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-10-15
 *
 */
public class ITCChecksum extends BufCodec {
    public final static int CHECKSUM_PAGE_COUNT = 1;

    public ITCChecksum(byte[] theBuf) {
        super(theBuf);
    }

    public ITCChecksum() {
        super(new byte[CHECKSUM_PAGE_COUNT * 4]);
    }


    public byte[] getChecksum() {
        return Arrays.copyOf(buf, buf.length);
    }

    public void setChecksum(byte[] checksum) throws InvalidParameterException {
        if(checksum.length != CHECKSUM_PAGE_COUNT * 4) {
            throw new InvalidParameterException("invalid checksum length");
        }
    }
}
