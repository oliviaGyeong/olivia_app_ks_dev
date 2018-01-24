package device.apps.pmpos;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import device.apps.pmpos.aidl.Callback;
import device.apps.pmpos.fragment.DealCompleteFragment;
import device.apps.pmpos.fragment.ICReadWaitFragment;
import device.apps.pmpos.fragment.LoginFragment;
import device.apps.pmpos.fragment.MsrSwipeWaitFragment;
import device.apps.pmpos.fragment.MainFragment;
import device.apps.pmpos.fragment.PaymentProgressFragment;
import device.apps.pmpos.fragment.ReceiptPrintFragment;
import device.apps.pmpos.fragment.listener.FragmentCallback;
import device.apps.pmpos.packet.AdminVanPacketManager;
import device.apps.pmpos.packet.ICReaderPacketManager;
import device.apps.pmpos.packet.ReceiptPrintData;

public class PMPosActivity extends AppCompatActivity
        implements
        FragmentCallback.MoveOtherFragmentListener, FragmentCallback.PaymentCancelListener, FragmentCallback.CallbackMainFragment {

    private final String TAG = PMPosActivity.class.getSimpleName();

    private final String VAN_RESULT_CODE_SUCCESS = "0000";

    private boolean mIsFirstRun = true;

    private Context mContext;
    private PMPosHelper mPMPosHelper;
    private ICReaderPacketManager mICReaderPacketManager;

    private Bundle mRecvRdPktBundle;
    private ReceiptPrintData mReceiptPrintData;

        private String parsingForResultCode(byte[] recvVanPktBuf) {
            byte[] ansCode = new byte[4];
            System.arraycopy(recvVanPktBuf, 61, ansCode, 0, ansCode.length);

            if (Utils.DEBUG_INFO) Log.i(TAG, "ansCode :" + new String(ansCode));
            return new String(ansCode);
        }

        private String parsingForResultErrorMsg(byte[] recvVanPktBuf) {
            byte[] errorMsg = new byte[32];
            System.arraycopy(recvVanPktBuf, 104, errorMsg, 0, errorMsg.length);
            String str = Utils.convertByteArrayToKoreanEncodeStr(errorMsg);
            return str;
        }

        private String getResultMsg(byte[] recvVanPktBuf) {
            String resultMsg = "";
            String ansCode = parsingForResultCode(recvVanPktBuf);

            if (ansCode.equals(VAN_RESULT_CODE_SUCCESS)) {
                resultMsg = getString(R.string.dialog_success_msg);
            } else {
                resultMsg = parsingForResultErrorMsg(recvVanPktBuf);
            }

            return resultMsg;
        }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mContext = this;
        mIsFirstRun = true;

        mPMPosHelper = new PMPosHelper(this);
        mRecvRdPktBundle = new Bundle();
        mReceiptPrintData = new ReceiptPrintData(mContext);

        keepUpScreenOn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPMPosHelper.onRdi();

        if (mIsFirstRun) {
            mIsFirstRun = false;
            initNavigationItem();
        }else{
            if(getCurrentFragmentId() == R.id.mainScene){
                changeFragment(MainFragment.newInstance());
            }
        }
    }

    @Override
    protected void onPause() {
        mPMPosHelper.offRdi();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mRecvRdPktBundle = null;

        mPMPosHelper.offRdi();

        releaseScreenOn();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (getCurrentFragmentId() == R.id.mainScene||getCurrentFragmentId()== R.id.loginScene) {
            super.onBackPressed();
        }
    }

    private int getCurrentFragmentId() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.content_fragment);
        int fragmentId = fragment.getId();

        if (fragment instanceof LoginFragment) {
            fragmentId = R.id.loginScene;
        } else if (fragment instanceof MainFragment) {
            fragmentId = R.id.mainScene;
        } else if (fragment instanceof ICReadWaitFragment) {
            fragmentId = R.id.icReadWaitScene;
        } else if (fragment instanceof MsrSwipeWaitFragment) {
            fragmentId = R.id.msrSwipeWaitScene;
        } else if (fragment instanceof PaymentProgressFragment) {
            fragmentId = R.id.paymentProgressScene;
        } else if (fragment instanceof DealCompleteFragment) {
            fragmentId = R.id.dealCompleteScene;
        } else if (fragment instanceof ReceiptPrintFragment) {
            fragmentId = R.id.receipt_print_Scene;
        }
        return fragmentId;
    }

    private void initNavigationItem() {
        changeFragment(LoginFragment.newInstance());
    }

    private void changeFragment(Fragment newFragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.content_fragment, newFragment);

        transaction.commitAllowingStateLoss();
