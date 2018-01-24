package device.apps.pmpos;

import android.content.Context;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Date;

import device.sdk.MsrManager;

public class RdiManager extends MsrManager{

    private final String TAG = RdiManager.class.getSimpleName();

    static {
        try {
            System.loadLibrary("pkt_VAN");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private native int checkReceivePacketFromIFM(byte[] vanPktBuf, byte[] rdPktBuf, int vanBufLen, int rdBufLen);

    private static Context mContext;

    public static int ENABLE = 1;
    public static int DISABLE = 0;

    private int TIMEOUT_3MIN =Utils.TIMEOUT_3MIN; //30000;
    private int TIMEOUT_3SEC =Utils.TIMEOUT_3SEC; //3000;
    private int TIMEOUT = TIMEOUT_3SEC;

    private boolean mIsRdiOpened = false;

    public RdiManager(Context context) {
        mContext = context;
    }

    CountDownTimer mCountDownTimer;
    public void startTimeout(int timeout) {
        mCountDownTimer = new CountDownTimer(timeout, 100) {
            public void onTick(long millisUntilFinished) {
                if(Utils.DEBUG_VALUE) Log.i(TAG, "onTick() millisUntilFinished : " + millisUntilFinished);
                if(Utils.DEBUG_VALUE)Log.i(TAG, "seconds remaining: " + millisUntilFinished / 100);
                //OK CODE RDI  --- RETURN OPEN
            }

            public void onFinish() {

            }
        }.start();
    }



    public boolean powerOn() {
        if (!mIsRdiOpened) {
            int opend  = rdiOpen();
            if (opend == 0) {
                mIsRdiOpened = true;
            }else if(opend == -1){
                if(Utils.DEBUG_VALUE)Log.e(TAG,"rdiOpen : false !!!!!!!!!!!!!!!!! ");
                mIsRdiOpened = false;
            }
        }
        return mIsRdiOpened;
    }

    public void powerDown() {
        if (rdiClose() == 0) {
            mIsRdiOpened = false;
        }
    }

    // enable 1, disable 0.
    public int setEnable(int enable) {
        return rdiSetEnable(enable);
    }

    public int isEnabled() {
        return rdiIsEnabled();
    }

    public int read(byte[] data ,int length) {
        return rdiRead(data,length);
    }

    public int numOfElementsInBuf() {
        return rdiNelem();
    }

    public int clear() {
        return rdiClear();
    }

    public int write(byte[] data, int length) {
        byte[] padingData = setPaddingData(data);
        if (Utils.DEBUG_ICREADER) Utils.debugTx(" Write_To_Rdi_Packet", padingData, padingData.length);
        threadCheckFlag = false;
        return rdiWrite(padingData, padingData.length);
    }

    public byte[] setPaddingData(byte[] data) {
        int length = data.length;
        if ((length % 8) == 7) {
            length++;
        }
        length += (8 - (length % 8)) + 11;
        byte[] returnData = new byte[length];

        for (int i = 0; i < length; i++) {
            if (i >= data.length) {
                returnData[i] = (byte) 0xFF;
            } else {
                returnData[i] = data[i];
            }
        }
        return returnData;
    }

    private Handler mMSRHandler = null;
    boolean threadCheckFlag = false;


    public void readData(Handler msrHandler,int timeout){
        mMSRHandler = msrHandler;
        new Thread(new ReadThread(timeout)).start();
    }

    private class ReadThread implements Runnable {
        int mTimeout;

        ReadThread( int timeout) {
            threadCheckFlag = true;
            mTimeout = timeout;
        }

        @Override
        public void run() {
            //Read
            long timeout = mTimeout;
            long stime = new Date().getTime();
            long rtime = 0L;
            int rvalues = 0;

            int sizeOfDataRead = 0;
            int sizeOfRecvPacket = 0;
            byte[] resMSRBuf = new byte[Utils.MAX_ICREADER_PACKET_SIZE]; //756
            byte[] rdPktBuf = new byte[Utils.MAX_ICREADER_PACKET_SIZE];

            while (rtime < timeout && threadCheckFlag && !Thread.interrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    threadCheckFlag = false;
                }
                rtime = System.currentTimeMillis() - stime;

                sizeOfDataRead = read(resMSRBuf, resMSRBuf.length);

                if (sizeOfDataRead == -1) {
                    setHandleMassege(null, Utils.MSG_RECEIVE_IC_READER_DATA_FAIL);
                    threadCheckFlag = false;
                }
                rvalues = rvalues + sizeOfDataRead;

                if(rtime > timeout) { //Timeout
                    setHandleMassege(null, Utils.MSG_RECEIVE_IC_READER_DATA_TIMEOUT);
                    threadCheckFlag = false;
                }

                if(rvalues >= 6 ){
                    threadCheckFlag = false;
                    break;
                }
            }


            if (sizeOfDataRead > 0) {
                sizeOfRecvPacket = checkReceivePacketFromIFM(rdPktBuf, resMSRBuf, rdPktBuf.length, sizeOfDataRead);

                if (sizeOfRecvPacket > 0) {
                    if (Utils.DEBUG_ICREADER) Utils.debugTx("rdPktBuf::", rdPktBuf, sizeOfRecvPacket);
                    byte[] recvData = new byte[sizeOfRecvPacket - 1];
                    System.arraycopy(rdPktBuf, 1, recvData, 0, sizeOfRecvPacket - 1);//checkData -1 ( FF|FE )

                    //Data check FF == ok, FE = Fail
                    if (rdPktBuf[0] == (byte) 0xff) {
                        setHandleMassege(recvData, Utils.MSG_RECEIVE_IC_READER_DATA_OK);
                    } else if (rdPktBuf[0] == (byte) 0xfe) {
                        setHandleMassege(recvData, Utils.MSG_RECEIVE_IC_READER_DATA_FAIL);
                    } else {
                        setHandleMassege(null, Utils.MSG_RECEIVE_IC_READER_DATA_FAIL);
                    }
                } else {
                    setHandleMassege(null, Utils.MSG_RECEIVE_IC_READER_DATA_FAIL);
                }
            }
        }


        public void setHandleMassege(byte[] recvData, int state){
            if(mMSRHandler == null){
                return;
            }
            Message obtainMsg = mMSRHandler.obtainMessage();
            obtainMsg.what = state;
            if(state == Utils.MSG_RECEIVE_IC_READER_DATA_OK ){
                obtainMsg.obj = recvData;
            }else if(state == Utils.MSG_RECEIVE_IC_READER_DATA_FAIL){
            }else if(state == Utils.MSG_RECEIVE_IC_READER_DATA_TIMEOUT){
            }

            mMSRHandler.sendMessage(obtainMsg);
            mMSRHandler = null;
        }
    }
}
