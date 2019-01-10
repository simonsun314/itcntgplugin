package com.nbs.itc.tdk;


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.Checksum;


/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-10-15
 *
 */
public class ITCDescriptors extends BufCodec {
    public final static int DESCRIPTOR_PAGE_COUNT = 100;
    private final static int OFFSET_DESC_COUNT = 0;
    private final static int OFFSET_DESC_LENGTH = 1;
    private final static int OFFSET_DESC_START = 4;


    public ITCDescriptors(byte[] theBuf) {
        super(theBuf);
    }

    public ITCDescriptors() {
        super(new byte[DESCRIPTOR_PAGE_COUNT * 4]);
    }


    public ITCDescriptor[] getDescriptors() {
        int descriptorCount = buf[OFFSET_DESC_COUNT];
        if(descriptorCount == 0) {
            return null;
        }
        ITCDescriptor[] descriptors = new ITCDescriptor[descriptorCount];
        int pointer = 0;
        for(int i = 0; i < descriptorCount; i++) {
            int descriptorType = buf[OFFSET_DESC_START + pointer] & 0xFF;
            pointer++;
            int descriptorLen = buf[OFFSET_DESC_START + pointer];
            pointer += 3;
            byte[] content = Arrays.copyOfRange(
                    buf, OFFSET_DESC_START + pointer, OFFSET_DESC_START + pointer + descriptorLen);
            int descriptorPages = descriptorLen / 4;
            if(descriptorLen % 4 > 0) {
                descriptorPages += 1;
            }
            pointer += descriptorPages * 4;
            descriptors[i] = new ITCDescriptor(
                    ITCDescriptor.ITCDescriptorType.getDescriptorType(descriptorType), content);
        }
        return descriptors;
    }

    public void setDescriptors(ITCDescriptor[] descriptors) {
        int descriptorCount = descriptors.length;
        if(descriptorCount == 0) {
            return;
        }

        buf[OFFSET_DESC_COUNT] = (byte)descriptorCount;
        int pointer = 0;
        for(ITCDescriptor descriptor : descriptors) {
            int type = descriptor.getType().getValue();
            buf[OFFSET_DESC_START + pointer] = (byte) type;
            pointer++;
            byte[] content = descriptor.getContent();
            int length = content.length;
            buf[OFFSET_DESC_START + pointer] = (byte) length;
            pointer += 3;
            System.arraycopy(content, 0, buf, OFFSET_DESC_START + pointer, length);
            int descriptorPages = length / 4;
            if (length % 4 > 0) {
                descriptorPages += 1;
            }
            pointer += descriptorPages * 4;
        }
        buf[OFFSET_DESC_LENGTH] = (byte)pointer;
        buf = Arrays.copyOf(buf, pointer + OFFSET_DESC_START);
    }

}
