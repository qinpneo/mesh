package wificonnecter;
/**
 * Created by paqin on 06/09/2017.
 */

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import Utils.Logger;

public class WiFiConnecter {

    // Combo scans can take 5-6s to complete
    private static final int WIFI_RESCAN_INTERVAL_MS = 10 * 1000;
    private static final int WIFI_CONNECT_INTERVAL_MS = 10 * 1000;

    //private static final String WIFI_CONNECT_DIRECTLY = "WifiConnectDirectly";
    //private static final String WIFI_CONNECT_DIRECTLY_RESULT = "WifiConnectDirectResult";

    static final int SECURITY_NONE = 0;
    static final int SECURITY_WEP = 1;
    static final int SECURITY_PSK = 2;
    static final int SECURITY_EAP = 3;
    private static final String TAG = WiFiConnecter.class.getSimpleName();
    public static final int MAX_SCAN_TRY_COUNT = 3;
    public static final int MAX_CONNECT_TRY_COUNT = 3;

    private Context mContext;
    private WifiManager mWifiManager;

    private final IntentFilter mFilter;
    private final BroadcastReceiver mReceiver;
    private final Scanner mScanner;
    private ActionListener mListener;
    private String mSsid;
    private String mPassword;

    private boolean isRegistered;
    private boolean isActiveScan;
    private boolean bySsidIgnoreCase;
    private boolean needScan = false;
    private boolean connectInProgress = false;

