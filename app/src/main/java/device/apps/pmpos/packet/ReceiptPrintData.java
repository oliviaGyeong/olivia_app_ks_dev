package device.apps.pmpos.packet;

import android.content.Context;
import android.util.Log;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import device.apps.pmpos.R;
import device.apps.pmpos.Utils;

/**
 * Created by olivia on 17. 11. 10.
 */

@SuppressWarnings("serial")
public  class ReceiptPrintData implements Serializable {
    private final String TAG = ReceiptPrintData.class.getSimpleName();
    Context mContext;
    //JNI +++
    static {
        try {
            System.loadLibrary("pkt_VAN");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private native int respPaymentReceiptPacket(byte[] vanPktBuf, int vanBufLen, byte[] pReceiptData, int DataLen);
    //JNI ---

    public static final int STX = ICReaderPacketManager.RdiFormat.STX;//1;
    public static final int DATA_LENGTH = ICReaderPacketManager.RdiFormat.DATA_LENGTH;//2;
    public static final int COMMANID = ICReaderPacketManager.RdiFormat.COMMANID;//1;
    public static final int ETX = ICReaderPacketManager.RdiFormat.ETX;//1;
    public static final int LRC = ICReaderPacketManager.RdiFormat.LRC;//1;

    public static final int getFormatAllSize = ICReaderPacketManager.RdiFormat.getAllSize;
    public static final int getFormatHeaderSize = ICReaderPacketManager.RdiFormat.getHeaderSize;
    public static final int getFormatFooterSize = ICReaderPacketManager.RdiFormat.getFooterSize;

    public static final byte CMD_STX = ICReaderPacketManager.RdiFormat.CMD_STX;   // Start of Text
    public static final byte CMD_ETX = ICReaderPacketManager.RdiFormat.CMD_ETX;    // End of Text


    public ReceiptPrintData(Context context){
        mContext = context;
    }

    //From DAEMONE +++
    public class FromDaemodLen {
        public static final int trdTypeLen = 2;
        public static final int amountLen = 10;             // 결제금액에서 거래금액(세금, 봉사료를 제외한 금액 ,
        public static final int feeLen = 9;                 // 봉사료,
        public static final int surTaxLen = 9;              // 세금,
        public static final int installmentLen = 2;          // 할부개월
        public static final int keyInLen = 20;              // keyin
        public static final int cashReceiptTypeLen = 2;     // 현금영수증 타입 개인/법인
        public static final int orgApprovalDateLen = 8;     // 원승인일자
        public static final int orgApprovalNoLen = 12;      // 원승인번호

        public static final int getAllDataSize =
                        trdTypeLen+
                        amountLen +
                        feeLen +
                        surTaxLen +
                        installmentLen +
                        keyInLen +
                        cashReceiptTypeLen +
                        orgApprovalDateLen +
                        orgApprovalNoLen;
    }

    public  byte[] mDeamonTrdType = new byte[FromDaemodLen.trdTypeLen];
    public  byte[] mDeamonAmount = new byte[FromDaemodLen.amountLen]; // 결제금액에서 거래금액(세금, 봉사료를 제외한 금액
    public  byte[] mDeamonFee = new byte[FromDaemodLen.feeLen];  // 봉사료
    public  byte[] mDeamonSurTax = new byte[FromDaemodLen.surTaxLen];// 세금
    public  byte[] mDeamonInstallment = new byte[FromDaemodLen.installmentLen];  // 할부개월
    public  byte[] mDeamonKeyIn = new byte[FromDaemodLen.keyInLen];  // KEY IN
    public  byte[] mDeamonCashReceiptType = new byte[FromDaemodLen.cashReceiptTypeLen];  // KEY IN
    public  byte[] mDeamonOrgApprovalDate = new byte[FromDaemodLen.orgApprovalDateLen]; // 원승인일자
    public  byte[] mDeamonOrgApprovalNo = new byte[FromDaemodLen.orgApprovalNoLen]; // 원승인번호


    public void  setDemonData(byte[] daemonData){
       Arrays.fill(mDeamonTrdType,(byte)0x20);
        Arrays.fill(mDeamonAmount,(byte)0x20);
        Arrays.fill(mDeamonFee ,(byte)0x20);
        Arrays.fill(mDeamonSurTax ,(byte)0x20);
        Arrays.fill(mDeamonInstallment ,(byte)0x20);
        Arrays.fill(mDeamonKeyIn ,(byte)0x20);
        Arrays.fill(mDeamonCashReceiptType ,(byte)0x20);
        Arrays.fill(mDeamonOrgApprovalDate ,(byte)0x20);
        Arrays.fill(mDeamonOrgApprovalNo ,(byte)0x20);


        int count = 0;
        count = getFormatHeaderSize;//STX,LEN,COMID

        System.arraycopy(daemonData, count, mDeamonTrdType, 0, mDeamonTrdType.length);
        count +=  mDeamonTrdType.length;
        System.arraycopy(daemonData, count, mDeamonAmount, 0, mDeamonAmount.length);
        count +=  mDeamonAmount.length;
        System.arraycopy(daemonData, count, mDeamonFee, 0, mDeamonFee.length);
        count +=  mDeamonFee.length;
        System.arraycopy(daemonData, count, mDeamonSurTax, 0, mDeamonSurTax.length);
        count +=  mDeamonSurTax.length;
        System.arraycopy(daemonData, count, mDeamonInstallment, 0, mDeamonInstallment.length);
        count +=  mDeamonInstallment.length;
        System.arraycopy(daemonData, count, mDeamonKeyIn, 0, mDeamonKeyIn.length);
        count +=  mDeamonKeyIn.length;
        System.arraycopy(daemonData, count, mDeamonCashReceiptType, 0, mDeamonCashReceiptType.length);
        count +=  mDeamonCashReceiptType.length;
        System.arraycopy(daemonData, count, mDeamonOrgApprovalDate, 0, mDeamonOrgApprovalDate.length);
        count +=  mDeamonOrgApprovalDate.length;
        System.arraycopy(daemonData, count, mDeamonOrgApprovalNo, 0, mDeamonOrgApprovalNo.length);
        count +=  mDeamonOrgApprovalNo.length;

        printFromDeamon();
    }

    public  void printFromDeamon(){
        String resResult ="printFromDeamon\n";
        resResult += ("mTrdType	      : " + new String(mDeamonTrdType) + "\n");
        resResult += ("mAmount	      : " + new String(mDeamonAmount) + "\n");
        resResult += ("mFee	          : " + new String(mDeamonFee) + "\n");
        resResult += ("mSurTax        : " + new String(mDeamonSurTax) + "\n");
        resResult += ("mInstallment    : " + new String(mDeamonInstallment) + "\n");
        resResult += ("mCardno  	  : " + new String(mDeamonKeyIn) + "\n");
        resResult += ("mSwipeType	  : " + new String(mDeamonCashReceiptType) + "\n");
        resResult += ("mOrgApprovalDate: " + new String(mDeamonOrgApprovalDate)  + "\n");
        resResult += ("morgApprovalNo : " + new String(mDeamonOrgApprovalNo) + "\n");

        if(Utils.DEBUG_VALUE) Log.v(TAG,resResult);
    }
    //From DAEMON ---


    //RESPONCE To DAEMON +++
    public static class ToDaemodLen {

        public static final int trdTypeLen = 2;             // 업무구분
        ////////////////////////////////////////////////////////////////////////////////// 요청전문에서 추출
        public static final int amountLen = 10;             // 결제금액에서 거래금액(세금, 봉사료를 제외한 금액 ,
        public static final int feeLen = 9;                 // 봉사료,
        public static final int surTaxLen = 9;              // 세금,
        public static final int totalAmountLen = 10;        // 결제금액(세금, 봉사료 포함),부
        public static final int installmentLen = 2;          // 할부개월
        public static final int cardNoLen = 40;             // masked card number (BIN 6-Byte만 처리할 것),
        public static final int swipeTypeLen = 1;           // Swipe여부,
        public static final int orgApprovalDateLen = 8;     // 원승인일자
        public static final int orgApprovalNoLen = 12;      // 원승인번호
        ////////////////////////////////////////////////////////////////////////////////// 응답전문에서 추출
        public static final int respCodeLen = 4;            // 응답코드 ?
        public static final int respMsgLen = 32;            // 응답메시지 ?
        public static final int approvalDateLen = 14;       // 승인실시
        public static final int approvalNoLen = 12;         // 승인번호
        public static final int acquirerCodeLen = 4;         // 매입사 코드
        public static final int acquirerNameLen = 8;        // 매입사명
        public static final int tranUniqueLen = 10;          // 거래일련번호
        public static final int issuerCodeLen = 4;          // 발급사코드
        public static final int issuerNameLen = 12;          // 발급사명
        public static final int cardType = 1;                //카드구
        //////////////////////////////////////////////////////////////////////////////////IC reder download data
        public static final int catidLen = 10; // 터미널ID
        public static final int businessNoLen = 10; // 사업자 번호분
        public static final int shopNameLen = 40;  // 가맹점명
        public static final int shopOwnerNameLen = 20; // 대표자명
        public static final int shopAddressLen = 50; // 가맹점주소
        public static final int shopTelNoLen = 15;  // 가맹점 전화번호


        //RESPONCE To DAEMON +++
        public static final int getAllDataSize =
                trdTypeLen +
                        amountLen +
                        feeLen +
                        surTaxLen +
//                        totalAmountLen +
                        installmentLen +
                        cardNoLen +
                        swipeTypeLen +
                        orgApprovalDateLen +
                        orgApprovalNoLen +

                        respCodeLen +
                        respMsgLen +
                        approvalDateLen +
                        approvalNoLen +
                        acquirerCodeLen +
                        acquirerNameLen +
                        tranUniqueLen +
                        issuerCodeLen +
                        issuerNameLen +
                        cardType +

                        catidLen +
                        businessNoLen +
                        shopNameLen +
                        shopOwnerNameLen +
                        shopAddressLen +
                        shopTelNoLen;
    }



    public static byte[] mAmount = new byte[ToDaemodLen.amountLen]; // 결제금액에서 거래금액(세금, 봉사료를 제외한 금액
    public static byte[] mFee = new byte[ToDaemodLen.feeLen];  // 봉사료
    public static byte[] mSurTax = new byte[ToDaemodLen.surTaxLen];// 세금
    public static byte[] mTotalAmount = new byte[ToDaemodLen.totalAmountLen];  // 결제금액(세금, 봉사료 포함)
//    public static byte[] mCardno = new byte[ToDaemodLen.cardNoLen]; // masked card number (BIN 6-Byte만 처리할 것)
//    public static byte[] mTranstype= new byte[ToDaemodLen.transtypeLen];  // Swipe여부
//    public static byte[] mInstalment = new byte[ToDaemodLen.instalmentLen];  // 할부개월
    public static byte[] mInstallment = new byte[ToDaemodLen.installmentLen];  // 할부개월
    public static byte[] mCardno = new byte[ToDaemodLen.cardNoLen]; // masked card number (BIN 6-Byte만 처리할 것)
    public static byte[] mSwipeType= new byte[ToDaemodLen.swipeTypeLen];  // Swipe여부
    public static byte[] mOrgApprovalDate = new byte[ToDaemodLen.orgApprovalDateLen]; // 원승인일자
    public static byte[] mOrgApprovalNo = new byte[ToDaemodLen.orgApprovalNoLen]; // 원승인번호

    public static byte[] mRespCode = new byte[ToDaemodLen.respCodeLen];         // 응답코드
    public static byte[] mRespMsg =  new byte[ToDaemodLen.respMsgLen];          // 응답메시지
    public static byte[] mApprovalDate= new byte[ToDaemodLen.approvalDateLen];  // 승인실시
    public static byte[] mApprovalNo  = new byte[ToDaemodLen.approvalNoLen];    // 승인번호
    public static byte[] mAcquirerCode  = new byte[ToDaemodLen.acquirerCodeLen];  // 매입사 코드
    public static byte[] mAcquirerName  = new byte[ToDaemodLen.acquirerNameLen];  // 매입사명
    public static byte[] mTranUnique  = new byte[ToDaemodLen.tranUniqueLen];      // 거래일련번호
    public static byte[] mIssuerCode  = new byte[ToDaemodLen.issuerCodeLen];    // 발급사코드
    public static byte[] mIssuerName  = new byte[ToDaemodLen.issuerNameLen];    // 발급사명
    public static byte[] mCardType  = new byte[ToDaemodLen.cardType];           //카드구분

    //IC reder download data
    public static byte[] mCatid  = new byte[ToDaemodLen.catidLen];              // 터미널ID
    public static byte[] mBusinessNo  = new byte[ToDaemodLen.businessNoLen];    // 사업자 번호
    public static byte[] mShopName  = new byte[ToDaemodLen.shopNameLen];        // 가맹점명
    public static byte[] mShopOwnerName  = new byte[ToDaemodLen.shopOwnerNameLen];// 대표자명
    public static byte[] mShopAddress  = new byte[ToDaemodLen.shopAddressLen];  // 가맹점주소
    public static byte[] mShopTelNo  = new byte[ToDaemodLen.shopTelNoLen];      // 가맹점 전화번호

    public static byte[] mTrdType = new byte[FromDaemodLen.trdTypeLen];



    byte[] mRecvVanData;

    //save VanPacket
    public void setRecvVanData(byte[] recvVanData) {
        mRecvVanData = recvVanData;
    }
    public byte[] getRecvVanData() {  //거래완료 VanPacket
       return mRecvVanData;
    }




    public byte[] getReceiptPacket() {
        if (mRecvVanData == null) {
            return null;
        }

        int total_length = 346;
        byte[] dataBuf = new byte[Utils.MAX_VAN_PACKET_SIZE];
        int size = respPaymentReceiptPacket(mRecvVanData, mRecvVanData.length, dataBuf, dataBuf.length);
        byte[] receiptBuf = new byte[size];
        System.arraycopy(dataBuf, 0, receiptBuf, 0, size); //32

        return receiptBuf;
    }

    public void setReceiptData() {
        byte[] receiptBuf = getReceiptPacket();
        if (receiptBuf == null) {
            return ;
        }

        if (Utils.DEBUG_VALUE) Utils.debugTx("receiptBuf ::", receiptBuf, receiptBuf.length);

        int count = 0;
        count = getFormatHeaderSize;
        System.arraycopy(receiptBuf, count, mTrdType, 0, ToDaemodLen.trdTypeLen);
        count += ToDaemodLen.trdTypeLen;

        System.arraycopy(receiptBuf, count, mAmount, 0, ToDaemodLen.amountLen);
        count += ToDaemodLen.amountLen;
        System.arraycopy(receiptBuf, count, mFee, 0, ToDaemodLen.feeLen);
        count += ToDaemodLen.feeLen;
        System.arraycopy(receiptBuf, count, mSurTax, 0, ToDaemodLen.surTaxLen);
        count += ToDaemodLen.surTaxLen;
//        System.arraycopy(receiptBuf, count, mTotalAmount, 0, ToDaemodLen.totalAmountLen);
//        count += ToDaemodLen.totalAmountLen;

        System.arraycopy(receiptBuf, count, mInstallment, 0, ToDaemodLen.installmentLen);
        count += ToDaemodLen.installmentLen;
        System.arraycopy(receiptBuf, count, mCardno, 0, ToDaemodLen.cardNoLen);
        count += ToDaemodLen.cardNoLen;
        System.arraycopy(receiptBuf, count, mSwipeType, 0, ToDaemodLen.swipeTypeLen);
        count += ToDaemodLen.swipeTypeLen;

        System.arraycopy(receiptBuf, count, mOrgApprovalDate, 0, ToDaemodLen.orgApprovalDateLen);
        count += ToDaemodLen.orgApprovalDateLen;
        System.arraycopy(receiptBuf, count, mOrgApprovalNo, 0, ToDaemodLen.orgApprovalNoLen);
        count += ToDaemodLen.orgApprovalNoLen;

        System.arraycopy(receiptBuf, count, mRespCode, 0, ToDaemodLen.respCodeLen);
        count += ToDaemodLen.respCodeLen;
        System.arraycopy(receiptBuf, count, mRespMsg, 0, ToDaemodLen.respMsgLen);
        count += ToDaemodLen.respMsgLen;
        System.arraycopy(receiptBuf, count, mApprovalDate, 0, ToDaemodLen.approvalDateLen);
        count += ToDaemodLen.approvalDateLen;
        System.arraycopy(receiptBuf, count, mApprovalNo, 0, ToDaemodLen.approvalNoLen);
        count += ToDaemodLen.approvalNoLen;
        System.arraycopy(receiptBuf, count, mAcquirerCode, 0, ToDaemodLen.acquirerCodeLen);
        count += ToDaemodLen.acquirerCodeLen;
        System.arraycopy(receiptBuf, count, mAcquirerName, 0, ToDaemodLen.acquirerNameLen);
        count += ToDaemodLen.acquirerNameLen;
        System.arraycopy(receiptBuf, count, mTranUnique, 0, ToDaemodLen.tranUniqueLen);
        count += ToDaemodLen.tranUniqueLen;
        System.arraycopy(receiptBuf, count, mIssuerCode, 0, ToDaemodLen.issuerCodeLen);
        count += ToDaemodLen.issuerCodeLen;
        System.arraycopy(receiptBuf, count, mIssuerName, 0, ToDaemodLen.issuerNameLen);
        count += ToDaemodLen.issuerNameLen;
        System.arraycopy(receiptBuf, count, mCardType, 0, ToDaemodLen.cardType);
        count += ToDaemodLen.cardType;

        System.arraycopy(receiptBuf, count, mCatid, 0, ToDaemodLen.catidLen);
        count += ToDaemodLen.catidLen;
        System.arraycopy(receiptBuf, count, mBusinessNo, 0, ToDaemodLen.businessNoLen);
        count += ToDaemodLen.businessNoLen;
        System.arraycopy(receiptBuf, count, mShopName, 0, ToDaemodLen.shopNameLen);
        count += ToDaemodLen.shopNameLen;
        System.arraycopy(receiptBuf, count, mShopOwnerName, 0, ToDaemodLen.shopOwnerNameLen);
        count += ToDaemodLen.shopOwnerNameLen;
        System.arraycopy(receiptBuf, count, mShopAddress, 0, ToDaemodLen.shopAddressLen);
        count += ToDaemodLen.shopAddressLen;
        System.arraycopy(receiptBuf, count, mShopTelNo, 0, ToDaemodLen.shopTelNoLen);
        count += ToDaemodLen.shopTelNoLen;

    }

    public String getReceiptDataStr() {
        setReceiptData();
        if(Utils.DEBUG_VALUE){
            print();
        }
        String approvalTitle ="";
        String trdtypeStr = new String(mTrdType);
        int amountInt = Integer.parseInt(new String(mAmount));
        String amountStr = ""+(-amountInt);
        if(trdtypeStr.equals(Utils.TRD_TYPE_IC_CREDIT_APPROVAL) || trdtypeStr.equals(Utils.TRD_TYPE_CASH_APPROVAL)){
            approvalTitle = mContext.getResources().getString(R.string.print_payment_approval_title);
            amountStr = ""+amountInt;
        }else{
            approvalTitle = mContext.getResources().getString(R.string.print_payment_cancel_title);
            amountStr = ""+(-amountInt);
        }

        String mInstalmentStr = new String(mInstallment);
        if (mInstalmentStr.equals(Utils.DEFAULT_VALUE_OF_MONTHLY_INSTALLMENT_PLAN)) {
            mInstalmentStr = mContext.getResources().getString(R.string.print_instalment_pay_in_full);
        }
        String mCardnoStr = new String(mCardno).substring(0, 16);

        String mShopTelNumber = Utils.convertByteArrayToKoreanEncodeStr(mShopTelNo);
        mShopTelNumber = Utils.phoneFomatToString(mShopTelNumber);

        String dateTime = setDateTimeFormat(DATE_FORMAT_TYPE_1, mApprovalDate);

        String cardType = new String(mCardType);
        String cardTypeStr = "";
        String balance = null;
        if(cardType.equals("1")){
            cardTypeStr = mContext.getResources().getString(R.string.print_card_type_1);
        }else if(cardType.equals("2")){
            cardTypeStr = mContext.getResources().getString(R.string.print_card_type_2);
        }else if(cardType.equals("3")) {
            cardTypeStr = mContext.getResources().getString(R.string.print_card_type_3);
        } else if(cardType.equals("4")){
            cardTypeStr = mContext.getResources().getString(R.string.print_card_type_4);
            balance =new String(mRespMsg);
        }

        String resResult = "";
        resResult += mContext.getResources().getString(R.string.print_start_star) + approvalTitle +mContext.getResources().getString(R.string.print_start_star)+"\n\n";
        resResult += (mContext.getResources().getString(R.string.print_shop_name) + Utils.convertByteArrayToKoreanEncodeStr(mShopName) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_business_no) + new String(mBusinessNo) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_shop_owoner_name) + Utils.convertByteArrayToKoreanEncodeStr(mShopOwnerName) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_shop_tel_number) + mShopTelNumber + "\n");
        resResult += (mContext.getResources().getString(R.string.print_shop_address) + Utils.convertByteArrayToKoreanEncodeStr(mShopAddress) + "\n");

