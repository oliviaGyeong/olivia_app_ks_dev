package device.apps.pmpos;

import android.app.Activity;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
//import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import device.apps.pmpos.aidl.DialogListener;
import device.apps.pmpos.fragment.sign.TouchDrawer;

public class DialogBGActivity extends  AppCompatActivity{
    private static final String TAG = DialogBGActivity.class.getSimpleName();
    public static Context mContext;

    public static final String KEY_EXTRA_DAILOG_CALLBACK=Utils.KEY_EXTRA_DAILOG_CALLBACK;
    public static final String KEY_EXTRA_DAILOG_NUM = Utils.KEY_EXTRA_DAILOG_NUM;
    public static final String KEY_EXTRA_DAILOG_MESSAGE = Utils.KEY_EXTRA_DAILOG_MESSAGE;
    public static final String KEY_EXTRA_DAILOG_BTN_CLICK_LISTENER = Utils.KEY_EXTRA_DAILOG_BTN_CLICK_LISTENER;

    public static final int  popup_Signature = Utils.popup_Signature;
    public static final int  popup_IcReadFail = Utils.popup_IcReadFail;
    public static final int  popup_Simple = Utils.popup_Simple;
    public static final int  popup_DisplayNoticeIC=Utils.popup_DisplayNoticeIC;
    public static final int  popup_DisplayNoticeMS= Utils.popup_DisplayNoticeMS;
    public static final int  popup_DisplayNoticeNOTPayment= Utils.popup_DisplayNoticeNOTPayment;

    //KEY_EXTRA_DAILOG_BTN_CLICK_LISTENER
    public static final int popup_create = Utils.popup_create;
    public static final int popup_cancel = Utils.popup_cancel;
    public static final int popup_Signature_cancel_btn = Utils.popup_Signature_cancel_btn;
    public static final int popup_Signature_complete_btn = Utils.popup_Signature_complete_btn;
    public static final int popup_IcReadFail_finish_btn = Utils.popup_IcReadFail_finish_btn;
    public static final int popup_IcReadFail_msr_btn = Utils.popup_IcReadFail_msr_btn;
    public static final int popup_Simple_ok_btn = Utils.popup_Simple_ok_btn;
    public static final int  popup_DisplayNoticeIC_ok_btn=Utils.popup_DisplayNoticeIC_ok_btn;
    public static final int  popup_DisplayNoticeMS_ok_btn= Utils.popup_DisplayNoticeMS_ok_btn;
    public static final int  popup_DisplayNoticeNOTPaymen_ok_btn= Utils.popup_DisplayNoticeNOTPaymen_ok_btn;

    private DialogListener.OnClickDialogCallback mCallback;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_dialog_bg);
        Intent intent = getIntent();
        String dialogTitleText = intent.getExtras().getString(Utils.KEY_EXTRA_DAILOG_TITLE,"");
        String dialogMessageText = intent.getExtras().getString(KEY_EXTRA_DAILOG_MESSAGE,"");

        int dialogNum = intent.getExtras().getInt(KEY_EXTRA_DAILOG_NUM,0);
        if(dialogNum == popup_Signature) {
            signaturePopup();
        }else if(dialogNum == popup_IcReadFail){
            icReadFailpopup();
        }else if(dialogNum == popup_DisplayNoticeIC || dialogNum == popup_DisplayNoticeMS ||  dialogNum == popup_DisplayNoticeNOTPayment){
            displayNoticeDialog(dialogTitleText,dialogMessageText,dialogNum);

        }else if(dialogNum == popup_Simple){
            simpleDailog("",dialogMessageText);


        }else{
            finish();
        }

        PMPosHelper pmPosHelper = PMPosHelper.getInstance();
        mCallback = pmPosHelper.mDialogCreateCallback ;

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        dialogClickLitener(popup_cancel);
    }




    @Override
    protected void onDestroy() {
        super.onDestroy();
        dialogOnCallback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    //Signagture +++
    Button mBtnComplete;
    private TouchDrawer.TouchTestResultCallback mResultCallback = new TouchDrawer.TouchTestResultCallback() {
        @Override
        public void onTouchTestResult(int result) {
            if (result == TouchDrawer.TEST_RESULT_SUCCESS) {
                mBtnComplete.setEnabled(true);
            }
        }
    };

    private void signaturePopup(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

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
                mAlertDialog.dismiss();
                dialogClickLitener(popup_Signature_cancel_btn);
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
                mAlertDialog.dismiss();
                dialogClickLitener(popup_Signature_complete_btn);
            }
        });
        mAlertDialog.show();
    }

    private void icReadFailpopup(){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("");
        builder.setMessage( mContext.getResources().getString(R.string.msg_error_response_code_08));
        builder.setNegativeButton(mContext.getResources().getString(R.string.btn_label_finish), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                dialogClickLitener(popup_IcReadFail_finish_btn);
            }
        });
        builder.setPositiveButton(mContext.getResources().getString(R.string.btn_label_continue_msr), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                dialogClickLitener(popup_IcReadFail_msr_btn);
            }
        });
        builder.show();
    }


    private void displayNoticeDialog(String title , String message, final int gotoFragmentNum) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(android.R.drawable.ic_dialog_alert);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(gotoFragmentNum == popup_DisplayNoticeIC){
                    dialogClickLitener(popup_DisplayNoticeIC_ok_btn);
                }else if(gotoFragmentNum == popup_DisplayNoticeMS){
                    dialogClickLitener(popup_DisplayNoticeMS_ok_btn);
                }else{
                    dialogClickLitener(popup_DisplayNoticeNOTPaymen_ok_btn);
                }
            }
        });
        builder.create().show();
    }


    private void simpleDailog(String title, String message){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(mContext);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                dialogClickLitener(popup_Simple_ok_btn);
            }
        });
        builder.show();

    }

    int mResultCode = popup_cancel;
    public void dialogClickLitener(int onClickResultCode) {
        mResultCode = onClickResultCode;
        finish();
    }
    public void dialogOnCallback() {
        mCallback.OnClickDialogCallback(mResultCode);
    }
}