//        transaction.commit();
    }


    // Fragment Listener Method - start
    @Override
    public void moveOtherFragment(int fragmentNumber, boolean havePassData, Object data) {
        Fragment fragment = null;

        switch (fragmentNumber) {
            case Utils.FRAG_NUM_LOGIN:
                fragment = LoginFragment.newInstance();
                break;
            case Utils.FRAG_NUM_MAIN:
                fragment = MainFragment.newInstance();
                String DownloadFlag = Utils.getSharedPreference(mContext, Utils.PREF_KEY_DOWNLOAD_FLAG);
                if (DownloadFlag.equals(Utils.PREF_KEY_FLAG_STR_TRUE)) {
                    setNativeDeviceInfo();
                } else {
                    openAdmin();
                }
                break;
            case Utils.FRAG_NUM_IC_READ_WAIT:
                fragment = ICReadWaitFragment.newInstance();
                break;
            case Utils.FRAG_NUM_MS_SWIPE_WAIT:
                fragment = MsrSwipeWaitFragment.newInstance();
                break;
            case Utils.FRAG_NUM_PAY_PROGRESS:
                if (havePassData) {
                    byte[] recvRdPktBuf = (byte[]) data;
                    fragment = PaymentProgressFragment.newInstance(recvRdPktBuf);
                } else {
                    fragment = PaymentProgressFragment.newInstance();
                }
                break;
            case Utils.FRAG_NUM_DEAL_COMPLETE:
                fragment = DealCompleteFragment.newInstance();
                break;
            case Utils.FRAG_NUM_RECEIPT_PRINT:
                fragment = ReceiptPrintFragment.newInstance(mReceiptPrintData);
                break;
        }

        if (fragment != null) {
            changeFragment(fragment);
        }
    }

    @Override
    public void onClickPaymentCancelButton() {
        mPMPosHelper.cancelPaymentIFM();
    }
    // Fragment Listener Method - end

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pmpos, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.menu_admin);

        if (getCurrentFragmentId() == R.id.mainScene) {
            item.setEnabled(true);
        } else {
            item.setEnabled(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_admin:
                if (getCurrentFragmentId() == R.id.mainScene) {
                    openAdmin();
                } else {
                    item.setEnabled(false);
                }
                return true;
            case R.id.menu_info:
                openInfo();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openAdmin() {
        Intent adminActivityIntent = new Intent(this, AdminActivity.class);
        startActivity(adminActivityIntent);
    }

    private void openInfo() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        String version = getString(R.string.msg_version_suffix);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pi != null) {
                version = pi.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        alert.setMessage(getString(R.string.app_name) + " v" + version);
        alert.show();
    }

    private void keepUpScreenOn() {
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void releaseScreenOn() {
        getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void setNativeDeviceInfo() {
        AdminVanPacketManager adminVanPacketManager = new AdminVanPacketManager(mContext);
        adminVanPacketManager.initDeviceinfo();
    }

    /**
     * Main Callback
     */
    @Override
    public void callbackMainFragment_Approval(String paymentOption,String trdType, String amount, String fee, String surTax, String instalment, String keyIn,String cashReceiptType) {
        mPMPosHelper.reqPreambleInit(mCallback, paymentOption, trdType, amount, fee, surTax, instalment, keyIn,cashReceiptType);
    }

    @Override
    public void callbackMainFragment_Cancel(String paymentOption,String trdType, String amount, String fee, String surTax, String instalment, String keyIn, String orgApprovalDate, String orgApprovalNo) {
        switch (trdType) {
            case Utils.TRD_TYPE_IC_CREDIT_CANCEL:   // F2
                mPMPosHelper.reqPreambleCancelInit(mCallback,paymentOption,trdType,amount,fee,surTax,instalment,keyIn,orgApprovalDate, orgApprovalNo);
                break;
            case Utils.TRD_TYPE_CASH_CANCEL:        // H4
                mPMPosHelper.reqPreambleCancelInit(mCallback,paymentOption,trdType,amount,fee,surTax,instalment,keyIn,orgApprovalDate, orgApprovalNo);
                break;
        }
    }

    /**
     * PMPOSHelper Callback
     * Callback_RDI, VAN
     */
    Callback.OnCallBack mCallback = new Callback.OnCallBack() {
        @Override
        public void OnCallBack(Callback.CallBackList callback, byte[] recvRdPktBuf, byte[] recvVanPktBuf) {
            switch(callback){
                case VAN_OK_READ_DATA:
                    break;
                case RDI_OK_READ_DATA:
                    mRecvRdPktBundle.putByteArray(Utils.KEY_EXTRA_IC_READER_PACKET_BUFFER, recvRdPktBuf);
                    if (mICReaderPacketManager == null) {
                        mICReaderPacketManager = new ICReaderPacketManager();
                    }
                    break;
                case RDI_FAIL_MAKE_PACKET:
                    changeFragment(MainFragment.newInstance());
                    break;
                case RDI_FAIL_READ_DATA:
                    displayErrorDialog(getString(R.string.dialog_error_msg_recv_reader_data_fail));
                    changeFragment(MainFragment.newInstance());
                    break;
                case RDI_FAIL_READ_TIMEOUT:
                    displayErrorDialog(getString(R.string.dialog_error_msg_recv_reader_data_timeout));
                    changeFragment(MainFragment.newInstance());
                    break;
                case VAN_FAIL_MAKE_PACKET:
                    displayErrorDialog(mContext.getString(R.string.dialog_error_msg_recv_van_data_fail));
                    changeFragment(MainFragment.newInstance());
                    break;
                case VAN_FAIL_READ_DATA:
                    displayErrorDialog(mContext.getString(R.string.dialog_error_msg_recv_van_data_fail));
                    changeFragment(MainFragment.newInstance());
                    break;
                case VAN_FAIL_READ_TIMEOUT:
                    displayErrorDialog(mContext.getString(R.string.dialog_error_msg_recv_van_data_timeout));
                    changeFragment(MainFragment.newInstance());
                    break;
                case VAN_FAIL_SERVER_ERROR:
                    displayErrorDialog(mContext.getString(R.string.dialog_error_msg_recv_van_server_error));
                    changeFragment(MainFragment.newInstance());
                    break;
                case CANCEL_DEVICE_ROOTING:
                    Utils.SimpleDailog(mContext,mContext.getResources().getString(R.string.dialog_error_msg_rooting_device),mContext.getResources().getString(R.string.dialog_error_msg_can_not_transaction));
                    break;
                case CANCEL_DEVICE_DOWNLOAD:
                    if (getCurrentFragmentId() == R.id.mainScene) {
                        openAdmin();
                    }
                    break;

                case GOTO_MAIN: //cancel , MSR popup에서 finish로 종료한 경우
                    if (Utils.DEBUG_INFO) Log.v(TAG, "[common] << GOTO_MAIN >>");
                    changeFragment(MainFragment.newInstance());
                    break;
                case GOTO_IC_READ_SCENE:
                    changeFragment(ICReadWaitFragment.newInstance());
                    break;
                case GOTO_MS_SWIPE_SCENE:
                    changeFragment(MsrSwipeWaitFragment.newInstance());
                    break;

                case GOTO_PROGRESS:
                    if (Utils.DEBUG_INFO) Log.v(TAG, "[common] << GOTO_PROGRESS >>");
                    changeFragment(PaymentProgressFragment.newInstance());
                    break;
                case GOTO_DEAL_COMPLETE: //Error  Message 출력 화면
                    if (Utils.DEBUG_INFO) Log.v(TAG, "[common] << GOTO_DEAL_COMPLETE >>");
                    if(recvVanPktBuf != null){
                        changeFragment(DealCompleteFragment.newInstance(getResultMsg(recvVanPktBuf)));
                    }else if(recvRdPktBuf != null ){
                        //olivia 171201 Korean Encoding issue
                        byte[] msgBuf = new byte[20];
                        System.arraycopy(recvRdPktBuf, 5, msgBuf, 0, msgBuf.length);
                        String resultMsg = Utils.convertByteArrayToKoreanEncodeStr(msgBuf);
                        changeFragment(DealCompleteFragment.newInstance(resultMsg));
                    }else{
                        changeFragment(DealCompleteFragment.newInstance(""));
                    }
                    break;
                case GOTO_RECEIPT_PRINT:
                    if (Utils.DEBUG_INFO) Log.v(TAG, "[common] << GOTO_RECEIPT_PRINT >>");
                    mReceiptPrintData.setRecvVanData(recvVanPktBuf);
                    changeFragment(ReceiptPrintFragment.newInstance(mReceiptPrintData));
                    break;
            }
        }

        private void displayErrorDialog(String errorMsg) {
            if(mContext!= null) {
                AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
                alert.setMessage(errorMsg);
                if (isFinishing() == false) {
                    alert.show();
                }
            }
        }
    };





}
