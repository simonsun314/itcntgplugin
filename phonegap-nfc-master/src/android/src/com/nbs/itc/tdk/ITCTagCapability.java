package com.nbs.itc.tdk;

public class ITCTagCapability {
    public enum Cap {
        UNKNOWN((byte)0),
        COUNT((byte)1),
        PASSWORD((byte)(1<<1)),
        AB_CODE((byte)(1<<2)),
        NATIVE_DS_READ((byte)(1<<3)),
        NATIVE_DS_WRITE((byte)(1<<4)),
        ITC_DS_WRITE((byte)(1<<5));
        Cap(byte value) {
            this.value = value;
        }

        private final byte value;


        public byte getValue() {
            return value;
        }
    }
    private byte cap;

    public ITCTagCapability() {

    }

    public ITCTagCapability(byte cap) {
        this.cap = cap;
    }

    public boolean isEnabled(Cap cap) {
        if((cap.getValue() & this.cap) == 0) {
            return false;
        }
        return true;
    }

    public void enable(Cap cap, boolean enabled) {
        if (enabled) {
            this.cap |= cap.getValue();
        } else {
            this.cap &= ~(cap.getValue());
        }
    }


    public byte getRaw() {
        return cap;
    }


}
