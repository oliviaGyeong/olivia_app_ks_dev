package device.apps.pmpos.packet;

import java.io.Serializable;


/**
 * Created by olivia on 17. 11. 10.
 */

@SuppressWarnings("serial")
public  class RecvDeviceInfoPacket implements Serializable {
    private final String TAG = RecvDeviceInfoPacket.class.getSimpleName();

    public static final int CertificationNumLen = 32;
    public static final int SerialNumLen = 10;
    public static final int VersionLen = 5;
    public static final int IsSecurityKeyLen = 2;
    public static final int FillerLen = 40;

    public byte[] mCertificationNum = new byte[CertificationNumLen];
    public byte[] mSerialNum = new byte[SerialNumLen];
    public byte[] mVersion = new byte[VersionLen];
    public byte[] mIsSecurityKey = new byte[IsSecurityKeyLen];
    public byte[] mFiller = new byte[FillerLen];

    public RecvDeviceInfoPacket(){}
    public void setRecvDeviceInfo(byte[] recvRdtPk) {

        int count = ICReaderPacketManager.RdiFormat.STX + ICReaderPacketManager.RdiFormat.DATA_LENGTH + ICReaderPacketManager.RdiFormat.COMMANID;

        System.arraycopy(recvRdtPk, count, mCertificationNum, 0, CertificationNumLen); //32
        count += CertificationNumLen;
        System.arraycopy(recvRdtPk, count, mSerialNum, 0, SerialNumLen); //10
        count += SerialNumLen;
        System.arraycopy(recvRdtPk, count, mVersion, 0, VersionLen); //5
        count += VersionLen;
        System.arraycopy(recvRdtPk, count, mIsSecurityKey, 0, IsSecurityKeyLen); //2
        count += IsSecurityKeyLen;
        System.arraycopy(recvRdtPk, count, mFiller, 0, FillerLen); //40
        count += FillerLen;

    }

    public byte[] setCertificationNum(byte[] data) {
        this.mCertificationNum = data;
        return data;
    }

    public byte[] setSerialNum(byte[] data) {
        this.mSerialNum = data;
        return data;
    }

    public byte[] setVersion(byte[] data) {
        this.mVersion = data;
        return data;
    }

    public byte[] setIsSecurityKey(byte[] data) {
        this.mIsSecurityKey = data;
        return data;
    }

    public byte[] setFiller(byte[] data) {
        this.mFiller = data;
        return data;
    }

    public byte[] getCertificationNum() {
        return mCertificationNum;
    }

    public byte[] getSerialNum() {
        return mSerialNum;
    }

    public byte[] getVersion() {
        return mVersion;
    }

    public byte[] getIsSecurityKey() {
        return mIsSecurityKey;
    }

    public byte[] getFiller() {
        return mFiller;
    }
}