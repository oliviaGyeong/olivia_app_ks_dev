package device.apps.pmpos;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;

public class TCPClient {
    private static final String TAG = TCPClient.class.getSimpleName();

    private static int SERVER_PORT = 0;
    private static String SERVER_IP = "";

    private static Context mContext;

    private static int VANHOST_RECONNECT_COUNT = 3;
    private static int SOCKET_TIMEOUT = 10000;
    private static int SOCKET_SLEEP_TIME = 1000;
    private static int SOCKET_CONNECT_TIMEOUT = 5000;

    private static Socket mSocket;
    private static Handler mPosServiceHandler = null;

    private static BufferedOutputStream mOutput;
    private static BufferedInputStream mInput;
    public static boolean mConnected = false;

    static {
        try {
            System.loadLibrary("pkt_VAN");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public TCPClient(Context context,Handler handler) {
        mContext = context;
        mPosServiceHandler = handler;
    }

    boolean CheckCTEOTFlag = false;
    public  void requestData(byte[] data, byte[] recvData) {

        if(recvData != null && recvData.length > 0){
            CheckCTEOTFlag = true;
        }
        new Thread(new RequestThread(data,recvData)).start();
    }

    public  class RequestThread implements Runnable {
        private byte[] mReqData;
        private byte[] mRecvData;

        public RequestThread(byte[] reqData, byte[] recvData) {
            mReqData = reqData;
            mRecvData = recvData;
        }

        boolean threadCheckFlag = true;

        @Override
        public void run() {
            if (!isConnected()) {
                if (!connect()) {
                    setHandleMassege(Utils.MSG_VANHOST_CONNECTION_ERROR, null);
                    threadCheckFlag = false;
                    return;
                }
            }
            //write
            if (!writeData(mReqData)) {
                setHandleMassege(Utils.MSG_VANHOST_CONNECTION_ERROR, null);
                threadCheckFlag = false;
            }
            //Read
            int sizeOfDataRead = 0;
            long timeout = SOCKET_TIMEOUT;
            long stime = new Date().getTime();
            long rtime = 0L;
            int rvalues = 0;

            int reConnectCount = 0;

            byte[] tempBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];

            while (rtime < timeout && threadCheckFlag) {

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    threadCheckFlag = false;
                }
                rtime = System.currentTimeMillis() - stime;
                if (rtime > timeout) { //Timeout
                    setHandleMassege(Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_TIMEOUT, null);
                    threadCheckFlag = false;
                }

                sizeOfDataRead = readData(tempBuf, sizeOfDataRead, (tempBuf.length - sizeOfDataRead));

                if (sizeOfDataRead == -1) {//Socket connection Error
                    if(reConnectCount < VANHOST_RECONNECT_COUNT){
                        if (reConnect()) {
                            sizeOfDataRead = 0;
                        }
                    }else{
                        setHandleMassege(Utils.MSG_VANHOST_CONNECTION_ERROR, null);
                        threadCheckFlag = false;
                    }
                } else {
                    rvalues = rvalues + sizeOfDataRead;
                    if (rvalues > 0 && tempBuf[sizeOfDataRead - 1] >= (byte) 0x0d) {
                        if ((tempBuf[0] == 'A' && (tempBuf[1] == '1' || tempBuf[1] == '2'))) {
                            mRecvData = new byte[sizeOfDataRead];
                            System.arraycopy(tempBuf, 0, mRecvData, 0, sizeOfDataRead);

                            String trdTypeStr = Utils.checkTrdType(mRecvData);
                            if (!trdTypeStr.isEmpty() && !trdTypeStr.equals(Utils.TRD_TYPE_IC_CREDIT_APPROVAL)) { //IC신용인경우만 check CTEOT 0x0d
                                setHandleMassege(Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK, mRecvData);
                                threadCheckFlag = false;
                            } else {
                              setHandleMassege(Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK_F1, mRecvData);
                              threadCheckFlag = false;
                                //continue;
                            }
                        } else if (tempBuf[0] == (byte) 0x43 && tempBuf[1] == 0x54) {
                            if (Utils.DEBUG_VAN)
                                Utils.debugTx("[CTEOT 0x0d] from VAN  :", tempBuf, rvalues);
                            if (Utils.DEBUG_VAN)
                                Utils.debugTx("recvData from VAN  :", mRecvData, mRecvData.length);
                            setHandleMassege(Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK, mRecvData);
                            threadCheckFlag = false;
                        }
                        //conntinue;
                    }

                }
            }
        }

        public  void setSocket(Context context) {
            String VANHOST_IP = Utils.getSharedPreference(mContext, Utils.PREF_KEY_VAN_IP);
            String VANHOST_PORT = Utils.getSharedPreference(mContext, Utils.PREF_KEY_VAN_PORT);
            if (VANHOST_IP.equals(Utils.PREF_KEY_FLAG_STR_FALSE)) {
                VANHOST_IP = context.getString(R.string.vanhost_ip_address_text);
                VANHOST_PORT = context.getString(R.string.vanhost_port_number_text);
            }
            SERVER_IP = VANHOST_IP;
            SERVER_PORT = Integer.parseInt(VANHOST_PORT);
        }


        public void disconnect() {
            try {
                if (mOutput != null) {
                    mOutput.close();
                    mOutput = null;
                }
                if (mInput != null) {
                    mInput.close();
                    mInput = null;
                }
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private  boolean connect() {
            setSocket(mContext);
            mConnected = false;
            try {
                InetSocketAddress socketAddress = new InetSocketAddress(SERVER_IP, SERVER_PORT);
                mSocket = new Socket();
                mSocket.connect(socketAddress);
                mInput = new BufferedInputStream(mSocket.getInputStream());
                mOutput = new BufferedOutputStream(mSocket.getOutputStream());
                mConnected = true;
                return mConnected;
            } catch (ConnectException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NetworkOnMainThreadException e) {
                e.printStackTrace();
            }
            return mConnected;
        }

        private  boolean reConnect() {
            if (isConnected()) {
                return true;
            } else {
                if (connect()) {
                    return true;
                }
            }
            return false;
        }

        private  boolean isConnected() {
            boolean flag= false;
            if (mSocket != null) {
                flag = mSocket.isConnected();
                return flag;
            }
            return flag;
        }


        private  boolean writeData(byte[] data) {
            if(Utils.DEBUG_VAN)Utils.debugTx("writeData DATA:",data,data.length);
            int size = 0;
            boolean writeDataFlag = false;

            if (data == null) {
                return writeDataFlag;
            }
            try {
                mOutput.write(data);
                mOutput.flush();
                writeDataFlag = true;
            } catch (IOException e) {
                e.printStackTrace();
                //Write Error
            }
            return writeDataFlag;

        }

        private  int readData(byte[] data, int offset, int length) {
            int size = 0;
            if(mInput!= null){
                try {
                    size = mInput.read(data, offset, length);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return size;
        }

        public  void setHandleMassege(int what, Object obj) {
            if(what == Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK || what== Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK_F1){
            }else {
                if(CheckCTEOTFlag && obj == null){
                    byte[] bytes = new byte[]{Utils.OBG_NETWORK_CANCEL};
                    obj= bytes;
                }
            }
            Message msg = Message.obtain();
            msg.what = what;
            msg.obj = obj;
            mPosServiceHandler.sendMessage(msg);

            if( what!= Utils.MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK_F1){
                disconnect();
            }
        }
    }
}
