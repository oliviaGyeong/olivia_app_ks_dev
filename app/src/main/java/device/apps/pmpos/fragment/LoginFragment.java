package device.apps.pmpos.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import device.apps.pmpos.R;
import device.apps.pmpos.Utils;
import device.apps.pmpos.fragment.listener.FragmentCallback;

public class LoginFragment extends Fragment {

    private final String TAG = LoginFragment.class.getSimpleName();

    private final int CHANGE_DEFAULT_PASSWORD = -1;
    private final int SUCCESS_ALL_INPUT_VALUE = 0;
    private final int ERROR_PWD_EMPTY = 1;
    private final int ERROR_PWD_NOT_SAME = 2;
    private final int ERROR_CURRENT_PWD_EMPTY = 3;
    private final int ERROR_NEW_PWD_EMPTY = 4;
    private final int ERROR_RETRY_PWD_EMPTY = 5;
    private final int ERROR_CURRENT_PWD_NOT_SAME_AS_USER_PWD = 6;
    private final int ERROR_NEW_PWD_NOT_SAME_AS_RETRY_PWD = 7;

    private EditText mEditPassword;
    private Button mBtnForgotPassword;
    private Button mBtnSignIn;

    private Context mContext;
    private FragmentCallback.MoveOtherFragmentListener mMoveFragListener;

    public LoginFragment() {

    }

    public static LoginFragment newInstance() {
        LoginFragment fragment = new LoginFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View LoginFragmentView = inflater.inflate(R.layout.fragment_login, container, false);

        mEditPassword = (EditText) LoginFragmentView.findViewById(R.id.editPassword);
        mBtnForgotPassword = (Button) LoginFragmentView.findViewById(R.id.btnForgotPassword);
        mBtnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePasswordDialog();
            }
        });

        mBtnSignIn = (Button) LoginFragmentView.findViewById(R.id.btnSignIn);
        mBtnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userPassword = mEditPassword.getText().toString();

                int resultValue = checkPasswordValue(userPassword);
                if (resultValue == SUCCESS_ALL_INPUT_VALUE) {
                    hideKeyBoard(mEditPassword);
                    mMoveFragListener.moveOtherFragment(Utils.FRAG_NUM_MAIN, false, null);
                } else if (resultValue == CHANGE_DEFAULT_PASSWORD){
                    changePasswordDialog();
                    mEditPassword.setText("");
                } else {
                    makeErrorMsgDialog(resultValue);
                }
            }
        });
        return LoginFragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;

        if (context instanceof FragmentCallback.MoveOtherFragmentListener) {
            mMoveFragListener = (FragmentCallback.MoveOtherFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MoveOtherFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mMoveFragListener = null;
    }

    private void changePasswordDialog() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.layout_change_pwd_dialog, null);

        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setIcon(android.R.drawable.ic_lock_lock);
        builder.setTitle(getString(R.string.dialog_builder_change_password_title));
        builder.setView(layout);
        builder.setCancelable(false);

        builder.setPositiveButton(android.R.string.ok, null);
        builder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog passwordDialog = builder.create();
        passwordDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                Button okBtn = passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
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
                Button cancelBtn = passwordDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
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

    private int checkPasswordValue(String password) {
        if (password.isEmpty() || (password.length() == 0)) {
            return ERROR_PWD_EMPTY;
        }

        String currentPwd = Utils.getSharedPrefLoginPwdValue(mContext);
        if (currentPwd.equals(password)) {
            if (currentPwd.equals(Utils.DEFAULT_LOGIN_PWD)) {
                return CHANGE_DEFAULT_PASSWORD;
            } else {
                return SUCCESS_ALL_INPUT_VALUE;
            }
        } else {
            return ERROR_PWD_NOT_SAME;
        }
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
            case ERROR_PWD_EMPTY:
                errorMsg = getString(R.string.dialog_error_msg_pwd_empty);
                break;
            case ERROR_PWD_NOT_SAME:
                errorMsg = getString(R.string.dialog_error_msg_current_pwd_not_same);
                break;
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
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        alert.setMessage(errorMsg);
        alert.create().show();
    }

    private void hideKeyBoard(EditText editText) {
        if (editText == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

}
