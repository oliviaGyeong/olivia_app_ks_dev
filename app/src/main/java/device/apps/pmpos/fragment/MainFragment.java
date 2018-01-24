package device.apps.pmpos.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import device.apps.pmpos.R;
import device.apps.pmpos.Utils;
import device.apps.pmpos.fragment.listener.FragmentCallback;

import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

public class MainFragment extends Fragment {

    private final String TAG = MainFragment.class.getSimpleName();

    private static boolean mHaveParam = false;

    private final int SUCCESS_ALL_INPUT_VALUE = 0;
    private final int ERROR_MONEY_EDIT_EMPTY = 1;
    private final int ERROR_PAYMENT_OPTION_RADIO_GROUP_EMPTY = 2;
    private final int ERROR_MONTH_EDIT_EMPTY = 3;
    private final int ERROR_CASH_RECEIPT_TYPE_RADIO_GROUP_EMPTY = 4;
    private final int ERROR_INDIVIDUAL_KEYIN_LENGTH_MIN = 5;
    private final int ERROR_CORPORATION_KEYIN_LENGTH = 6;
    private final int ERROR_MONTH_NO_EDIT_VALUE = 7;
    private final int ERROR_APPROVAL_NUMBER_EMPTY = 8;
    private final int ERROR_APPROVAL_DATE_EMPTY = 9;

    private final int INSTALLABLE_AMOUNT = Utils.INSTALLABLE_AMOUNT; //50000;

    private final int INDIVIDUAL_KEYIN_LENGTH_MIN = 11;
    private final int CORPORATION_KEYIN_LENGTH = 10;

    private final String DEFAULT_VALUE_OF_DEVICE_AUTHORIZATION_NUMBER = "false";


    private RadioGroup mRadioGroupApprovalOption;
    private RelativeLayout mApprovalLayout;
    private RelativeLayout mCancelLayout;

    private TextView mTextDeviceAuthNum;
    private TextView mTextDeviceAppNum;
    private EditText mEditSumOfMoney;
    private RadioGroup mRadioGroupPaymentOption;
    private EditText mEditServiceCharge;
    private EditText mEditTax;

    private EditText mEditApprovalNumber;
    private EditText mEditApprovalDate;

    private RelativeLayout mCreditCardLayout;
    private EditText mEditMonthlyInstallmentPlan;

    private RelativeLayout mCashReceiptLayout;
    private RadioGroup mRadioGroupCashReceiptType;
    private EditText mEditKeyIn;

    private Button mBtnCancel;
    private Button mBtnClear;
    private Button mBtnOk;

    private Context mContext;
    private FragmentCallback.CallbackMainFragment mCallbackMain;

    public MainFragment() {
    }

