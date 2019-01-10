package com.nbs.itc.tdk;

import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.provider.Contacts;
import android.util.Log;

import com.nbs.itc.ndk.NTag216;
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
public class ITCNTag216 extends NTag216 {
    private final static int PAGE_UID = 0x00;

    public ITCNTag216(Tag tag) {
        super(tag);
    }

    public UID readUID() throws IOException {
        byte[] buf = super.read(PAGE_UID);
        return new UID(Arrays.copyOfRange(buf, 0, LEN_UID));
    }

    public ITCData readITCData() throws IOException, FormatException, BufCodec.BufValidator.InvalidChecksumException {
        return ITCRW.readTag(this);
    }

    public void writeITCData(ITCData data, byte[] password, byte[] pack) throws Exception, InvalidParameterException, BufCodec.BufValidator.InvalidChecksumException {
       ITCRW.writeTag(this, data, password, pack);
    }


    public static ITCNTag216 wrap(NTag21x nTag21x) {
        return new ITCNTag216(nTag21x.tag);
    }
}