package com.chariotsolutions.nfc.plugin;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
// import android.support.v7.app.AppCompatActivity;
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


public class NfcPlugin extends CordovaPlugin implements NfcAdapter.OnNdefPushCompleteCallback {
    private static final String REGISTER_MIME_TYPE = "registerMimeType";
    private static final String REMOVE_MIME_TYPE = "removeMimeType";
    private static final String REGISTER_NDEF = "registerNdef";
    private static final String REMOVE_NDEF = "removeNdef";
    private static final String REGISTER_NDEF_FORMATABLE = "registerNdefFormatable";
    private static final String REGISTER_DEFAULT_TAG = "registerTag";
    private static final String REMOVE_DEFAULT_TAG = "removeTag";
    private static final String WRITE_TAG = "writeTag";
    private static final String READ_AUTH_TAG = "readAuthTag";
    private static final String MAKE_READ_ONLY = "makeReadOnly";
    private static final String ERASE_TAG = "eraseTag";
    private static final String SHARE_TAG = "shareTag";
    private static final String UNSHARE_TAG = "unshareTag";
    private static final String HANDOVER = "handover"; // Android Beam
    private static final String STOP_HANDOVER = "stopHandover";
    private static final String ENABLED = "enabled";
    private static final String INIT = "init";
    private static final String SHOW_SETTINGS = "showSettings";

    private static final String NDEF = "ndef";
    private static final String NDEF_MIME = "ndef-mime";
    private static final String NDEF_FORMATABLE = "ndef-formatable";
    private static final String TAG_DEFAULT = "tag";
    private static final String ITC_MESSAGE = "itc-msg";

    private static final String READER_MODE = "readerMode";
    private static final String DISABLE_READER_MODE = "disableReaderMode";

    // TagTechnology IsoDep, NfcA, NfcB, NfcV, NfcF, MifareClassic, MifareUltralight
    private static final String CONNECT = "connect";
    private static final String CLOSE = "close";
    private static final String TRANSCEIVE = "transceive";
    private TagTechnology tagTechnology = null;
    private Class<?> tagTechnologyClass;

    private static final String CHANNEL = "channel";

    private static final String STATUS_NFC_OK = "NFC_OK";
    private static final String STATUS_NO_NFC = "NO_NFC";
    private static final String STATUS_NFC_DISABLED = "NFC_DISABLED";
    private static final String STATUS_NDEF_PUSH_DISABLED = "NDEF_PUSH_DISABLED";

    private static final String TAG = "NfcPlugin";
    private final List<IntentFilter> intentFilters = new ArrayList<>();
    private final ArrayList<String[]> techLists = new ArrayList<>();

    private NdefMessage p2pMessage = null;
    private PendingIntent pendingIntent = null;

    private Intent savedIntent = null;

    private CallbackContext readerModeCallback;
    private CallbackContext channelCallback;
    private CallbackContext shareTagCallback;
    private CallbackContext handoverCallback;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        Log.d(TAG, "execute " + action);

        // showSettings can be called if NFC is disabled
        // might want to skip this if NO_NFC
        if (action.equalsIgnoreCase(SHOW_SETTINGS)) {
            showSettings(callbackContext);
            return true;
        }

        // the channel is set up when the plugin starts
        if (action.equalsIgnoreCase(CHANNEL)) {
            channelCallback = callbackContext;
            return true; // short circuit
        }

        // allow reader mode to be disabled even if nfc is disabled
        if (action.equalsIgnoreCase(DISABLE_READER_MODE)) {
            disableReaderMode(callbackContext);
            return true; // short circuit
        }

        if (!getNfcStatus().equals(STATUS_NFC_OK)) {
            callbackContext.error(getNfcStatus());
            return true; // short circuit
        }

        createPendingIntent();

