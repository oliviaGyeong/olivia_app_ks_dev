package device.apps.pmpos;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import device.apps.pmpos.aidl.Callback;
import device.apps.pmpos.aidl.DialogListener;
import device.apps.pmpos.fragment.sign.TouchDrawer;
import device.apps.pmpos.packet.AdminVanPacketManager;
import device.apps.pmpos.packet.ICReaderPacketManager;
import device.apps.pmpos.packet.ReceiptPrintData;
import device.apps.pmpos.packet.RecvDeviceInfoPacket;
import device.apps.pmpos.packet.VanHostPacketManager;

/**
 * Created by olivia on 17. 12. 21.
 */

public class PMPosHelper extends Service{// implements DialogListener.OnDialogCreateCallback{

    private final String TAG = PMPosHelper.class.getSimpleName();

    private Context mContext;

    private boolean mIsCalledByDeamon = false; //from Deamon true, pmposApp false;

    //PMPos AppView MSG
    private final int MSG_GOTO_MAIN_SCENE = 501;
    private final int MSG_GOTO_IC_READ_SCENE = 502;
    private final int MSG_GOTO_IC_READ_FAIL_SCENE = 503;
    private final int MSG_GOTO_MS_SWIPE_SCENE = 504;

    //IFM  Result Code
    private final String IC_RESULT_CODE_SUCCESS = "00";  // IC/MSR 정상처리 응답코드
    private final String IC_RESULT_CODE_ERROR_01 = "01"; // Fallback, (Chip 전원O, 응답X)
    private final String IC_RESULT_CODE_ERROR_02 = "02"; // Fallback, (상호지원 app X)
    private final String IC_RESULT_CODE_ERROR_03 = "03"; // Fallback, (칩데이터 읽기 실)
    private final String IC_RESULT_CODE_ERROR_04 = "04"; // Fallback, (Mandatory 데이터 미포)
    private final String IC_RESULT_CODE_ERROR_05 = "05"; // Fallback, (CVM 커맨드 응답 실)
    private final String IC_RESULT_CODE_ERROR_06 = "06"; // Fallback, (EMV 커맨드 잘못설정)
    private final String IC_RESULT_CODE_ERROR_07 = "07"; // Fallback, (터미널 오작동)
    private final String IC_RESULT_CODE_ERROR_08 = "08"; // Fallback, (IC 카드 읽기 실패)
    private final String IC_RESULT_CODE_ERROR_09 = "09"; // IC 우선 처리 응답
    private final String IC_RESULT_CODE_ERROR_10 = "10"; // 처리 불가 카드 응답
    private final String IC_RESULT_CODE_ERROR_11 = "11"; // Fallback MSR 수신 후 MS 읽기 실패

    private final String VAN_RESULT_CODE_SUCCESS = "0000";

    //Daemon msg
    private final int HANDLER_MSG_DAEMON_RDI_ON = 33;

    private String rootingCheckCommand = "su";

    private ReceiptPrintData mReceiptPrintData;
    private byte[] recvRdPktBufForSign = null;


    //TODO: RDI ON >> REQ  selftest & devicesInfo Timeout +++
    private boolean mSelfProtection = false;
    private boolean mReqDevicesInfo = false;

    private Callback.OnCallBack mCallback;


    //IFM
    private ICReaderPacketManager mICReaderPacketManager;
    private RdiManager mRdiManager;

    //VAN
    private VanHostPacketManager mVanHostPacketManager;
    private VanManager mVanManager;


    ApprovalValue mApprovalValue;

    public PMPosHelper() { //For service
        mContext = this;
        PMPosHelperInstance = this;
    }


    public PMPosHelper(Context context) {
        mContext = context;
        PMPosHelperInstance = this;
    }

    private static PMPosHelper PMPosHelperInstance ;
    public static synchronized PMPosHelper getInstance() {
        if (PMPosHelperInstance == null) {
            PMPosHelperInstance = new PMPosHelper();
        }
        return PMPosHelperInstance;
    }


    //Recv ICreader
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (Utils.DEBUG_INFO) Log.i(TAG, "mHandler - handleMessage() +++");
            byte[] recvRdPktBuf = (byte[]) msg.obj;
            device.apps.pmpos.aidl.Callback.CallBackList failCallback = device.apps.pmpos.aidl.Callback.CallBackList.RDI_FAIL_READ_DATA;

            int cmdId = Utils.getSharedPreferenceInt(mContext, Utils.PREF_KEY_IC_COMD_ID);

