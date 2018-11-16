package com.nbs.itc.tdk;

import android.nfc.Tag;
import android.provider.Contacts;
import android.util.Log;

import com.nbs.itc.ndk.NTag213TagTamper;
import com.nbs.itc.ndk.NTag21x;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Arrays;

import static com.nbs.itc.tdk.BufCodec.bytesToHexString;
import static com.nbs.itc.tdk.UID.LEN_UID;

/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-10-15
 *
 */
public class ITCNTag213TagTamper extends NTag213TagTamper {
    private final static int PAGE_UID = 0x00;
    private final static int PAGE_ITC_DATA_START = 0x04;
    private final static int PAGE_ITC_DESC_END = 0x27;
    private final static int OFFSET_PAGE_ITCID = 0;
    private final static int OFFSET_PAGE_HEADER = 3;
    private final static int OFFSET_PAGE_DESCRIPTORS = 5;
    private final static int OFFSET_PAGE_CHECKSUM = 35;

    public ITCNTag213TagTamper(Tag tag) {
        super(tag);
    }

    public UID readUID() throws IOException {
        byte[] buf = super.read(PAGE_UID);
        return new UID(Arrays.copyOfRange(buf, 0, LEN_UID));
    }

    public ITCData readITCData() throws IOException {
        // read itcid & header
        byte[] unencryptedBuf = super.fastRead(PAGE_ITC_DATA_START + OFFSET_PAGE_ITCID,
                PAGE_ITC_DATA_START + ITCID.ITCID_PAGE_COUNT + ITCHeader.HEADER_PAGE_COUNT);
        ITCID itcid = new ITCID(Arrays.copyOfRange(unencryptedBuf, 0, ITCID.ITCID_PAGE_COUNT * 4));
        ITCHeader header = new ITCHeader(Arrays.copyOfRange(unencryptedBuf, ITCID.ITCID_PAGE_COUNT * 4, unencryptedBuf.length));
        ITCDescriptors descs = null;
        ITCChecksum checksum = null;
        try {
            byte[] encryptBuf = super.fastRead(PAGE_ITC_DATA_START + OFFSET_PAGE_DESCRIPTORS, PAGE_ITC_DESC_END);
            descs = new ITCDescriptors(Arrays.copyOfRange(encryptBuf, 0, ITCDescriptors.DESCRIPTOR_PAGE_COUNT * 4));
            checksum = new ITCChecksum(Arrays.copyOfRange(encryptBuf,
                    ITCDescriptors.DESCRIPTOR_PAGE_COUNT * 4, encryptBuf.length));
        } catch (IOException ex) {
            // ignore
        }
        return new ITCData(itcid, header, descs, checksum);
    }

    public void writeUserMemory(ITCData data) throws Exception, InvalidParameterException, BufCodec.BufValidator.InvalidChecksumException {
        data.validateCheckSum();
        byte[] itcidBuf = data.getITCID().getRaw();
        byte[] headerBuf = data.getItcHeader().getRaw();
        byte[] descriptorsBuf = data.getItcDescriptors().getRaw();
        byte[] checkSumBuf = data.getItcChecksum().getRaw();
        int len = itcidBuf.length + headerBuf.length + descriptorsBuf.length + checkSumBuf.length;
        if(len > ((PAGE_USER_END - PAGE_USER_START + 1) * 4)) {
            throw new InvalidParameterException("data is too long");
        }
        byte[] userMemory = new byte[len];
        System.arraycopy(itcidBuf, 0, userMemory, 0, itcidBuf.length);
        System.arraycopy(headerBuf, 0, userMemory, itcidBuf.length, headerBuf.length);
        System.arraycopy(descriptorsBuf, 0, userMemory,
                itcidBuf.length + headerBuf.length, descriptorsBuf.length);
        System.arraycopy(checkSumBuf, 0, userMemory,
                itcidBuf.length + headerBuf.length +descriptorsBuf.length, checkSumBuf.length);
        int page = PAGE_USER_START;
        for(int i = 0; i < userMemory.length; i=i+4) {
            byte[] byteToWrite = Arrays.copyOfRange(userMemory, i, i + 4);
            write(page, byteToWrite);
            page++;
        }
    }

    public static ITCNTag213TagTamper wrap(NTag21x nTag21x) {
        return new ITCNTag213TagTamper(nTag21x.tag);
    }
}