    public WiFiConnecter(Context context) {
        this.mContext = context;
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        mFilter = new IntentFilter();
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mFilter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
        mFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        mFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handleEvent(context, intent);
            }
        };

        context.registerReceiver(mReceiver, mFilter);
        isRegistered = true;
        bySsidIgnoreCase = true;
        mScanner = new Scanner();
    }

    private void clearFlags() {
        this.needScan = false;
        this.connectInProgress = false;
    }

    /**
     * Connect to a WiFi with the given ssid and password
     *
     * @param ssid
     * @param password
     * @param listener : for callbacks on start or success or failure. Can be null.
     */
    public void connect(String ssid, String password, ActionListener listener, boolean needscan) {
        this.mListener = listener;
        this.mSsid = ssid;
        this.mPassword = password;
        this.needScan = needscan;
        this.connectInProgress = true;

        if (listener != null) {
            listener.onStarted(ssid, needscan);
        }

        WifiInfo info = mWifiManager.getConnectionInfo();
        String quotedString = StringUtils.convertToQuotedString(mSsid);
        boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(info.getSSID())
                : quotedString.equals(info.getSSID());
        if (ssidEquals) {
            if (listener != null) {
                listener.onSuccess(info, mSsid, needscan);
                listener.onFinished(true, needscan);
            }
            clearFlags();
            return;
        }

        if(needscan) {

            mScanner.forceScan();
        } else {
            mScanner.forceConnect();

        }
    }

    public void destroy() {
        this.onPause();
        this.mListener = null;
        //this.mScanner = null;
    }

    /*private void postHandleEvent(boolean success) {
        Intent intent = new Intent();
        intent.setAction(WIFI_CONNECT_DIRECTLY);
        intent.putExtra(WIFI_CONNECT_DIRECTLY_RESULT, success);
        handleEvent(mContext, intent);
    }*/
    private void handleEvent(Context context, Intent intent) {
        String action = intent.getAction();
        //An access point scan has completed, and results are available from the supplicant.
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action) && isActiveScan) {
            List<ScanResult> results = mWifiManager.getScanResults();
            for (ScanResult result : results) {
                String currentSSID = result.SSID;
                //1.scan dest of ssid
                String quotedString = StringUtils.convertToQuotedString(mSsid);
                boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(result.SSID)
                        : quotedString.equals(result.SSID);
                if(!ssidEquals) {
                    ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(StringUtils.convertToQuotedString(result.SSID))
                            : quotedString.equals(StringUtils.convertToQuotedString(result.SSID));
                }

                if (ssidEquals) {
                    //TODO ?
                    mScanner.pause();
                    //2.input error password
                    if (WiFi.connectToNewNetwork(mWifiManager, result, mPassword)) {
                        if (mListener != null) {
                            mListener.onFailure(result.SSID, this.needScan);
                            mListener.onFinished(false, this.needScan);
                        }
                        clearFlags();
                        onPause();
                    }
                    break;
                }
            }

            //Broadcast intent action indicating that the state of Wi-Fi connectivity has changed.
        } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action) && connectInProgress) {
            NetworkInfo mInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
            //ssid equals&&connected
            if (mWifiInfo != null && mInfo.isConnected() && mWifiInfo.getSSID() != null) {
                String quotedString = StringUtils.convertToQuotedString(mSsid);
                boolean ssidEquals = bySsidIgnoreCase ? quotedString.equalsIgnoreCase(mWifiInfo.getSSID())
                        : quotedString.equals(mWifiInfo.getSSID());
                if(ssidEquals) {
                    Logger.log(String.format("network status is: %s => %s", mSsid, mInfo.getDetailedState().name()));
                }
                if (ssidEquals && mInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                    if (mListener != null) {
                        mListener.onSuccess(mWifiInfo, mSsid, needScan);
                        mListener.onFinished(true, needScan);
                    }
                    clearFlags();
                    mScanner.clearMessage();
                    onPause();
                }

            }
        }
    }

    public void onPause() {
        if (isRegistered) {
            mContext.unregisterReceiver(mReceiver);
            isRegistered = false;
        }
        clearFlags();
        mScanner.pause();
    }

    /*public void onResume() {
        if (!isRegistered) {
            mContext.registerReceiver(mReceiver, mFilter);
            isRegistered = true;
        }
        mScanner.resume();
    } */

    @SuppressLint("HandlerLeak")
    private class Scanner extends Handler {
        private int mScanRetry = 0;

        private int mConnectRetry = 0;


        void forceConnect() {
            removeMessages(1);
            sendEmptyMessage(1);
        }
        void forceScan() {
            removeMessages(0);
            sendEmptyMessage(0);
        }

        void clearMessage() {
            removeMessages(0);
            removeMessages(1);
        }
        void pause() {
            mScanRetry = 0;
            isActiveScan = false;
            removeMessages(0);
        }

        private void handleForceScanEvent() {
            if (mScanRetry < MAX_SCAN_TRY_COUNT) {
                mScanRetry++;
                isActiveScan = true;
                //1.打开Wifi
                if (!mWifiManager.isWifiEnabled()) {
                    mWifiManager.setWifiEnabled(true);
                }
                //TODO startScan什么时候返回false
                boolean startScan = mWifiManager.startScan();
                Log.d(TAG, "startScan:" + startScan);
                //执行扫描失败（bind机制）
                if (!startScan) {
                    if (mListener != null) {
                        mListener.onFailure(mSsid, needScan);
                        mListener.onFinished(false, needScan);
                    }
                    onPause();
                    return;
                }
            } else {
                mScanRetry = 0;
                isActiveScan = false;
                if (mListener != null) {
                    mListener.onFailure(mSsid, needScan);
                    mListener.onFinished(false, needScan);
                }
                onPause();
                return;
            }
            sendEmptyMessageDelayed(0, WIFI_RESCAN_INTERVAL_MS);
        }

        private void handleForceConnectEvent() {
            if (mConnectRetry < MAX_CONNECT_TRY_COUNT) {
                mConnectRetry++;
                //1.打开Wifi
                if (!mWifiManager.isWifiEnabled()) {
                    mWifiManager.setWifiEnabled(true);
                }
                 WifiConfiguration config = new WifiConfiguration();
                 config.SSID = StringUtils.convertToQuotedString(mSsid);
                 config.preSharedKey = StringUtils.convertToQuotedString(mPassword);
                 config.priority = -1;
                 config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                 int networkId = WiFi.connectToConfiguredNetwork2(mWifiManager, config, true);
                 if(networkId == -1) {
                     if(mListener != null) {
                         mListener.onFailure(mSsid, needScan);
                         mListener.onFinished(false, needScan);
                     }
                     onPause();
                     clearFlags();
                     mScanner.clearMessage();
                     return;
                 } else {

                     try {
                         ClassLoader loader = WifiManager.class.getClassLoader();
                         Class interfazz = loader.loadClass("android.net.wifi.WifiManager$ActionListener");
                         Object clazzInstance = Proxy.newProxyInstance(loader, new Class[] { interfazz }, new InvocationHandler() {

                             @Override
                             public Object invoke(Object obj, Method method, Object[] args) throws Throwable {
                                 if (method.getName().equals("onSuccess")) {
                                     Logger.log("OnSuccess from ActionListener");
                                     if(mListener != null) {
                                         mListener.onSuccess(null, mSsid, needScan);
                                         mListener.onFinished(true, needScan);
                                     }
                                     onPause();
                                     clearFlags();
                                     mScanner.clearMessage();
                                 } else if (method.getName().equals("onFailure")){
                                     Logger.log("OnFailure from ActionListener");
                                     if(mListener != null) {
                                         mListener.onFailure(mSsid, needScan);
                                         mListener.onFinished(false, needScan);
                                     }
                                     onPause();
                                     clearFlags();
                                     mScanner.clearMessage();
                                 } else {
                                     return method.invoke(obj, args);
                                 }
                                 return null;
                             }
                         });

                         Method[] methods = WifiManager.class.getMethods();
                         for (int i = 0; i < methods.length; i++) {
                             if (methods[i].getName().equals("connect")) {
                                 Class<?>[] typeList = methods[i].getParameterTypes();
                                 for(Class<?> item : typeList) {
                                     //get the connection
                                     if(item.isPrimitive()) {
                                        methods[i].invoke(mWifiManager, networkId, clazzInstance);
                                         return;
                                     }
                                 }
                             }
                         }

                     } catch (Exception e) {
                         if(mListener != null) {
                             mListener.onFailure(mSsid, needScan);
                             mListener.onFinished(false, needScan);
                         }
                         onPause();
                         clearFlags();
                         mScanner.clearMessage();
                         return;
                     }
                 }
                 /*boolean connectSuccess = WiFi.connectToConfiguredNetwork(mWifiManager, config, true);
                 if(!connectSuccess) {
                     if(mListener != null) {
                         mListener.onFailure();
                         mListener.onFinished(false, needScan);
                     }
                     onPause();
                     clearFlags();
                     mScanner.clearMessage();
                     return;
                 }*/
            } else {
                mConnectRetry = 0;
                //isActiveScan = false;
                if (mListener != null) {
                    mListener.onFailure(mSsid, needScan);
                    mListener.onFinished(false, needScan);
                }
                onPause();
                clearFlags();
                return;
            }
            sendEmptyMessageDelayed(1, WIFI_CONNECT_INTERVAL_MS);
        }
        @Override
        public void handleMessage(Message message) {
            if(message.what == 0) {
                handleForceScanEvent();
            } else if(message.what == 1) {
                handleForceConnectEvent();
            }
        }
    }

    public interface ActionListener {

        /**
         * The operation started
         *
         * @param ssid
         */
        public void onStarted(String ssid, boolean scan);

        /**
         * The operation succeeded
         *
         * @param info
         */
        public void onSuccess(WifiInfo info, String ssid,  boolean scan);

        /**
         * The operation failed
         */
        public void onFailure(String ssid, boolean scan);

        /**
         * The operation finished
         *
         * @param isSuccessed
         */
        public void onFinished(boolean isSuccessed, boolean scannedFinish);
    }

}