        resResult += (mContext.getResources().getString(R.string.print_date_time) + dateTime + "\n");
        resResult += (mContext.getResources().getString(R.string.print_type_1) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_acquier_code) + new String(mAcquirerCode) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_acquier_name) + Utils.convertByteArrayToKoreanEncodeStr(mAcquirerName) + "\n");

        resResult += (mContext.getResources().getString(R.string.print_card_type) + cardTypeStr + "\n");
        if(balance !=null){resResult += (mContext.getResources().getString(R.string.print_balance) + balance + "\n");}
        resResult += (mContext.getResources().getString(R.string.print_instalment) + mInstalmentStr + "\n");
        resResult += (mContext.getResources().getString(R.string.print_card_no) + mCardnoStr + "\n");
        resResult += (mContext.getResources().getString(R.string.print_approval_no)+ new String(mApprovalNo) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_type_2) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_amount) + amountStr + "\n");
        resResult += (mContext.getResources().getString(R.string.print_fee) + Integer.parseInt(new String(mFee))+ "\n");
        resResult += (mContext.getResources().getString(R.string.print_surTax) + Integer.parseInt(new String(mSurTax)) + "\n");
//        resResult += ("TotalAmount   : " + new String(mTotalAmount) + "\n");
        resResult += (mContext.getResources().getString(R.string.print_type_2)+"\n");

        return  resResult;
    }

    int DATE_FORMAT_TYPE_1 = 1;
    public String setDateTimeFormat(int formatType, byte[] sourceData)
    {
        String  strDateTime = new String(sourceData);

        String  strYear = strDateTime.substring(0, 4);
        String  strMonth = strDateTime.substring(4, 6);
        String  strDay = strDateTime.substring(6, 8);
        String  strHour = strDateTime.substring(8, 10);
        String  strMinute = strDateTime.substring(10, 12);
        String  strSecond = strDateTime.substring(12, 14);

        String resResult = "";

        if(formatType == DATE_FORMAT_TYPE_1)
            resResult += (strYear + "/" + strMonth + "/" + strDay + " " + strHour + ":" + strMinute + ":" + strSecond);
        else
            resResult += (strYear + "/" + strMonth + "/" + strDay + "::" + strHour + ":" + strMinute + ":" + strSecond);

        return resResult;
    }

    public void print(){
        String resResult ="";
        resResult += ("mTrdType	      : " +  new String(mTrdType) + "\n");
        resResult += ("mAmount	      : " + new String(mAmount) + "\n");
        resResult += ("mFee	          : " + new String(mFee) + "\n");
        resResult += ("mSurTax        : " + new String(mSurTax) + "\n");
        resResult += ("mInstallment    : " + new String(mInstallment) + "\n");
        resResult += ("mCardno  	  : " + new String(mCardno) + "\n");
        resResult += ("mSwipeType	  : " + new String(mSwipeType) + "\n");
        resResult += ("mOrgApprovalDate: " + new String(mOrgApprovalDate)  + "\n");
        resResult += ("mOrgApprovalNo : " + new String(mOrgApprovalNo) + "\n");
        resResult += ("mRespCode      : " + new String(mRespCode) + "\n");
        resResult += ("mRespMsg    	  : " + new String(mRespMsg) + "\n");
        resResult += ("mApprovalDate  : " + new String(mApprovalDate) + "\n");
        resResult += ("mApprovalNo	  : " + new String(mApprovalNo) + "\n");
        resResult += ("mAcquirerCode   : " + new String(mAcquirerCode) + "\n");
        resResult += ("mAcquirerName   : " + new String(mAcquirerName) + "\n");
        resResult += ("mTranUnique     : " + new String(mTranUnique) + "\n");
        resResult += ("mIssuerCode    : " + new String(mIssuerCode) + "\n");
        resResult += ("mIssuerName    : " + new String(mIssuerName) + "\n");
        resResult += ("mCardType      : " + new String(mCardType) + "\n");
        resResult += ("mCatid         : " + new String(mCatid) + "\n");
        resResult += ("mShopName      : " + new String(mShopName) + "\n");
        resResult += ("mShopOwnerName : " + new String(mShopOwnerName) + "\n");
        resResult += ("mShopAddress   : " + new String(mShopAddress) + "\n");
        resResult += ("mShopTelNo     : " + new String(mShopTelNo) + "\n");
        resResult += ("mBusinessNo    : " + new String(mBusinessNo) + "\n");

        if(Utils.DEBUG_VALUE) Log.v(TAG,resResult);
    }

    public void setFailPacketToDeamon(byte[] resultCode, byte[] resultMessage){
        mRespCode = resultCode;
        mRespMsg = resultMessage;
        Arrays.fill(mAmount, (byte)0x20);
        Arrays.fill(mFee, (byte)0x20);
        Arrays.fill(mSurTax, (byte)0x20);
        Arrays.fill(mInstallment, (byte)0x20);
        Arrays.fill(mCardno, (byte)0x20);
        Arrays.fill(mSwipeType, (byte)0x20);
        Arrays.fill(mOrgApprovalDate, (byte)0x20);
        Arrays.fill(mOrgApprovalNo, (byte)0x20);
//        Arrays.fill(mRespCode, (byte)0x20); //
//        Arrays.fill(mRespMsg, (byte)0x20); //
        Arrays.fill(mApprovalDate, (byte)0x20);
        Arrays.fill(mApprovalNo, (byte)0x20);
        Arrays.fill(mAcquirerCode, (byte)0x20);
        Arrays.fill(mAcquirerName, (byte)0x20);
        Arrays.fill(mTranUnique, (byte)0x20);
        Arrays.fill(mIssuerCode, (byte)0x20);
        Arrays.fill(mIssuerName, (byte)0x20);
        Arrays.fill(mCardType, (byte)0x20);
        Arrays.fill(mCatid, (byte)0x20);
        Arrays.fill(mBusinessNo, (byte)0x20);
        Arrays.fill(mShopName, (byte)0x20);
        Arrays.fill(mShopOwnerName, (byte)0x20);
        Arrays.fill(mShopAddress, (byte)0x20);
        Arrays.fill(mShopTelNo, (byte)0x20);
        Arrays.fill(mTrdType, (byte)0x20);
        return ;
    }



    public String getDaemonCallbackStr(){
        setReceiptData();
        if(Utils.DEBUG_VALUE){
            print();
        }
        String sendStr = Utils.SCHEME + "://" + Utils.HOST + "?";
        try {
            sendStr += Utils.key_trdtype + "=" + new String(mTrdType);
            sendStr += "&";
            sendStr += Utils.key_amount + "=" + new String(mAmount);
            sendStr += "&";
            sendStr += Utils.key_fee + "=" + new String(mFee);
            sendStr += "&";
            sendStr += Utils.key_surtax + "=" + new String(mSurTax);
            sendStr += "&";
            sendStr += Utils.key_installment + "=" + new String(mInstallment);
            sendStr += "&";
            sendStr += Utils.key_card_no + "=" + new String(mCardno);
            sendStr += "&";
            sendStr += Utils.key_swipe_type + "=" + new String(mSwipeType);
            sendStr += "&";
            sendStr += Utils.key_org_approval_date + "=" + new String(mOrgApprovalDate);
            sendStr += "&";
            sendStr += Utils.key_org_approval_no + "=" + new String(mOrgApprovalNo);
            sendStr += "&";
            sendStr += Utils.key_resp_code + "=" + new String(mRespCode);
            sendStr += "&";
            sendStr += Utils.key_resp_msg + "=" + new String(mRespMsg);
            sendStr += "&";
            sendStr += Utils.key_approval_date + "=" + new String(mApprovalDate);
            sendStr += "&";
            sendStr += Utils.key_approval_no + "=" + new String(mApprovalNo);
            sendStr += "&";
            sendStr += Utils.key_acquirer_code + "=" + new String(mAcquirerCode);
            sendStr += "&";
            sendStr += Utils.key_acquirer_name + "=" + new String(mAcquirerName, "EUC-KR");
            sendStr += "&";
            sendStr += Utils.key_tran_unique + "=" + new String(mTranUnique);
            sendStr += "&";
            sendStr += Utils.key_issuer_code + "=" + new String(mIssuerCode);
            sendStr += "&";
            sendStr += Utils.key_issuer_name + "=" + new String(mIssuerName, "EUC-KR");
            sendStr += "&";
            sendStr += Utils.key_card_type + "=" + new String(mCardType);
            sendStr += "&";
            sendStr += Utils.key_catid + "=" + new String(mCatid);
            sendStr += "&";
            sendStr += Utils.key_business_no + "=" + new String(mBusinessNo);
            sendStr += "&";
            sendStr += Utils.key_shop_name + "=" + new String(mShopName, "EUC-KR");
            sendStr += "&";
            sendStr += Utils.key_shop_owner_name + "=" + new String(mShopOwnerName, "EUC-KR");
            sendStr += "&";
            sendStr += Utils.key_shop_address + "=" + new String(mShopAddress, "EUC-KR");
            sendStr += "&";
            sendStr += Utils.key_shop_tel_no + "=" + new String(mShopTelNo);
        } catch (UnsupportedEncodingException e) {
            sendStr = null;
        }
        return sendStr;
    }
}