package com.nbs.itc.ndk;

import android.nfc.Tag;


import java.io.IOException;
import java.util.Arrays;

/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-10-25
 *
 */
public class NTag213 extends NTag21x {


    public NTag213(Tag tag) {
        super(tag);
        super.PAGE_USER_START = (byte) 0x04;
        super.PAGE_USER_END   = (byte) 0x27;
        super.AUTH0_CONFIG_PAGE = (byte) 0x29;
        super.ACCESS_CONFIG_PAGE = (byte) 0x2A;
        super.PWD_CONFIG_PAGE = (byte) 0x2B;
        super.PACK_CONFIG_PAGE = (byte) 0x2C;
        super.PAGE_CONFIG_0 = (byte) 0x29;
        super.PAGE_CONFIG_1 = (byte) 0x2A;
    }



}
