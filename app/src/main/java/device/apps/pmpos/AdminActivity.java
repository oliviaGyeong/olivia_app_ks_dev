package device.apps.pmpos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import device.apps.pmpos.packet.AdminVanPacketManager;
import device.apps.pmpos.packet.ICReaderPacketManager;
import device.apps.pmpos.packet.RecvDeviceInfoPacket;

public class AdminActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = AdminActivity.class.getSimpleName();

    private final int SUCCESS_ALL_INPUT_VALUE = 0;
    private final int ERROR_CURRENT_PWD_EMPTY = 1;
    private final int ERROR_NEW_PWD_EMPTY = 2;
    private final int ERROR_RETRY_PWD_EMPTY = 3;
    private final int ERROR_CURRENT_PWD_NOT_SAME_AS_USER_PWD = 4;
    private final int ERROR_NEW_PWD_NOT_SAME_AS_RETRY_PWD = 5;

    public final int  PERMISSIONS_REQUEST_EXTERNAL_STORAGE = 20;

    private Button mBtnUpdateFK;
    private Button mBtnRunSP;
    private Button mBtnCheckSPLog;
    private Button mBtnAuthenMA;
    private Button mBtnDownload;
    private Button mBtnDeviceFactoryReset;
    private Button mBtnIPSettings;
    private Button mBtnFWUpdate;
    private Button mBtnPwdChange;
    private TextView mTextResult;

    private Context mContext;
    private VanManager mVanManager;
    private ICReaderPacketManager mICReaderPacketManager;
    private AdminVanPacketManager mAdminVanPacketManager;
    private RdiManager mRdiManager;

    private DrawerLayout mDrawer;

    boolean BTN_ENABLE = true;
    boolean BTN_DISABLE = false;
    boolean checkBackPreaseFlag = true;

    private boolean mSelfProtection = false;
    private boolean mReqDevicesInfo = false;
    int rdiOnCount = 0;
    int MAX_RdiOnCount = 5;
    boolean mRdiOnInitThreadFlag= true;

    private String rootingCheckCommand = "su";
    private String rdi_ResultCode_ok = "00";
    private String rdi_ResultCode_fail_1 = "01";
    private String rdi_ResultCode_fail_2 = "02";




    @SuppressLint("HandlerLeak")
    private Handler mVanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String resultMsg = "";
            byte[] rdPktBuf = null;
            byte[] recvVanPktBuf = (byte[]) msg.obj;

            switch (msg.what) {
                case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_FAIL:
                    resultMsg = getResources().getString(R.string.vanhost_msg_data_fail);
                    setResultText(resultMsg);
                    finishProcess();
                    break;
                case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_TIMEOUT:
                    resultMsg = getResources().getString(R.string.vanhost_msg_timeout);
                    setResultText(resultMsg);
                    finishProcess();
                    break;
                case Utils.MSG_VANHOST_CONNECTION_ERROR:
                    resultMsg = getResources().getString(R.string.vanhost_msg_cnnection_error);
                    setResultText(resultMsg);
                    finishProcess();
                    break;
                case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK:
                    if (recvVanPktBuf != null) {
                        switch (Utils.checkTrdType(recvVanPktBuf)) {
                            case Utils.TRD_TYPE_FUTURE_KEY_UPDATE:
                                rdPktBuf = mICReaderPacketManager.makeICReaderPacket(recvVanPktBuf, recvVanPktBuf.length, Utils.CMD_ID_REQ_UPDATE_FUTURE_KEY);
                                sendDataToMSR(rdPktBuf);
                                break;

                            case Utils.TRD_TYPE_MUTUAL_AUTHENTICATION:
                                rdPktBuf = mICReaderPacketManager.makeICReaderPacket(recvVanPktBuf, recvVanPktBuf.length, Utils.CMD_ID_REQ_MUTUAL_AUTH_INFO_RESULT);
                                sendDataToMSR(rdPktBuf);

                                break;

                            case Utils.TRD_TYPE_DEVICE_DOWNLOAD:
                                mAdminVanPacketManager.setDeviceInfo(recvVanPktBuf);
                                byte[] resultCodeBuf = new byte[4];
                                byte[] resultMsgBuf = new byte[40];
                                String resultDIMsg;

                                System.arraycopy(recvVanPktBuf, 61, resultCodeBuf, 0, resultCodeBuf.length);
                                System.arraycopy(recvVanPktBuf, 85, resultMsgBuf, 0, resultMsgBuf.length);
                                byte[] successBuf = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
                                if (resultCodeBuf == successBuf) {
                                    setResultText(getResources().getString(R.string.result_download_success));
                                } else {
                                    resultDIMsg = Utils.convertByteArrayToKoreanEncodeStr(resultMsgBuf);
                                    setResultText(resultDIMsg);
                                }
                                finishProcess();
                                break;
                        }
                        break;
                    }
            }
        }
    };


    public BroadcastReceiver mReceiveDataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(Utils.ACTION_RECEIVE_DATA_FROM_VANHOST_TO_ADMIN)) {
                String resultMsg = "";
                byte[] rdPktBuf = null;
                byte[] vanPktBuf = intent.getByteArrayExtra(Utils.KEY_EXTRA_VANHOST_DATA);

                int state = intent.getIntExtra(Utils.KEY_EXTRA_VANHOST_MSG, -1);
                switch (state) {
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_FAIL:
                        resultMsg = getResources().getString(R.string.vanhost_msg_data_fail);
                        setResultText(resultMsg);
                        finishProcess();
                        break;
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_TIMEOUT:
                        resultMsg = getResources().getString(R.string.vanhost_msg_timeout);
                        setResultText(resultMsg);
                        finishProcess();
                        break;
                    case Utils.MSG_VANHOST_CONNECTION_ERROR:
                        resultMsg = getResources().getString(R.string.vanhost_msg_cnnection_error);
                        setResultText(resultMsg);
                        finishProcess();
                        break;
                    case Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK:
                        byte[] recvVanPktBuf = intent.getByteArrayExtra(Utils.KEY_EXTRA_VANHOST_DATA);
                        if (recvVanPktBuf != null) {
                        }
                        break;
                }

                if (vanPktBuf != null) {
                    switch (Utils.checkTrdType(vanPktBuf)) {
                        case Utils.TRD_TYPE_FUTURE_KEY_UPDATE:
                            rdPktBuf = mICReaderPacketManager.makeICReaderPacket(vanPktBuf, vanPktBuf.length, Utils.CMD_ID_REQ_UPDATE_FUTURE_KEY);
                            sendDataToMSR(rdPktBuf);
                            break;

                        case Utils.TRD_TYPE_MUTUAL_AUTHENTICATION:
                            rdPktBuf = mICReaderPacketManager.makeICReaderPacket(vanPktBuf, vanPktBuf.length, Utils.CMD_ID_REQ_MUTUAL_AUTH_INFO_RESULT);
                            sendDataToMSR(rdPktBuf);

                            break;

                        case Utils.TRD_TYPE_DEVICE_DOWNLOAD:
                            mAdminVanPacketManager.setDeviceInfo(vanPktBuf);
                            byte[] resultCodeBuf = new byte[4];
                            byte[] resultMsgBuf = new byte[40];
                            String resultDIMsg;

                            System.arraycopy(vanPktBuf, 61,resultCodeBuf,0,resultCodeBuf.length );
                            System.arraycopy(vanPktBuf, 85,resultMsgBuf,0,resultMsgBuf.length );
                            byte[] successBuf = new byte[]{(byte)0x00,(byte)0x00 ,(byte)0x00 ,(byte)0x00};
                            if(resultCodeBuf == successBuf ){
                                setResultText(getResources().getString(R.string.result_download_success));
                            }else{
                                resultDIMsg =  Utils.convertByteArrayToKoreanEncodeStr(resultMsgBuf);
                                setResultText(resultDIMsg);
                            }
                            finishProcess();
                            break;

                        default:
                            finishProcess();
                            break;
                    }
                }

            }
        }
    };


    @SuppressLint("HandlerLeak")
    private Handler mMSRHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int comdID = Utils.getSharedPreferenceInt(mContext,Utils.PREF_KEY_IC_COMD_ID);

            switch (msg.what) {
                case Utils.MSG_RECEIVE_IC_READER_DATA_OK:
                    byte[] recvRdPktBuf = (byte[]) msg.obj;

                    byte[] vanPktBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];

                    switch (recvRdPktBuf[3]) {
                        case Utils.CMD_ID_RECV_CREATE_FUTURE_KEY:
                            vanPktBuf = mAdminVanPacketManager.makeVanPaket(recvRdPktBuf,recvRdPktBuf.length,Utils.TRD_TYPE_FUTURE_KEY_UPDATE);
                            sendDataToVan(vanPktBuf);
                            break;

                        case Utils.CMD_ID_RECV_DEVICE_INFO:
                            if(mIsProcess){
                                downloadInputPopup(recvRdPktBuf);
                            }
                            else{
                                String dvTag = "";
                                String dvMessage = mContext.getResources().getString(R.string.toast_msg_device_info);

                                RecvDeviceInfoPacket deviceInfoPacket = new RecvDeviceInfoPacket();
                                deviceInfoPacket.setRecvDeviceInfo(recvRdPktBuf);

                                String certificationNumStr = Utils.byteArrayToHex(deviceInfoPacket.getCertificationNum());
                                Utils.setSharedPreference(mContext, Utils.PREF_KEY_AUTHENNUM, certificationNumStr);

                                byte[] isSecurityKey = deviceInfoPacket.getIsSecurityKey();
                                if (new String(isSecurityKey).equals(rdi_ResultCode_ok)) {
                                    Utils.setSharedPreference(mContext, Utils.PREF_KEY_FUTUREFW_FLAG, Utils.PREF_KEY_FLAG_STR_TRUE);
                                    Utils.showToastMessage(mContext, dvTag, dvMessage);
                                    mReqDevicesInfo = true;
                                    reqSelfProtection();
                                } else {
                                    mReqDevicesInfo = false;
                                    Utils.setSharedPreference(mContext, Utils.PREF_KEY_FUTUREFW_FLAG, Utils.PREF_KEY_FLAG_STR_FALSE);
                                    Utils.showToastMessage(mContext, "", mContext.getResources().getString(R.string.toast_msg_device_info_not_have_fk)); //TODO: string 변경 해야함.
                                }
                            }
                            break;

                        case Utils.CMD_ID_RECV_SELF_PROTECTION:
                            byte[] resultSPBuf = mICReaderPacketManager.getRecvResult(recvRdPktBuf);
                            String mSelfProtectionResult = new String(resultSPBuf);
                            if(mIsProcess){
                                if(mSelfProtectionResult.equals(rdi_ResultCode_ok)){
                                }else {
                                    faildInitSelfProtection();
                                }
                                String spResult = Utils.setSuccessFailStr(mSelfProtectionResult);
                                Utils.saveSPLog(mContext,spResult, "");

                                setResultText(spResult);
                                finishProcess();
                            }else{
                                if (mSelfProtectionResult.equals(rdi_ResultCode_ok)) {
                                    String sptag = "";
                                    String spmessage = mContext.getResources().getString(R.string.toast_msg_selftest_ok);
                                    Utils.showToastMessage(mContext, sptag, spmessage);
                                    mSelfProtection = true;
                                } else {
                                    mSelfProtection = false;
                                    faildInitSelfProtection();
                                }
                                String spResult = Utils.setSuccessFailStr(mSelfProtectionResult);
                                Utils.saveSPLog(mContext, spResult, "");
                                mRdiOnInitThreadFlag = false;
                            }
                            break;

                        case Utils.CMD_ID_RECV_MUTUAL_AUTHENTICATION:
                            vanPktBuf = mAdminVanPacketManager.makeVanPaket(recvRdPktBuf,recvRdPktBuf.length,Utils.TRD_TYPE_MUTUAL_AUTHENTICATION);
                            sendDataToVan(vanPktBuf);
                            break;

                        case Utils.CMD_ID_RECV_MUTUAL_AUTH_INFO_RESULT:
                            byte[] resultMABuf = mICReaderPacketManager.getRecvResult(recvRdPktBuf);
                            String mMutualAuthResultResult = Utils.byteArrayToHex(resultMABuf);
                            String maResult = Utils.setSuccessFailStr(mMutualAuthResultResult);

                            setResultText(maResult);
                            finishProcess();
                            break;

                        case Utils.CMD_ID_RECV_DEVICE_FACTORY_RESET:
                            byte[] resultFRBuf = mICReaderPacketManager.getRecvResult(recvRdPktBuf);

                            String mFactoryResetResult= Utils.byteArrayToHex(resultFRBuf);
                            String frResult = Utils.setSuccessFailStr(mFactoryResetResult);

                            setResultText(frResult);
                            finishProcess();
                            break;

                        case Utils.CMD_ID_RECV_DEVICE_FIRMWERE_UPDATE:
                            byte[] resultFWBuf = mICReaderPacketManager.getRecvResult(recvRdPktBuf);
                            String mFWStrResult = new String(resultFWBuf);
                            String mFWUpdateResult= Utils.byteArrayToHex(resultFWBuf);
                            String fwResult = Utils.setSuccessFailStr(mFWUpdateResult);

                            byte[] resultFWMsgBuf =  mICReaderPacketManager.getRecvMassege(recvRdPktBuf);
                            String fwResultMsg = new String(resultFWMsgBuf);

                            if(mFWStrResult.equals(rdi_ResultCode_ok)){
                                if(mICReaderPacketManager.remindFileSize >0){
                                    setProgressbarDialog((mICReaderPacketManager.mTotalFileSize - mICReaderPacketManager.remindFileSize),mICReaderPacketManager.mTotalFileSize);
                                    fwUpdateSendPacket(null);
                                }else{
                                    stopProgressbarDialog();
                                    Utils.SimpleDailog(mContext,getResources().getString(R.string.update_fw_title), getResources().getString(R.string.update_fw_msg_complete));
                                }
                            }else if(mFWStrResult.equals(rdi_ResultCode_fail_1)){
                                stopProgressbarDialog();
                                setResultText(fwResult+": "+fwResultMsg);
                                finishProcess();
                            }else if(mFWStrResult.equals(rdi_ResultCode_fail_2)){
                                stopProgressbarDialog();
                                setResultText(fwResult+": "+fwResultMsg);
                                finishProcess();
                            }else{
                                stopProgressbarDialog();
                                setResultText(fwResult+": "+getResources().getString(R.string.fail));
                                finishProcess();
                            }
                            break;

                        case Utils.CMD_ID_RECV_ACK:
                            if((byte)comdID == Utils.CMD_ID_REQ_CREATE_FUTURE_KEY){
                                setResultText(getResources().getString(R.string.result_ack));
                                finishProcess();
                            }else if((byte)comdID == Utils.CMD_ID_REQ_INIT_SYSTEM_DATA_TIME){
                            }
                            setResultText(getResources().getString(R.string.result_ack));
                            finishProcess();
                            break;
                        case Utils.CMD_ID_RECV_NAK:
                            if((byte)comdID == Utils.CMD_ID_REQ_CREATE_FUTURE_KEY){
                            }
                            setResultText(getResources().getString(R.string.result_nak));
                            finishProcess();
                            break;
                    }
                    break;
                case Utils.MSG_RECEIVE_IC_READER_DATA_FAIL:
                    setResultText(getResources().getString(R.string.result_icreader_data_fail));
                    finishProcess();
                    break;

                case Utils.MSG_RECEIVE_IC_READER_DATA_TIMEOUT:
                    setResultText(getResources().getString(R.string.result_icreader_timeout));
                    finishProcess();
                    break;

                default:
                    finishProcess();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        mContext = this;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mDrawer = (DrawerLayout) findViewById(R.id.drawer_layout_admin);

        mBtnUpdateFK = (Button) findViewById(R.id.btnUpdateFK);
        mBtnUpdateFK.setOnClickListener(this);
        mBtnRunSP = (Button) findViewById(R.id.btnRunSP);
        mBtnRunSP.setOnClickListener(this);
        mBtnCheckSPLog = (Button) findViewById(R.id.btnCheckSPLog);
        mBtnCheckSPLog.setOnClickListener(this);
        mBtnAuthenMA = (Button) findViewById(R.id.btnAuthenMA);
        mBtnAuthenMA.setOnClickListener(this);
        mBtnDownload = (Button) findViewById(R.id.btnDownload);
        mBtnDownload.setOnClickListener(this);
        mBtnDeviceFactoryReset = (Button) findViewById(R.id.btnDeviceFactoryReset);
        mBtnDeviceFactoryReset.setOnClickListener(this);
        mBtnIPSettings = (Button) findViewById(R.id.btnIPSetting);
        mBtnIPSettings.setOnClickListener(this);
        mBtnFWUpdate = (Button)findViewById(R.id.btnUpdateFW);
        mBtnFWUpdate.setOnClickListener(this);
        mBtnPwdChange = (Button)findViewById(R.id.btnPwdChange);
        mBtnPwdChange.setOnClickListener(this);

        mBtnDeviceFactoryReset.setEnabled(false);
        mBtnAuthenMA.setVisibility(View.GONE);
        mBtnAuthenMA.setEnabled(false);

        mTextResult = (TextView) findViewById(R.id.textResult);

        mICReaderPacketManager = new ICReaderPacketManager();
        mAdminVanPacketManager = new AdminVanPacketManager(this);
        mVanManager = new VanManager(mContext);
    }

    @Override
    protected void onResume() {
        super.onResume();
        onRdi();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Utils.ACTION_RECEIVE_DATA_FROM_VANHOST_TO_ADMIN);
        registerReceiver(mReceiveDataReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        offRdi();

        unregisterReceiver(mReceiveDataReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(checkBackPreaseFlag){
            super.onBackPressed();
        }else{
        }
    }

    @Override
    public void onClick(View v) {
        byte[] rdPktBuf;
        setResultText("");
        switch (v.getId()) {
            case R.id.btnUpdateFK:
                startProcess();
                rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null,0,Utils.CMD_ID_REQ_CREATE_FUTURE_KEY);
                sendDataToMSR(rdPktBuf);
                break;
            case R.id.btnRunSP:
                startProcess();
                rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null,0,Utils.CMD_ID_REQ_SELF_PROTECTION);
                sendDataToMSR(rdPktBuf);
                break;
            case R.id.btnCheckSPLog:
                spLogPopup();
                break;
            case R.id.btnAuthenMA:
                startProcess();
                rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null,0,Utils.CMD_ID_REQ_MUTUAL_AUTHENTICATION);
                sendDataToMSR(rdPktBuf);
                break;
            case R.id.btnDownload:
                startProcess();
                rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null,0,Utils.CMD_ID_REQ_DEVICE_INFO);
                sendDataToMSR(rdPktBuf);
                break;
            case R.id.btnDeviceFactoryReset:

                break;

            case R.id.btnIPSetting:
                inputIPSettings();
                break;
            case R.id.btnUpdateFW:

                startProgressbarDialog();
                byte[] fileBuf =  fwUpdate();
                if(fileBuf != null) {
                    fwUpdateSendPacket(fileBuf);
                }else{
                    finishProcess();
                }
                break;
            case R.id.btnPwdChange:
                changePasswordDialog();
                break;
        }
    }


    public void inputDownloadPopup(RecvDeviceInfoPacket recvDeviceInfoPacket) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        Context context = getApplicationContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_download_dialog, (ViewGroup) findViewById(R.id.download_dialog_root));

        final EditText mEdtBuisnessNum = (EditText) layout.findViewById(R.id.editBuisnessNum);
        final EditText mEdtStoreInfo = (EditText) layout.findViewById(R.id.editStoreInfo);
        final EditText mEdtTerminalId = (EditText) layout.findViewById(R.id.editTerminalId);
        final EditText mEdtSerialNum = (EditText) layout.findViewById(R.id.editSerialNum);

        builder.setTitle(R.string.edtx_input_buisness_num);
        builder.setView(layout);

        RecvDeviceInfoPacket mRecvDeviceInfoPacket = recvDeviceInfoPacket;
        String mCertificationNum = Utils.byteArrayToHex(mRecvDeviceInfoPacket.getCertificationNum());
        Utils.setSharedPreference(mContext,Utils.PREF_KEY_AUTHENNUM, mCertificationNum);

        final String mSerialNum = Utils.byteArrayToHex(mRecvDeviceInfoPacket.getSerialNum());
        final String mVersion = Utils.byteArrayToHex(mRecvDeviceInfoPacket.getVersion());

        if (mSerialNum != null) {
            if(Utils.DEBUG_TEST){
                mEdtSerialNum.setText(mContext.getResources().getString(R.string.van_serial_num));
            }else{
                mEdtSerialNum.setText(mSerialNum);
            }
        }
        if(Utils.DEBUG_TEST){
            mEdtBuisnessNum.setText(mContext.getResources().getString(R.string.van_buisness_num));
            mEdtTerminalId.setText(mContext.getResources().getString(R.string.van_terminal_id));
        }

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button okBtn = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okBtn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String buisnessNum = "";
                        String storeInfo = "";
                        String terminalId = "";
                        String serialNum = mSerialNum;
                        String swVersion = mVersion;

                        if (mEdtBuisnessNum.getText() != null) {
                            buisnessNum = mEdtBuisnessNum.getText().toString();
                            if (mEdtStoreInfo.getText() != null) {
                                storeInfo = mEdtStoreInfo.getText().toString();
                            }
                            if (mEdtTerminalId.getText() != null) {
                                terminalId = mEdtTerminalId.getText().toString();
                            }
                            if (mEdtSerialNum.getText() != null) {
                                serialNum = mEdtSerialNum.getText().toString();
                            }
                        }
                        if (checkBuisnessNum(buisnessNum)) {
                            mAlertDialog.dismiss();
                            byte[] dataForVanPkt = makeReqDownloadData(terminalId, swVersion, serialNum, buisnessNum, storeInfo);
                            byte[] vanPktBuf = mAdminVanPacketManager.makeVanPaket(dataForVanPkt, dataForVanPkt.length, Utils.TRD_TYPE_DEVICE_DOWNLOAD);
                            sendDataToVan(vanPktBuf);
                        } else {
                            mEdtBuisnessNum.setText("");
                            mEdtBuisnessNum.setHint(R.string.edtx_retry_buisness_num_hint);
                        }
                    }
                });
                Button cancelBtn = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAlertDialog.dismiss();
                        enableButton();
                    }
                });
            }
        });
        mAlertDialog.show();
    }


    public void downloadInputPopup(byte[] recvRdPktBuf) {
        RecvDeviceInfoPacket recvDeviceInfoPacket = pasingDataFromRecvDeviceInfo(recvRdPktBuf);
        inputDownloadPopup(recvDeviceInfoPacket);
    }

    private boolean checkBuisnessNum(String buisnessNum){
        int size = buisnessNum.getBytes().length;
        if(buisnessNum !=null && size !=0 && size == 10){
            return true;
        }else{
            return false;
        }
    }

    public void spLogPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        Context context = getApplicationContext();
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_splog_dialog, (ViewGroup)findViewById(R.id.splogdialog_root));
        final TextView tx = (TextView)layout.findViewById(R.id.log_text);

        String spLog = Utils.readSPLog(mContext);
        Log.v(TAG,""+spLog);
        tx.setText(spLog);

        builder.setTitle(R.string.splog_title);
        builder.setMessage(R.string.splog_message);
        builder.setView(layout);
        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(R.string.btn_label_clear, null);

        final AlertDialog mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button okBtn = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mAlertDialog.dismiss();
                    }
                });
                Button clearBtn = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                clearBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Utils.clearSPLog(mContext);
                        tx.setText("");
                    }
                });
            }
        });
        mAlertDialog.show();
    }

    public void inputIPSettings() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // custom layout
        Context context = getApplicationContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.layout_ip_settings_dialog, (ViewGroup) findViewById(R.id.ipsettings_dialog_root));

        final EditText mEdtIP = (EditText) layout.findViewById(R.id.editIP);
        final EditText mEdtPort = (EditText) layout.findViewById(R.id.editPort);

        final String VANHOST_IP =  Utils.getSharedPreference(mContext,Utils.PREF_KEY_VAN_IP);
        final String VANHOST_PORT = Utils.getSharedPreference(mContext,Utils.PREF_KEY_VAN_PORT);
        if(VANHOST_IP.equals(Utils.PREF_KEY_FLAG_STR_FALSE )){
            mEdtIP.setText(R.string.vanhost_ip_address_text);
            mEdtPort.setText(R.string.vanhost_port_number_text);
        }else{
            mEdtIP.setText(VANHOST_IP);
            mEdtPort.setText(VANHOST_PORT);
        }

        builder.setTitle(R.string.title_ip_settings);
        builder.setView(layout);

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog mAlertDialog = builder.create();
        mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button okBtn = mAlertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                okBtn.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String ip = "";
                        String port = "";

                        if (mEdtIP.getText() != null && !mEdtIP.getText().equals("")) {
                            ip = mEdtIP.getText().toString();
                            if (mEdtPort.getText() != null && !mEdtPort.getText().equals("")) {
                                port = mEdtPort.getText().toString();

                                Utils.setSharedPreference(mContext, Utils.PREF_KEY_VAN_IP, ip);
                                Utils.setSharedPreference(mContext, Utils.PREF_KEY_VAN_PORT, port);
                                mAlertDialog.dismiss();
                            } else {
                                mEdtIP.setText("");
                                mEdtPort.setText("");
                                mEdtIP.setHint(R.string.vanhost_ip_address_text);
                                mEdtPort.setHint(R.string.vanhost_port_number_text);
                            }
                        } else {
                            mEdtIP.setText("");
                            mEdtPort.setText("");
                            mEdtIP.setHint(R.string.vanhost_ip_address_text);
                            mEdtPort.setHint(R.string.vanhost_port_number_text);
                        }
                    }
                });
                Button cancelBtn = mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mAlertDialog.dismiss();
                        enableButton();
                    }
                });
            }
        });
        mAlertDialog.show();
    }



    public byte[] fwUpdate () {
        boolean checkStorage = checkExternalStorage();
        byte[] fileBuf = null;
        if (!checkStorage) {
            Utils.showToastMessage(mContext,"",mContext.getResources().getString(R.string.update_fw_fail_read_bin));
        }else {
            if(getPermission()){
                fileBuf =  checkHighFileVer();
                if(fileBuf == null){
                    Utils.showToastMessage(mContext,"",mContext.getResources().getString(R.string.update_fw_fail_read_bin));
                }
            }else{
                Utils.showToastMessage(mContext,"",mContext.getResources().getString(R.string.update_fw_fail_read_bin));
            }
        }
        return fileBuf;
    }

    public void fwUpdateSendPacket(byte[] fileBuf){
        int fileSize = 0;
        if(fileBuf !=null){
            fileSize = fileBuf.length;
        }
        byte[] rdPktBuf = new byte[Utils.MAX_ICREADER_PACKET_SIZE];
        rdPktBuf = mICReaderPacketManager.makeICReaderPacket(fileBuf, fileSize, Utils.CMD_ID_REQ_UPDATE_FIRMWERE);
        sendDataToMSR(rdPktBuf);

    }


    ProgressDialog progressBarDialog = null;
    public void startProgressbarDialog(){
        progressBarDialog = new ProgressDialog(this);    // Activity의 this
        progressBarDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);    // 바 형태로 변경
        progressBarDialog.setMessage(getResources().getString(R.string.update_fw_msg));
        progressBarDialog.show();

    }
    public void setProgressbarDialog(int value,int totalSize){
        if (progressBarDialog != null) {
            progressBarDialog.setMax(totalSize);
            progressBarDialog.setProgress(value);
        }
    }
    public void stopProgressbarDialog() {
        if (progressBarDialog != null) {

            progressBarDialog.dismiss();
        }

    }

    public void disableButtons(Button[] btnArray) {
        if(btnArray == null ||btnArray.length <= 0){
            return;
        }
        for(int i = 0; i<btnArray.length; i++){
            btnArray[i].setEnabled(BTN_DISABLE);
        }
        checkBackPreaseFlag = true;
    }

    public void enableButton() {
        mBtnUpdateFK.setEnabled(BTN_ENABLE);
        mBtnRunSP.setEnabled(BTN_ENABLE);
        mBtnCheckSPLog.setEnabled(BTN_ENABLE);
        mBtnAuthenMA.setEnabled(BTN_ENABLE);
        mBtnDownload.setEnabled(BTN_ENABLE);
//        mBtnDeviceFactoryReset.setEnabled(BTN_ENABLE);
        mBtnIPSettings.setEnabled(BTN_ENABLE);
        mBtnFWUpdate.setEnabled(BTN_ENABLE);

        checkBackPreaseFlag = true;
    }
    public void disableButton() {
        mBtnUpdateFK.setEnabled(BTN_DISABLE);
        mBtnRunSP.setEnabled(BTN_DISABLE);
        mBtnCheckSPLog.setEnabled(BTN_DISABLE);
        mBtnAuthenMA.setEnabled(BTN_DISABLE);
        mBtnDownload.setEnabled(BTN_DISABLE);
//        mBtnDeviceFactoryReset.setEnabled(BTN_DISABLE);
        mBtnIPSettings.setEnabled(BTN_DISABLE);
        mBtnFWUpdate.setEnabled(BTN_DISABLE);

        checkBackPreaseFlag = false;
    }


    private boolean mIsProcess=false;
    public void startProcess(){
        mIsProcess = true;
        disableButton();
    }
    public void finishProcess(){
        mIsProcess = false;
        stopProgressbarDialog();
        enableButton();
        disableRdi();
//        mPMPosService.disConnectedToVanHost();
    }




    public void setResultText(String resultData){
        if(mTextResult!=null){
            mTextResult.setText(resultData);
        }
    }

    public RecvDeviceInfoPacket pasingDataFromRecvDeviceInfo(byte[] recvData) {
        RecvDeviceInfoPacket recvDeviceInfoPacket = new RecvDeviceInfoPacket();
        if (recvDeviceInfoPacket != null) {
            recvDeviceInfoPacket.setRecvDeviceInfo(recvData);
        }
        return recvDeviceInfoPacket;
    }


    public byte[] makeReqDownloadData(String mTerminalId,String mSWVersion,
                                      String mSerialNum,String mBuisnessNum,String mStoreInfo){
         int terminalIdLen = 10;
         int currentTimeLen = 14;
         int swVersionLen = 5;
         int serialNumLen = 20;
         int businessNumLen = 10;
         int mchData_Len = 20;

        byte[] terminalId = checkLenForDownVanPkt(mTerminalId.getBytes(),terminalIdLen);
        byte[] currentTime = Utils.convertCurrentDateToByteArray();
        byte[] swVersion = checkLenForDownVanPkt(mSWVersion.getBytes(),swVersionLen);
        byte[] SN = checkLenForDownVanPkt(mSerialNum.getBytes(),serialNumLen);
        byte[] BN = checkLenForDownVanPkt(mBuisnessNum.getBytes(),terminalIdLen);
        byte[] merchantData =checkLenForDownVanPkt(mStoreInfo.getBytes(),mchData_Len);

        int dataForVanPktLen = terminalIdLen+currentTimeLen+swVersionLen+serialNumLen+businessNumLen+mchData_Len;//mchData_Len TODO:가맹점정보 추가 PACKET정의
        byte[] dataForVanPkt =new byte[dataForVanPktLen] ;

        RecvDeviceInfoPacket.class.getSimpleName();

        int count = 0;
        System.arraycopy(terminalId, 0, dataForVanPkt, count, terminalId.length);
        count += terminalIdLen;
        System.arraycopy(currentTime, 0, dataForVanPkt, count, currentTime.length);
        count += currentTimeLen;
        System.arraycopy(swVersion, 0, dataForVanPkt, count, swVersion.length);
        count += swVersionLen;
        System.arraycopy(SN, 0, dataForVanPkt, count, SN.length);
        count += serialNumLen;
        System.arraycopy(BN, 0, dataForVanPkt, count, BN.length);
        count += businessNumLen;
        System.arraycopy(merchantData, 0, dataForVanPkt, count, merchantData.length);
        count += mchData_Len;


        if (Utils.DEBUG_VALUE) {
            Utils.debugTx(TAG, dataForVanPkt, count);
        }
        return dataForVanPkt;
    }

    public byte[] checkLenForDownVanPkt(byte[] data, int len){
        byte[] result = new byte[len];
        byte nullValue = (byte)0x20;
        for(int i=0; i<len; i++){
            if(i > data.length-1){
                result[i] = nullValue;
            }else {
                result[i] = data[i];
            }
        }
        return result;
    }

    public void onRdi() {
        mSelfProtection = false;
        mReqDevicesInfo = false;

        //rooting check
        if (RootingDeviceCheck()) {
            Utils.SimpleDailog(mContext,mContext.getResources().getString(R.string.dialog_error_msg_rooting_device),mContext.getResources().getString(R.string.dialog_error_msg_can_not_transaction));
            return;
        }
        //device Download check
        String DownloadFlag = Utils.getSharedPreference(mContext, Utils.PREF_KEY_DOWNLOAD_FLAG);
        if (DownloadFlag.equals(Utils.PREF_KEY_FLAG_STR_TRUE)) {
            enableButton();
        }else{
            String title = getResources().getString(R.string.dialog_error_title_down_popup);
            String message = getResources().getString(R.string.dialog_error_msg_down_popup);
            Utils.SimpleDailog(mContext,title,message);
            Button[] btnArray = {mBtnUpdateFK, mBtnAuthenMA, mBtnDeviceFactoryReset, mBtnIPSettings};
            disableButtons(btnArray);
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
                rdiOnCount = 0;
            }
            return;
        }
    }



    public void offRdi() {
        boolean setFlag = false;
        if (mRdiManager != null){
            if(mRdiManager.isEnabled() == mRdiManager.ENABLE){
                mRdiManager.setEnable(mRdiManager.DISABLE);
            }
            mRdiManager.powerDown();
            setFlag = true;
        }else{
            setFlag = false;
        }
    }

    public void setupRdi() {
        if (mRdiManager == null){
            onRdi();
        }
        if (mRdiManager != null){
            if(mRdiManager.isEnabled() != mRdiManager.ENABLE){
                mRdiManager.setEnable(mRdiManager.ENABLE);
            }
            int resclear = mRdiManager.clear();
        }
    }

    public void disableRdi() {
        if (mRdiManager != null){
            if(mRdiManager.isEnabled() != mRdiManager.DISABLE){
                mRdiManager.setEnable(mRdiManager.DISABLE);
            }
            int resclear = mRdiManager.clear();

        }
    }

    public int writeDataToRdi(byte[] dataPk, int size) {
        setupRdi();
        mRdiManager.clear();

        int result = mRdiManager.write(dataPk, size);
        return result;
    }

    public void readRdi(){
        if (mRdiManager != null && mRdiManager.isEnabled() == 1) {
            mRdiManager.readData(mMSRHandler,Utils.TIMEOUT_3SEC);
        }
    }

    public void sendDataToMSR(byte[] rdPkt){
        if(rdPkt == null){
            Utils.showToastMessage(mContext,"",mContext.getResources().getString(R.string.result_icreader_make_data_fail));
            return;
        }else {
            Utils.setSharedPreferenceInt(mContext, Utils.PREF_KEY_IC_COMD_ID, mICReaderPacketManager.getCommandId(rdPkt));

            writeDataToRdi(rdPkt, rdPkt.length);
            readRdi();
        }
    }


    private boolean checkForVanData(byte[] vanPktBuf){
        if(vanPktBuf!=null){
            if(vanPktBuf.length > 0){
                return true;
            }else {
                return false;
            }
        }else{
            return false;
        }
    }

    private void sendDataToVan(byte[] vanPktBuf) {
        if(checkForVanData(vanPktBuf)){
            if(mVanManager == null){
                mVanManager = new VanManager(mContext);
            }
            if (mVanManager != null) {
                mVanManager.requestDataToVanHost(vanPktBuf,true,mVanHandler);
            }
        } else {
            Utils.showToastMessage(mContext, TAG, mContext.getResources().getString(R.string.result_van_make_data_fail));
            finishProcess();
        }
    }

    private void changePasswordDialog() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.layout_change_pwd_dialog, null);

        final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(mContext);
        builder.setIcon(android.R.drawable.ic_lock_lock);
        builder.setTitle(getString(R.string.dialog_builder_change_password_title));
        builder.setView(layout);
        builder.setCancelable(false);

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, null);

        final android.app.AlertDialog passwordDialog = builder.create();
        passwordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button okBtn = passwordDialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
                okBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        EditText editCurrentPassword = (EditText) layout.findViewById(R.id.editCurrentPassword);
                        EditText editNewPassword = (EditText) layout.findViewById(R.id.editNewPassword);
                        EditText editRetryNewPassword = (EditText) layout.findViewById(R.id.editRetryNewPassword);

                        String userCurrentPwd = editCurrentPassword.getText().toString();
                        String userNewPwd = editNewPassword.getText().toString();
                        String userRetryNewPwd = editRetryNewPassword.getText().toString();

                        int resultValue = checkPasswordValue(userCurrentPwd, userNewPwd, userRetryNewPwd);
                        if (resultValue == SUCCESS_ALL_INPUT_VALUE) {
                            Utils.setSharedPrefLoginPwdValue(mContext, userNewPwd);
                            passwordDialog.dismiss();
                        } else {
                            makeErrorMsgDialog(resultValue);

                            editCurrentPassword.setText("");
                            editNewPassword.setText("");
                            editRetryNewPassword.setText("");
                        }
                    }
                });
                Button cancelBtn = passwordDialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
                cancelBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        passwordDialog.dismiss();
                    }
                });
            }
        });
        passwordDialog.show();
    }

    private int checkPasswordValue(String userCurrentPwd, String userNewPwd, String userRetryNewPwd) {
        if (userCurrentPwd.isEmpty() || (userCurrentPwd.length() == 0)) {
            return ERROR_CURRENT_PWD_EMPTY;
        }

        if (userNewPwd.isEmpty() || (userNewPwd.length() == 0)) {
            return ERROR_NEW_PWD_EMPTY;
        }

        if (userRetryNewPwd.isEmpty() || (userRetryNewPwd.length() == 0)) {
            return ERROR_RETRY_PWD_EMPTY;
        }

        String currentPwd = Utils.getSharedPrefLoginPwdValue(mContext);
        if (currentPwd.equals(userCurrentPwd)) {
            if (userNewPwd.equals(userRetryNewPwd)) {
                return SUCCESS_ALL_INPUT_VALUE;
            } else {
                return ERROR_NEW_PWD_NOT_SAME_AS_RETRY_PWD;
            }
        } else {
            return ERROR_CURRENT_PWD_NOT_SAME_AS_USER_PWD;
        }
    }

    private void makeErrorMsgDialog(int resultValue) {
        String errorMsg = "";

        switch (resultValue) {
            case ERROR_CURRENT_PWD_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_current_pwd_empty);
                break;
            case ERROR_NEW_PWD_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_new_pwd_empty);
                break;
            case ERROR_RETRY_PWD_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_retry_pwd_empty);
                break;
            case ERROR_CURRENT_PWD_NOT_SAME_AS_USER_PWD:
                errorMsg = getString(R.string.dialog_error_msg_current_pwd_not_same_as_user_pwd);
                break;
            case ERROR_NEW_PWD_NOT_SAME_AS_RETRY_PWD:
                errorMsg = getString(R.string.dialog_error_msg_new_pwd_not_same_as_retry_pwd);
                break;
        }
        android.app.AlertDialog.Builder alert = new android.app.AlertDialog.Builder(mContext);
        alert.setMessage(errorMsg);
        alert.create().show();
    }

    public void reqDeviceInfo() {

        byte[] rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null, 0, Utils.CMD_ID_REQ_DEVICE_INFO);
        sendDataToMSR(rdPktBuf);
    }

    public void reqSelfProtection() {
        byte[] rdPktBuf = mICReaderPacketManager.makeICReaderPacket(null, 0, Utils.CMD_ID_REQ_SELF_PROTECTION);
        sendDataToMSR(rdPktBuf);
    }
    public void faildInitSelfProtection(){
        Utils.SimpleDailog(mContext,"",mContext.getResources().getString(R.string.dialog_error_msg_integrity_popup));
    }

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

