package com.nbs.itc.tdk;

import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ITCData implements BufCodec.BufValidator {
    private ITCID itcid;
    private byte[] ds;
    private ITCHeader itcHeader;
    private ITCDescriptors itcDescriptors;
    private ITCChecksum itcChecksum;
    public ITCData(ITCID itcid, ITCHeader itcHeader, byte[] ds, ITCDescriptors itcDescriptors, ITCChecksum itcChecksum) {
        this.ds = Arrays.copyOf(ds, ds.length);
        this.itcid = itcid;
        this.itcHeader = itcHeader;
        this.itcDescriptors = itcDescriptors;
        this.itcChecksum = itcChecksum;
    }

    public ITCData(ITCID itcid, ITCHeader itcHeader, byte[] ds,  ITCDescriptors itcDescriptors) {
        this.ds = Arrays.copyOf(ds, ds.length);
        this.itcid = itcid;
        this.itcHeader = itcHeader;
        this.itcDescriptors = itcDescriptors;
    }

    public ITCID getITCID() {
        return itcid;
    }

    public byte[] getDS() {
        return ds;
    }

    public ITCHeader getITCHeader() {
        return itcHeader;
    }

    public ITCDescriptors getITCDescriptors() {
        return itcDescriptors;
    }

    public ITCChecksum getITCChecksum() {
        return itcChecksum;
    }

    @Override
    public void validateCheckSum() throws InvalidChecksumException {
        if (itcChecksum == null) {
            throw new InvalidChecksumException("invalid checksum bytes");
        }
        byte[] checkSumBytes = itcChecksum.getChecksum();
        ByteBuffer bb = ByteBuffer.wrap(checkSumBytes);
        int storedCRC32 = bb.getInt();
        int currentCRC32;
        try {
            currentCRC32 = calcCRC32();
        } catch (InvalidObjectException ex) {
            throw new InvalidChecksumException("failed to calculate CRC32");
        }
        if (storedCRC32 != currentCRC32) {
            throw new InvalidChecksumException("CRC32 dose not match");
        }

    }

    public void makeCheckSum() throws InvalidObjectException {
        int checkSum = calcCRC32();
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(checkSum);
        byte[] checkSumBytes = bb.array();
        itcChecksum = new ITCChecksum(checkSumBytes);
    }


    private int calcCRC32() throws InvalidObjectException {
        if(itcDescriptors == null) {
            throw new InvalidObjectException("invalid data");
        }
        byte[] bufCalc = new byte[itcDescriptors.buf.length];

        System.arraycopy(itcDescriptors.buf, 0, bufCalc,
                0, itcDescriptors.buf.length);
        Checksum checksum = new CRC32();
        checksum.update(bufCalc, 0, bufCalc.length);

        // get current checksum value
        return (int)checksum.getValue();

    }
}
