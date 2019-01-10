package com.nbs.itc.tdk;

import android.util.Log;

import java.util.Arrays;

public class BufCodec {

    public interface BufValidator {
        class InvalidChecksumException extends Exception {
            public InvalidChecksumException() {
                throw new RuntimeException("Stub!");
            }

            public InvalidChecksumException(String message) {
                super(message);
            }

            public InvalidChecksumException(String message, Throwable cause) {
                throw new RuntimeException("Stub!");
            }

            public InvalidChecksumException(Throwable cause) {
                throw new RuntimeException("Stub!");
            }
        }

        void validateCheckSum() throws InvalidChecksumException;
    }




    protected byte[] buf;

    public BufCodec(byte[] theBuf) {
        buf = Arrays.copyOf(theBuf, theBuf.length);
    }

    // get raw buffer
    public byte[] getRaw() {
        return Arrays.copyOf(buf, buf.length);
    }

    public final static String bytesToHexString(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    protected static final byte bccChecksum(byte[] bytes) {
        byte sum = 0;
        for (byte b : bytes) {
            sum ^= b;
        }
        return sum;
    }

    public void printPages() {
        byte[] printBuf = Arrays.copyOf(buf, buf.length / 4 * 4);
        for(int i = 0; i < printBuf.length; i=i+4) {
            Log.d("itc", String.format("%X, %X, %X, %X",
                    printBuf[i], printBuf[i+1], printBuf[i+2], printBuf[i+3]));
        }
    }

}
