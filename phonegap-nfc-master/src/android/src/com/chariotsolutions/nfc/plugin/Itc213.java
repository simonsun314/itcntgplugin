
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
    private String errCode;
    private String CustomerCode;
    private String CommodityCode;
    private String InstanceCode;
    private String PassProt;
    private String itcidval;

    public Itc213() {
        errCode = "";
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
    public String getErrCode(){
        return errCode;
    }
    public void setErrCode(String errstr){
        errCode = errstr;
    }

    public String getCustomCode(){
        return CustomerCode;
    }
    public void setCustomCode(String customstr){
        CustomerCode = customstr;
    }
    public String getCommodityCode(){
        return CommodityCode;
    }
    public void setCommodityCode(String comstr){
        CommodityCode = comstr;
    }
    public String getInstanceCode(){
        return InstanceCode;
    }
    public void setInstanceCode(String Instntstr){
        InstanceCode = Instntstr;
    }
    public String getPassProtStatus(){
        return PassProt;
    }
    public void setPassProtStatus(String passprotstat){
        PassProt = passprotstat;
    }

    public String getItcid(){
        return itcidval;
    }
    public void setItcid(String itcidstr){
        itcidval = itcidstr;
    }

  }