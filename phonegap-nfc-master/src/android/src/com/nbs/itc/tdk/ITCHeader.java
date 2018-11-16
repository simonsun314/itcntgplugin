package com.nbs.itc.tdk;


/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-10-30
 *
 */
public class ITCHeader extends BufCodec {
    public final static int HEADER_PAGE_COUNT = 2;
    private final static int OFFSET_FORMAT_VERSION = 0;
    private final static int OFFSET_TAG_CAPABILITY = 1;
    private final static int OFFSET_DESC_COUNT = 8;
    private final static int OFFSET_DESC_LENGTH = 9;

    public ITCHeader(byte[] theBuf) {
        super(theBuf);
    }

    public ITCHeader() {
        super(new byte[HEADER_PAGE_COUNT * 4]);
    }


    public int getFormatVersion() {
        return (int) buf[OFFSET_FORMAT_VERSION];
    }

    public void setFormatVersion(int formatVersion) {
        buf[OFFSET_FORMAT_VERSION] = (byte) formatVersion;
    }

    public void setTagCapability(ITCTagCapability cap) {
        buf[OFFSET_TAG_CAPABILITY] = cap.getRaw();
    }

    public ITCTagCapability getTagCapability() {
        return new ITCTagCapability(buf[OFFSET_TAG_CAPABILITY]);
    }
}
