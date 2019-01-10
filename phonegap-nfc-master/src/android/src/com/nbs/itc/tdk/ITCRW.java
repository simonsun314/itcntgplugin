package com.nbs.itc.tdk;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import com.nbs.itc.ndk.NTag21x;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.security.InvalidParameterException;
import java.util.Arrays;

public class ITCRW {
    private final static int TLV_NDEF_LENGTH_BYTE = 1;
    private final static byte TLV_NDEF = 0x03;
    private final static byte TLV_TERMINATOR = (byte)0xfe;
    private final static String NDEF_ITC_TYPE = "itc";
    public static void writeTag(NTag21x tag, ITCData data, byte[] password, byte[] pack) throws Exception, BufCodec.BufValidator.InvalidChecksumException, InvalidObjectException {
        data.validateCheckSum();
        ITCNdef ndef = new ITCNdef(data.getITCID(), data.getDS(), data.getITCHeader());
        ndef.makeCheckSum();
        byte[] ndefBytes = ndef.getRaw();
        NdefRecord record = NdefRecord.createMime(NDEF_ITC_TYPE, ndefBytes);
        NdefMessage message = new NdefMessage(new NdefRecord[] {record});
        ndefBytes = message.toByteArray();
        byte[] ndefBytesWithTLV = new byte[ndefBytes.length + 3];
        ndefBytesWithTLV[0] = TLV_NDEF;
        ndefBytesWithTLV[TLV_NDEF_LENGTH_BYTE] = (byte)ndefBytes.length;
        ndefBytesWithTLV[ndefBytesWithTLV.length - 1] = TLV_TERMINATOR;
        System.arraycopy(ndefBytes, 0, ndefBytesWithTLV, 2, ndefBytes.length);
        int ndefLength = ndefBytesWithTLV.length;
        int ndefPageCount = ndefLength / 4;
        if (ndefLength % 4 > 0) {
            ndefPageCount += 1;
        }
        byte[] descriptorsBuf = data.getITCDescriptors().getRaw();
        byte[] checkSumBuf = data.getITCChecksum().getRaw();
        int descLen = descriptorsBuf.length + checkSumBuf.length;
        int descPageCount = descLen / 4;
        if (descLen % 4 > 0) {
            descPageCount += 1;
        }
        if((ndefPageCount + descPageCount) > ((tag.PAGE_USER_END - tag.PAGE_USER_START + 1))) {
            throw new InvalidParameterException("data is too long");
        }

        byte[] userMemory = new byte[(ndefPageCount + descPageCount) * 4];
        System.arraycopy(ndefBytesWithTLV, 0, userMemory, 0, ndefBytesWithTLV.length);

        System.arraycopy(descriptorsBuf, 0, userMemory,
                ndefPageCount * 4, descriptorsBuf.length);
        System.arraycopy(checkSumBuf, 0, userMemory,
                ndefPageCount * 4 + descriptorsBuf.length, checkSumBuf.length);
        int page = tag.PAGE_USER_START;
        for(int i = 0; i < userMemory.length; i=i+4) {
            byte[] byteToWrite = Arrays.copyOfRange(userMemory, i, i + 4);
            tag.write(page, byteToWrite);
            page++;
        }
        // static lock
        tag.write(tag.PAGE_STATIC_LOCK, new byte[] {
                0x00, 0x00, (byte)0xff, (byte)0xff
        });
        // dynamic lock
        tag.write(tag.PAGE_DYNAMIC_LOCK, new byte[] {
                (byte)0xff, (byte)(0x0f), (byte)(0xff), 0x00
        });
        // write password & pack
        tag.write(tag.PWD_CONFIG_PAGE, password);
        // write pack
        tag.write(tag.PACK_CONFIG_PAGE, new byte[] {
                pack[0], pack[1], 0x00, 0x00
        });
        // write config bytes
        tag.write(tag.PAGE_CONFIG_1, new byte[] {
                (byte)0xd0, (byte)0x00, (byte)0x00, (byte)0x00
        });
        tag.write(tag.PAGE_CONFIG_0, new byte[] {
                (byte)0x84, (byte)0x00, (byte)0x06, (byte)(tag.PAGE_USER_START + ndefPageCount + 1)
        });
    }

    public static ITCData readTag(NTag21x tag) throws IOException, FormatException, BufCodec.BufValidator.InvalidChecksumException {
        // read ndef message
        byte[] headPage = tag.read(tag.PAGE_USER_START);
        int ndefLength = headPage[TLV_NDEF_LENGTH_BYTE];
        ndefLength += 3; // add TLV Blocks
        int ndefPages = ndefLength / 4;
        if (ndefLength % 4 > 0) {
            ndefPages += 1;
        }
        byte[] ndefBytes = tag.fastRead(tag.PAGE_USER_START, tag.PAGE_USER_START + ndefPages);
        ndefBytes = Arrays.copyOfRange(ndefBytes, 2, ndefLength-1);
        NdefMessage ndefMessage = new NdefMessage(ndefBytes);

        NdefRecord[]  records = ndefMessage.getRecords();
        if (records.length != 1) {
            throw new FormatException("invalid records count");
        }
        NdefRecord itcRecord = records[0];
        String typeString = new String(itcRecord.getType(), "UTF-8");
        if(!typeString.equals(NDEF_ITC_TYPE)) {
            throw new FormatException("invalid record type");
        }
        byte[] ndefPayload = itcRecord.getPayload();

        ITCNdef ndef = ITCNdef.parsePayload(ndefPayload);
        ndef.validateCheckSum();
        byte[] ds = ndef.getDS();

        ITCID itcid = ndef.getITCID();
        ITCHeader header = ndef.getItcHeader();

        int descInfoPage = tag.PAGE_USER_START + ndefPages;
        byte[] descInfoBytes = tag.read(tag.PAGE_USER_START + ndefPages);
        int descLength = descInfoBytes[1] & 0xFF;
        int descPageCount = descLength / 4;
        if (descLength % 4 > 0) {
            descPageCount += 1;
        }
        ITCDescriptors descs = null;
        ITCChecksum checksum = null;
        try {
            byte[] encryptBuf = tag.fastRead(descInfoPage, descInfoPage + descPageCount + 1);
            descs = new ITCDescriptors(Arrays.copyOfRange(encryptBuf, 0, (descPageCount + 1) * 4));
            checksum = new ITCChecksum(Arrays.copyOfRange(encryptBuf,
                    (descPageCount + 1) * 4, encryptBuf.length));
        } catch (IOException ex) {
            // ignore
        }
        return new ITCData(itcid, header, ds, descs, checksum);
    }
}
