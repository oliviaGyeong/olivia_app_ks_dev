package device.apps.pmpos;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class Utils {

    public static final boolean DEBUG_INFO = false;
    public static final boolean DEBUG_VALUE = false;
    public static final boolean DEBUG_TEST = false;

    public static final boolean DEBUG_VAN = false;       //VAN Log
    public static final boolean DEBUG_ICREADER = false;  //RDI Log
    public static final boolean DEBUG_INIT = false;

    public static final String DEFAULT_LOGIN_PWD = "0000";

    public static final String PMPOS_DIRECTORY = "pm-pos";

    public static final int FRAG_NUM_MAIN = 100;
    public static final int FRAG_NUM_CANCEL = 101;
    public static final int FRAG_NUM_IC_READ_WAIT = 102;
    public static final int FRAG_NUM_IC_READ_FAIL = 103;
    public static final int FRAG_NUM_MS_SWIPE_WAIT = 104;
//    public static final int FRAG_NUM_SIGNATURE = 105;
    public static final int FRAG_NUM_PAY_PROGRESS = 106;
    public static final int FRAG_NUM_DEAL_COMPLETE = 107;
	public static final int FRAG_NUM_LOGIN = 108;
    public static final int FRAG_NUM_RECEIPT_PRINT = 109;

    public static final int MAX_VAN_PACKET_SIZE = 2560;
    public static final int MAX_ICREADER_PACKET_SIZE = 756;
    public static final int MAX_DEAMON_PACKET_SIZE = 80;

//    public static final int MSG_TRANSFER_DATA_TO_SERVICE_FROM_VANHOST = 0;
    public static final int MSG_TRANSFER_DATA_TO_SERVICE_FROM_DEAMON = 1;
    public static final int DEAMON_FLAG = 1;


    public static final String KEY_EXTRA_DAILOG_CALLBACK= "key_dialog_create_callback";
    public static final String KEY_EXTRA_DAILOG_NUM = "key_dialog_num";
    public static final String KEY_EXTRA_DAILOG_TITLE = "key_dialog_title";
    public static final String KEY_EXTRA_DAILOG_MESSAGE = "key_dialog_message";
    public static final String KEY_EXTRA_DAILOG_BTN_CLICK_LISTENER = "key_dialog_btn_listner";

    public static final int  popup_Signature = 20;   //signaturePopup ;
    public static final int  popup_IcReadFail = 21;  //icReadFailpopup
    public static final int  popup_Simple = 23;
    public static final int  popup_DisplayNoticeIC=24;
    public static final int  popup_DisplayNoticeMS= 25;
    public static final int  popup_DisplayNoticeNOTPayment= 26;

    public static final int popup_create = 11;
    public static final int popup_cancel = 99;
    public static final int popup_Signature_cancel_btn = 1;
    public static final int popup_Signature_complete_btn = 2;
    public static final int popup_IcReadFail_finish_btn = 3;
    public static final int popup_IcReadFail_msr_btn = 4;
    public static final int popup_Simple_ok_btn = 5;
    public static final int  popup_DisplayNoticeIC_ok_btn=6;
    public static final int  popup_DisplayNoticeMS_ok_btn= 7;
    public static final int  popup_DisplayNoticeNOTPaymen_ok_btn= 8;


    public static final int MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK = 10;
    public static final int MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_OK_F1 = 14;
    public static final int MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_FAIL = 11;
    public static final int MSG_DATA_TO_SERVICE_FROM_VANHOST_DATA_TIMEOUT = 12;
    public static final int MSG_VANHOST_CONNECTION_ERROR = 13;
    public static final byte OBG_NETWORK_CANCEL = (byte)0x01;

    //add ICReader readThread handle MSG
    public static final int MSG_RECEIVE_IC_READER_DATA_OK = 51;
    public static final int MSG_RECEIVE_IC_READER_DATA_FAIL = 52;
    public static final int MSG_RECEIVE_IC_READER_DATA_TIMEOUT = 53;
    //add ICReader Timeout
    public static int TIMEOUT_3MIN = 30000;
    public static int TIMEOUT_3SEC = 5000;
    public static int TIMEOUT_1SEC = 1000;

    public static int TIME_BUF_LEN  = 14;

    public static final String KEY_EXTRA_VANHOST_MSG = "KEY_EXTRA_VANHOST_MSG";
    public static final String KEY_EXTRA_VANHOST_DATA = "KEY_EXTRA_VANHOST_DATA";

    public static final String KEY_EXTRA_ERROR_MSG = "KEY_EXTRA_ERROR_MSG";
    public static final String KEY_EXTRA_RESULT_MSG = "KEY_EXTRA_RESULT_MSG";
    public static final String KEY_EXTRA_RECEIPT_DATA = "KEY_EXTRA_RECEIPT_DATA";
    public static final String KEY_EXTRA_IC_READER_PACKET_BUFFER = "KEY_EXTRA_IC_READER_PACKET_BUFFER";
    public static final String KEY_EXTRA_VAN_PACKET_BUFFER = "KEY_EXTRA_VAN_PACKET_BUFFER";
    public static final String KEY_EXTRA_VANHOST_NETWORK_CANCEL = "KEY_EXTRA_VANHOST_NETWORK_CANCEL";

    public static final String ACTION_RECEIVE_DATA_FROM_VANHOST = "device.apps.pmpos.recvdata.van";
    public static final String ACTION_RECEIVE_DATA_FROM_VANHOST_TO_ADMIN = "device.apps.pmpos.recvdata.van.admin";

    public static final String PREF_KEY_LOGIN_PWD = "login_pwd";
    public static final String PREF_KEY_IC_RESULT_CODE = "ic_result_code";
    public static final String PREF_KEY_VAN_TRANS_SEQ_NUM = "van_trans_seq_num";
    //Download  sharedPreference Key
    public static final String PREF_KEY_FUTUREFW_FLAG = "key_futurefw_flag";   //futurefw  //return true,false
    public static final String PREF_KEY_DOWNLOAD_FLAG = "key_download_flag";   //download  //return true,false
    public static String PREF_KEY_FLAG_STR_TRUE = "true";
    public static String PREF_KEY_FLAG_STR_FALSE = "false";
    public static final String PREF_KEY_IC_COMD_ID = "ic_comd_id";             //Admin KEY_IC_COMMAND_ID
    public static final String PREF_KEY_AUTHENNUM = "key_authenNum";
    public static final String PREF_KEY_SIRIALNUM = "key_sirialNum";

    public static final String PREF_KEY_VAN_IP = "key_van_ip";
    public static final String PREF_KEY_VAN_PORT = "key_van_port";


    public static final String FILE_NAME_SIGNATURE = "pmpos_sign_file.bmp";

    public static final String TRD_TYPE_IC_CREDIT_APPROVAL = "F1";     // IC 신용(승인)
    public static final String TRD_TYPE_IC_CREDIT_CANCEL = "F2";       // IC 신용(취소)
    public static final String TRD_TYPE_CASH_APPROVAL = "H3";          // 현금 신용(승인)
    public static final String TRD_TYPE_CASH_CANCEL = "H4";            // 현금 신용(취소)
    public static final String TRD_TYPE_NETWORK_CANCEL = "FC";         // 망 취소

    public static final String TRD_TYPE_FUTURE_KEY_UPDATE = "F9";      // 보안키업데이트
    public static final String TRD_TYPE_MUTUAL_AUTHENTICATION = "T2";  // 상호인증
    public static final String TRD_TYPE_DEVICE_DOWNLOAD = "T1";        // 단말기 다운로드

    public static final String PAYMENT_OPTION_IC = "01";
    public static final String PAYMENT_OPTION_CASH = "06";
    public static final String PAYMENT_OPTION_MS = "07";

    public static final String CASH_RECEIPT_TYPE_INDIVIDUAL = "1";
    public static final String CASH_RECEIPT_TYPE_CORPORATION = "2";

    public static final byte CMD_ID_REQ_IC_PAYMENT_FROM_DEAMON = (byte)0xB1;

    public static final byte CMD_ID_REQ_INIT_SYSTEM_DATA_TIME = (byte)0xF1;
    public static final byte CMD_ID_REQ_IC_PREAMBLE = (byte)0x10;
    public static final byte CMD_ID_REQ_IC_CANCEL_PAYMENT = (byte)0xF2;
    public static final byte CMD_ID_REQ_IC_TRANSACTION_RESULT = (byte)0x20;

    public static final byte CMD_ID_REQ_IC_DEVICE_INFO = (byte)0xF3;
    public static final byte CMD_ID_REQ_KEYIN_ENCRYPTION = (byte)0x28;

    public static final byte CMD_ID_REQ_CREATE_FUTURE_KEY = (byte)0x30;
    public static final byte CMD_ID_REQ_UPDATE_FUTURE_KEY = (byte)0x40;
    public static final byte CMD_ID_REQ_SELF_PROTECTION = (byte)0x50;
    public static final byte CMD_ID_REQ_MUTUAL_AUTHENTICATION = (byte)0x52;
    public static final byte CMD_ID_REQ_MUTUAL_AUTH_INFO_RESULT = (byte)0x54;
    public static final byte CMD_ID_REQ_DEVICE_FACTORY_RESET = (byte)0x62;
    public static final byte CMD_ID_REQ_DEVICE_INFO = (byte)0x58;
    public static final byte CMD_ID_REQ_UPDATE_FIRMWERE = (byte)0x56;

    public static final byte CMD_ID_REQ_TEST_INSERT_CARD = (byte)0x22;
    public static final byte CMD_ID_REQ_TEST_REMOVE_CARD = (byte)0x24;
    public static final byte CMD_ID_REQ_TEST_CHECK_CARD_STATUS = (byte)0x26;

    public static final byte CMD_ID_RECV_IC_PREAMBLE = (byte)0x11;
    public static final byte CMD_ID_RECV_IC_TRANSACTION_RESULT = (byte)0x21;
    //public static final byte CMD_ID_RECV_KEYIN_ENCRYPTION = (byte)0x11;

    public static final byte CMD_ID_RECV_CREATE_FUTURE_KEY = (byte)0x41;
    public static final byte CMD_ID_RECV_SELF_PROTECTION = (byte)0x51;
    public static final byte CMD_ID_RECV_MUTUAL_AUTHENTICATION = (byte)0x53;
    public static final byte CMD_ID_RECV_MUTUAL_AUTH_INFO_RESULT = (byte)0x55;
    public static final byte CMD_ID_RECV_DEVICE_FACTORY_RESET = (byte)0x63;
    public static final byte CMD_ID_RECV_DEVICE_INFO = (byte)0x59;
    public static final byte CMD_ID_RECV_DEVICE_FIRMWERE_UPDATE = (byte)0x57;
    public static final byte CMD_ID_RECV_TEST_CHECK_CARD_STATUS = (byte)0x19;

    public static final byte CMD_ID_RECV_ACK = (byte)0x06;
    public static final byte CMD_ID_RECV_NAK = (byte)0x15;
    public static final byte CMD_ID_RECV_ESC = (byte)0x18;
    public static final byte CMD_ID_RECV_EOT = (byte)0x04;

    public static final byte[] DATA_CTACK = new byte[]{(byte)0x43, (byte)0x54,(byte)0x41,(byte)0x43,(byte)0x4B,(byte)0x0d};

    public static final int REQ_DATA_LENGTH_IC_PREAMBLE = 87;
    public static final int REQ_DATA_LENGTH_IC_PAYMENT_FROM_DEAMON = 5;

    public static final int REQ_DATA_LENGTH_IC_TRANSACTION_RESULT = 314;
    public static final int REQ_DATA_LENGTH_IC_KEYIN_ENCRYPTION = 40;
    public static final int REQ_DATA_LENGTH_IC_UPDATE_FUTURE_KEY = 62;
    public static final int REQ_DATA_LENGTH_IC_MUTUAL_AUTHENTICATION = 4;
    public static final int REQ_DATA_LENGTH_IC_MUTUAL_AUTH_INFO_RESULT = 766;
    public static final int REQ_DATA_LENGTH_IC_MUTUAL_AUTH_INFO = 762;
    public static final int REQ_DATA_LENGTH_IC_TEST_DEVICE_INFO = 14;
    public static final int REQ_DATA_LENGTH_IC_INITIALIZ = 2;


    public static final int RES_DATA_LENGTH_IC_MUTUAL_AUTHENTICATION = 224;

    public static final int MAX_SP_LOG_LINE = 100;

    public static final int INSTALLABLE_AMOUNT = 50000;
    public static final String DEFAULT_VALUE_OF_MONTHLY_INSTALLMENT_PLAN = "00";

    //Daemon +++
    public static final String ACTION_RECEIVE_DATA_FROM_DAEMON = "pmpos.approval.data.daemon";
    public static final String ACTION_CALLBACK_DATA_TO_DAEMON = "pmpos.approval.callback.data.daemon";
    public static final String SCHEME = "pmpos";
    public static final String HOST = "pointmobile";

    //Daemon ResultCode
    public static final byte[] PAYMENT_COMPLETE = "0000".getBytes();
    public static final byte[] RDI_COMMUNICATION_FAILURE = "0020".getBytes();
    public static final byte[] VAN_COMMUNICATION_FAILURE = "0021".getBytes();
    public static final byte[] PAYMENT_CANCEL = "0022".getBytes();
    //Daemon ResultMessage
    public static final byte[] RDI_FAIL_MAKE_PACKET_BUF = "RDI_FAIL_MAKE_PACKET".getBytes();
    public static final byte[] RDI_FAIL_READ_DATA_BUF = "RDI_FAIL_READ_DATA".getBytes();
    public static final byte[] RDI_FAIL_READ_TIMEOUT_BUF = "RDI_FAIL_READ_TIMEOUT".getBytes();

    public static final byte[] VAN_FAIL_MAKE_PACKET_BUF = "VAN_FAIL_MAKE_PACKET".getBytes();
    public static final byte[] VAN_FAIL_READ_DATA_BUF ="VAN_FAIL_READ_DATA".getBytes();
    public static final byte[] VAN_FAIL_READ_TIMEOUT_BUF= "VAN_FAIL_READ_TIMEOUT".getBytes();
    public static final byte[] VAN_FAIL_SERVER_ERROR_BUF = "VAN_FAIL_SERVER_ERROR".getBytes();

    public static final byte[] PAYMENT_CANCEL_BUF = "PAYMENT_CANCEL".getBytes();
    public static final byte[] DEVICE_ROOTING_BUF = "DEVICE_ROOTING".getBytes();
    public static final byte[] ISNOT_DEVICE_DOWNLOAD_BUF = "ISNOT_DEVICE_DOWNLOAD".getBytes();

    //URI KEY From Daemon
    public static final String trdtype = "trdtype";
    public static final String amount = "amount";
    public static final String fee = "fee";
    public static final String surtax = "surtax";
    public static final String installment = "installment";
    public static final String keyin = "keyin";
    public static final String cash_receipt_type = "cash_receipt_type";
    public static final String org_approval_date = "org_approval_date";
    public static final String org_approval_no = "org_approval_no";

    //URI KEY To Daemon
    public static final String key_trdtype = "trdtype";
    public static final String key_amount = "amount";
    public static final String key_fee = "fee";
    public static final String key_surtax= "surtax";
    public static final String key_installment = "installment";
    public static final String key_card_no = "card_no";
    public static final String key_swipe_type = "swipe_type";
    public static final String key_org_approval_date = "org_approval_date";
    public static final String key_org_approval_no = "org_approval_no";
    public static final String key_resp_code = "response_code";
    public static final String key_resp_msg = "response_msg";
    public static final String key_approval_date = "approval_date";
    public static final String key_approval_no = "approval_no";
    public static final String key_acquirer_code = "acquirer_code";
    public static final String key_acquirer_name = "acquirer_name";
    public static final String key_tran_unique = "tran_unique";
    public static final String key_issuer_code= "issuer_code";
    public static final String key_issuer_name = "issuer_name";
    public static final String key_card_type = "card_type";
    public static final String key_catid = "catid";
    public static final String key_business_no = "business_no";
    public static final String key_shop_name = "shop_name";
    public static final String key_shop_owner_name = "shop_owner_name";
    public static final String key_shop_address = "shop_address";
    public static final String key_shop_tel_no = "shop_tel_no";
    //Daemon ---


    public static void showToastMessage(final Context context, final String tag, final String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private int checkLength(byte HL, byte LL) {
        return (int) ((HL << 8) | LL) + 4 ;
    }

    public static String checkTrdType(byte[] vanPktBuf) {
        byte[] trdTypeBytes = new byte[2];
        System.arraycopy(vanPktBuf, 4, trdTypeBytes, 0, 2);
        String trdType = new String(trdTypeBytes);

        return trdType;
    }

    // Convert function - start
    public static String convertByteArrayToAsciiString(byte[] bytes) {
        String hexString = new BigInteger(bytes).toString(16);
        int hexNum = Integer.parseInt(hexString, 16);
        return Integer.toString(hexNum);
    }

    public static String convertByteArrayToString(byte[] srcBuf, int startIndex, int length) {
        byte[] destbuf = new byte[length];
        System.arraycopy(srcBuf, startIndex, destbuf, 0, destbuf.length);
        String destStr = new String(destbuf);
        return destStr;
    }

    public static byte[] convertIntegerToByteArray(int capacity, final int num) {
        ByteBuffer byteBuf = ByteBuffer.allocate(capacity).putInt(num);
        return byteBuf.array();
    }
    public static String convertByteArrayToKoreanEncodeStr(byte[] bytes) {
        String str = "";
        try {
            str = new String(bytes, "EUC-KR");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }
    // Convert function - end

    public static byte[] convertCurrentDateToByteArray() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        String date = sdf.format(new Date(System.currentTimeMillis()));

        return date.getBytes();
    }

    public static String convertCurrentDateToString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yy/MM/dd HH:mm");
        String date = sdf.format(new Date(System.currentTimeMillis()));
        return date;
    }

    public static byte[] RJLZ(byte[] src, int capacity) {
        byte[] dest = new byte[capacity];
        Arrays.fill(dest, (byte)0x30);
        int destLength = capacity-1;

        for (int i=src.length-1; i>=0; i--) {
            dest[destLength--] = src[i];
        }

        return dest;
    }


    public static byte[] LJFS(byte[] src, int capacity) {
        byte[] dest = new byte[capacity];
        Arrays.fill(dest, (byte)0x20);

        for (int i=0; i<src.length; i++) {
            dest[i] = src[i];
        }
        return dest;
    }

    // SharedPreference Login password - start
    public static SharedPreferences getSharedPrefLoginPwd(Context context) {
        return context.getSharedPreferences(PREF_KEY_LOGIN_PWD, Context.MODE_PRIVATE);
    }
    public static String getSharedPrefLoginPwdValue(Context context) {
        return context.getSharedPreferences(PREF_KEY_LOGIN_PWD, Context.MODE_PRIVATE).getString(PREF_KEY_LOGIN_PWD, DEFAULT_LOGIN_PWD);
    }
    public static void setSharedPrefLoginPwdValue(Context context, String password) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_KEY_LOGIN_PWD, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_KEY_LOGIN_PWD, password);
        editor.commit();
    }
    // SharedPreference Login password - stop


    // SharedPreference result code of IC response preamble - start
    public static SharedPreferences getSharedPrefResultCode(Context context) {
        return context.getSharedPreferences(PREF_KEY_IC_RESULT_CODE, Context.MODE_PRIVATE);
    }
    public static String getSharedPrefResultCodeValue(Context context) {
        return context.getSharedPreferences(PREF_KEY_IC_RESULT_CODE, Context.MODE_PRIVATE).getString(PREF_KEY_IC_RESULT_CODE, "");
    }
    public static void setSharedPrefResultCodeValue(Context context, String resultCode) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_KEY_IC_RESULT_CODE, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_KEY_IC_RESULT_CODE, resultCode);
        editor.commit();
    }
    // SharedPreference result code of IC response preamble - stop

    // SharedPreference transaction sequence number - start
    public static SharedPreferences getSharedPrefTransSeqNum(Context context) {
        return context.getSharedPreferences(PREF_KEY_VAN_TRANS_SEQ_NUM, Context.MODE_PRIVATE);
    }
    public static String getSharedPrefTransSeqNumValue(Context context) {
        return context.getSharedPreferences(PREF_KEY_VAN_TRANS_SEQ_NUM, Context.MODE_PRIVATE).getString(PREF_KEY_VAN_TRANS_SEQ_NUM, "0");
    }
    public static void setSharedPrefTransSeqNumValue(Context context, String transSeqNum) {
        SharedPreferences.Editor editor = context.getSharedPreferences(PREF_KEY_VAN_TRANS_SEQ_NUM, Context.MODE_PRIVATE).edit();
        editor.putString(PREF_KEY_VAN_TRANS_SEQ_NUM, transSeqNum);
        editor.commit();
    }
    // SharedPreference itransaction sequence number - stop

    public static byte[] hexToByteArray(String hex) {
        if (hex == null || hex.length() == 0) {
            return null;
        }
        byte[] ba = new byte[hex.length() / 2];
        for (int i = 0; i < ba.length; i++) {
            ba[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return ba;
    }

    public static String byteArrayToHex(byte[] ba) {
        if (ba == null || ba.length == 0) {
            return null;
        }
        StringBuffer sb = new StringBuffer(ba.length * 2);
        String hexNumber;
        for (int x = 0; x < ba.length; x++) {
            hexNumber = "0" + Integer.toHexString(0xff & ba[x]);
            sb.append(hexNumber.substring(hexNumber.length() - 2));
        }
        return sb.toString();
    }

    public static byte[] stringToBytesASCII(String str) {
        byte[] b = new byte[str.length()];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) str.charAt(i);
        }
        return b;
    }


    public static byte[] randToByteArray(int length) {
        int count = (length/8);
        int size = 0;
        byte[] randByteArray =  new byte[length];
        for (int i = 0; i < count; i++) {
            double randVal = Math.random();

            byte[] converBuf = convertDoubleToByteArray(randVal);

            if(i*8 >= length){
                System.arraycopy(converBuf, 0, randByteArray,size , length-size);
                size += length-size;
            }else{
                System.arraycopy(converBuf, 0, randByteArray,size , converBuf.length);
                size += converBuf.length;
            }
        }
        return randByteArray;
    }


    public static byte[] convertDoubleToByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
        buffer.putDouble(value);
        return buffer.array();
    }

    private static final String UTF8 = "utf-8";
    private static final char[] SEKRIT = "pointmobile.co.kr".toCharArray();
    private static final String algorithm = "PBEWithMD5AndDES";
    public static String encrypt(Context context, String value) {

        try {
            final byte[] bytes = value != null ? value.getBytes(UTF8)
                    : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance(algorithm);
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            Cipher pbeCipher = Cipher.getInstance(algorithm);
            pbeCipher.init(
                    Cipher.ENCRYPT_MODE,
                    key,
                    new PBEParameterSpec(Settings.Secure.getString(
                            context.getContentResolver(),
                            Settings.System.ANDROID_ID).getBytes(UTF8), 20));
            return new String(Base64.encode(pbeCipher.doFinal(bytes),
                    Base64.NO_WRAP), UTF8);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static String decrypt(Context context, String value) {
        try {
            final byte[] bytes = value != null ? Base64.decode(value,
                    Base64.DEFAULT) : new byte[0];
            SecretKeyFactory keyFactory = SecretKeyFactory
                    .getInstance(algorithm);
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(SEKRIT));
            Cipher pbeCipher = Cipher.getInstance(algorithm);
            pbeCipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new PBEParameterSpec(Settings.Secure.getString(
                            context.getContentResolver(),
                            Settings.System.ANDROID_ID).getBytes(UTF8), 20));
            return new String(pbeCipher.doFinal(bytes), UTF8);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void debugTx(String tag, byte[] dataBuf,int len) {
        Log.d(tag, " len " +len);
        String str = "";
        for (int i = 1; i < len+1; i++) {
            str+=String.format("%02x", dataBuf[i-1])+" ";
            if(i%10 == 0){
                str+="\n";
            }
        }
        Log.d(tag, str);
    }

    public static String debugLogStr( byte[] dataBuf,int len) {
        String str = "";
        for (int i = 1; i < len+1; i++) {
            str+=String.format("%02x", dataBuf[i-1])+" ";
            if(i%16 == 0){
                str+="\n";
            }
        }
    return str;
    }

    public static void setSharedPreference(Context mContext,String key, String val) {
       String enVal = encrypt(mContext,val);
        SharedPreferences pref = mContext.getSharedPreferences(key, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, enVal);
        editor.commit();
    }
    public static String getSharedPreference(Context mContext,String key) {
        SharedPreferences pref = mContext.getSharedPreferences(key, mContext.MODE_PRIVATE);
        String val = pref.getString(key, Utils.PREF_KEY_FLAG_STR_FALSE);
        String deVal;
        if(val.equals(PREF_KEY_FLAG_STR_FALSE)){
            deVal = val;
        }else{
            deVal = decrypt(mContext,val);
        }
        return deVal;
    }

    public static void setSharedPreferenceInt(Context mContext,String key, int val) {
        SharedPreferences pref = mContext.getSharedPreferences(key, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(key, val);
        editor.commit();
    }
    public static int getSharedPreferenceInt(Context mContext,String key) {
        SharedPreferences pref = mContext.getSharedPreferences(key, mContext.MODE_PRIVATE);
        return pref.getInt(key,-1);
    }

    private static void removeSharedPreferences(Context mContext,String key) {
        SharedPreferences pref = mContext.getSharedPreferences(key, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.remove(key);
        editor.commit();
    }

    private static void removeAllSharedPreferences(Context mContext,String key) {
        SharedPreferences pref = mContext.getSharedPreferences(key, mContext.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.clear();
        editor.commit();
    }

    public static void SimpleDailog(Context mContext, String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        builder.show();

    }


    /******************* File +++  if Utils >> TODO: add input Context ******************************/
    //FileName
    public static String FileName_SPLogFile = "SPLogFile";
    public static String FileName_DeviceDownloadInfoFile = "infofile";

    public static File writeInputStreamToFile(Context mContext, String fileName, InputStream in) throws IOException {
        File file = new File(mContext.getCacheDir(), fileName);
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);

            int read = 0;
            byte[] bytes = new byte[4096];

            while ((read = in.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }

            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return  null;
        }
    }



    public static void writeFile(Context mContext, String fileName, String text) {
        File file = new File(mContext.getFilesDir(), fileName);
        byte[] file_content = text.getBytes();

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileOutputStream fos;
        if (file != null && file.exists() && file_content != null) {
            try {
                fos = new FileOutputStream(file);
                try {
                    fos.write(file_content);
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static String readFile(Context mContext,String fileName) {
        File file = new File(mContext.getFilesDir(), fileName);
        String readData = "";
        String line = "";
        //Creates a reader for the file
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            while ((line = reader.readLine()) != null) {
                readData += line;
                readData += "\n";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return readData;
    }

    public static void clearFile(Context mContext, String fileName) {
        File file = new File(mContext.getFilesDir(), fileName);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write("");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeFile(Context mContext, String fileName) {
        File file = new File(mContext.getFilesDir(), fileName);
        //create File
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public static void saveLogFile(Context mContext, String fileName,String result) {
        int timeLen = Utils.TIME_BUF_LEN;
        ArrayList<String> spLogList = new ArrayList<String>();
        File file = new File(mContext.getFilesDir(), fileName);

        StringBuilder writeStr = new StringBuilder();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (file != null && file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String lineStr = "";
                int i =0;
                while ((lineStr = reader.readLine()) != null) {
                    spLogList.add(i,lineStr);
                    i++;
                }
                reader.close();

                BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));

                StringBuilder fileStr = new StringBuilder();
                String time = Utils.convertCurrentDateToString();
                fileStr.append(time);
                fileStr.append(" "+mContext.getResources().getString(R.string.splog_result));
                fileStr.append(result+" ");

                String text = fileStr.toString();
                spLogList.add(i,text);

                if(spLogList.size() >= Utils.MAX_SP_LOG_LINE){
                    spLogList.remove(0);

                    clearFile(mContext, fileName);
                    for(int j=0; j<spLogList.size(); j++){
                        if(spLogList.get(j) != null) {
                            writeStr.append(spLogList.get(j));
                        }else{
                            return;
                        }
                    }
                }else{
                    writeStr = fileStr;
                }
                //encode
                String encodWriteStr = Utils.encrypt(mContext,writeStr.toString());
                writer.write(encodWriteStr);
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String readSPLog(Context context) {
        String readStr = readFile(context, FileName_SPLogFile);
        String decodReadStr = Utils.decrypt(context,readStr);
        return decodReadStr;
    }
    public static void saveSPLog(Context context, String resultStr ,String msgStr){
        Utils.saveLogFile(context,FileName_SPLogFile,resultStr);
    }
    public static void clearSPLog(Context context){
        Utils.clearFile(context,FileName_SPLogFile);
    }


    public static String setSuccessFailStr(String resultStr){
        String spResult;
        if(resultStr.equals("00")){
            spResult ="SUCCESS" ;
        }else{
            spResult ="Fail" ;
        }
        return spResult;
    }

    //Admin Util source  ---
    public static String phoneFomatToString(String telNumber) {
        Pattern tellPattern = Pattern.compile( "^(01\\d{1}|02|0\\d{1,2})-?(\\d{3,4})-?(\\d{4})");
        if(telNumber == null) {
            return telNumber;
        }
        telNumber = telNumber.trim();
        Matcher matcher = tellPattern.matcher( telNumber);
        if(matcher.matches()) {
            return matcher.group(1) + "-" + matcher.group(2) + "-" + matcher.group(3);
        } else {
            return telNumber;
        }
    }
}
