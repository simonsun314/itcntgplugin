package com.nbs.itc.tdk;


import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ITCDescriptor {
    public enum ITCDescriptorType {
        UNKNOWN(-1),
        SGTIN(0x00), SSCC(0x01), SGLN(0x02), LGTIN(0x03),
        PRODUCTION_DATE(0x20), BEST_BEFORE_DATE(0x21), EXPIRATION_DATE(0x22), PRODUCTION_TIME(0x23),
        BGTIN(0x30), BSSCC(0x31), EGLN(0x32), CAI(0x33), SERIAL_NUMBER(0x34), BATCH_LOT(0x35),
        ITC_UID(0x90), A_CODE(0x91), ITC_DIGITAL_SIGNATURE(0x92);
        ITCDescriptorType(int value) {
            this.value = value;
        }

        private final int value;


        public static ITCDescriptorType getDescriptorType(int v) {
            for(ITCDescriptorType i : values()) {
                if(i.getValue() == v) {
                    return i;
                }
            }
            Log.d("itc", String.format("UNKOWN Desc Type: %x", v));
            return UNKNOWN;
        }

        public int getValue() {
            return value;
        }
    }

    private ITCDescriptorType type;
    private byte[] content;

    public ITCDescriptor(ITCDescriptorType type, byte[] content) {
        this.type = type;
        this.content = Arrays.copyOf(content, content.length);
    }


    public ITCDescriptorType getType() {
        return type;
    }


    public byte[] getContent() {
        return Arrays.copyOf(content, content.length);
    }

    public String getContentString() {
        return new String(content);
    }

}