    public static MainFragment newInstance() {
        mHaveParam = false;
        MainFragment fragment = new MainFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View MainFragmentView = inflater.inflate(R.layout.fragment_main, container, false);


        mRadioGroupApprovalOption = (RadioGroup) MainFragmentView.findViewById(R.id.radioGroupApprovalCancel);
        mRadioGroupApprovalOption.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.approvalRadioBtn:
                        setApprovalInputUI();
                        break;
                    case R.id.cancelRadioBtn:
                        setCancelInputUI();
                        break;
                }
            }
        });

        mApprovalLayout = (RelativeLayout) MainFragmentView.findViewById(R.id.approvalLayout);
        mCancelLayout = (RelativeLayout) MainFragmentView.findViewById(R.id.cancelLayout);

        mTextDeviceAuthNum = (TextView) MainFragmentView.findViewById(R.id.textDeviceAuthNum);
        mEditSumOfMoney = (EditText) MainFragmentView.findViewById(R.id.editSumOfMoney);
        mEditSumOfMoney.setImeOptions(EditorInfo.IME_ACTION_NEXT);

        mEditServiceCharge = (EditText) MainFragmentView.findViewById(R.id.editServiceCharge);
        mEditTax = (EditText) MainFragmentView.findViewById(R.id.editTax);

        mCreditCardLayout = (RelativeLayout) MainFragmentView.findViewById(R.id.creditCardLayout);
        mEditMonthlyInstallmentPlan = (EditText) MainFragmentView.findViewById(R.id.editMonthlyInstallmentPlan);

        mCashReceiptLayout = (RelativeLayout) MainFragmentView.findViewById(R.id.cashReceiptLayout);
        mRadioGroupCashReceiptType = (RadioGroup) MainFragmentView.findViewById(R.id.radioGroupCashReceiptType);
        mEditKeyIn = (EditText) MainFragmentView.findViewById(R.id.editKeyIn);

        mEditApprovalNumber = (EditText) MainFragmentView.findViewById(R.id.editApprovalNumber);
        mEditApprovalDate = (EditText) MainFragmentView.findViewById(R.id.editApprovalDate);

        mRadioGroupPaymentOption = (RadioGroup) MainFragmentView.findViewById(R.id.radioGroupPaymentOption);
        mRadioGroupPaymentOption.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {
                switch (checkedId) {
                    case R.id.creditCardRadioBtn:
                        clearCashReceiptInputUI();
                        mCreditCardLayout.setVisibility(View.VISIBLE);
                        break;
                    case R.id.cashReceiptRadioBtn:
                        clearCreditCardInputUI();
                        mCashReceiptLayout.setVisibility(View.VISIBLE);
                        break;
                }
            }
        });


        mEditSumOfMoney.setEnabled(true);

        mBtnCancel = (Button) MainFragmentView.findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String sumOfMoney = mEditSumOfMoney.getText().toString();
                String approvalNum = mEditApprovalNumber.getText().toString();
                String approvalDate = mEditApprovalDate.getText().toString();

                int resultValue = checkInputUI(sumOfMoney, approvalNum, approvalDate);

                if (resultValue == 0) {
                    int paymentOptionRadioId = mRadioGroupPaymentOption.getCheckedRadioButtonId();
                    String paymentOption;
                    switch (paymentOptionRadioId) {
                        case R.id.creditCardRadioBtn:
                            paymentOption = Utils.PAYMENT_OPTION_IC;
                            mCallbackMain.callbackMainFragment_Cancel(paymentOption, Utils.TRD_TYPE_IC_CREDIT_CANCEL, sumOfMoney, null, null, null, null, approvalDate, approvalNum);
                            break;
                        case R.id.cashReceiptRadioBtn:
                            paymentOption = Utils.PAYMENT_OPTION_CASH;
                            mCallbackMain.callbackMainFragment_Cancel(paymentOption, Utils.TRD_TYPE_CASH_CANCEL, sumOfMoney, null, null, null, null, approvalDate, approvalNum);
                            break;
                    }
                } else {
                    makeErrorMsgDialog(resultValue);
                }
            }
        });
        mBtnCancel.setVisibility(View.GONE);

        mBtnClear = (Button) MainFragmentView.findViewById(R.id.btnClear);
        mBtnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearInputUI();
            }
        });

        mBtnOk = (Button) MainFragmentView.findViewById(R.id.btnOk);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int approvalOptionRadioId = mRadioGroupApprovalOption.getCheckedRadioButtonId();
                String sumOfMoney = mEditSumOfMoney.getText().toString();
                int resultValue= -1;
                switch (approvalOptionRadioId) {
                    case R.id.cancelRadioBtn:
                        String approvalNum = mEditApprovalNumber.getText().toString();
                        String approvalDate = mEditApprovalDate.getText().toString();
                        resultValue = checkInputUI(sumOfMoney, approvalNum, approvalDate);

                        if (resultValue == SUCCESS_ALL_INPUT_VALUE) {
                            int paymentOptionRadioId = mRadioGroupPaymentOption.getCheckedRadioButtonId();
                            String paymentOption;
                            switch (paymentOptionRadioId) {
                                case R.id.creditCardRadioBtn:
                                    paymentOption = Utils.PAYMENT_OPTION_IC;
                                    mCallbackMain.callbackMainFragment_Cancel(paymentOption, Utils.TRD_TYPE_IC_CREDIT_CANCEL, sumOfMoney, null, null, null, null, approvalDate, approvalNum);
                                    break;
                                case R.id.cashReceiptRadioBtn:
                                    paymentOption = Utils.PAYMENT_OPTION_CASH;
                                    mCallbackMain.callbackMainFragment_Cancel(paymentOption, Utils.TRD_TYPE_CASH_CANCEL, sumOfMoney, null, null, null, null, approvalDate, approvalNum);
                                    break;
                            }
                        } else {
                            makeErrorMsgDialog(resultValue);
                        }
                        break;
                    case R.id.approvalRadioBtn:
                        int paymentOptionRadioId = mRadioGroupPaymentOption.getCheckedRadioButtonId();
                        resultValue = checkInputUI(paymentOptionRadioId, sumOfMoney);
                        if (resultValue == SUCCESS_ALL_INPUT_VALUE) {

                            String serviceCharge = mEditServiceCharge.getText().toString();
                            String tax = mEditTax.getText().toString();
                            String installment = mEditMonthlyInstallmentPlan.getText().toString();
                            String paymentOption = "";
                            boolean isCardReading = true;
                            String keyInStr = null;

                            switch (paymentOptionRadioId) {
                                case R.id.creditCardRadioBtn:
                                    paymentOption = Utils.PAYMENT_OPTION_IC;
                                    mCallbackMain.callbackMainFragment_Approval(paymentOption, Utils.TRD_TYPE_IC_CREDIT_APPROVAL, sumOfMoney, serviceCharge, tax, installment, null, null);

                                    break;
                                case R.id.cashReceiptRadioBtn:
                                    paymentOption = Utils.PAYMENT_OPTION_CASH;
                                    String cashReceiptType = Utils.CASH_RECEIPT_TYPE_INDIVIDUAL;

                                    int cashReceiptTypeRadioId = mRadioGroupCashReceiptType.getCheckedRadioButtonId();
                                    switch (cashReceiptTypeRadioId) {
                                        case R.id.individualRadioBtn:
                                            cashReceiptType = Utils.CASH_RECEIPT_TYPE_INDIVIDUAL;
                                            break;
                                        case R.id.corporationRadioBtn:
                                            cashReceiptType = Utils.CASH_RECEIPT_TYPE_CORPORATION;
                                            break;
                                    }

                                    if (!isKeyInDataEmpty()) {
                                        isCardReading = false;
                                        keyInStr = mEditKeyIn.getText().toString();
                                    } else {
                                        paymentOption = Utils.PAYMENT_OPTION_MS;
                                    }
                                    mCallbackMain.callbackMainFragment_Approval(paymentOption, Utils.TRD_TYPE_CASH_APPROVAL, sumOfMoney, serviceCharge, tax, installment, keyInStr, cashReceiptType);
                            }
                            hideAllSoftKeypad(isCardReading);
                        } else {
                            makeErrorMsgDialog(resultValue);
                        }
                        break;
                }
            }
        });


        setDeviceAuthNum();
        setApprovalInputUI();
        return MainFragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;
        if (context instanceof FragmentCallback.CallbackMainFragment) {
            mCallbackMain = (FragmentCallback.CallbackMainFragment) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement CallbackMainFragment");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
    private void setDeviceAuthNum() {
        if(mTextDeviceAuthNum==null){
            return;
        }
        String authNum = Utils.getSharedPreference(mContext, Utils.PREF_KEY_AUTHENNUM);
        if (authNum.equalsIgnoreCase(DEFAULT_VALUE_OF_DEVICE_AUTHORIZATION_NUMBER)) {
            mTextDeviceAuthNum.setText(" " + getString(R.string.text_value_device_authorization_number_default));
            Utils.SimpleDailog(mContext, "", mContext.getResources().getString(R.string.dialog_error_msg_unknown_device_authnum) +"\n"+mContext.getResources().getString(R.string.dialog_error_msg_can_not_transaction));
        } else {
            byte[] authNumBuf= Utils.hexToByteArray(authNum);
            authNum = new String(authNumBuf);
            String authNumStr = getResources().getString(R.string.text_lable_device_authorization_number) + " " + authNum;
            String appNumStr = getResources().getString(R.string.text_lable_device_app_number) + " " + mContext.getResources().getString(R.string.text_lable_device_app_number_default);

            mTextDeviceAuthNum.setText(authNumStr + "\n" + appNumStr);
        }
    }
    private void makeErrorMsgDialog(int resultValue) {
        String errorMsg = "";

        switch (resultValue) {
            case ERROR_MONEY_EDIT_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_empty_sum_of_money);
                break;
            case ERROR_PAYMENT_OPTION_RADIO_GROUP_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_empty_payment_option);
                break;
            case ERROR_MONTH_EDIT_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_empty_monthly_installment_plan);
                break;
            case ERROR_CASH_RECEIPT_TYPE_RADIO_GROUP_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_empty_cash_receipt_type);
                break;
            case ERROR_INDIVIDUAL_KEYIN_LENGTH_MIN:
                errorMsg = getString(R.string.dialog_error_msg_length_individual_keyin);
                break;
            case ERROR_CORPORATION_KEYIN_LENGTH:
                errorMsg = getString(R.string.dialog_error_msg_length_corporation_keyin);
                break;
            case ERROR_MONTH_NO_EDIT_VALUE:
                errorMsg = getString(R.string.dialog_error_msg_no_value_monthly_installment_plan);
                break;
            case ERROR_APPROVAL_NUMBER_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_empty_approval_number);
                break;
            case ERROR_APPROVAL_DATE_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_empty_approval_date);
                break;
        }
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setMessage(errorMsg);
        alert.show();
    }

    private int checkInputSumOfMoneyValue(String sumOfMoneyStr) {
        if (!sumOfMoneyStr.isEmpty() && (sumOfMoneyStr.length() != 0)) {
            return SUCCESS_ALL_INPUT_VALUE;
        } else {
            return ERROR_MONEY_EDIT_EMPTY;
        }
    }
    private int checkInputUI(String sumOfMoneyStr, String approvalNum, String approvalDate) {
        if (checkInputSumOfMoneyValue(sumOfMoneyStr) == ERROR_MONEY_EDIT_EMPTY) {
            return ERROR_MONEY_EDIT_EMPTY;
        }

        if (approvalNum.length() == 0) {
            return ERROR_APPROVAL_NUMBER_EMPTY;
        }

        if (approvalDate.length() == 0) {
            return ERROR_APPROVAL_DATE_EMPTY;
        }
        return 0;
    }

    private int checkInputUI(int paymentOptionRadioId, String sumOfMoneyStr) {
        int resultCode = -1;

        if (checkInputSumOfMoneyValue(sumOfMoneyStr) == ERROR_MONEY_EDIT_EMPTY) {
            return ERROR_MONEY_EDIT_EMPTY;
        }

        if (paymentOptionRadioId == -1) {
            return ERROR_PAYMENT_OPTION_RADIO_GROUP_EMPTY;
        } else if (paymentOptionRadioId == R.id.creditCardRadioBtn) {
            resultCode = checkCreditCardInputUI(sumOfMoneyStr);
        } else if (paymentOptionRadioId == R.id.cashReceiptRadioBtn) {
            resultCode = checkCashReceiptInputUI();
        }
        return resultCode;
    }

    private int checkCreditCardInputUI(String sumOfMoneyStr) {
        if (Integer.parseInt(sumOfMoneyStr) >= INSTALLABLE_AMOUNT) {
            if (isMonthlyInstallmentPlanEmpty()) {
                mEditMonthlyInstallmentPlan.setText(Utils.DEFAULT_VALUE_OF_MONTHLY_INSTALLMENT_PLAN);
            } else {
            }
        } else {
            if (isMonthlyInstallmentPlanEmpty()) {
                mEditMonthlyInstallmentPlan.setText(Utils.DEFAULT_VALUE_OF_MONTHLY_INSTALLMENT_PLAN);
            } else {
                return ERROR_MONTH_NO_EDIT_VALUE;
            }
        }
        return SUCCESS_ALL_INPUT_VALUE;
    }

    private int checkCashReceiptInputUI() {
        int cashReceiptTypeRadioId = mRadioGroupCashReceiptType.getCheckedRadioButtonId();

        if (cashReceiptTypeRadioId == -1) {
            return ERROR_CASH_RECEIPT_TYPE_RADIO_GROUP_EMPTY;
        } else {
            switch (cashReceiptTypeRadioId) {
                case R.id.individualRadioBtn:
                    if (!isKeyInDataEmpty()) {
                        int keyInDataLength = mEditKeyIn.getText().toString().length();

                        if (keyInDataLength < INDIVIDUAL_KEYIN_LENGTH_MIN) {
                            return ERROR_INDIVIDUAL_KEYIN_LENGTH_MIN;
                        }
                    }
                    break;
                case R.id.corporationRadioBtn:
                    if (!isKeyInDataEmpty()) {
                        int keyInDataLength = mEditKeyIn.getText().toString().length();

                        if (keyInDataLength != CORPORATION_KEYIN_LENGTH) {
                            return ERROR_CORPORATION_KEYIN_LENGTH;
                        }
                    }

                    break;
            }
        }
        return SUCCESS_ALL_INPUT_VALUE;
    }

    private void setApprovalInputUI(){
        clearInputUIForApplovalchoice();
        mApprovalLayout.setVisibility(View.VISIBLE);
        mCancelLayout.setVisibility(View.GONE);
    }
    private void setCancelInputUI(){
        clearInputUIForApplovalchoice();
        mApprovalLayout.setVisibility(View.GONE);
        mCancelLayout.setVisibility(View.VISIBLE);
    }

    private void clearInputUIForApplovalchoice() {
        mRadioGroupPaymentOption.check(R.id.creditCardRadioBtn);
        mEditServiceCharge.setText("");
        mEditTax.setText("");
        mEditMonthlyInstallmentPlan.setText("");
        mEditApprovalDate.setText("");
        mEditApprovalNumber.setText("");
    }

    private void clearInputUI() {
        mEditSumOfMoney.setText("");
        mRadioGroupPaymentOption.check(R.id.creditCardRadioBtn);
        mEditServiceCharge.setText("");
        mEditTax.setText("");
        mEditMonthlyInstallmentPlan.setText("");
        mEditApprovalDate.setText("");
        mEditApprovalNumber.setText("");
        clearCashReceiptInputUI();
    }

    private void clearCreditCardInputUI() {
        mCreditCardLayout.setVisibility(View.GONE);
        mEditMonthlyInstallmentPlan.setText("");
    }

    private void clearCashReceiptInputUI() {
        mCashReceiptLayout.setVisibility(View.GONE);
        mRadioGroupCashReceiptType.check(R.id.individualRadioBtn);
        mEditKeyIn.setText("");
    }


    private boolean isKeyInDataEmpty() {
        boolean retValue = true;
        String keyIn = mEditKeyIn.getText().toString();
        if (!keyIn.isEmpty() && (keyIn.length() != 0)) {
            retValue = false;
        } else {
            retValue = true;
        }
        return retValue;
    }

    private boolean isMonthlyInstallmentPlanEmpty() {
        boolean retValue = true;

        String monthlyInstallmentPlanStr = mEditMonthlyInstallmentPlan.getText().toString();
        if (!monthlyInstallmentPlanStr.isEmpty() && (monthlyInstallmentPlanStr.length() != 0)) {
            retValue = false;
        } else {
            retValue = true;
        }

        return retValue;
    }

    private void hideAllSoftKeypad(boolean isCardReading) {
        hideKeyBoard(mEditSumOfMoney);
        hideKeyBoard(mEditServiceCharge);
        hideKeyBoard(mEditTax);

        hideKeyBoard(mEditApprovalNumber);
        hideKeyBoard(mEditApprovalDate);

        if (isCardReading) {
            hideKeyBoard(mEditMonthlyInstallmentPlan);
        } else {
            hideKeyBoard(mEditKeyIn);
        }
    }
    private void hideKeyBoard(EditText editText) {
        if (editText == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }
}
