package device.apps.pmpos.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import device.apps.pmpos.R;
import device.apps.pmpos.Utils;
import device.apps.pmpos.fragment.listener.FragmentCallback;
import device.apps.pmpos.packet.ReceiptPrintData;

public class ReceiptPrintFragment extends Fragment {

    private final String TAG = ReceiptPrintFragment.class.getSimpleName();

    private static boolean mHaveParam = false;

    private TextView mTextPrintMsg;
    private Button mBtnOk;

    private Context mContext;
    private FragmentCallback.MoveOtherFragmentListener mMoveFragListener;
    ReceiptPrintData mReceiptData;

    public ReceiptPrintFragment() {
    }

    public static ReceiptPrintFragment newInstance(ReceiptPrintData receiptData) {
        mHaveParam = true;
        ReceiptPrintFragment fragment = new ReceiptPrintFragment();
        Bundle args = new Bundle();
        args.putSerializable(Utils.KEY_EXTRA_RECEIPT_DATA, receiptData);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ReceiptPrintFragment = inflater.inflate(R.layout.fragment_receipt_print, container, false);
        mReceiptData = (ReceiptPrintData)getArguments().getSerializable(Utils.KEY_EXTRA_RECEIPT_DATA);
        if(mReceiptData == null){

        }
        String receiptStr = mReceiptData.getReceiptDataStr();
        String printMsg = receiptStr;

        mTextPrintMsg = (TextView) ReceiptPrintFragment.findViewById(R.id.textPrintMsg);
        mTextPrintMsg.setText(printMsg);

        mBtnOk = (Button) ReceiptPrintFragment.findViewById(R.id.btnOk);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMoveFragListener.moveOtherFragment(Utils.FRAG_NUM_MAIN, false, null);
            }
        });

        return ReceiptPrintFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mContext = context;

        if (context instanceof FragmentCallback.MoveOtherFragmentListener) {
            mMoveFragListener = (FragmentCallback.MoveOtherFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement MoveOtherFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMoveFragListener = null;
    }
}