//External Storage
    public boolean checkExternalStorage() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {                  // read/write o
            return true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)){  // read o
            return false;
        } else{                                                         // read/write x
            return false;
        }
    }

    // Permission popup
    public boolean getPermission(){
        int READ_EXTERNAL_STORAGE = ContextCompat.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE);
        int WRITE_EXTERNAL_STORAGE= ContextCompat.checkSelfPermission(mContext.getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(READ_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED && WRITE_EXTERNAL_STORAGE == PackageManager.PERMISSION_GRANTED) {
            return true;
        }else if(READ_EXTERNAL_STORAGE == PackageManager.PERMISSION_DENIED || WRITE_EXTERNAL_STORAGE == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
        } else {
            return false;
        }
        return false;
    }
    // Permission popup  callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_EXTERNAL_STORAGE:
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            byte[] fileBuf =  checkHighFileVer();
                            fwUpdateSendPacket(fileBuf);
                        } else {
                            if(Utils.DEBUG_VALUE)Log.v(TAG,mContext.getResources().getString(R.string.update_fw_fail_permission_denied));
                            finishProcess();
                        }
                    }
                }
                break;
        }
    }

    public byte[] checkHighFileVer() {
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + Utils.PMPOS_DIRECTORY + File.separator;//+ "KOCES.000000";
        FilenameFilter fileFilter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith("bin");
            }
        };
        int VERSION_TOKEN = 1;
        File file = new File(path);
        File[] files = file.listFiles(fileFilter);
        if (files == null) {
            return null;
        }
        File fileHighVer = null;
        int fileVer = 0;
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory()) {
                if (Utils.DEBUG_VALUE) Log.v(TAG, "Directory : " + files[i].getName());
            } else {
                if (Utils.DEBUG_VALUE) Log.v(TAG, "File :" + files[i].getName());
                String fileNameStr = files[i].getName();
                String[] verStr = fileNameStr.split("\\.");
                if (verStr != null && fileVer < Integer.parseInt(verStr[VERSION_TOKEN])) {
                    fileVer = Integer.parseInt(verStr[VERSION_TOKEN]);
                    fileHighVer = files[i];
                }
            }
        }

        byte[] totalFileBuf = null;
        if (fileHighVer != null) {
            int integerFileSize = (int) (long) fileHighVer.length();
            int size = 0;
            totalFileBuf = new byte[integerFileSize];
            try {
                final FileInputStream inputStreamfile = new FileInputStream(fileHighVer);
                size = inputStreamfile.read(totalFileBuf);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (Utils.DEBUG_VALUE)
                Log.v(TAG, mContext.getResources().getString(R.string.update_fw_fail_save_bin));
            finishProcess();
        }
        return totalFileBuf;
    }
}
