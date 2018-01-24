package device.apps.pmpos.aidl;


import java.io.Serializable;

public class DialogListener implements Serializable{
    public interface OnClickDialogCallback{
        void OnClickDialogCallback(int onClickResultCode);
    }
}





