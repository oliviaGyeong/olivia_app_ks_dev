package device.apps.pmpos.packet;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import device.apps.pmpos.PMPosHelper;
import device.apps.pmpos.Utils;

public class VanHostPacketManager {
    private final String TAG = VanHostPacketManager.class.getSimpleName();

    private static final String nativeLibName = "pkt_VAN";
    private final String SWIPE_TYPE_IC_I = "I";
    private final String SWIPE_TYPE_CASH_K = "K";
    private final String SWIPE_TYPE_MS_S = "S";

    private final String TERMINAL_CANCEL_TYPE_SUCCESS = "0";
    private final String TERMINAL_CANCEL_TYPE_NETWORK_CANCEL = "1";
    private final String TERMINAL_CANCEL_TYPE_IC_CARD_CANCEL = "3";

    private final String ELECT_SIGN_TYPE_USE = "1";
    private final String ELECT_SIGN_TYPE_NOT_USE = "0";

    private Context mContext;

    public VanHostPacketManager(Context context) {
        mContext = context;
    }


    /*
    *   These functions is used in PMPosActivity to VAN.
    * */

    static {
        try {
            System.loadLibrary(nativeLibName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private native int reqApprovalCancelToVAN(byte[] vanPktBuf, byte[] rdPktBuf, byte[] dataPktBuf, int vanPktLen, int rdPktLen, int dataPktLen);

    public byte[] makeApprovalRequestPacket(byte[] rdPktBuf, PMPosHelper.ApprovalValue approvalValue, String fResultCode, String cashReceiptType) {
        byte[] vanPktBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];
        VanApprovalCancelData vacd = new VanApprovalCancelData();

        switch (approvalValue.mPaymentOption) {
            case Utils.PAYMENT_OPTION_IC:
                vacd.setTransType(Utils.TRD_TYPE_IC_CREDIT_APPROVAL.getBytes());
                vacd.setfSwipe(SWIPE_TYPE_IC_I.getBytes());
                vacd.setInst(Utils.RJLZ(approvalValue.mInstalment.getBytes(),2));
                if (Integer.parseInt(approvalValue.mAmount) >= Utils.INSTALLABLE_AMOUNT) {
                    setElectSignData(vacd);
                } else {//no sign
                    vacd.setfElectSign(ELECT_SIGN_TYPE_NOT_USE.getBytes());
                }
                break;
            case Utils.PAYMENT_OPTION_MS:
                vacd.setTransType(Utils.TRD_TYPE_IC_CREDIT_APPROVAL.getBytes());
                vacd.setfSwipe(SWIPE_TYPE_MS_S.getBytes());
                vacd.setInst(Utils.RJLZ(approvalValue.mInstalment.getBytes(),2));
                if (Integer.parseInt(approvalValue.mAmount) >= Utils.INSTALLABLE_AMOUNT) {
                    setElectSignData(vacd);
                } else {//no sign
                    vacd.setfElectSign(ELECT_SIGN_TYPE_NOT_USE.getBytes());
                }
                vacd.setFallbackCode(Utils.getSharedPrefResultCodeValue(mContext).getBytes());
                break;

        }

        setHeaderFields(vacd);

        vacd.setTransAmt(Utils.RJLZ(approvalValue.mAmount.getBytes(), 12));
        vacd.setFeeAmt(Utils.RJLZ(approvalValue.mFee.getBytes(), 9));
        vacd.setSurtax(Utils.RJLZ(approvalValue.mSurTax.getBytes(), 9));
        vacd.setfTmnlCancel(TERMINAL_CANCEL_TYPE_SUCCESS.getBytes());

        byte[] dataPktBuf = vacd.getDataPktBuf();

        int size = reqApprovalCancelToVAN(vanPktBuf, rdPktBuf, dataPktBuf, vanPktBuf.length, rdPktBuf.length, dataPktBuf.length);

        if (size <= 0) {
            return null;
        }

        byte[] realVanPktBuf = new byte[size];
        System.arraycopy(vanPktBuf, 0, realVanPktBuf, 0, size);

        return realVanPktBuf;
    }

    // H3
    public byte[] makeCashApprovalRequestPacket(byte[] rdPktBuf, PMPosHelper.ApprovalValue approvalValue) {
        int rdPktBufLen = 0;
        if(rdPktBuf!= null){
            rdPktBufLen = rdPktBuf.length;
        }
        byte[] vanPktBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];
        VanApprovalCancelData vacd = new VanApprovalCancelData();

        vacd.setTransType(Utils.TRD_TYPE_CASH_APPROVAL.getBytes());
        setHeaderFields(vacd);

        switch (approvalValue.mPaymentOption){
            case Utils.PAYMENT_OPTION_MS:
                vacd.setTransType(Utils.TRD_TYPE_CASH_APPROVAL.getBytes());
                vacd.setfSwipe(SWIPE_TYPE_MS_S.getBytes());
                vacd.setfElectSign(ELECT_SIGN_TYPE_NOT_USE.getBytes());
                vacd.setIsIndividual(approvalValue.mCashReceiptType.getBytes());
                break;

            case Utils.PAYMENT_OPTION_CASH:
                vacd.setMaskedTrack2(Utils.LJFS(approvalValue.mKeyIn.getBytes(), 40));
                vacd.setfSwipe(SWIPE_TYPE_CASH_K.getBytes());
                vacd.setfElectSign(ELECT_SIGN_TYPE_NOT_USE.getBytes());
                vacd.setIsIndividual(approvalValue.mCashReceiptType.getBytes());
                break;
        }

        vacd.setTransAmt(Utils.RJLZ(approvalValue.mAmount.getBytes(), 12));
        vacd.setFeeAmt(Utils.RJLZ(approvalValue.mFee.getBytes(), 9));
        vacd.setSurtax(Utils.RJLZ(approvalValue.mSurTax.getBytes(), 9));
        vacd.setfTmnlCancel(TERMINAL_CANCEL_TYPE_SUCCESS.getBytes());

        byte[] dataPktBuf = vacd.getDataPktBuf();

        int size = reqApprovalCancelToVAN(vanPktBuf, rdPktBuf, dataPktBuf, vanPktBuf.length, rdPktBufLen, dataPktBuf.length);
        if (size <= 0) {
            return null;
        }

        byte[] realVanPktBuf = new byte[size];
        System.arraycopy(vanPktBuf, 0, realVanPktBuf, 0, size);
        return realVanPktBuf;
    }


    private void setElectSignData(VanApprovalCancelData vacd) {
        vacd.setfElectSign(ELECT_SIGN_TYPE_USE.getBytes()); //전자서명사용여부
        vacd.setfES_length(Utils.convertIntegerToByteArray(4, 1086)); //전자서명길이

        File bmpFile = new File(mContext.getFilesDir(), Utils.FILE_NAME_SIGNATURE);
        byte[] signBuf = new byte[1086];

        if (bmpFile.exists()) {
            try {
                FileInputStream fis = new FileInputStream(bmpFile);
                int fileSize = 0;
                while ((fileSize = fis.read(signBuf)) != -1) {
                }
                vacd.setfES_Data(signBuf); //전자서명 data
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
        }
    }

    private void setHeaderFields(VanApprovalCancelData vacd) {
        vacd.setTransDate(Utils.convertCurrentDateToByteArray());

        byte[] destTransSeqNum = new byte[10];
        Arrays.fill(destTransSeqNum, (byte)0x30);
        byte[] srcTransSeqNum = Utils.getSharedPrefTransSeqNumValue(mContext).getBytes();
        System.arraycopy(srcTransSeqNum, 0, destTransSeqNum, 6, srcTransSeqNum.length);

        vacd.setTransSeqNo(destTransSeqNum);
    }


    //F2
    public byte[] makeNormalCancelPacket(byte[] rdPktBuf, PMPosHelper.ApprovalValue approvalValue ) {
        byte[] vanPktBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];
        VanApprovalCancelData vacd = new VanApprovalCancelData();
        vacd.setTransType(approvalValue.mTrdType.getBytes());
        setHeaderFields(vacd);
        vacd.setTransAmt(Utils.RJLZ(approvalValue.mAmount.getBytes(), 12));
        vacd.setApprovalNo(approvalValue.mOrgApprovalNo.getBytes());
        vacd.setApprovalDate(approvalValue.mOrgApprovalDate.getBytes());
        vacd.setfTmnlCancel(TERMINAL_CANCEL_TYPE_SUCCESS.getBytes());

        byte[] dataPktBuf = vacd.getDataPktBuf();

        int size = reqApprovalCancelToVAN(vanPktBuf, rdPktBuf, dataPktBuf, vanPktBuf.length, rdPktBuf.length, dataPktBuf.length);

        if (size <= 0) {
            return null;
        }

        byte[] realVanPktBuf = new byte[size];
        System.arraycopy(vanPktBuf, 0, realVanPktBuf, 0, size);
        return realVanPktBuf;
    }


    //F2
    public byte[] makeIcCardCancelPacket(PMPosHelper.ApprovalValue approvalValue) {
        byte[] vanPktBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];
        VanApprovalCancelData vacd = new VanApprovalCancelData();
        vacd.setTransType(Utils.TRD_TYPE_IC_CREDIT_CANCEL.getBytes());
        setHeaderFields(vacd);
        vacd.setKsn(Utils.hexToByteArray(approvalValue.mKsn));
        vacd.setfSwipe(SWIPE_TYPE_IC_I.getBytes());
        vacd.setApprovalNo(approvalValue.mOrgApprovalNo.getBytes());
        vacd.setApprovalDate(approvalValue.mOrgApprovalDate.getBytes());
        vacd.setfTmnlCancel(TERMINAL_CANCEL_TYPE_IC_CARD_CANCEL.getBytes());

        byte[] dataPktBuf = vacd.getDataPktBuf();

        int size = reqApprovalCancelToVAN(vanPktBuf, null, dataPktBuf, vanPktBuf.length, 0, dataPktBuf.length);

        if (size <= 0) {
            return null;
        }

        byte[] realVanPktBuf = new byte[size];
        System.arraycopy(vanPktBuf, 0, realVanPktBuf, 0, size);
        return realVanPktBuf;
    }

