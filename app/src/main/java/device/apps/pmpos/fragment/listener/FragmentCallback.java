package device.apps.pmpos.fragment.listener;

/**
 * Created by olivia on 18. 1. 5.
 */

public class FragmentCallback {
    public interface MoveOtherFragmentListener {
        void moveOtherFragment(int fragmentNumber, boolean havePassData, Object data);
    }
    public interface CallbackMainFragment{
        void callbackMainFragment_Approval(String paymentOption,String trdType,String amount,String fee ,String surTax,String instalment,String keyIn,String cashReceiptType);
        void callbackMainFragment_Cancel(String paymentOption,String trdType,String amount,String fee ,String surTax,String instalment,String keyInint,String orgApprovalDate,String orgApprovalNo);

    }
    public interface PaymentCancelListener {
        void onClickPaymentCancelButton();
    }
}
