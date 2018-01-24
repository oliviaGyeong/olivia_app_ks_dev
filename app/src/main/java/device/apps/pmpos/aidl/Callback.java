package device.apps.pmpos.aidl;



public class Callback {
    public interface OnCallBack {
        void OnCallBack(CallBackList callback, byte[] recvRdPktBuf, byte[] recvVanPktBuf );
    }
//    public interface OnDaemonCallback {
//        void OnDaemonCallback(DaemonCallbackList callbackList, byte[] bytes);
//    }
//    //DaemonCallbackList
//    public enum DaemonCallbackList{
//        DAEMON_OK_READ_DATA,
//
//        DAEMON_FAIL_READ_DATA,
//        DAEMON_FAIL_READ_TIMEOUT,
//        DAEMON_FAIL_SERVER_ERROR,
//        DAEMON_SEND_FINISH
//    }

    //CallBack List
    public enum CallBackList{
        VAN_OK_READ_DATA,

        RDI_OK_READ_DATA,

        RDI_FAIL_MAKE_PACKET,
        RDI_FAIL_READ_DATA,
        RDI_FAIL_READ_TIMEOUT,
        RDI_FAIL_CONNECT_ERROR,

        VAN_FAIL_MAKE_PACKET,
        VAN_FAIL_READ_DATA,
        VAN_FAIL_READ_TIMEOUT,
        VAN_FAIL_SERVER_ERROR,

        CANCEL_DEVICE_ROOTING,
        CANCEL_DEVICE_DOWNLOAD,

        GOTO_MAIN,
        GOTO_IC_READ_SCENE,     //for IC UI
        GOTO_MS_SWIPE_SCENE,    //for MS UI
        GOTO_PROGRESS,          //for Van UI
        GOTO_DEAL_COMPLETE,
        GOTO_RECEIPT_PRINT
    }
}





