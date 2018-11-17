
/**
 * @author Simon Sun
 * @version 1.0.0
 * @date 2018-11-17
 * Itc213 class encapsuate data of ntag213
 * Then it passed to fireevent to send data back
 * to javascript
 */
package com.chariotsolutions.nfc.plugin;


public class Itc213 {
  

    private String signature;
    private String uid;

    public Itc213() {
    }
    public String getSig(){
        return signature;
    }
    public void setSig(String sigstr){
        signature = sigstr;
    }
    public String getUid(){
        return uid;
    }
    public void setUid(String uidstr){
        uid = uidstr;
    }

  }