package device.apps.pmpos.packet;


import android.util.Log;
import device.apps.pmpos.PMPosHelper;
import device.apps.pmpos.Utils;

public class ICReaderPacketManager {
    private final String TAG = ICReaderPacketManager.class.getSimpleName();
    private static final String nativeLibName = "pkt_IFM";

    static {
        try {
            System.loadLibrary(nativeLibName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private native int reqInitializeIFM(byte[] rdPktBuf, int rdPktLen);
    private native int reqPreambleToIFM(byte[] rdPktBuf, byte[] dataPktBuf, int rdPktLen, int dataPtkLen);
    private native int reqCancelPaymentToIFM(byte[] rdPktBuf, byte[] dataPktBuf, int rdPktLen, int dataPtkLen);
    private native int reqCreditApprovalResultToIFM(byte[] rdPktBuf, byte[] vanPktBuf, int rdPktLen, int vanPktLen);

    private native int pm1100DeviceInfo(byte[] rdPktBuf, byte[] dateTime, int rdBufLen);
    private native int pm1100CreateFutureKey(byte[] rdPktBuf, int rdBufLen);
    private native int pm1100UpdateFutureKey(byte[] rdPktBuf, byte[] vanPktBuf, int rdBufLen, int vanPktBufLen);
    private native int pm1100SelfProtection(byte[] rdPktBuf, int rdBufLen);
    private native int pm1100MutualAuthentication(byte[] rdPktBuf, byte[] reqType, int rdBufLen);
    private native int pm1100MutualAuthInfoResult(byte[] rdPktBuf, byte[] vanPktBuf, int rdBufLen, int vanPktBufLen);//reqMutualAuthResult
    //private native int pm1100DeviceFactoryReset();  //Add pm1100DeviceFactoryReset();
    private native int reqSetSystemDateTime(byte[] rdPktBuf, byte[] dateTime, int dateBufLen, int rdBufLen);
    private native int pm1100FirmwereUpdate(byte[] rdPktBuf,int rdBufLen, byte[] pFileData, int nSendDataLen, int nFileTotalLen);


    public ICReaderPacketManager() {
    }


    public static class RdiFormat {
        public static final int STX = 1;
        public static final int DATA_LENGTH = 2;
        public static final int COMMANID = 1;
        public static final int ETX = 1;
        public static final int LRC = 1;

        public static final int getAllSize = STX+DATA_LENGTH+COMMANID+ETX+LRC;
        public static final int getHeaderSize = STX+DATA_LENGTH+COMMANID;
        public static final int getFooterSize = ETX+LRC;

        public static final byte CMD_STX = (byte) 0x02;   // Start of Text
        public static final byte CMD_ETX = (byte) 0X03;   // End of Text
    }

    private final int RDI_FORMAT_BASE_PACKET_LENGTH = RdiFormat.STX +
            RdiFormat.DATA_LENGTH + RdiFormat.COMMANID + RdiFormat.ETX +RdiFormat.LRC;

    private final int REQ_PACKET_LENGTH_IC_CREATE_FUTURE_KEY = RDI_FORMAT_BASE_PACKET_LENGTH;
    private final int REQ_PACKET_LENGTH_IC_UPDATE_FUTURE_KEY = RDI_FORMAT_BASE_PACKET_LENGTH + Utils.REQ_DATA_LENGTH_IC_UPDATE_FUTURE_KEY;
    private final int REQ_PACKET_LENGTH_IC_SELF_PROTECTION = RDI_FORMAT_BASE_PACKET_LENGTH;
    private final int REQ_PACKET_LENGTH_IC_MUTUAL_AUTHENTICATION = RDI_FORMAT_BASE_PACKET_LENGTH + Utils.REQ_DATA_LENGTH_IC_MUTUAL_AUTHENTICATION;
    private final int REQ_PACKET_LENGTH_IC_MUTUAL_AUTH_INFO_RESULT = RDI_FORMAT_BASE_PACKET_LENGTH + Utils.REQ_DATA_LENGTH_IC_MUTUAL_AUTH_INFO_RESULT;
    private final int REQ_PACKET_LENGTH_IC_DEVICE_INFO = RDI_FORMAT_BASE_PACKET_LENGTH + Utils.REQ_DATA_LENGTH_IC_TEST_DEVICE_INFO;
    private final int REQ_PACKET_LENGTH_IC_DEVICE_FACTORY_RESET = RDI_FORMAT_BASE_PACKET_LENGTH;
    private final int REQ_PACKET_LENGTH_IC_INIT_SYSTEM_DATA_TIME = RDI_FORMAT_BASE_PACKET_LENGTH +14;
    private final int REQ_PACKET_LENGTH_IC_MAX_FIRMWERE_UPDATE = 2048+30;

    public static final int FLAG_MA_REQUEST_BREAKDOWN = 1; //요청구분 0001 :최신펌웨어, 0003 : EMV KEY
    public static final byte[] FLAG_MA_REQUEST_BREAKDOWN_FW = new byte[]{0x30, 0x30, 0x30, 0x31};
    public static final byte[] FLAG_MA_REQUEST_BREAKDOWN_EMVKEY = new byte[]{0x30, 0x30, 0x30, 0x33};

    private final int REQ_PACKET_LENGTH_IC_PREAMBLE = 6 + Utils.REQ_DATA_LENGTH_IC_PREAMBLE;
    private final int REQ_PACKET_LENGTH_IC_CANCEL_PAYMENT = 6;
    private final int REQ_PACKET_LENGTH_IC_TRANSACTION_RESULT = 6 + Utils.REQ_DATA_LENGTH_IC_TRANSACTION_RESULT;
    private final int REQ_PACKET_LENGTH_IC_INITIALIZE = 6 + Utils.REQ_DATA_LENGTH_IC_INITIALIZ;


    /*
      *   These functions is used in mainActivity to IC.
      */
    public byte[] makeICReaderPacketIFM(byte[] vanPktBuf,byte comdId, PMPosHelper.ApprovalValue approvalValue) {
        byte[] rdPktBuf = null;
        int size = 0;
        byte[] dataPktBuf;

        switch (comdId) {
            case Utils.CMD_ID_REQ_INIT_SYSTEM_DATA_TIME:
               break;

            case Utils.CMD_ID_REQ_IC_PREAMBLE:
                String paymentOption= approvalValue.mPaymentOption;
                String trdType=approvalValue.mTrdType;
                String amount=approvalValue.mAmount;
                String fee=approvalValue.mFee;
                String surTax=approvalValue.mSurTax;
                String instalment=approvalValue.mInstalment;
                String keyIn=approvalValue.mKeyIn;

                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_PREAMBLE];

                dataPktBuf = new byte[31];
                dataPktBuf[0] = Utils.CMD_ID_REQ_IC_PREAMBLE;
                byte[] paymentOptionArray = paymentOption.getBytes();
                byte[] sumOfMoneyArray =Utils.RJLZ(amount.getBytes(), 10);
                byte[] date = Utils.convertCurrentDateToByteArray();
                byte[] receiptCount = new String("0001").getBytes();

                int startIndex = 1;
                System.arraycopy(paymentOptionArray, 0, dataPktBuf, startIndex, paymentOptionArray.length);
                startIndex += paymentOptionArray.length;
                System.arraycopy(sumOfMoneyArray, 0, dataPktBuf, startIndex, sumOfMoneyArray.length);
                startIndex += sumOfMoneyArray.length;
                System.arraycopy(date, 0, dataPktBuf, startIndex, date.length);
                startIndex += date.length;
                System.arraycopy(receiptCount, 0, dataPktBuf, startIndex, receiptCount.length);

                size = reqPreambleToIFM(rdPktBuf, dataPktBuf, rdPktBuf.length, dataPktBuf.length);
                break;

            case Utils.CMD_ID_REQ_IC_CANCEL_PAYMENT:
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_CANCEL_PAYMENT];
                dataPktBuf = new byte[1];
                dataPktBuf[0] = Utils.CMD_ID_REQ_IC_CANCEL_PAYMENT;
                size = reqCancelPaymentToIFM(rdPktBuf, dataPktBuf, rdPktBuf.length, dataPktBuf.length);
                break;
            case Utils.CMD_ID_REQ_IC_TRANSACTION_RESULT:
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_TRANSACTION_RESULT];
                size = reqCreditApprovalResultToIFM(rdPktBuf, vanPktBuf, rdPktBuf.length, vanPktBuf.length);
                break;

            default:
                rdPktBuf = null;
                break;
        }
        if (size <= 0) {
            if (Utils.DEBUG_ICREADER) Log.e(TAG, "makeICReaderPacket size <= 0");
            return null;
        }else {
            if(Utils.DEBUG_ICREADER)Utils.debugTx("makeICReaderPacket() rdPktBuf   ", rdPktBuf, size);
        }
        return rdPktBuf;
    }




