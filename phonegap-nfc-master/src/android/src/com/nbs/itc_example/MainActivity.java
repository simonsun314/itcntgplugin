package com.nbs.itc_example;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.nbs.itc.ndk.NTag213TagTamper;
import com.nbs.itc.ndk.NTag21x;
import com.nbs.itc.tdk.ITCData;
import com.nbs.itc.tdk.ITCDescriptor;
import com.nbs.itc.tdk.ITCDescriptors;
import com.nbs.itc.tdk.ITCHeader;
import com.nbs.itc.tdk.ITCID;
import com.nbs.itc.tdk.ITCNTag213;
import com.nbs.itc.tdk.ITCNTag213TagTamper;
import com.nbs.itc.tdk.ITCTagCapability;
import com.nbs.itc.tdk.UID;



import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

// using wildcard imports so we can support Cordova 3.x
import org.apache.cordova.*; // Cordova 3.x

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.net.Uri;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.TagTechnology;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;


public class MainActivity extends AppCompatActivity {
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showToast(intent.getAction());
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED) ||
                intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED) ||
                intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            processNfcA(tag);

        }

    }

    private void readNTag213TT(ITCNTag213TagTamper nTag213TT) {
        try {
            Log.d("itc", "Connected NTag213TT");
            byte[] signature = nTag213TT.readSignature();
            Log.d("itc", "Signature:" + bytesToHex(signature));
            // uid
            UID uid = nTag213TT.readUID();
            uid.validateCheckSum();
            Log.d("itc", "UID:" + bytesToHex(uid.getRaw()));
            // itcdata
            ITCData itcData = nTag213TT.readITCData();
            Log.d("itc", "ITCData is loaded");

            if (itcData.getItcChecksum() != null) {
                itcData.validateCheckSum();
                Log.d("itc", "ITCData verified");
            }

            // parse ITCID
            ITCID itcid = itcData.getITCID();
            // verify ITCID
            itcid.validateCheckSum();
            Log.d("itc", "ITCID CheckSum verified");
            Log.d("itc", String.format("Customer Code: %X", itcid.getCustomerCode()));
            Log.d("itc", String.format("Commodity Code: %X", itcid.getCommodityCode()));
            Log.d("itc", "Instance Code: "+ bytesToHex(itcid.getInstanceCode()));

            // parse header
            ITCHeader itcHeader = itcData.getItcHeader();
            Log.d("itc", String.format("formatVersion: %d", itcHeader.getFormatVersion()));
            ITCTagCapability capability = itcHeader.getTagCapability();
            boolean passwordProtected = false;
            for(ITCTagCapability.Cap cap : ITCTagCapability.Cap.values()) {
                if (cap == ITCTagCapability.Cap.UNKNOWN) {
                    continue;
                }
                if (cap == ITCTagCapability.Cap.PASSWORD && capability.isEnabled(cap)) {
                    passwordProtected = true;
                }
                Log.d("itc", String.format("Capability[%s]: %b", cap.name(), capability.isEnabled(cap)));
            }

            if(passwordProtected) {
                // password auth
                nTag213TT.authenticatePwd(new byte[]{
                        0x11, 0x22, 0x33, 0x44
                }, new byte[]{
                        (byte) 0xAA, (byte) 0xBB
                });
                Log.d("itc", "Password authenticated");

                // reload itcdata
                itcData = nTag213TT.readITCData();
                Log.d("itc", "ITCData is reloaded");
                itcData.validateCheckSum();
                Log.d("itc", "ITCData verified");
            }

            ITCDescriptors descs = itcData.getItcDescriptors();


            ITCDescriptor[] descList = descs.getDescriptors();
            for (ITCDescriptor desc : descList) {
                Log.d("itc", String.format("[desc]%s: %s", desc.getType().name(), desc.getContentString()));
            }
            byte[] version = nTag213TT.getVersion();
            Log.d("itc", "Version:" + bytesToHex(version));
            byte[] configBytes = nTag213TT.getConfigBytes();
            Log.d("itc", "Config Bytes:" + bytesToHex(configBytes));
            byte[] counter = nTag213TT.readCounter();
            Log.d("itc", "Counter:" + bytesToHex(counter));
            byte[] ttStatus = nTag213TT.readTTStatus();
            Log.d("itc", "TTStatus:" + bytesToHex(ttStatus));
        } catch (Exception ex) {
            Log.e("itc", "Exception" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void readNTag213(ITCNTag213 nTag213) {
        try {
            Log.d("itc", "Connected NTag213");
            byte[] signature = nTag213.readSignature();
            Log.d("itc", "Signature:" + bytesToHex(signature));
            // uid
            UID uid = nTag213.readUID();
            uid.validateCheckSum();
            Log.d("itc", "UID:" + bytesToHex(uid.getRaw()));
            // itcdata
            ITCData itcData = nTag213.readITCData();
            Log.d("itc", "ITCData is loaded");

            if (itcData.getItcChecksum() != null) {
                itcData.validateCheckSum();
                Log.d("itc", "ITCData verified");
            }

            // parse ITCID
            ITCID itcid = itcData.getITCID();
            // verify ITCID
            itcid.validateCheckSum();
            Log.d("itc", "ITCID CheckSum verified");
            Log.d("itc", String.format("Customer Code: %X", itcid.getCustomerCode()));
            Log.d("itc", String.format("Commodity Code: %X", itcid.getCommodityCode()));
            Log.d("itc", "Instance Code: "+ bytesToHex(itcid.getInstanceCode()));

            // parse header
            ITCHeader itcHeader = itcData.getItcHeader();
            Log.d("itc", String.format("formatVersion: %d", itcHeader.getFormatVersion()));
            ITCTagCapability capability = itcHeader.getTagCapability();
            boolean passwordProtected = false;
            for(ITCTagCapability.Cap cap : ITCTagCapability.Cap.values()) {
                if (cap == ITCTagCapability.Cap.UNKNOWN) {
                    continue;
                }
                if (cap == ITCTagCapability.Cap.PASSWORD && capability.isEnabled(cap)) {
                    passwordProtected = true;
                }
                Log.d("itc", String.format("Capability[%s]: %b", cap.name(), capability.isEnabled(cap)));
            }

            if(passwordProtected) {
                // password auth
                nTag213.authenticatePwd(new byte[]{
                        0x11, 0x22, 0x33, 0x44
                }, new byte[]{
                        (byte) 0xAA, (byte) 0xBB
                });
                Log.d("itc", "Password authenticated");

                // reload itcdata
                itcData = nTag213.readITCData();
                Log.d("itc", "ITCData is reloaded");
                itcData.validateCheckSum();
                Log.d("itc", "ITCData verified");
            }

            ITCDescriptors descs = itcData.getItcDescriptors();


            ITCDescriptor[] descList = descs.getDescriptors();
            for (ITCDescriptor desc : descList) {
                Log.d("itc", String.format("[desc]%s: %s", desc.getType().name(), desc.getContentString()));
            }
            byte[] version = nTag213.getVersion();
            Log.d("itc", "Version:" + bytesToHex(version));
            byte[] configBytes = nTag213.getConfigBytes();
            Log.d("itc", "Config Bytes:" + bytesToHex(configBytes));
            byte[] counter = nTag213.readCounter();
            Log.d("itc", "Counter:" + bytesToHex(counter));
        } catch (Exception ex) {
            Log.e("itc", "Exception" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void writeITCUserMemory(ITCNTag213TagTamper nTag213TT) throws Exception {
        // ITCID
        ITCID itcid = new ITCID();
        itcid.setServiceType(ITCID.ServiceType.PUBLIC);
        itcid.setCustomerCode((short)0x1234);
        itcid.setCommodityCode((short)0x5678);
        itcid.setInstanceCode(ITCID.hashToInstanceCode(1));
        itcid.makeCheckSum();

        // ITCHeader
        ITCHeader header = new ITCHeader();
        ITCTagCapability cap = new ITCTagCapability();
        cap.enable(ITCTagCapability.Cap.COUNT, true);
        cap.enable(ITCTagCapability.Cap.NATIVE_DS_READ, true);
        cap.enable(ITCTagCapability.Cap.PASSWORD, true);
        cap.enable(ITCTagCapability.Cap.NATIVE_DS_WRITE, true);
        cap.enable(ITCTagCapability.Cap.AB_CODE, true);
        header.setTagCapability(cap);
        header.setFormatVersion(0x01);

        // Descriptors
        ITCDescriptors descs = new ITCDescriptors();
        ITCDescriptor[] descList = new ITCDescriptor[4];
        String sgtin = "urn:epc:id:sgtin:0614141.112345.400";
        descList[0] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.SGTIN, sgtin.getBytes());
        String bestBeforeDate = "180826";
        descList[1] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.BEST_BEFORE_DATE, bestBeforeDate.getBytes());
        String expireDate = "180830";
        descList[2] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.EXPIRATION_DATE, expireDate.getBytes());
        descList[3] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.A_CODE, new byte[] {
                0x00, 0x01, 0x02, 0x03
        });
        descs.setDescriptors(descList);


        ITCData data = new ITCData(itcid, header, descs);
        data.makeCheckSum();
        nTag213TT.writeUserMemory(data);
    }

    private void writeITCUserMemory(ITCNTag213 nTag213) throws Exception {
        // ITCID
        ITCID itcid = new ITCID();
        itcid.setServiceType(ITCID.ServiceType.PUBLIC);
        itcid.setCustomerCode((short)0x1234);
        itcid.setCommodityCode((short)0x5678);
        itcid.setInstanceCode(ITCID.hashToInstanceCode(1));
        itcid.makeCheckSum();

        // ITCHeader
        ITCHeader header = new ITCHeader();
        header.setFormatVersion(0x01);
        ITCTagCapability cap = new ITCTagCapability();
        cap.enable(ITCTagCapability.Cap.COUNT, true);
        cap.enable(ITCTagCapability.Cap.NATIVE_DS_READ, true);
        cap.enable(ITCTagCapability.Cap.PASSWORD, true);
        cap.enable(ITCTagCapability.Cap.ITC_DS_WRITE, true);
        header.setTagCapability(cap);

        // Descriptors
        ITCDescriptors descs = new ITCDescriptors();
        ITCDescriptor[] descList = new ITCDescriptor[4];
        String sgtin = "urn:epc:id:sgtin:0614141.112345.400";
        descList[0] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.SGTIN, sgtin.getBytes());
        String bestBeforeDate = "180826";
        descList[1] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.BEST_BEFORE_DATE, bestBeforeDate.getBytes());
        String expireDate = "180830";
        descList[2] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.EXPIRATION_DATE, expireDate.getBytes());
        descList[3] = new ITCDescriptor(ITCDescriptor.ITCDescriptorType.ITC_DIGITAL_SIGNATURE, new byte[] {
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
                0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37
        });

        descs.setDescriptors(descList);

        ITCData data = new ITCData(itcid, header, descs);
        data.makeCheckSum();
        nTag213.writeUserMemory(data);
    }


    private void writeNTag213TT(ITCNTag213TagTamper nTag213TT) {
        try {
            Log.d("itc", "Connected NTag213TT");
            // write tt message
            nTag213TT.writeCustomTTMessage(new byte[] {
                    0x11, 0x22, 0x33, 0x44
            });
            Log.d("itc", "TT Message is written");

            writeITCUserMemory(nTag213TT);
            Log.d("itc", "User Memory is written");

            // write static lock 0xff 0xff
            nTag213TT.write(0x02, new byte[] {
                    0x00, 0x00, (byte)0xff, (byte)0xff
            });
            Log.d("itc", "static lock is written");
            nTag213TT.write(0x28, new byte[] {
                    (byte)0xFF, (byte)(0x0F), (byte)(0x6F), 0x00
            });
            Log.d("itc", "dynamic lock is written");
            // write password
            nTag213TT.write(0x2B, new byte[] {
                    (byte)0x11, (byte)0x22, (byte)0x33, 0x44
            });
            Log.d("itc", "password is written");
            // write pack
            nTag213TT.write(0x2C, new byte[] {
                    (byte)0xAA, (byte)0xBB, (byte)0x00, 0x00
            });
            Log.d("itc", "pack is written");
            // write CFG1(access)
            nTag213TT.write(0x2A, new byte[] {
                    (byte)0xDF, (byte)0x00, (byte)0x00, 0x00
            });
            Log.d("itc", "access is written");
            // write CFG0
            nTag213TT.write(0x29, new byte[] {
                    (byte)0x00, (byte)0x06, (byte)0x26, 0x09
            });
            Log.d("itc", "CONFIG_0 is written");
            nTag213TT.writeSignature(new byte[] {
                      0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                      0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17,
                      0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27,
                      0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37
            });

            Log.d("itc", "signature is written");
            nTag213TT.lockSignature(NTag213TagTamper.SigLockType.LOCK_PERMANENTLY);
            Log.d("itc", "signature is permanently locked");
        } catch (Exception ex) {
            Log.e("itc", "Exception" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void writeNTag213(ITCNTag213 nTag213) {
        try {
            Log.d("itc", "Connected NTag213");

            writeITCUserMemory(nTag213);
            Log.d("itc", "User Memory is written");

            // write static lock 0xff 0xff
            nTag213.write(0x02, new byte[] {
                    0x00, 0x00, (byte)0xff, (byte)0xff
            });
            Log.d("itc", "static lock is written");
            nTag213.write(0x28, new byte[] {
                    (byte)0xFF, (byte)(0x0F), (byte)(0x6F), 0x00
            });
            Log.d("itc", "dynamic lock is written");
            // write password
            nTag213.write(0x2B, new byte[] {
                    (byte)0x11, (byte)0x22, (byte)0x33, 0x44
            });
            Log.d("itc", "password is written");
            // write pack
            nTag213.write(0x2C, new byte[] {
                    (byte)0xAA, (byte)0xBB, (byte)0x00, 0x00
            });
            Log.d("itc", "pack is written");
            // write CFG1(access)
            nTag213.write(0x2A, new byte[] {
                    (byte)0xDF, (byte)0x00, (byte)0x00, 0x00
            });
            Log.d("itc", "access is written");
            // write CFG0
            nTag213.write(0x29, new byte[] {
                    (byte)0x00, (byte)0x06, (byte)0x26, 0x09
            });
            Log.d("itc", "CONFIG_0 is written");
        } catch (Exception ex) {
            Log.e("itc", "Exception" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void processNfcA(Tag tag) {
        boolean read = true;
        try {
            NTag21x nTag21x = new NTag21x(tag);
            nTag21x.connect();
            byte[] version = nTag21x.getVersion();
            String sVersion = bytesToHex(version);
            if(sVersion.equals(bytesToHex(NTag21x.TAG_VERSION_NTAG_213))) {
                ITCNTag213 nTag213 = ITCNTag213.wrap(nTag21x);
               if(read) {
                   readNTag213(nTag213);
               } else {
                   writeNTag213(nTag213);
               }
            } else if (sVersion.equals(bytesToHex(NTag21x.TAG_VERSION_NTAG_213_TAG_TAMPER))){
                ITCNTag213TagTamper nTag213tt = ITCNTag213TagTamper.wrap(nTag21x);
                if(read) {
                    readNTag213TT(nTag213tt);
                } else {
                    writeNTag213TT(nTag213tt);
                }
            }
            nTag21x.close();
        } catch(Exception ex) {
            Log.d("itc", ex.getMessage());
        }

    }

    protected void showToast(String msg) {
        Log.d("Toast","[Toast]" + msg);
        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpForegroundDispatchSystem();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatchSystem();
    }


    private void setUpForegroundDispatchSystem() {
        this.pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        this.nfcAdapter.enableForegroundDispatch(this, this.pendingIntent, null, null);
    }

    private void disableForegroundDispatchSystem() {
        this.nfcAdapter.disableForegroundDispatch(this);
    }

    /**m
     * Parse byte[] to hexadecimal string format
     * @param bytes the array of bytes
     * @return hexadecimal String
     */
    private String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