        if (action.equalsIgnoreCase(READER_MODE)) {
            int flags = data.getInt(0);
            readerMode(flags, callbackContext);

        } else if (action.equalsIgnoreCase(REGISTER_MIME_TYPE)) {
            registerMimeType(data, callbackContext);

        } else if (action.equalsIgnoreCase(REMOVE_MIME_TYPE)) {
            removeMimeType(data, callbackContext);

        } else if (action.equalsIgnoreCase(REGISTER_NDEF)) {
            registerNdef(callbackContext);

        } else if (action.equalsIgnoreCase(REMOVE_NDEF)) {
            removeNdef(callbackContext);

        } else if (action.equalsIgnoreCase(REGISTER_NDEF_FORMATABLE)) {
            registerNdefFormatable(callbackContext);

        } else if (action.equals(REGISTER_DEFAULT_TAG)) {
            registerDefaultTag(callbackContext);

        } else if (action.equals(REMOVE_DEFAULT_TAG)) {
            removeDefaultTag(callbackContext);

        } else if (action.equalsIgnoreCase(WRITE_TAG)) {
            writeTag(data, callbackContext);
        }
        else if (action.equalsIgnoreCase(READ_AUTH_TAG)) {
            Log.d(TAG, "execute read auth\n" );

            readAuthTag(data, callbackContext);
    
            
        } else if (action.equalsIgnoreCase(MAKE_READ_ONLY)) {
            makeReadOnly(callbackContext);

        } else if (action.equalsIgnoreCase(ERASE_TAG)) {
            eraseTag(callbackContext);

        } else if (action.equalsIgnoreCase(SHARE_TAG)) {
            shareTag(data, callbackContext);

        } else if (action.equalsIgnoreCase(UNSHARE_TAG)) {
            unshareTag(callbackContext);

        } else if (action.equalsIgnoreCase(HANDOVER)) {
            handover(data, callbackContext);

        } else if (action.equalsIgnoreCase(STOP_HANDOVER)) {
            stopHandover(callbackContext);

        } else if (action.equalsIgnoreCase(INIT)) {
            init(callbackContext);

        } else if (action.equalsIgnoreCase(ENABLED)) {
            // status is checked before every call
            // if code made it here, NFC is enabled
            callbackContext.success(STATUS_NFC_OK);

        } else if (action.equalsIgnoreCase(CONNECT)) {
            String tech = data.getString(0);
            int timeout = data.optInt(1, -1);
            connect(tech, timeout, callbackContext);

        } else if (action.equalsIgnoreCase(TRANSCEIVE)) {
            CordovaArgs args = new CordovaArgs(data); // execute is using the old signature with JSON data

            byte[] command = args.getArrayBuffer(0);
            transceive(command, callbackContext);

        } else if (action.equalsIgnoreCase(CLOSE)) {
            close(callbackContext);

        } else {
            // invalid action
            return false;
        }