    /*
    *   These functions is used in AdminActivity to IC.
    */
    public byte[] makeICReaderPacket(byte[] vanPktBuf, int VanBufLen, byte comdId) {

        byte[] rdPktBuf = null;
        int size = 0;

        switch (comdId) {
            case Utils.CMD_ID_REQ_CREATE_FUTURE_KEY:
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_CREATE_FUTURE_KEY];
                size = pm1100CreateFutureKey(rdPktBuf, rdPktBuf.length); //Utils.CMD_ID_REQ_CREATE_FUTURE_KEY, null);
                break;

            case Utils.CMD_ID_REQ_UPDATE_FUTURE_KEY:
                if (vanPktBuf != null) {
                    rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_UPDATE_FUTURE_KEY];
                    size = pm1100UpdateFutureKey(rdPktBuf, vanPktBuf, rdPktBuf.length, vanPktBuf.length);
                }
                break;

            case Utils.CMD_ID_REQ_SELF_PROTECTION:
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_SELF_PROTECTION];
                size = pm1100SelfProtection(rdPktBuf, rdPktBuf.length);
                break;

            case Utils.CMD_ID_REQ_MUTUAL_AUTHENTICATION:
                byte[] reqType;
                if (FLAG_MA_REQUEST_BREAKDOWN == 3) {
                    reqType = FLAG_MA_REQUEST_BREAKDOWN_EMVKEY;
                } else {
                    reqType = FLAG_MA_REQUEST_BREAKDOWN_FW;
                }
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_MUTUAL_AUTHENTICATION];
                size = pm1100MutualAuthentication(rdPktBuf, reqType, rdPktBuf.length);
                break;

            case Utils.CMD_ID_REQ_MUTUAL_AUTH_INFO_RESULT:
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_MUTUAL_AUTH_INFO_RESULT];
                size = pm1100MutualAuthInfoResult(rdPktBuf, vanPktBuf, rdPktBuf.length, vanPktBuf.length);
                break;

            case Utils.CMD_ID_REQ_DEVICE_INFO:
                byte[] dataTime = Utils.convertCurrentDateToByteArray();
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_DEVICE_INFO];
                size = pm1100DeviceInfo(rdPktBuf, dataTime, rdPktBuf.length);
                break;

            case Utils.CMD_ID_REQ_DEVICE_FACTORY_RESET:
