package device.apps.pmpos.packet;

import android.content.Context;
import android.util.Log;
import device.apps.pmpos.Utils;


/**
 * Created by olivia on 17. 10. 23.
 */

public class AdminVanPacketManager {
    private final String TAG = "AdminVanPacketManager";
    private Context mContext = null;


    public AdminVanPacketManager(Context context) {
        mContext = context;
    }

    static {
        try {
            System.loadLibrary("pkt_VAN");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private native int futureKeyUpdateRequest(byte[] vanPktBuf, byte[] rdPktBuf, byte[] transData, int vanBufLen, int rdBufLen);
    private native int mutualAuthenticationRequest(byte[] vanPktBuf, byte[] rdPktBuf, byte[] transData, int vanBufLen, int rdBufLen);
    private native int deviceDownloadRequest(byte[] vanPktBuf, byte[] rdPktBuf, int vanBufLen, int rdBufLen);
    private native int setTerminalDownData(byte[] recvVanPktBuf,int recvVanPktBufLen);
    private native int getTerminalDownData(byte[] recvVanPktBuf,int recvVanPktBufLen);

    private String PREF_KEY_AUTHENNUM = Utils.PREF_KEY_AUTHENNUM;
    private String PREF_KEY_FUTUREFW_FLAG = Utils.PREF_KEY_FUTUREFW_FLAG;
    private String PREF_KEY_DOWNLOAD_FLAG = Utils.PREF_KEY_DOWNLOAD_FLAG;
    private String PREF_KEY_FLAG_STR_TRUE =  Utils.PREF_KEY_FLAG_STR_TRUE;
    private String PREF_KEY_FLAG_STR_FALSE = Utils.PREF_KEY_FLAG_STR_FALSE;


    public byte[] makeVanPaket(byte[] recvRdPktBuf, int recvRdPktBufLen, String trdType) {
        byte[] packet = new byte[Utils.MAX_VAN_PACKET_SIZE];
        byte[] vanPktBuf = null;
        int size = 0;
        if (trdType == null) {
            return null;
        }
        if (trdType.equals(Utils.TRD_TYPE_FUTURE_KEY_UPDATE)) {
            byte[] transDate = Utils.convertCurrentDateToByteArray();
            size = futureKeyUpdateRequest(packet, recvRdPktBuf, transDate, packet.length, recvRdPktBufLen);
            vanPktBuf = new byte[size];
            System.arraycopy(packet, 0, vanPktBuf, 0, size);

        } else if (trdType.equals(Utils.TRD_TYPE_MUTUAL_AUTHENTICATION)) {
            byte[] transDate = Utils.convertCurrentDateToByteArray();
            size = mutualAuthenticationRequest(packet, recvRdPktBuf,transDate, packet.length, recvRdPktBufLen);
            vanPktBuf = new byte[size];
            System.arraycopy(packet, 0, vanPktBuf, 0, size);
        } else if (trdType.equals(Utils.TRD_TYPE_DEVICE_DOWNLOAD)) {
            size = deviceDownloadRequest(packet, recvRdPktBuf, packet.length, recvRdPktBufLen);
            vanPktBuf = new byte[size];
            System.arraycopy(packet, 0, vanPktBuf, 0, size);
        }
        return vanPktBuf;
    }



    public void setDeviceInfo(byte[] deviceInfoData) { //len 595
        String fullInfoData = Utils.byteArrayToHex(deviceInfoData);

        String encodeFulldata= Utils.encrypt(mContext,fullInfoData);
        Utils.clearFile(mContext, Utils.FileName_DeviceDownloadInfoFile);
        Utils.writeFile(mContext, Utils.FileName_DeviceDownloadInfoFile, encodeFulldata);

        initDeviceinfo();
        Utils.setSharedPreference(mContext, PREF_KEY_DOWNLOAD_FLAG, PREF_KEY_FLAG_STR_TRUE);
    }

    public void initDeviceinfo(){
         String data = Utils.readFile(mContext, Utils.FileName_DeviceDownloadInfoFile);
        String decodeFulldata = Utils.decrypt(mContext,data);
        byte[] deviceInfoData = Utils.hexToByteArray(decodeFulldata);
        String authennumStr = Utils.getSharedPreference(mContext,PREF_KEY_AUTHENNUM);
        byte[] authenNum = Utils.hexToByteArray(authennumStr);

        byte[] nativeInfoData = new byte[deviceInfoData.length+authenNum.length];
        int count = 0;
        System.arraycopy(deviceInfoData, 0, nativeInfoData, count, deviceInfoData.length);
        count += deviceInfoData.length;
        System.arraycopy(authenNum, 0, nativeInfoData, count, authenNum.length);
        count +=authenNum.length;

        int setCfileSize = setTerminalDownData (nativeInfoData, nativeInfoData.length);

        byte[] terminalDownData = new byte[nativeInfoData.length];
        int getCfileSize = getTerminalDownData(terminalDownData, terminalDownData.length);

    }
}
