package device.apps.pmpos;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

public class VanManager {
    private static final String TAG = VanManager.class.getSimpleName();

    private Context mContext;

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private Handler mPosVanHandler;

    private boolean mIsDataForAdmin = false;

    private TCPClient mTCPClient= null;

    public VanManager(Context context){
        mContext = context;
    }

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public synchronized void handleMessage(Message msg) {
            if (mServiceHandler != null) {
                byte[] data = (byte[]) msg.obj;
                Intent vanHostIntent;
                Intent vanHostAdminIntent;
                switch (msg.what) {
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK:
                        Utils.showToastMessage(mContext,TAG,mContext.getResources().getString(R.string.vanhost_msg_data_ok));
                        break;

                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_FAIL:
                        Utils.showToastMessage(mContext,TAG,mContext.getResources().getString(R.string.vanhost_msg_data_fail));
                        break;
                    case Utils.MSG_VANHOST_CONNECTION_ERROR:
                        Utils.showToastMessage(mContext,TAG,mContext.getResources().getString(R.string.vanhost_msg_cnnection_error));
                        break;
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_TIMEOUT:
                        Utils.showToastMessage(mContext, TAG, mContext.getResources().getString(R.string.vanhost_msg_timeout));
                        break;
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK_F1:  //VAN 업무구분 F1인 경우 Req CTACK0x0d
                        requestSuccessToVanHost(data);
                        return;
                    default:
                        Utils.showToastMessage(mContext,TAG,mContext.getResources().getString(R.string.vanhost_msg_cnnection_error));
                        break;
                }

                if(msg.what != Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK_F1){
                    if (isDataForAdmin(data)) {
                        vanHostIntent = new Intent(Utils.ACTION_RECEIVE_DATA_FROM_VANHOST_TO_ADMIN);
                    } else {
                        vanHostIntent = new Intent(Utils.ACTION_RECEIVE_DATA_FROM_VANHOST);
                    }
                    sendHandleMassege(msg.what, data);
//                    mContext.sendBroadcast(vanHostIntent);
                    stopHandlerThread();
//                    mTCPClient.disconnect();
                    mTCPClient = null;


                }
            }
        }
    }

    public  void sendHandleMassege(int what, Object obj) {
        Message msg = Message.obtain();
        msg.what = what;
        msg.obj = obj;
        mPosVanHandler.sendMessage(msg);
    }


    public void startHandlerThread(){
        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
        mServiceLooper = thread.getLooper();
        mServiceHandler = new VanManager.ServiceHandler(mServiceLooper);
    }
    public void stopHandlerThread(){
        if(mServiceLooper !=null){
            mServiceLooper.getThread().interrupt();
            mServiceLooper = null;
        }
        if(mServiceHandler!=null){
            mServiceHandler = null;
        }
    }


    public boolean isDataForAdmin(byte[] vanPktBuf) {
        boolean ret = false;
        if(vanPktBuf == null || vanPktBuf.length <= 0){
            return mIsDataForAdmin;
        }else {
            String trdTypeStr = Utils.checkTrdType(vanPktBuf);
            if (!trdTypeStr.isEmpty()&&
                    ((trdTypeStr.equals(Utils.TRD_TYPE_FUTURE_KEY_UPDATE))||
                    (trdTypeStr.equals(Utils.TRD_TYPE_MUTUAL_AUTHENTICATION))||
                    (trdTypeStr.equals(Utils.TRD_TYPE_DEVICE_DOWNLOAD)))) {
                ret = true;
            } else {
                ret = false;
            }
        }
        return ret;
    }

    public void requestSuccessToVanHost(byte[] recvdata) {
        byte[] mACKData = Utils.DATA_CTACK;
        mTCPClient.requestData(mACKData, recvdata);

    }

    public void requestDataToVanHost(byte[] reqData, boolean adminFlag, Handler handler) {
        mIsDataForAdmin = adminFlag;
        mPosVanHandler = handler;

        startHandlerThread();
        mTCPClient = new TCPClient(mContext, mServiceHandler);
        mTCPClient.requestData(reqData, null);
    }

}
