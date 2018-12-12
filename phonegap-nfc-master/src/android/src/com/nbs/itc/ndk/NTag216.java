package com.nbs.itc.ndk;

import android.nfc.Tag;


import java.io.IOException;
import java.util.Arrays;

/**
 * @author samuel zhou
 * @version 1.0.0
 * @date 2018-12-12
 *
 */
public class NTag216 extends NTag21x {


    public NTag216(Tag tag) {
        super(tag);
        super.PAGE_USER_START = 0x04;
        super.PAGE_USER_END   = 0xE1;
        super.AUTH0_CONFIG_PAGE = 0xE3;
        super.ACCESS_CONFIG_PAGE = 0xE4;
        super.PWD_CONFIG_PAGE = 0xE5;
        super.PACK_CONFIG_PAGE = 0xE6;
        super.PAGE_CONFIG_0 = 0xE3;
        super.PAGE_CONFIG_1 = 0xE4;
    }



}
