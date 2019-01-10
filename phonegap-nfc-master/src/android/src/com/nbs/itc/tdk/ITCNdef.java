package com.nbs.itc.tdk;

import java.io.InvalidObjectException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class ITCNdef implements BufCodec.BufValidator {
    private final static int NDEF_COUNTER_LENGTH = 6;
    private final static int NDEF_DS_LENGTH_LENGTH = 1;
    private final static int DS_LENGTH_BYTE = 6;
    private final static int NDEF_ITCID_LENGTH = 12;
    private final static int NDEF_ITC_HEADER_LENGTH = 8;
    private final static int NDEF_CHECKSUM_LENGTH = 4;
    private final static int NDEF_WITHOUT_DS_LENGTH =
            (NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH + NDEF_ITCID_LENGTH + NDEF_ITC_HEADER_LENGTH + NDEF_CHECKSUM_LENGTH);


    private ITCID itcid;
    private byte[] ds;
    private ITCHeader itcHeader;
    private byte[] ndefChecksum;

    public ITCNdef(ITCID itcid, byte[] ds, ITCHeader itcHeader) {
        this.itcid = itcid;
        this.ds = Arrays.copyOf(ds, ds.length);
        this.itcHeader = itcHeader;
    }


    public ITCID getITCID() {
        return itcid;
    }

    public ITCHeader getItcHeader() {
        return itcHeader;
    }

    public byte[] getDS() {
        return Arrays.copyOf(ds, ds.length);
    }

    private ITCNdef(ITCID itcid, byte[] ds, ITCHeader itcHeader, byte[] ndefChecksum){
        this.itcid = itcid;
        this.ds = Arrays.copyOf(ds, ds.length);
        this.itcHeader = itcHeader;
        this.ndefChecksum = Arrays.copyOf(ndefChecksum, ndefChecksum.length);
    }

    public static ITCNdef parsePayload(byte[] payload) throws InvalidParameterException {
        if (payload.length < NDEF_WITHOUT_DS_LENGTH) {
            throw new InvalidParameterException("invalid payload length");
        }
        int dsLength = payload[DS_LENGTH_BYTE];
        if (payload.length  != (NDEF_WITHOUT_DS_LENGTH + dsLength)) {
            throw new InvalidParameterException("invalid payload length");
        }

        byte[] ds = new byte[dsLength];
        System.arraycopy(payload, NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH, ds, 0, dsLength);
        byte[] itcidBytes = new byte[NDEF_ITCID_LENGTH];
        System.arraycopy(payload, NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH + ds.length, itcidBytes, 0, NDEF_ITCID_LENGTH);
        ITCID itcid = new ITCID(itcidBytes);
        byte[] itcHeaderBytes = new byte[NDEF_ITC_HEADER_LENGTH];
        System.arraycopy(payload, NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH + ds.length + NDEF_ITCID_LENGTH,
                itcHeaderBytes, 0, NDEF_ITC_HEADER_LENGTH);
        ITCHeader itcHeader = new ITCHeader(itcHeaderBytes);
        byte[] checksumBytes = new byte[NDEF_CHECKSUM_LENGTH];
        System.arraycopy(payload, NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH + ds.length + NDEF_ITCID_LENGTH + NDEF_ITC_HEADER_LENGTH,
                checksumBytes, 0, NDEF_CHECKSUM_LENGTH);
        return new ITCNdef(itcid, ds, itcHeader, checksumBytes);
    }


    @Override
    public void validateCheckSum() throws InvalidChecksumException {
        if (ndefChecksum == null) {
            throw new InvalidChecksumException("invalid checksum bytes");
        }
        ByteBuffer bb = ByteBuffer.wrap(ndefChecksum);
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
        ndefChecksum = Arrays.copyOf(checkSumBytes, checkSumBytes.length);
    }


    private int calcCRC32() throws InvalidObjectException {
        byte[] bufCalc = getRawWithoutChecksum();
        Checksum checksum = new CRC32();
        checksum.update(bufCalc, 0, bufCalc.length);
        // get current checksum value
        return (int)checksum.getValue();

    }

    private byte[] getRawWithoutChecksum() throws InvalidObjectException {
        if (itcid == null || ds == null || itcHeader == null) {
            throw new InvalidObjectException("invalid data");
        }
        byte[] buf = new byte[NDEF_WITHOUT_DS_LENGTH + ds.length - NDEF_CHECKSUM_LENGTH];
        buf[DS_LENGTH_BYTE] = (byte)ds.length;
        System.arraycopy(ds, 0, buf, NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH, ds.length);
        byte[] itcidBuf = itcid.getRaw();
        System.arraycopy(itcidBuf, 0, buf, NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH + ds.length, NDEF_ITCID_LENGTH);
        byte[] itcHeaderBuf = itcHeader.getRaw();
        System.arraycopy(itcHeaderBuf, 0,  buf,
                NDEF_COUNTER_LENGTH + NDEF_DS_LENGTH_LENGTH+ ds.length + NDEF_ITCID_LENGTH, NDEF_ITC_HEADER_LENGTH);
        return buf;
    }

    public byte[] getRaw() throws InvalidObjectException {
        byte[] bufWithoutChecksum = getRawWithoutChecksum();
        if (ndefChecksum == null) {
            throw new InvalidObjectException("invalid checksum");
        }
        byte[] buf = new byte[bufWithoutChecksum.length + NDEF_CHECKSUM_LENGTH];
        System.arraycopy(bufWithoutChecksum, 0, buf, 0, bufWithoutChecksum.length);
        System.arraycopy(ndefChecksum, 0, buf, bufWithoutChecksum.length, NDEF_CHECKSUM_LENGTH);
        return buf;
    }
}
