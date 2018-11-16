package com.nbs.itc.tdk;

import android.util.Log;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-10-15
 *
 */
public class ITCID extends BufCodec implements BufCodec.BufValidator {

    public enum ServiceType {
        PRIVATE((byte)0), PUBLIC((byte)1);
        ServiceType(byte value) {
            this.value = value;
        }

        private final byte value;


        public static ServiceType getServiceType(byte v) {
            for(ServiceType i : values()) {
                if(i.getValue() == v) {
                    return i;
                }
            }
            throw new RuntimeException("unknown service type");
        }

        public byte getValue() {
            return value;
        }
    }


    public final static int ITCID_LENGTH = 12;
    public final static int INSTANCE_CODE_LENGTH = 6; // bytes
    private final static int OFFSET_SERVICE_TYPE = 0; // 1 bit
    private final static int OFFSET_CUSTOMER_CODE = 1; // 2 bytes
    private final static int OFFSET_COMMODITY_CODE = 3; // 2 bytes
    private final static int OFFSET_INSTANCE_CODE = 5; // 6 bytes
    private final static int OFFSET_BCC = 11;
    public final static int ITCID_PAGE_COUNT = 3;



    public ITCID(byte[] theBuf) {
        super(theBuf);
    }

    public ITCID() {
        super(new byte[ITCID_LENGTH]);
    }

    @Override
    public void validateCheckSum() throws InvalidChecksumException {
        if(buf.length != ITCID_LENGTH) {
            throw new InvalidChecksumException("invalid ITCID len");
        }
        if(bccChecksum(Arrays.copyOfRange(buf, 0, OFFSET_BCC)) != buf[OFFSET_BCC]) {
            throw new InvalidChecksumException("invalid BCC");
        }
    }


    public ServiceType getServiceType() {
        byte t = buf[OFFSET_SERVICE_TYPE];
        t >>= 3;
        return ServiceType.getServiceType(t);
    }

    public void setServiceType(ServiceType type) {
        byte b = type.getValue();
        b <<= 3;
        buf[OFFSET_SERVICE_TYPE] |= b;
    }

    public short getCustomerCode() {
        byte[] customerCode = Arrays.copyOfRange(buf, OFFSET_CUSTOMER_CODE, OFFSET_CUSTOMER_CODE + 2);
        ByteBuffer bb = ByteBuffer.wrap(customerCode);
        return bb.getShort();
    }

    public void setCustomerCode(short customerCode) {
        buf[OFFSET_CUSTOMER_CODE + 1] = (byte)(customerCode & 0xff);
        buf[OFFSET_CUSTOMER_CODE] = (byte)((customerCode >> 8) & 0xff);
    }

    public short getCommodityCode() {
        byte[] customerCode = Arrays.copyOfRange(buf, OFFSET_COMMODITY_CODE, OFFSET_COMMODITY_CODE + 2);
        ByteBuffer bb = ByteBuffer.wrap(customerCode);
        return bb.getShort();
    }

    public void setCommodityCode(short commodityCode) {
        buf[OFFSET_COMMODITY_CODE + 1] = (byte)(commodityCode & 0xff);
        buf[OFFSET_COMMODITY_CODE] = (byte)((commodityCode >> 8) & 0xff);
    }

    public byte[] getInstanceCode() {
        return Arrays.copyOfRange(buf, OFFSET_INSTANCE_CODE, OFFSET_INSTANCE_CODE + INSTANCE_CODE_LENGTH);
    }

    // set 6 bytes of instance code
    public void setInstanceCode(byte[] instanceCode) {
        if(instanceCode.length != INSTANCE_CODE_LENGTH) {
            throw new RuntimeException("invalid instance code length");
        }
        System.arraycopy(instanceCode, 0, buf, OFFSET_INSTANCE_CODE, INSTANCE_CODE_LENGTH);

    }

    public void makeCheckSum() {
        byte bccCheckSum = bccChecksum(Arrays.copyOfRange(buf, 0, OFFSET_BCC));
        buf[OFFSET_BCC] = bccCheckSum;
    }

    public static byte[] hashToInstanceCode(long index) {
        if(index > 0xffffffffffffl) {
            throw new RuntimeException("invalid index");
        }
        try {
            ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
            buffer.putLong(index);
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(buffer.array());
            byte[] result = crypt.digest();
            return Arrays.copyOf(result, INSTANCE_CODE_LENGTH);
        } catch(Exception ex) {
            return null;
        }
    }
}