            switch (msg.what) {
                case Utils.MSG_RECEIVE_IC_READER_DATA_OK:
                    if (mICReaderPacketManager == null) {
                        mICReaderPacketManager = new ICReaderPacketManager();
                    }

                    switch (mICReaderPacketManager.getCommandId(recvRdPktBuf)) {
                        case Utils.CMD_ID_RECV_IC_PREAMBLE:
                            String resultCode = Utils.convertByteArrayToString(recvRdPktBuf, 436, 2);
                            Utils.setSharedPrefResultCodeValue(mContext, resultCode);

                            switch (resultCode) {
                                case IC_RESULT_CODE_SUCCESS:
                                    byte[] ksnBuf = new byte[10];
                                    System.arraycopy(recvRdPktBuf, 76, ksnBuf, 0, ksnBuf.length);
                                    mApprovalValue.mKsn = Utils.byteArrayToHex(ksnBuf);
                                    String paymentOption = mApprovalValue.mPaymentOption;
                                    //APPROVAL
                                    switch (mApprovalValue.mTrdType) {
                                        case Utils.TRD_TYPE_IC_CREDIT_APPROVAL:
                                            if (Integer.parseInt(mApprovalValue.mAmount) >= Utils.INSTALLABLE_AMOUNT) {
                                                recvRdPktBufForSign = recvRdPktBuf;
                                                dialogStart(Utils.popup_Signature,null,null,recvRdPktBuf);
                                            } else {//no sign
                                                approvalRequestVAN(recvRdPktBuf);
                                            }
                                            break;
                                        case Utils.TRD_TYPE_CASH_APPROVAL:
                                            approvalRequestVAN(recvRdPktBuf);
                                            break;
                                        //CANCEL
                                        case Utils.TRD_TYPE_IC_CREDIT_CANCEL:
                                            normalCancelVAN(recvRdPktBuf);
                                            break;
                                        case Utils.TRD_TYPE_CASH_CANCEL:
                                            switch (paymentOption) {
                                                case Utils.PAYMENT_OPTION_MS:
                                                    normalCancelVAN(recvRdPktBuf);
                                                    break;
                                                case Utils.PAYMENT_OPTION_CASH:
                                                    normalCancelVAN(recvRdPktBuf);
                                                    break;
                                            }
                                            break;
                                    }
                                    break;
                                case IC_RESULT_CODE_ERROR_01:
                                case IC_RESULT_CODE_ERROR_02:
                                case IC_RESULT_CODE_ERROR_03:
                                case IC_RESULT_CODE_ERROR_04:
                                case IC_RESULT_CODE_ERROR_05:
                                case IC_RESULT_CODE_ERROR_06:
                                case IC_RESULT_CODE_ERROR_07:
                                case IC_RESULT_CODE_ERROR_08:
                                    if (Utils.DEBUG_INFO)
                                    dialogStart(Utils.popup_IcReadFail, null, null, null);

                                    break;
                                case IC_RESULT_CODE_ERROR_09:
                                    noticeDialog(mContext.getResources().getString(R.string.dialog_builder_notice_msg_error_code_read_ic_again), MSG_GOTO_IC_READ_SCENE);
                                    break;
                                case IC_RESULT_CODE_ERROR_10: //비정상 fallback
                                    noticeDialog(mContext.getResources().getString(R.string.dialog_builder_notice_msg_error_code_read_fail), MSG_GOTO_MAIN_SCENE);
                                    break;
                                case IC_RESULT_CODE_ERROR_11:
                                    Utils.showToastMessage(mContext, "", mContext.getResources().getString(R.string.dialog_builder_notice_msg_error_code_read_ms_again));
                                    break;
                                default:
                                    sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MAIN, null, null);
                                    break;
                            }
                            break;
                        case Utils.CMD_ID_RECV_IC_TRANSACTION_RESULT:
                            if (recvRdPktBuf[4] == 0x30) {
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_RECEIPT_PRINT, null, mReceiptPrintData.getRecvVanData());
                            } else {
                                byte[] msgBuf = new byte[20];
                                System.arraycopy(recvRdPktBuf, 5, msgBuf, 0, msgBuf.length);
                                String resultMsg = new String(msgBuf);
                                Utils.showToastMessage(mContext, "", resultMsg);
                                icCardCancelVAN();
                            }
                            break;
                        case Utils.CMD_ID_RECV_ACK:
                            switch (cmdId) {
                                case Utils.CMD_ID_REQ_IC_CANCEL_PAYMENT:
                                    if(mApprovalValue != null ) {
                                        sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MAIN, recvRdPktBuf, null);
                                    }
                                    break;
                            }
                            break;
                        case Utils.CMD_ID_RECV_NAK:
                            switch (cmdId) {
                                case Utils.CMD_ID_REQ_IC_CANCEL_PAYMENT:
                                    if(mApprovalValue != null ) {
                                        sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MAIN, recvRdPktBuf, null);
                                    }
                                    break;
                                default://A/S
                                    faildInitASpopup();
                                    sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MAIN, recvRdPktBuf, null);
                                    break;
                            }
                            break;

                        case Utils.CMD_ID_RECV_DEVICE_INFO:
                            String dvTag = "";
                            String dvMessage = mContext.getResources().getString(R.string.toast_msg_device_info);

                            //FK Key  &  RecvDeviceInfoPacket
                            RecvDeviceInfoPacket deviceInfoPacket = new RecvDeviceInfoPacket();
                            deviceInfoPacket.setRecvDeviceInfo(recvRdPktBuf);

                            String certificationNumStr = Utils.byteArrayToHex(deviceInfoPacket.getCertificationNum());
                            Utils.setSharedPreference(mContext, Utils.PREF_KEY_AUTHENNUM, certificationNumStr);

                            byte[] isSecurityKey = deviceInfoPacket.getIsSecurityKey();
                            if (new String(isSecurityKey).equals("00")) {
                                Utils.setSharedPreference(mContext, Utils.PREF_KEY_FUTUREFW_FLAG, Utils.PREF_KEY_FLAG_STR_TRUE);
                                Utils.showToastMessage(mContext, dvTag, dvMessage);
                                mReqDevicesInfo = true;
                                reqSelfProtection();
                            } else {
                                mReqDevicesInfo = false;
                                Utils.setSharedPreference(mContext, Utils.PREF_KEY_FUTUREFW_FLAG, Utils.PREF_KEY_FLAG_STR_FALSE);
                                Utils.showToastMessage(mContext, "", mContext.getResources().getString(R.string.toast_msg_device_info_not_have_fk)); //TODO: string 변경 해야함.
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MAIN, null, null);
                            }
                            break;

                        case Utils.CMD_ID_RECV_SELF_PROTECTION:
                            byte[] resultSPBuf = mICReaderPacketManager.getRecvResult(recvRdPktBuf);
                            String mSelfProtectionResult = new String(resultSPBuf);
                            if (mSelfProtectionResult.equals("00")) {
                                String sptag = "";
                                String spmessage = mContext.getResources().getString(R.string.toast_msg_selftest_ok);
                                Utils.showToastMessage(mContext, sptag, spmessage);
                                mSelfProtection = true;
                            } else {
                                mSelfProtection = false;
                                faildInitASpopup();
                            }
                            String spResult = Utils.setSuccessFailStr(mSelfProtectionResult);
                            Utils.saveSPLog(mContext, spResult, "");
                            mRdiOnInitThreadFlag = false;
                            break;
                        default:
                            sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.RDI_FAIL_READ_DATA, recvRdPktBuf, null);
                            break;
                    }
                    break;
                case Utils.MSG_RECEIVE_IC_READER_DATA_FAIL:
                    failCallback = device.apps.pmpos.aidl.Callback.CallBackList.RDI_FAIL_READ_DATA;
                    Utils.showToastMessage(mContext, TAG, mContext.getResources().getString(R.string.result_icreader_data_fail));
                    sendToCallback(failCallback, null, null);
                    break;
                case Utils.MSG_RECEIVE_IC_READER_DATA_TIMEOUT:
                    failCallback = device.apps.pmpos.aidl.Callback.CallBackList.RDI_FAIL_READ_TIMEOUT;
                    Utils.showToastMessage(mContext, TAG, mContext.getResources().getString(R.string.result_icreader_timeout));
                    sendToCallback(failCallback, null, null);
                    break;
            }

            //ICReader Buf init
            if (recvRdPktBuf != null) {
                recvRdPktBuf = Utils.randToByteArray(recvRdPktBuf.length);
                if(Utils.DEBUG_INIT)Utils.debugTx("rand init", recvRdPktBuf, recvRdPktBuf.length);
                Arrays.fill(recvRdPktBuf, (byte) 0x00);
                if(Utils.DEBUG_INIT)Utils.debugTx("0x00 init", recvRdPktBuf, recvRdPktBuf.length);
                Arrays.fill(recvRdPktBuf, (byte) 0xFF);
                if(Utils.DEBUG_INIT)Utils.debugTx("0xFF init", recvRdPktBuf, recvRdPktBuf.length);
            }
        }

        private void noticeDialog(String message, final int gotoFragmentNum) {
            String title = mContext.getString(R.string.dialog_builder_notice_title);
            if (gotoFragmentNum == MSG_GOTO_IC_READ_SCENE) {
                dialogStart(Utils.popup_DisplayNoticeIC,title,message,null);
            } else if (gotoFragmentNum == MSG_GOTO_MS_SWIPE_SCENE) {
                dialogStart(Utils.popup_DisplayNoticeMS,title,message,null);
            } else {
                dialogStart(Utils.popup_DisplayNoticeNOTPayment,title,message,null);
            }
        }
    };



    private Handler mVanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            byte[] recvVanPktBuf = (byte[]) msg.obj;
            if (recvVanPktBuf != null && recvVanPktBuf.length != 0) {
                if (recvVanPktBuf[0] == Utils.OBG_NETWORK_CANCEL) {
                    networkCommunicationCancelVAN();
                    return;
                }
                mReceiptPrintData.setRecvVanData(recvVanPktBuf);

                String resultCode = parsingForResultCode(recvVanPktBuf);
                switch (Utils.checkTrdType(recvVanPktBuf)) {
                    case Utils.TRD_TYPE_IC_CREDIT_APPROVAL:
                        String paymentOption = mApprovalValue.mPaymentOption;
                        if (paymentOption.equals(Utils.PAYMENT_OPTION_IC)) {
                            if (resultCode.equals(VAN_RESULT_CODE_SUCCESS)) {
                                reqTransactionResultIFM(recvVanPktBuf);
                                setTransSeqNum();
                            } else {
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_DEAL_COMPLETE, null, recvVanPktBuf);
                                cancelPaymentIFM();
                                return;
                            }
                        } else {
                            if (resultCode.equals(VAN_RESULT_CODE_SUCCESS)) {
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_RECEIPT_PRINT, null, recvVanPktBuf);
                            } else {
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_DEAL_COMPLETE, null, recvVanPktBuf);
                            }
                        }
                        break;
                    case Utils.TRD_TYPE_IC_CREDIT_CANCEL://  F2
                        if (mApprovalValue.mTrdType.equals(Utils.TRD_TYPE_IC_CREDIT_APPROVAL)) {
                            if (resultCode.equals(VAN_RESULT_CODE_SUCCESS)) {
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_DEAL_COMPLETE, null, recvVanPktBuf);
                            } else { //fail
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_DEAL_COMPLETE, null, recvVanPktBuf);
                            }
                        } else {
                            if (resultCode.equals(VAN_RESULT_CODE_SUCCESS)) {
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_RECEIPT_PRINT, null, recvVanPktBuf);
                            } else { //fail
                                sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_DEAL_COMPLETE, null, recvVanPktBuf);
                            }
                        }
                        break;

                    case Utils.TRD_TYPE_CASH_APPROVAL://  H3
                        if (resultCode.equals(VAN_RESULT_CODE_SUCCESS)) {
                            sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_RECEIPT_PRINT, null, recvVanPktBuf);
                        } else { //fail
                            sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_DEAL_COMPLETE, null, recvVanPktBuf);
                        }
                        break;
                    case Utils.TRD_TYPE_CASH_CANCEL:// H4
                        if (resultCode.equals(VAN_RESULT_CODE_SUCCESS)) {
                            sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_RECEIPT_PRINT, null, recvVanPktBuf);
                        } else { //fail
                            sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_DEAL_COMPLETE, null, recvVanPktBuf);
                        }
                        break;
                }

                //Van Buf init
                recvVanPktBuf = Utils.randToByteArray(recvVanPktBuf.length);
                if (Utils.DEBUG_INIT) Utils.debugTx("rand init", recvVanPktBuf, recvVanPktBuf.length);
                Arrays.fill(recvVanPktBuf, (byte) 0x00);
                if (Utils.DEBUG_INIT) Utils.debugTx("0x00 init", recvVanPktBuf, recvVanPktBuf.length);
                Arrays.fill(recvVanPktBuf, (byte) 0xFF);
                if (Utils.DEBUG_INIT) Utils.debugTx("0xFF init", recvVanPktBuf, recvVanPktBuf.length);


            } else {
                device.apps.pmpos.aidl.Callback.CallBackList failCallback = device.apps.pmpos.aidl.Callback.CallBackList.VAN_FAIL_READ_DATA;
                switch (msg.what) {
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_FAIL:
                        failCallback = device.apps.pmpos.aidl.Callback.CallBackList.VAN_FAIL_READ_DATA;
                        break;
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_TIMEOUT:
                        failCallback = device.apps.pmpos.aidl.Callback.CallBackList.VAN_FAIL_READ_TIMEOUT;
                        break;
                    case Utils.MSG_VANHOST_CONNECTION_ERROR:
                        failCallback = device.apps.pmpos.aidl.Callback.CallBackList.VAN_FAIL_SERVER_ERROR;
                        break;
                }
                sendToCallback(failCallback, null, null);
                return;
            }
        }

        private String parsingForResultCode(byte[] recvVanPktBuf) {
            byte[] ansCode = new byte[4];
            System.arraycopy(recvVanPktBuf, 61, ansCode, 0, ansCode.length);
            return new String(ansCode);
        }

        private final int MAX_TRANS_SEQ_NUM = 9999;

        private void setTransSeqNum() {
            int transSeqNum = Integer.parseInt(Utils.getSharedPrefTransSeqNumValue(mContext));

            if (transSeqNum == MAX_TRANS_SEQ_NUM) {
                transSeqNum = 0;
            } else {
                transSeqNum++;
            }
            Utils.setSharedPrefTransSeqNumValue(mContext, String.valueOf(transSeqNum));
        }
    };


    public void create() {
        mICReaderPacketManager = new ICReaderPacketManager();
        mVanHostPacketManager = new VanHostPacketManager(mContext);
        mRdiManager = new RdiManager(mContext);
        mVanManager = new VanManager(mContext);
        mReceiptPrintData = new ReceiptPrintData(mContext);
    }
    public void destroy() {
        mICReaderPacketManager =null;
        mVanHostPacketManager =null;
        mRdiManager =null;
        mVanManager=null;
        mReceiptPrintData =null;
    }

    int rdiOnCount = 0;
    int MAX_RdiOnCount = 5;

    public void onRdi() {
        create();
        mSelfProtection = false;
        mReqDevicesInfo = false;

        //rooting check
        if(RootingDeviceCheck()){
            sendToCallback(Callback.CallBackList.CANCEL_DEVICE_ROOTING ,null,null);
            return;
        }
        //device Download check
        String DownloadFlag = Utils.getSharedPreference(mContext, Utils.PREF_KEY_DOWNLOAD_FLAG);
        if (DownloadFlag.equals(Utils.PREF_KEY_FLAG_STR_TRUE)) {
            setNativeDeviceInfo();
        } else {
            sendToCallback(Callback.CallBackList.CANCEL_DEVICE_DOWNLOAD ,null,null);
            return;
        }
        boolean setFlag = false;
        if (mRdiManager == null) {
            mRdiManager = new RdiManager(mContext);
        }
        if (mRdiManager != null) {
            mRdiManager.setEnable(mRdiManager.DISABLE);
            mRdiManager.powerDown();
            setFlag = mRdiManager.powerOn();
        }
        rdiOnCount++;
        if (setFlag) {
            reqDeviceInfo();
            rdiOnCount = 0;
        } else {
            if (rdiOnCount < MAX_RdiOnCount) {
                onRdi();
            } else {
                Utils.showToastMessage(mContext, TAG, mContext.getResources().getString(R.string.result_icreader_connection_error));
                sendToCallback(Callback.CallBackList.RDI_FAIL_CONNECT_ERROR, null, null);
                rdiOnCount = 0;
            }
            return;
        }
    }


    public void offRdi() {
        boolean setFlag = false;
        if (mRdiManager != null) {
            if (mRdiManager.isEnabled() == mRdiManager.ENABLE) {
                mRdiManager.setEnable(mRdiManager.DISABLE);
            }
            mRdiManager.powerDown();
            setFlag = true;
        } else {
            setFlag = false;
        }
        destroy();
    }

    public void setupRdi() {
        if (mRdiManager == null) {
            onRdi();
        }

        if (mRdiManager != null) {
            if (mRdiManager.isEnabled() != mRdiManager.ENABLE) {
                mRdiManager.setEnable(mRdiManager.ENABLE);
            }
            int resclear = mRdiManager.clear();
        }
    }

    public void disableRdi() {
        if (mRdiManager != null) {
            if (mRdiManager.isEnabled() != mRdiManager.DISABLE) {
                mRdiManager.setEnable(mRdiManager.DISABLE);
            }
            int resclear = mRdiManager.clear();
        } else {
        }
    }

    public void writeDataToRdi(byte[] data, int size, int readTimeout) {
        Utils.setSharedPreferenceInt(mContext, Utils.PREF_KEY_IC_COMD_ID, mICReaderPacketManager.getCommandId(data));
        setupRdi();
        int result = mRdiManager.write(data, size);
        mRdiManager.readData(mHandler, readTimeout);
    }




    public void reqPreambleInit(Callback.OnCallBack callback, String paymentOption, String trdType, String amount, String fee, String surTax, String instalment, String keyIn, String cashReceiptType) {
        mCallback = callback;
        if(callback == null){
            mIsCalledByDeamon = true;
        }else{
            mIsCalledByDeamon = false;
        }
        mApprovalValue = new ApprovalValue();
        mApprovalValue.mPaymentOption = paymentOption;
        mApprovalValue.mTrdType = trdType;
        mApprovalValue.mAmount = amount;
        mApprovalValue.mFee = fee;
        mApprovalValue.mSurTax = surTax;
        mApprovalValue.mInstalment = instalment;
        mApprovalValue.mKeyIn = keyIn;
        mApprovalValue.mCashReceiptType = cashReceiptType;

        switch (mApprovalValue.mTrdType) {
            case Utils.TRD_TYPE_IC_CREDIT_APPROVAL:
                sendToCallback(Callback.CallBackList.GOTO_IC_READ_SCENE, null, null);
                requestPreambleIFM(mApprovalValue);
                break;
            case Utils.TRD_TYPE_CASH_APPROVAL:
                if (mApprovalValue.mPaymentOption.equals(Utils.PAYMENT_OPTION_CASH)) {
                    sendToCallback(Callback.CallBackList.GOTO_PROGRESS, null, null);
                    approvalRequestVAN(null);
                } else {
                    sendToCallback(Callback.CallBackList.GOTO_MS_SWIPE_SCENE, null, null);
                    requestPreambleIFM(mApprovalValue);
                }
                break;
        }
    }

    public void reqPreambleCancelInit(Callback.OnCallBack callback, String paymentOption, String trdType, String amount, String fee, String surTax, String instalment, String keyIn, String orgApprovalDate, String orgApprovalNo) {
        mCallback = callback;
        if(callback == null){
            mIsCalledByDeamon = true;
        }else{
            mIsCalledByDeamon = false;
        }
        mApprovalValue = new ApprovalValue();
        mApprovalValue.mPaymentOption = paymentOption;
        mApprovalValue.mTrdType = trdType;
        mApprovalValue.mAmount = amount;
        mApprovalValue.mFee = fee;
        mApprovalValue.mSurTax = surTax;
        mApprovalValue.mInstalment = instalment;
        mApprovalValue.mKeyIn = keyIn;

        mApprovalValue.mOrgApprovalDate = orgApprovalDate;
        mApprovalValue.mOrgApprovalNo = orgApprovalNo;


        switch (mApprovalValue.mTrdType) {
            case Utils.TRD_TYPE_IC_CREDIT_CANCEL:
                sendToCallback(Callback.CallBackList.GOTO_IC_READ_SCENE, null, null);
                requestPreambleIFM(mApprovalValue);
                break;
            case Utils.TRD_TYPE_CASH_CANCEL:
                if (mApprovalValue.mPaymentOption.equals(Utils.PAYMENT_OPTION_CASH)) {
                    sendToCallback(Callback.CallBackList.GOTO_PROGRESS, null, null);
                    approvalRequestVAN(null);
                } else {
                    sendToCallback(Callback.CallBackList.GOTO_MS_SWIPE_SCENE, null, null);
                    requestPreambleIFM(mApprovalValue);
                }
                break;
        }

    }
    //rdi
    public void reqDeviceInfo() {

        byte[] rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null, 0, Utils.CMD_ID_REQ_DEVICE_INFO);
        if (rdPktBuf == null) {
            Utils.showToastMessage(mContext, "",  mContext.getResources().getString(R.string.result_icreader_make_data_fail));
            return;
        }
        writeDataToRdi(rdPktBuf, rdPktBuf.length, Utils.TIMEOUT_3SEC);
    }

    public void reqSelfProtection() {
        byte[] rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null, 0, Utils.CMD_ID_REQ_SELF_PROTECTION);
        if (rdPktBuf == null) {
            Utils.showToastMessage(mContext, "", mContext.getResources().getString(R.string.result_icreader_make_data_fail));
            return;
        }
        writeDataToRdi(rdPktBuf, rdPktBuf.length, Utils.TIMEOUT_3SEC);
    }

    public void faildInitASpopup() {
        if (mContext != null) {
            String failMessage = mContext.getResources().getString(R.string.dialog_error_msg_integrity_popup);
            dialogStart(Utils.popup_Simple,null,failMessage,null);
        }
        mSelfProtection = false;
    }

    public void requestPreambleIFM(ApprovalValue approvalValue) {
        byte[] rdPktBuf = mICReaderPacketManager.makeICReaderPacketIFM(null, Utils.CMD_ID_REQ_IC_PREAMBLE, approvalValue);
        if (rdPktBuf != null) {
            writeDataToRdi(rdPktBuf, rdPktBuf.length, Utils.TIMEOUT_3MIN);
        } else {
            sendToCallback(Callback.CallBackList.RDI_FAIL_MAKE_PACKET, null, null);
        }
    }

    public void reqTransactionResultIFM(byte[] recvVanPktBuf) {
        byte[] rdPktBuf = mICReaderPacketManager.makeICReaderPacketIFM(recvVanPktBuf, Utils.CMD_ID_REQ_IC_TRANSACTION_RESULT, mApprovalValue);
        if (rdPktBuf != null) {
            writeDataToRdi(rdPktBuf, rdPktBuf.length, Utils.TIMEOUT_3SEC);
        } else {
            sendToCallback(Callback.CallBackList.RDI_FAIL_MAKE_PACKET, null, null);
        }

    }

    public void cancelPaymentIFM() {
        byte[] rdPktBuf = mICReaderPacketManager.makeICReaderPacketIFM(null, Utils.CMD_ID_REQ_IC_CANCEL_PAYMENT, mApprovalValue);
        if (rdPktBuf != null) {
            writeDataToRdi(rdPktBuf, rdPktBuf.length, Utils.TIMEOUT_3SEC);
        } else {
            sendToCallback(Callback.CallBackList.RDI_FAIL_MAKE_PACKET, null, null);
        }
    }

    public void sendDataToVan(byte[] data) {
        if (data == null || data.length <= 0) {
            sendToCallback(Callback.CallBackList.GOTO_MAIN, null, null);
            return;
        }

        if (mVanManager != null) {
            mVanManager.requestDataToVanHost(data, false, mVanHandler);
        } else {
            sendToCallback(Callback.CallBackList.VAN_FAIL_SERVER_ERROR, null, null);
        }
    }


    public void approvalRequestVAN(byte[] recvRdPktBuf) {
        byte[] vanPktBuf = null;

        switch (mApprovalValue.mTrdType) {
            case Utils.TRD_TYPE_IC_CREDIT_APPROVAL: //F1
                switch (mApprovalValue.mPaymentOption) {
                    case Utils.PAYMENT_OPTION_IC:
                        vanPktBuf = mVanHostPacketManager.makeApprovalRequestPacket(recvRdPktBuf, mApprovalValue, null, null);
                        break;
                    case Utils.PAYMENT_OPTION_MS:
                        String resultCode = Utils.getSharedPrefResultCodeValue(mContext);
                        vanPktBuf = mVanHostPacketManager.makeApprovalRequestPacket(recvRdPktBuf, mApprovalValue, resultCode, null);
                        break;
                }
                break;

            case Utils.TRD_TYPE_CASH_APPROVAL:
                switch (mApprovalValue.mPaymentOption) {
                    case Utils.PAYMENT_OPTION_MS:
                        vanPktBuf = mVanHostPacketManager.makeCashApprovalRequestPacket(recvRdPktBuf, mApprovalValue);
                        break;
                    case Utils.PAYMENT_OPTION_CASH:
                        vanPktBuf = mVanHostPacketManager.makeCashApprovalRequestPacket(null, mApprovalValue);
                        break;
                }
                break;
        }
        if (vanPktBuf != null) {
            sendToCallback(Callback.CallBackList.GOTO_PROGRESS, null, null);
            sendDataToVan(vanPktBuf);
        } else {
            sendToCallback(Callback.CallBackList.VAN_FAIL_MAKE_PACKET, null, null);
        }
    }


    // F2  0
    public void normalCancelVAN(byte[] recvRdPktBuf) {

        byte[] vanPktBuf = mVanHostPacketManager.makeNormalCancelPacket(recvRdPktBuf, mApprovalValue);
        if (vanPktBuf != null) {
            if (Utils.DEBUG_INFO) {
                Log.i(TAG, "*************************************************");
                Log.e(TAG, "< SUCCESS > make Cash approval request packet - V");
                Log.i(TAG, "*************************************************");
            }
            sendToCallback(Callback.CallBackList.GOTO_PROGRESS, null, null);
            sendDataToVan(vanPktBuf);
        } else {
            sendToCallback(Callback.CallBackList.VAN_FAIL_MAKE_PACKET, null, null);
        }
    }

    //F2  3
    public void icCardCancelVAN() {
        byte[] vanPktBuf = mVanHostPacketManager.makeIcCardCancelPacket(mApprovalValue);
        if (vanPktBuf != null) {
            sendToCallback(Callback.CallBackList.GOTO_PROGRESS, null, null);
            sendDataToVan(vanPktBuf);
        } else {
            sendToCallback(Callback.CallBackList.VAN_FAIL_MAKE_PACKET, null, null);
        }
    }

    //F2  1
    public void networkCommunicationCancelVAN() {
        byte[] vanPktBuf = mVanHostPacketManager.makeNetworkCommunicationCancelPacket(mApprovalValue);
        Utils.showToastMessage(mContext, "", mContext.getResources().getString(R.string.toast_msg_network_cancel));
        if (vanPktBuf != null) {
            sendToCallback(Callback.CallBackList.GOTO_PROGRESS, null, null);
            sendDataToVan(vanPktBuf);
        } else {
            sendToCallback(Callback.CallBackList.VAN_FAIL_MAKE_PACKET, null, null);
        }
    }


    public void sendToCallback(Callback.CallBackList callBackList, byte[] recvRdPktBuf, byte[] recvVanPktBuf) {
        if (mCallback != null) {
            mCallback.OnCallBack(callBackList, recvRdPktBuf, recvVanPktBuf);
        }
        if(callBackList == Callback.CallBackList.GOTO_DEAL_COMPLETE || callBackList == Callback.CallBackList.GOTO_RECEIPT_PRINT){
            mApprovalValue = null;
        }
        if (mIsCalledByDeamon) {
            sendToDaemon(callBackList, recvVanPktBuf);
        }
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        mContext = this;
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    Uri mRecvDaemonUri ;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mContext = this;
        if(intent != null) {
            mRecvDaemonUri= intent.getData();
            startDeamonService(Utils.DEAMON_FLAG);
        }
        return super.onStartCommand(intent, flags, startId);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (Utils.DEBUG_VALUE) Log.d(TAG, "service onDestroy");
    }

    boolean mRdiOnInitThreadFlag= true;
    public void startDeamonService(int flag) {
        mIsCalledByDeamon = true;
        mRdiOnInitThreadFlag= true;
        onRdi();

        new Thread(new Runnable() {
            @Override
            public void run() {
                long timeout = 5000;
                long stime = new Date().getTime();
                long rtime = 0L;
                while (rtime < timeout && mRdiOnInitThreadFlag && !Thread.interrupted()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        mRdiOnInitThreadFlag = false;
                    }
                    rtime = System.currentTimeMillis() - stime;
                    if (rtime > timeout) {
                        mRdiOnInitThreadFlag = false;
                    }
                }

                if (mSelfProtection) {
                    mRdiOnInitThreadFlag = false;
                    Message msg = Message.obtain();
                    msg.what = HANDLER_MSG_DAEMON_RDI_ON;
                    mDaemonRdiOnHandler.sendMessage(msg);
                }
            }
        }).start();

    }

    @SuppressLint("HandlerLeak")
    private Handler mDaemonRdiOnHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int state = msg.what;
            switch (state){
                case HANDLER_MSG_DAEMON_RDI_ON:
                    recvDaemonData();
                    break;
            }
        }
    };

    public class ApprovalValue {
        public String mPaymentOption;
        public String mTrdType;
        public String mAmount;
        public String mFee;
        public String mSurTax;
        public String mInstalment;
        public String mKeyIn;
        public String mCashReceiptType;
        public String mOrgApprovalDate;
        public String mOrgApprovalNo;
        public String mKsn;
    }

    private void recvDaemonData() {
        if (mRecvDaemonUri != null) {
            String trdType = mRecvDaemonUri.getQueryParameter(Utils.trdtype);
            String amount = mRecvDaemonUri.getQueryParameter(Utils.amount);
            String fee = mRecvDaemonUri.getQueryParameter(Utils.fee);
            String surTax = mRecvDaemonUri.getQueryParameter(Utils.surtax);
            String instalment = mRecvDaemonUri.getQueryParameter(Utils.installment);
            String keyIn = mRecvDaemonUri.getQueryParameter(Utils.keyin);
            String cashReceiptType = mRecvDaemonUri.getQueryParameter(Utils.cash_receipt_type);
            String orgApprovalDate = mRecvDaemonUri.getQueryParameter(Utils.org_approval_date);
            String orgApprovalNo = mRecvDaemonUri.getQueryParameter(Utils.org_approval_no);

            if (!isDaemonData(fee)) {
                fee = "0";
            } else if (!isDaemonData(surTax)) {
                surTax = "0";
            } else if (!isDaemonData(instalment)) {
                instalment = "00";
            } else if (!isDaemonData(keyIn)) {
                keyIn = null;
            } else if (!isDaemonData(cashReceiptType)) {
                cashReceiptType = null;
            } else if (!isDaemonData(orgApprovalDate)) {
                orgApprovalDate = null;
            } else if (!isDaemonData(orgApprovalNo)) {
                orgApprovalNo = null;
            }

            if (isDaemonData(trdType) && isDaemonData(amount)) {
                String paymentOption = Utils.PAYMENT_OPTION_IC;

                if (trdType.equals(Utils.TRD_TYPE_IC_CREDIT_APPROVAL)) {
                    paymentOption = Utils.PAYMENT_OPTION_IC;
                    if (!isDaemonData(instalment)) {
                        instalment = "00";
                    }
                    reqPreambleInit(null, paymentOption, trdType, amount, fee, surTax, instalment, keyIn, cashReceiptType);
                } else if (trdType.equals(Utils.TRD_TYPE_CASH_APPROVAL)) {
                    if (isDaemonData(keyIn)) {
                        if (isDaemonData(cashReceiptType)) {
                            paymentOption = Utils.PAYMENT_OPTION_CASH;
                        } else {
                            toastFailedRecvDaemonData();
                        }
                    } else {
                        paymentOption = Utils.PAYMENT_OPTION_MS;
                    }
                    reqPreambleInit(null, paymentOption, trdType, amount, fee, surTax, instalment, keyIn, cashReceiptType);

                } else if (trdType.equals(Utils.TRD_TYPE_CASH_CANCEL) || trdType.equals(Utils.TRD_TYPE_IC_CREDIT_CANCEL)) {
                    if (isDaemonData(orgApprovalDate) && isDaemonData(orgApprovalNo)) {
                        reqPreambleCancelInit(null, paymentOption, trdType, amount, fee, surTax, instalment, keyIn, orgApprovalDate, orgApprovalNo);
                    } else {
                        toastFailedRecvDaemonData();
                    }
                }
            } else {
                toastFailedRecvDaemonData();
            }
        } else {
            toastFailedRecvDaemonData();
        }

    }

    private void toastFailedRecvDaemonData(){
        Utils.showToastMessage(mContext, "", mContext.getResources().getString(R.string.demon_no_response_packet));
    }

    private boolean isDaemonData(String data) {
        boolean retValue = false;
        if (!data.isEmpty() && (data.length() != 0)) {
            retValue = true;
        } else {
            retValue = false;
        }
        return retValue;
    }


    public void sendToDaemon(Callback.CallBackList callBackList, byte[] sendBuf) {
        byte[] resultCode = Utils.PAYMENT_COMPLETE; //성공
        byte[] resultMessage = Utils.PAYMENT_CANCEL_BUF;
        switch (callBackList) {
            case GOTO_DEAL_COMPLETE:
            case GOTO_RECEIPT_PRINT:
                resultCode = Utils.PAYMENT_COMPLETE;
                break;

            case RDI_FAIL_MAKE_PACKET:
                resultMessage = Utils.RDI_FAIL_MAKE_PACKET_BUF;
                resultCode = Utils.RDI_COMMUNICATION_FAILURE;
                break;
            case RDI_FAIL_READ_DATA:
                resultMessage = Utils.RDI_FAIL_READ_DATA_BUF;
                resultCode = Utils.RDI_COMMUNICATION_FAILURE;
                break;
            case RDI_FAIL_READ_TIMEOUT:
                resultMessage = Utils.RDI_FAIL_READ_TIMEOUT_BUF;
                resultCode = Utils.RDI_COMMUNICATION_FAILURE;
                break;

            case VAN_FAIL_MAKE_PACKET:
                resultMessage = Utils.VAN_FAIL_MAKE_PACKET_BUF;
                resultCode = Utils.VAN_COMMUNICATION_FAILURE;
                break;
            case VAN_FAIL_READ_DATA:
                resultMessage = Utils.VAN_FAIL_READ_DATA_BUF;
                resultCode = Utils.VAN_COMMUNICATION_FAILURE;
                break;
            case VAN_FAIL_READ_TIMEOUT:
                resultMessage = Utils.VAN_FAIL_READ_TIMEOUT_BUF;
                resultCode = Utils.VAN_COMMUNICATION_FAILURE;
                break;
            case VAN_FAIL_SERVER_ERROR:
                resultMessage = Utils.VAN_FAIL_SERVER_ERROR_BUF;
                resultCode = Utils.VAN_COMMUNICATION_FAILURE;
                break;

            case GOTO_MAIN:
                resultMessage = Utils.PAYMENT_CANCEL_BUF;
                resultCode = Utils.PAYMENT_CANCEL;
                break;
            case CANCEL_DEVICE_ROOTING: //거래불가 루팅
                resultMessage = Utils.DEVICE_ROOTING_BUF;
                resultCode = Utils.PAYMENT_CANCEL;
                break;
            case CANCEL_DEVICE_DOWNLOAD: //not Device Download
                resultMessage = Utils.ISNOT_DEVICE_DOWNLOAD_BUF;
                resultCode = Utils.PAYMENT_CANCEL;
                break;

            case GOTO_IC_READ_SCENE:
                toastForDaemon(mContext.getResources().getString(R.string.toast_msg_ic_card_redding));
                return;
            case GOTO_MS_SWIPE_SCENE:
                toastForDaemon(mContext.getResources().getString(R.string.toast_msg_ms_card_redding));
                return;
            case GOTO_PROGRESS:
                toastForDaemon(mContext.getResources().getString(R.string.toast_msg_van_communicating));
                return;

        }

        if (resultCode.equals(Utils.PAYMENT_COMPLETE)) {
            if(callBackList==callBackList.GOTO_RECEIPT_PRINT){
                toastForDaemon(mContext.getResources().getString(R.string.toast_msg_payment_successful));
            }else{
                toastForDaemon(mContext.getResources().getString(R.string.toast_msg_payment_fail));
            }
        } else {
            toastForDaemon(mContext.getResources().getString(R.string.toast_msg_payment_fail));
            mReceiptPrintData.setFailPacketToDeamon(resultCode, resultMessage);
        }

        String sendStr = mReceiptPrintData.getDaemonCallbackStr();
        Intent intent = new Intent(Utils.ACTION_CALLBACK_DATA_TO_DAEMON);
        intent.setData(Uri.parse(sendStr));
        sendBroadcast(intent);

        mIsCalledByDeamon= false;
        offRdi();
        stopService(PMPosReceiver.ServiceIntent);
    }

    public void toastForDaemon(String message) {
        if (mIsCalledByDeamon) {
            Utils.showToastMessage(mContext, "", message);
        }
    }


    Button mBtnComplete;
    private TouchDrawer.TouchTestResultCallback mResultCallback = new TouchDrawer.TouchTestResultCallback() {
        @Override
        public void onTouchTestResult(int result) {
            if (result == TouchDrawer.TEST_RESULT_SUCCESS) {
                mBtnComplete.setEnabled(true);
            }
        }
    };

    public void signaturePopup(final byte[] recvRdPktBuf) {
        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_signature_dialog, null);

        builder.setTitle(mContext.getResources().getString(R.string.dialog_signature_title));
        builder.setView(layout);
        final AlertDialog mAlertDialog = builder.create();

        mAlertDialog.setCancelable(false);

        final TouchDrawer mFrameDrawer = new TouchDrawer(mContext);
        mFrameDrawer.testResult(mResultCallback);
        mFrameDrawer.clear();

        final FrameLayout mSignatureFrame = (FrameLayout) layout.findViewById(R.id.signatureLayout);
        mSignatureFrame.addView(mFrameDrawer);

        final Button mBtnCancel = (Button) layout.findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelPaymentIFM();
                mAlertDialog.dismiss();
            }
        });

        Button mBtnClear = (Button) layout.findViewById(R.id.btnClear);
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFrameDrawer.clear();
            }
        });

        mBtnComplete = (Button) layout.findViewById(R.id.btnComplete);
        mBtnComplete.setEnabled(false);
        mBtnComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFrameDrawer.saveBitmapToFile(Utils.FILE_NAME_SIGNATURE);
                switch(mApprovalValue.mTrdType){
                    case Utils.TRD_TYPE_IC_CREDIT_CANCEL: //F2
                        normalCancelVAN(recvRdPktBuf);
                        break;
                    case Utils.TRD_TYPE_IC_CREDIT_APPROVAL://F1
                        approvalRequestVAN(recvRdPktBuf);
                        break;
                }
                mAlertDialog.dismiss();
            }
        });
        mAlertDialog.show();
    }
    public void icReadFailpopup(){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(mContext);
        builder.setTitle("");
        builder.setMessage( mContext.getResources().getString(R.string.msg_error_response_code_08));
        builder.setNegativeButton(mContext.getResources().getString(R.string.btn_label_finish), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                sendToCallback(Callback.CallBackList.GOTO_MAIN, null,null);
                dialog.dismiss();
            }
        });
        builder.setPositiveButton(mContext.getResources().getString(R.string.btn_label_continue_msr), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                sendToCallback(Callback.CallBackList.GOTO_MS_SWIPE_SCENE, null,null);
                mApprovalValue.mPaymentOption =  Utils.PAYMENT_OPTION_MS;
                requestPreambleIFM(mApprovalValue);
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void displayNoticeDialog(String messageResId, final int gotoFragmentNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(mContext.getString(R.string.dialog_builder_notice_title));
        builder.setMessage(messageResId);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(gotoFragmentNum == MSG_GOTO_IC_READ_SCENE){
                    sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_IC_READ_SCENE, null,null);
                    mApprovalValue.mPaymentOption = Utils.PAYMENT_OPTION_IC;
                }else{ //MSG_GOTO_MS_SWIPE_SCENE
                    sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MS_SWIPE_SCENE, null,null);
                    mApprovalValue.mPaymentOption = Utils.PAYMENT_OPTION_MS;
                }
                requestPreambleIFM(mApprovalValue);
            }
        });
        builder.create().show();
    }

    public void dialogStart(int dialogNum, String title, String message, byte[] RdpktBuf) {
        if (mIsCalledByDeamon == true) {
            Intent intent = new Intent(mContext, DialogBGActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.putExtra(Utils.KEY_EXTRA_DAILOG_NUM, dialogNum);
            intent.putExtra(Utils.KEY_EXTRA_DAILOG_TITLE, title);
            intent.putExtra(Utils.KEY_EXTRA_DAILOG_MESSAGE, message);
            mContext.startActivity(intent);
        } else {
            if (dialogNum == Utils.popup_Signature) {
                signaturePopup(RdpktBuf);
            } else if (dialogNum ==  Utils.popup_IcReadFail) {
                icReadFailpopup();
            } else if (dialogNum ==  Utils.popup_DisplayNoticeIC || dialogNum ==  Utils.popup_DisplayNoticeMS || dialogNum == Utils.popup_DisplayNoticeNOTPayment) {
                displayNoticeDialog(message, dialogNum);
            } else if (dialogNum ==  Utils.popup_Simple) {
                Utils.SimpleDailog(mContext, "", message);
            }
        }
    }



    public DialogListener.OnClickDialogCallback mDialogCreateCallback = new DialogListener.OnClickDialogCallback() {
        @Override
        public void OnClickDialogCallback(int onClickResultCode) {
            switch (onClickResultCode) {
                case Utils.popup_cancel:
                    sendToCallback(Callback.CallBackList.GOTO_MAIN, null, null);
                    break;

                case Utils.popup_Signature_cancel_btn:
                    cancelPaymentIFM();
                    break;
                case Utils.popup_Signature_complete_btn:
                    switch (mApprovalValue.mTrdType) {
                        case Utils.TRD_TYPE_IC_CREDIT_CANCEL: //F2
                            normalCancelVAN(recvRdPktBufForSign);
                            break;
                        case Utils.TRD_TYPE_IC_CREDIT_APPROVAL://F1
                            approvalRequestVAN(recvRdPktBufForSign);
                            break;
                    }
                    break;

                case Utils.popup_IcReadFail_finish_btn:
                    sendToCallback(Callback.CallBackList.GOTO_MAIN, null, null);
                    break;
                case Utils.popup_IcReadFail_msr_btn:
                    sendToCallback(Callback.CallBackList.GOTO_MS_SWIPE_SCENE, null, null);
                    mApprovalValue.mPaymentOption = Utils.PAYMENT_OPTION_MS;
                    requestPreambleIFM(mApprovalValue);
                    break;

                case Utils.popup_Simple_ok_btn:
                    break;

                case Utils.popup_DisplayNoticeIC_ok_btn:
                    sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_IC_READ_SCENE, null, null);
                    mApprovalValue.mPaymentOption = Utils.PAYMENT_OPTION_IC;
                    requestPreambleIFM(mApprovalValue);
                    break;
                case Utils.popup_DisplayNoticeMS_ok_btn:
                    sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MS_SWIPE_SCENE, null, null);
                    mApprovalValue.mPaymentOption = Utils.PAYMENT_OPTION_MS;
                    requestPreambleIFM(mApprovalValue);
                    break;
                case Utils.popup_DisplayNoticeNOTPaymen_ok_btn:
                    sendToCallback(device.apps.pmpos.aidl.Callback.CallBackList.GOTO_MAIN, null, null);
                    break;
            }
        }
    };


    public boolean RootingDeviceCheck(){
        boolean RootingCheck = false;
        try{
            Runtime.getRuntime().exec(rootingCheckCommand);
            RootingCheck = true;
        }catch (Exception e){
            RootingCheck = false;
        }
        return RootingCheck;
    }

    public void setNativeDeviceInfo() {
        AdminVanPacketManager adminVanPacketManager = new AdminVanPacketManager(mContext);
        adminVanPacketManager.initDeviceinfo();
    }

}