        return true;
    }

    private String getNfcStatus() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        if (nfcAdapter == null) {
            return STATUS_NO_NFC;
        } else if (!nfcAdapter.isEnabled()) {
            return STATUS_NFC_DISABLED;
        } else {
            return STATUS_NFC_OK;
        }
    }

    private void readerMode(int flags, CallbackContext callbackContext) {
        Bundle extras = new Bundle(); // not used
        readerModeCallback = callbackContext;
        getActivity().runOnUiThread(() -> {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
            nfcAdapter.enableReaderMode(getActivity(), callback, flags, extras);
        });

    }

    private void disableReaderMode(CallbackContext callbackContext) {
        getActivity().runOnUiThread(() -> {
            readerModeCallback = null;
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
            if (nfcAdapter != null) {
                nfcAdapter.disableReaderMode(getActivity());
            }
            callbackContext.success();
        });
    }

    private NfcAdapter.ReaderCallback callback = new NfcAdapter.ReaderCallback() {
        @Override
        public void onTagDiscovered(Tag tag) {

            JSONObject json;

            // If the tag supports Ndef, try and return an Ndef message
            List<String> techList = Arrays.asList(tag.getTechList());
            if (techList.contains(Ndef.class.getName())) {
                Ndef ndef = Ndef.get(tag);
                json = Util.ndefToJSON(ndef);
            } else {
                json = Util.tagToJSON(tag);
            }

            PluginResult result = new PluginResult(PluginResult.Status.OK, json);
            result.setKeepCallback(true);
            readerModeCallback.sendPluginResult(result);

        }
    };

    private void registerDefaultTag(CallbackContext callbackContext) {
        addTagFilter();
        restartNfc();
        callbackContext.success();
    }

    private void removeDefaultTag(CallbackContext callbackContext) {
        removeTagFilter();
        restartNfc();
        callbackContext.success();
    }

    private void registerNdefFormatable(CallbackContext callbackContext) {
        addTechList(new String[]{NdefFormatable.class.getName()});
        restartNfc();
        callbackContext.success();
    }

    private void registerNdef(CallbackContext callbackContext) {
        addTechList(new String[]{Ndef.class.getName()});
        restartNfc();
        callbackContext.success();
    }

    private void removeNdef(CallbackContext callbackContext) {
        removeTechList(new String[]{Ndef.class.getName()});
        restartNfc();
        callbackContext.success();
    }

    private void unshareTag(CallbackContext callbackContext) {
        p2pMessage = null;
        stopNdefPush();
        shareTagCallback = null;
        callbackContext.success();
    }

    private void init(CallbackContext callbackContext) {
        Log.d(TAG, "Enabling plugin " + getIntent());

        startNfc();
        if (!recycledIntent()) {
            parseMessage();
        }
        callbackContext.success();
    }

    private void removeMimeType(JSONArray data, CallbackContext callbackContext) throws JSONException {
        String mimeType = data.getString(0);
        removeIntentFilter(mimeType);
        restartNfc();
        callbackContext.success();
    }

    private void registerMimeType(JSONArray data, CallbackContext callbackContext) throws JSONException {
        String mimeType = "";
        try {
            mimeType = data.getString(0);
            intentFilters.add(createIntentFilter(mimeType));
            restartNfc();
            callbackContext.success();
        } catch (MalformedMimeTypeException e) {
            callbackContext.error("Invalid MIME Type " + mimeType);
        }
    }

    // Cheating and writing an empty record. We may actually be able to erase some tag types.
    private void eraseTag(CallbackContext callbackContext) {
        Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord[] records = {
                new NdefRecord(NdefRecord.TNF_EMPTY, new byte[0], new byte[0], new byte[0])
        };
        writeNdefMessage(new NdefMessage(records), tag, callbackContext);
    }

    private void writeTag(JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (getIntent() == null) {  // TODO remove this and handle LostTag
            callbackContext.error("Failed to write tag, received null intent");
        }

        Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
        writeNdefMessage(new NdefMessage(records), tag, callbackContext);
    }

    private void readAuthTag(JSONArray data, CallbackContext callbackContext) throws JSONException {
        if (getIntent() == null) {  // TODO remove this and handle LostTag
            callbackContext.error("Failed to write tag, received null intent");
        }
       
        Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        // NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
        //  byte[] payload = Util.jsonTobyteRecords(data.getString(0));
        //writeNdefMessage(new NdefMessage(records), tag, callbackContext);
        Log.d(TAG, "again parse nfc");

        CordovaArgs args = new CordovaArgs(data); // execute is using the old signature with JSON data

        byte[] command = args.getArrayBuffer(0);
        processNfcAwithAuth(tag,command,callbackContext);
        // processNfcAwithAuth(tag,payload,callbackContext);
    }

    private void writeNdefMessage(final NdefMessage message, final Tag tag, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                Ndef ndef = Ndef.get(tag);
                if (ndef != null) {
                    ndef.connect();

                    if (ndef.isWritable()) {
                        int size = message.toByteArray().length;
                        if (ndef.getMaxSize() < size) {
                            callbackContext.error("Tag capacity is " + ndef.getMaxSize() +
                                    " bytes, message is " + size + " bytes.");
                        } else {
                            ndef.writeNdefMessage(message);
                            callbackContext.success();
                        }
                    } else {
                        callbackContext.error("Tag is read only");
                    }
                    ndef.close();
                } else {
                    NdefFormatable formatable = NdefFormatable.get(tag);
                    if (formatable != null) {
                        formatable.connect();
                        formatable.format(message);
                        callbackContext.success();
                        formatable.close();
                    } else {
                        callbackContext.error("Tag doesn't support NDEF");
                    }
                }
            } catch (FormatException e) {
                callbackContext.error(e.getMessage());
            } catch (TagLostException e) {
                callbackContext.error(e.getMessage());
            } catch (IOException e) {
                callbackContext.error(e.getMessage());
            }
        });
    }

    private void makeReadOnly(final CallbackContext callbackContext) {

        if (getIntent() == null) { // Lost Tag
            callbackContext.error("Failed to make tag read only, received null intent");
            return;
        }

        final Tag tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag == null) {
            callbackContext.error("Failed to make tag read only, tag is null");
            return;
        }

        cordova.getThreadPool().execute(() -> {
            boolean success = false;
            String message = "Could not make tag read only";

            Ndef ndef = Ndef.get(tag);

            try {
                if (ndef != null) {

                    ndef.connect();

                    if (!ndef.isWritable()) {
                        message = "Tag is not writable";
                    } else if (ndef.canMakeReadOnly()) {
                        success = ndef.makeReadOnly();
                    } else {
                        message = "Tag can not be made read only";
                    }

                } else {
                    message = "Tag is not NDEF";
                }

            } catch (IOException e) {
                Log.e(TAG, "Failed to make tag read only", e);
                if (e.getMessage() != null) {
                    message = e.getMessage();
                } else {
                    message = e.toString();
                }
            }

            if (success) {
                callbackContext.success();
            } else {
                callbackContext.error(message);
            }
        });
    }

    private void shareTag(JSONArray data, CallbackContext callbackContext) throws JSONException {
        NdefRecord[] records = Util.jsonToNdefRecords(data.getString(0));
        this.p2pMessage = new NdefMessage(records);

        startNdefPush(callbackContext);
    }

    // setBeamPushUris
    // Every Uri you provide must have either scheme 'file' or scheme 'content'.
    // Note that this takes priority over setNdefPush
    //
    // See http://developer.android.com/reference/android/nfc/NfcAdapter.html#setBeamPushUris(android.net.Uri[],%20android.app.Activity)
    private void handover(JSONArray data, CallbackContext callbackContext) throws JSONException {

        Uri[] uri = new Uri[data.length()];

        for (int i = 0; i < data.length(); i++) {
            uri[i] = Uri.parse(data.getString(i));
        }

        startNdefBeam(callbackContext, uri);
    }

    private void stopHandover(CallbackContext callbackContext) {
        stopNdefBeam();
        handoverCallback = null;
        callbackContext.success();
    }

    private void showSettings(CallbackContext callbackContext) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NFC_SETTINGS);
            getActivity().startActivity(intent);
        } else {
            Intent intent = new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS);
            getActivity().startActivity(intent);
        }
        callbackContext.success();
    }

    private void createPendingIntent() {
        if (pendingIntent == null) {
            Activity activity = getActivity();
            Intent intent = new Intent(activity, activity.getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            pendingIntent = PendingIntent.getActivity(activity, 0, intent, 0);
        }
    }

    private void addTechList(String[] list) {
        this.addTechFilter();
        this.addToTechList(list);
    }

    private void removeTechList(String[] list) {
        this.removeTechFilter();
        this.removeFromTechList(list);
    }

    private void addTechFilter() {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED));
    }

    private void removeTechFilter() {
        Iterator<IntentFilter> iterator = intentFilters.iterator();
        while (iterator.hasNext()) {
            IntentFilter intentFilter = iterator.next();
            if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intentFilter.getAction(0))) {
                iterator.remove();
            }
        }
    }

    private void addTagFilter() {
        intentFilters.add(new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED));
    }

    private void removeTagFilter() {
        Iterator<IntentFilter> iterator = intentFilters.iterator();
        while (iterator.hasNext()) {
            IntentFilter intentFilter = iterator.next();
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intentFilter.getAction(0))) {
                iterator.remove();
            }
        }
    }

    private void restartNfc() {
        stopNfc();
        startNfc();
    }

    private void startNfc() {
        createPendingIntent(); // onResume can call startNfc before execute

        getActivity().runOnUiThread(() -> {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (nfcAdapter != null && !getActivity().isFinishing()) {
                try {
                    IntentFilter[] intentFilters = getIntentFilters();
                    String[][] techLists = getTechLists();
                    // don't start NFC unless some intent filters or tech lists have been added,
                    // because empty lists act as wildcards and receives ALL scan events
                    if (intentFilters.length > 0 || techLists.length > 0) {
                        nfcAdapter.enableForegroundDispatch(getActivity(), getPendingIntent(), intentFilters, techLists);
                    }

                    if (p2pMessage != null) {
                        nfcAdapter.setNdefPushMessage(p2pMessage, getActivity());
                    }
                } catch (IllegalStateException e) {
                    // issue 110 - user exits app with home button while nfc is initializing
                    Log.w(TAG, "Illegal State Exception starting NFC. Assuming application is terminating.");
                }

            }
        });
    }

    private void stopNfc() {
        Log.d(TAG, "stopNfc");
        getActivity().runOnUiThread(() -> {

            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (nfcAdapter != null) {
                try {
                    nfcAdapter.disableForegroundDispatch(getActivity());
                } catch (IllegalStateException e) {
                    // issue 125 - user exits app with back button while nfc
                    Log.w(TAG, "Illegal State Exception stopping NFC. Assuming application is terminating.");
                }
            }
        });
    }

    private void startNdefBeam(final CallbackContext callbackContext, final Uri[] uris) {
        getActivity().runOnUiThread(() -> {

            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (nfcAdapter == null) {
                callbackContext.error(STATUS_NO_NFC);
            } else if (!nfcAdapter.isNdefPushEnabled()) {
                callbackContext.error(STATUS_NDEF_PUSH_DISABLED);
            } else {
                nfcAdapter.setOnNdefPushCompleteCallback(NfcPlugin.this, getActivity());
                try {
                    nfcAdapter.setBeamPushUris(uris, getActivity());

                    PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                    result.setKeepCallback(true);
                    handoverCallback = callbackContext;
                    callbackContext.sendPluginResult(result);

                } catch (IllegalArgumentException e) {
                    callbackContext.error(e.getMessage());
                }
            }
        });
    }

    private void startNdefPush(final CallbackContext callbackContext) {
        getActivity().runOnUiThread(() -> {

            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (nfcAdapter == null) {
                callbackContext.error(STATUS_NO_NFC);
            } else if (!nfcAdapter.isNdefPushEnabled()) {
                callbackContext.error(STATUS_NDEF_PUSH_DISABLED);
            } else {
                nfcAdapter.setNdefPushMessage(p2pMessage, getActivity());
                nfcAdapter.setOnNdefPushCompleteCallback(NfcPlugin.this, getActivity());

                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true);
                shareTagCallback = callbackContext;
                callbackContext.sendPluginResult(result);
            }
        });
    }

    private void stopNdefPush() {
        getActivity().runOnUiThread(() -> {

            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (nfcAdapter != null) {
                nfcAdapter.setNdefPushMessage(null, getActivity());
            }

        });
    }

    private void stopNdefBeam() {
        getActivity().runOnUiThread(() -> {

            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

            if (nfcAdapter != null) {
                nfcAdapter.setBeamPushUris(null, getActivity());
            }

        });
    }

    private void addToTechList(String[] techs) {
        techLists.add(techs);
    }

    private void removeFromTechList(String[] techs) {
        Iterator<String[]> iterator = techLists.iterator();
        while (iterator.hasNext()) {
            String[] list = iterator.next();
            if (Arrays.equals(list, techs)) {
                iterator.remove();
            }
        }
    }

    private void removeIntentFilter(String mimeType) {
        Iterator<IntentFilter> iterator = intentFilters.iterator();
        while (iterator.hasNext()) {
            IntentFilter intentFilter = iterator.next();
            String mt = intentFilter.getDataType(0);
            if (mimeType.equals(mt)) {
                iterator.remove();
            }
        }
    }

    private IntentFilter createIntentFilter(String mimeType) throws MalformedMimeTypeException {
        IntentFilter intentFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        intentFilter.addDataType(mimeType);
        return intentFilter;
    }

    private PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    private IntentFilter[] getIntentFilters() {
        return intentFilters.toArray(new IntentFilter[intentFilters.size()]);
    }

    private String[][] getTechLists() {
        //noinspection ToArrayCallWithZeroLengthArrayArgument
        return techLists.toArray(new String[0][0]);
    }

    private void parseMessage() {
        cordova.getThreadPool().execute(() -> {
            Log.d(TAG, "parseMessage " + getIntent());
            Intent intent = getIntent();
            String action = intent.getAction();
            Log.d(TAG, "action " + action);
            if (action == null) {
                return;
            }

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Parcelable[] messages = intent.getParcelableArrayExtra((NfcAdapter.EXTRA_NDEF_MESSAGES));

            if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
                Ndef ndef = Ndef.get(tag);
                fireNdefEvent(NDEF_MIME, ndef, messages);

            } else if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {
                for (String tagTech : tag.getTechList()) {
                    Log.d(TAG, tagTech);
                    if (tagTech.equals(NdefFormatable.class.getName())) {
                        fireNdefFormatableEvent(tag);
                    } else if (tagTech.equals(Ndef.class.getName())) { //
                        Ndef ndef = Ndef.get(tag);
                        fireNdefEvent(NDEF, ndef, messages);
                    }
                }
            }

            if (action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
                fireTagEvent(tag);
            }

            setIntent(new Intent());
        });
    }

    // Send the event data through a channel so the JavaScript side can fire the event
    private void sendEvent(String type, JSONObject tag) {

        try {
            JSONObject event = new JSONObject();
            event.put("type", type);       // TAG_DEFAULT, NDEF, NDEF_MIME, NDEF_FORMATABLE
            event.put("tag", tag);         // JSON representing the NFC tag and NDEF messages

            PluginResult result = new PluginResult(PluginResult.Status.OK, event);
            result.setKeepCallback(true);
            channelCallback.sendPluginResult(result);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending NFC event through the channel", e);
        }

    }

    private void fireNdefEvent(String type, Ndef ndef, Parcelable[] messages) {
        JSONObject json = buildNdefJSON(ndef, messages);
        sendEvent(type, json);
    }

    private void fireNdefFormatableEvent(Tag tag) {
        sendEvent(NDEF_FORMATABLE, Util.tagToJSON(tag));
    }

    //simon add string 
    private void fireTagEvent(String tag) {
        sendEvent(TAG_DEFAULT, Util.stringToJSON(tag));
    }

    private void fireTagEvent(Itc213 tag) {
        sendEvent(TAG_DEFAULT, Util.stringToJSON(tag));
    }
    
    private void fireITCEvent(Itc213 tag){
        sendEvent(ITC_MESSAGE, Util.stringToJSON(tag));
    }

    private void fireTagEvent(Tag tag) {
        sendEvent(TAG_DEFAULT, Util.tagToJSON(tag));
    }

    private JSONObject buildNdefJSON(Ndef ndef, Parcelable[] messages) {

        JSONObject json = Util.ndefToJSON(ndef);

        // ndef is null for peer-to-peer
        // ndef and messages are null for ndef format-able
        if (ndef == null && messages != null) {

            try {

                if (messages.length > 0) {
                    NdefMessage message = (NdefMessage) messages[0];
                    json.put("ndefMessage", Util.messageToJSON(message));
                    // guessing type, would prefer a more definitive way to determine type
                    json.put("type", "NDEF Push Protocol");
                }

                if (messages.length > 1) {
                    Log.wtf(TAG, "Expected one ndefMessage but found " + messages.length);
                }

            } catch (JSONException e) {
                // shouldn't happen
                Log.e(Util.TAG, "Failed to convert ndefMessage into json", e);
            }
        }
        return json;
    }

    private boolean recycledIntent() { // TODO this is a kludge, find real solution

        int flags = getIntent().getFlags();
        if ((flags & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) {
            Log.i(TAG, "Launched from history, killing recycled intent");
            setIntent(new Intent());
            return true;
        }
        return false;
    }

    @Override
    public void onPause(boolean multitasking) {
        Log.d(TAG, "onPause " + getIntent());
        super.onPause(multitasking);
        if (multitasking) {
            // nfc can't run in background
            stopNfc();
        }
    }

    @Override
    public void onResume(boolean multitasking) {
        Log.d(TAG, "onResume " + getIntent());
        super.onResume(multitasking);
        startNfc();
    }

    @Override
    public void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent " + intent);
        super.onNewIntent(intent);
        setIntent(intent);
        savedIntent = intent;
        //showToast(intent.getAction());
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED) ||
                intent.getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED) ||
                intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            processNfcA(tag);

        }
        //parseMessage();
    }

    private String bytesToHex(char[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
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
    

    private static int byteArrayToInt(byte[] b) {
        return b[0] & 0xFF |
                (b[1] & 0xFF) << 8 |
                (b[2] & 0xFF) << 16;
//                |(b[3] & 0xFF) << 24;
    }

    private void readNTag213(ITCNTag213 nTag213) {
        Itc213 itctag = new Itc213();
        try {
            Log.d("itc", "Connected NTag213");
            byte[] signature = nTag213.readSignature();
            Log.d("itc", "Signature:" + bytesToHex(signature));
            
            //simon add return to js
            itctag.setSig(bytesToHex(signature));
            //fireTagEvent(bytesToHex(signature));  
            //fireTagEvent(itctag.getSig());  
            // uid
            UID uid = nTag213.readUID();
            uid.validateCheckSum();
            Log.d("itc", "UID:" + bytesToHex(uid.getRaw()));
            itctag.setUid(bytesToHex(uid.getRaw()));
            //fireTagEvent(itctag);
            
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
            itcid.validateCheckSum();  //err will throw exception
            Log.d("itc", "ITCID CheckSum verified");
            Log.d("itc", "ITCID " + bytesToHex(itcid.getRaw()));
            Log.d("itc", String.format("Customer Code: %X", itcid.getCustomerCode()));
            Log.d("itc", String.format("Customer Code: %X", itcid.getCustomerCode()));
            Log.d("itc", String.format("Commodity Code: %X", itcid.getCommodityCode()));
            Log.d("itc", "Instance Code: "+ bytesToHex(itcid.getInstanceCode()));
            itctag.setItcid(bytesToHex(itcid.getRaw()));
            itctag.setCustomCode(String.format("%X", itcid.getCustomerCode()));
            itctag.setCommodityCode(String.format("%X", itcid.getCommodityCode()));
            itctag.setInstanceCode(bytesToHex(itcid.getInstanceCode()));
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
                itctag.setPassProtStatus("yes");
                // password auth
                // nTag213.authenticatePwd(new byte[]{
                //         0x11, 0x22, 0x33, 0x44
                // }, new byte[]{
                //         (byte) 0xAA, (byte) 0xBB
                // });
                // Log.d("itc", "Password authenticated");

                // // reload itcdata
                // itcData = nTag213.readITCData();
                // Log.d("itc", "ITCData is reloaded");
                // itcData.validateCheckSum();
                // Log.d("itc", "ITCData verified");
            }else{
                itctag.setPassProtStatus("no");
            }
            fireTagEvent(itctag);
            // ITCDescriptors descs = itcData.getItcDescriptors();


            // ITCDescriptor[] descList = descs.getDescriptors();
            // for (ITCDescriptor desc : descList) {
            //     Log.d("itc", String.format("[desc]%s: %s", desc.getType().name(), desc.getContentString()));
            // }
            // byte[] version = nTag213.getVersion();
            // Log.d("itc", "Version:" + bytesToHex(version));
            // byte[] configBytes = nTag213.getConfigBytes();
            // Log.d("itc", "Config Bytes:" + bytesToHex(configBytes));
            // byte[] counter = nTag213.readCounter();
            // Log.d("itc", "Counter:" + bytesToHex(counter));
        } catch (Exception ex) {
            Log.e("itc", "Exception" + ex.getMessage());
            ex.printStackTrace();
            itctag.setErrCode(ex.getMessage());
            fireTagEvent(itctag);
        }
    }

    //simon add readntag213auth(auth pass)
    private void readNTag213Auth(ITCNTag213 nTag213,byte[] authwd) {
        Itc213 itctag = new Itc213();
        try {
            Log.d("itc", "Connected NTag213");
            byte[] signature = nTag213.readSignature();
            Log.d("itc", "Signature:" + bytesToHex(signature));
            
            //simon add return to js
            itctag.setSig(bytesToHex(signature));
            //fireTagEvent(bytesToHex(signature));  
            //fireTagEvent(itctag.getSig());  
            // uid
            UID uid = nTag213.readUID();
            uid.validateCheckSum();
            Log.d("itc", "UID:" + bytesToHex(uid.getRaw()));
            itctag.setUid(bytesToHex(uid.getRaw()));
            
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
            itctag.setCustomCode(String.format("%X", itcid.getCustomerCode()));
            itctag.setCommodityCode(String.format("%X", itcid.getCommodityCode()));
            itctag.setInstanceCode(bytesToHex(itcid.getInstanceCode()));
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
                nTag213.authenticatePwd(authwd, new byte []{authwd[4],authwd[5]});
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
                // Log.d("itc", String.format("[desc]%s: %s", desc.getType().name(), bytesToHex(desc.getContentString().toCharArray())));
                if(desc.getType().name().equals("ITC_DIGITAL_SIGNATURE")){
                    Log.d("itc","get signature");
                    Log.d("itc", String.format("[desc]%s: %s", desc.getType().name(), bytesToHex(desc.getContent())));
                    itctag.setItcSig(bytesToHex(desc.getContent()));
                }
           
            }
            byte[] version = nTag213.getVersion();
            Log.d("itc", "Version:" + bytesToHex(version));
            byte[] configBytes = nTag213.getConfigBytes();
            Log.d("itc", "Config Bytes:" + bytesToHex(configBytes));
            byte[] counter = nTag213.readCounter();
            Log.d("itc", "Counter:" + bytesToHex(counter));
            Log.d("itc", "Counter:" + byteArrayToInt(counter));
            itctag.setCount(byteArrayToInt(counter));
            fireITCEvent(itctag);
        } catch (Exception ex) {
            Log.e("itc", "Exception" + ex.getMessage());
            ex.printStackTrace();
            itctag.setErrCode(ex.getMessage());
            fireITCEvent(itctag);  //if error read throw exception event to app
        }
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

    private void readNTag213TT(ITCNTag213TagTamper nTag213TT) {
        try {
            Log.d("itc", "Connected NTag213TT");
            byte[] signature = nTag213TT.readSignature();
            Log.d("itc", "Signature:" + bytesToHex(signature));
            // uid
            UID uid = nTag213TT.readUID();
            uid.validateCheckSum();
            Log.d("itc", "UID:" + bytesToHex(uid.getRaw()));
            //simon add return to js
            fireTagEvent(bytesToHex(signature));    

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
                   //setIntent(new Intent());
               } else {
                   writeNTag213(nTag213);
               }
            } else if (sVersion.equals(bytesToHex(NTag21x.TAG_VERSION_NTAG_213_TAG_TAMPER))){
                //ITCNTag213TagTamper nTag213tt = ITCNTag213TagTamper.wrap(nTag21x);
                ITCNTag213 nTag213 = ITCNTag213.wrap(nTag21x);
                if(read) {
                    //readNTag213TT(nTag213tt);
                    readNTag213(nTag213);
                } else {
                    //writeNTag213TT(nTag213tt);
                    writeNTag213(nTag213);
                }
            }
            nTag21x.close();
        } catch(Exception ex) {
            Log.d("itc", ex.getMessage());
            Itc213 itctag = new Itc213();
            itctag.setErrCode(ex.getMessage());
            fireTagEvent(itctag);
        }
        //setIntent(new Intent()); //create intent for write simon 
    }


    private void processNfcAwithAuth(Tag tag,byte[] authwd,final CallbackContext callbackContext) {
        boolean read = true;
        try {
            NTag21x nTag21x = new NTag21x(tag);
            nTag21x.connect();
            byte[] version = nTag21x.getVersion();
            String sVersion = bytesToHex(version);
            if(sVersion.equals(bytesToHex(NTag21x.TAG_VERSION_NTAG_213))) {
                ITCNTag213 nTag213 = ITCNTag213.wrap(nTag21x);
               if(read) {
                    readNTag213Auth(nTag213,authwd);
               } else {
                   writeNTag213(nTag213);
               }
            } else if (sVersion.equals(bytesToHex(NTag21x.TAG_VERSION_NTAG_213_TAG_TAMPER))){
                // ITCNTag213TagTamper nTag213tt = ITCNTag213TagTamper.wrap(nTag21x);
                ITCNTag213 nTag213 = ITCNTag213.wrap(nTag21x);
                if(read) {
                    //readNTag213TT(nTag213tt);
                    readNTag213Auth(nTag213,authwd);
                } else {
                    // writeNTag213TT(nTag213tt);
                    writeNTag213(nTag213);
                }
            }
            nTag21x.close();
            callbackContext.success();
        } catch(Exception ex) {
            Log.d("itc", ex.getMessage());
            callbackContext.error("processNfcAwithAuth error");
            Itc213 itctag = new Itc213();
            itctag.setErrCode(ex.getMessage());
            fireITCEvent(itctag);  //if error read throw exception event to app
        }

    }


    private Activity getActivity() {
        return this.cordova.getActivity();
    }

    private Intent getIntent() {
        return getActivity().getIntent();
    }

    private void setIntent(Intent intent) {
        getActivity().setIntent(intent);
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {

        // handover (beam) take precedence over share tag (ndef push)
        if (handoverCallback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, "Beamed Message to Peer");
            result.setKeepCallback(true);
            handoverCallback.sendPluginResult(result);
        } else if (shareTagCallback != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, "Shared Message with Peer");
            result.setKeepCallback(true);
            shareTagCallback.sendPluginResult(result);
        }

    }

    /**
     * Enable I/O operations to the tag from this TagTechnology object.
     * *
     *
     * @param tech            TagTechnology class name e.g. 'android.nfc.tech.IsoDep' or 'android.nfc.tech.NfcV'
     * @param timeout         tag timeout
     * @param callbackContext Cordova callback context
     */
    private void connect(final String tech, final int timeout, final CallbackContext callbackContext) {
        this.cordova.getThreadPool().execute(() -> {
            try {

                Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
                if (tag == null) {
                    tag = savedIntent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                }

                if (tag == null) {
                    Log.e(TAG, "No Tag");
                    callbackContext.error("No Tag");
                    return;
                }

                // get technologies supported by this tag
                List<String> techList = Arrays.asList(tag.getTechList());
                if (techList.contains(tech)) {
                    // use reflection to call the static function Tech.get(tag)
                    tagTechnologyClass = Class.forName(tech);
                    Method method = tagTechnologyClass.getMethod("get", Tag.class);
                    tagTechnology = (TagTechnology) method.invoke(null, tag);
                }

                if (tagTechnology == null) {
                    callbackContext.error("Tag does not support " + tech);
                    return;
                }

                tagTechnology.connect();
                setTimeout(timeout);
                callbackContext.success();

            } catch (IOException ex) {
                Log.e(TAG, "Tag connection failed", ex);
                callbackContext.error("Tag connection failed");

                // Users should never get these reflection errors
            } catch (ClassNotFoundException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error(e.getMessage());
            } catch (NoSuchMethodException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error(e.getMessage());
            } catch (IllegalAccessException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error(e.getMessage());
            } catch (InvocationTargetException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error(e.getMessage());
            }
        });
    }

    // Call tagTech setTimeout with reflection or fail silently
    private void setTimeout(int timeout) {
        if (timeout < 0) {
            return;
        }
        try {
            Method setTimeout = tagTechnologyClass.getMethod("setTimeout", int.class);
            setTimeout.invoke(tagTechnology, timeout);
        } catch (NoSuchMethodException e) {
            // ignore
        } catch (IllegalAccessException e) {
            // ignore
        } catch (InvocationTargetException e) {
            // ignore
        }
    }

    /**
     * Disable I/O operations to the tag from this TagTechnology object, and release resources.
     *
     * @param callbackContext Cordova callback context
     */
    private void close(CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {

                if (tagTechnology != null && tagTechnology.isConnected()) {
                    tagTechnology.close();
                    tagTechnology = null;
                    callbackContext.success();
                } else {
                    // connection already gone
                    callbackContext.success();
                }

            } catch (IOException ex) {
                Log.e(TAG, "Error closing nfc connection", ex);
                callbackContext.error("Error closing nfc connection " + ex.getLocalizedMessage());
            }
        });
    }

    /**
     * Send raw commands to the tag and receive the response.
     *
     * @param data            byte[] command to be passed to the tag
     * @param callbackContext Cordova callback context
     */
    private void transceive(final byte[] data, final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(() -> {
            try {
                if (tagTechnology == null) {
                    Log.e(TAG, "No Tech");
                    callbackContext.error("No Tech");
                    return;
                }
                if (!tagTechnology.isConnected()) {
                    Log.e(TAG, "Not connected");
                    callbackContext.error("Not connected");
                    return;
                }

                // Use reflection so we can support many tag types
                Method transceiveMethod = tagTechnologyClass.getMethod("transceive", byte[].class);
                @SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
                byte[] response = (byte[]) transceiveMethod.invoke(tagTechnology, data);

                callbackContext.success(response);

            } catch (NoSuchMethodException e) {
                String error = "TagTechnology " + tagTechnologyClass.getName() + " does not have a transceive function";
                Log.e(TAG, error, e);
                callbackContext.error(error);
            } catch (IllegalAccessException e) {
                Log.e(TAG, e.getMessage(), e);
                callbackContext.error(e.getMessage());
            } catch (InvocationTargetException e) {
                Log.e(TAG, e.getMessage(), e);
                Throwable cause = e.getCause();
                callbackContext.error(cause.getMessage());
            }
        });
    }

}