//                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_DEVICE_FACTORY_RESET];
//                size = pm1100DeviceFactoryReset(rdPktBuf,dataTime, rdPktBuf.length);
                byte[] packet2 = new byte[REQ_PACKET_LENGTH_IC_DEVICE_FACTORY_RESET];
//                    rdPktBuf = pm1100DeviceFactoryReset(null,packet2,0);
                break;

            case Utils.CMD_ID_REQ_UPDATE_FIRMWERE:
                if (!isWritingBinFilePacket) {
                    settingFile(vanPktBuf);
                }
                byte[] binfileBuf = readFileBuf();
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_MAX_FIRMWERE_UPDATE];
                size = pm1100FirmwereUpdate(rdPktBuf, rdPktBuf.length, binfileBuf, mTotalFileSize - remindFileSize, mTotalFileSize);
                initFileFalg();
                break;

            case Utils.CMD_ID_REQ_INIT_SYSTEM_DATA_TIME:
                byte[] systemDataTime = Utils.convertCurrentDateToByteArray();
                rdPktBuf = new byte[REQ_PACKET_LENGTH_IC_INIT_SYSTEM_DATA_TIME]; //TEST
                size = reqSetSystemDateTime(rdPktBuf, systemDataTime, rdPktBuf.length,systemDataTime.length);
                break;

            default:
                rdPktBuf = null;
                break;
        }
        if (size <= 0) {
            if (Utils.DEBUG_ICREADER) Log.e(TAG, "makeICReaderPacket size <= 0");
            return null;
        }else {
            if(Utils.DEBUG_ICREADER)Utils.debugTx("makeICReaderPacket() rdPktBuf   ", rdPktBuf, size);
        }
        return rdPktBuf;
    }


    boolean isWritingBinFilePacket = false;
    public byte[] mTotalFileBuf ;
    public Integer mTotalFileSize= -1 ;//(int) (long) fileSize;
    public int ROOF_COUNT = -1;
    public int REMIND_BUF = -1;
    public int remindFileSize = 0;
    public int offset = 0;
    int WRIT_BIN_PACKET_SIZE= 2048; // 2kb


    public void initFileFalg() {
        if (remindFileSize <= 0) {
            mTotalFileSize = -1;//(int) (long) fileSize;
            ROOF_COUNT = -1;
            REMIND_BUF = -1;
            remindFileSize = 0;
            offset = 0;
            mTotalFileBuf= null;
            isWritingBinFilePacket = false;
        }
    }

    public void settingFile(byte[] totalFileBuf) {
        initFileFalg();
        isWritingBinFilePacket = true;
        mTotalFileBuf = totalFileBuf;
        mTotalFileSize =  totalFileBuf.length;
        remindFileSize = mTotalFileSize;

        ROOF_COUNT = mTotalFileSize / WRIT_BIN_PACKET_SIZE;
        REMIND_BUF = mTotalFileSize % WRIT_BIN_PACKET_SIZE;
        offset = 0;
    }

    public byte[] readFileBuf(){
        offset =(mTotalFileSize - remindFileSize);
        byte[] binfileBuf;
         if(ROOF_COUNT  == 0){
             binfileBuf = new byte[REMIND_BUF];
         }else{
             ROOF_COUNT--;
             binfileBuf = new byte[WRIT_BIN_PACKET_SIZE];
         }

        System.arraycopy(mTotalFileBuf, offset, binfileBuf, 0, binfileBuf.length);
        remindFileSize=remindFileSize-binfileBuf.length;
        return binfileBuf;
    }

    public byte getCommandId(byte[] packet) {
        return packet[device.apps.pmpos.packet.ICReaderPacketManager.RdiFormat.STX + device.apps.pmpos.packet.ICReaderPacketManager.RdiFormat.DATA_LENGTH];//STX 1 , DATALEN 2
    }

    public class RecvResult {
        public static final int mResultLen = 2;
        public static final int mMassegeLen =20;
    }

    public byte[] getRecvResult(byte[] data) {
        int resultLen = RecvResult.mResultLen;
        byte[] resultBuf = new byte[resultLen];
        System.arraycopy(data, RdiFormat.STX + RdiFormat.DATA_LENGTH + RdiFormat.COMMANID ,
                resultBuf, 0, resultLen);

        return resultBuf;
    }

    public byte[] getRecvMassege(byte[] data) {
        int messageLen = RecvResult.mMassegeLen;
        byte[] resultMsgBuf = new byte[messageLen];
        System.arraycopy(data, RdiFormat.STX + RdiFormat.DATA_LENGTH + RdiFormat.COMMANID+RecvResult.mResultLen,
                resultMsgBuf, 0, messageLen);

        return resultMsgBuf;
    }

    public byte[] haveFKkeyFlag(byte[] data) {
        int messageLen = RecvResult.mMassegeLen;
        byte[] resultMsgBuf = new byte[messageLen];
        System.arraycopy(data, RdiFormat.STX + RdiFormat.DATA_LENGTH + RdiFormat.COMMANID+RecvResult.mResultLen,
                resultMsgBuf, 0, messageLen);

        return resultMsgBuf;
    }
}