    //F2
    public byte[] makeNetworkCommunicationCancelPacket(PMPosHelper.ApprovalValue approvalValue) {
        byte[] vanPktBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];
        VanApprovalCancelData vacd = new VanApprovalCancelData();
        vacd.setTransType(Utils.TRD_TYPE_IC_CREDIT_CANCEL.getBytes());
        setHeaderFields(vacd);

        String paymentOption = approvalValue.mPaymentOption;
        switch (paymentOption) {
            case Utils.PAYMENT_OPTION_IC:
                vacd.setfSwipe(SWIPE_TYPE_IC_I.getBytes());
                break;
            case Utils.PAYMENT_OPTION_MS:
                vacd.setfSwipe(SWIPE_TYPE_MS_S.getBytes());
                break;
            case Utils.PAYMENT_OPTION_CASH:
                vacd.setfSwipe(SWIPE_TYPE_CASH_K.getBytes());
                break;
        }
        vacd.setfTmnlCancel(TERMINAL_CANCEL_TYPE_NETWORK_CANCEL.getBytes());
        byte[] dataPktBuf = vacd.getDataPktBuf();

        int size = reqApprovalCancelToVAN(vanPktBuf, null, dataPktBuf, vanPktBuf.length, 0, dataPktBuf.length);

        if (size <= 0) {
            return null;
        }

        byte[] realVanPktBuf = new byte[size];
        System.arraycopy(vanPktBuf, 0, realVanPktBuf, 0, size);
        return realVanPktBuf;
    }
}
