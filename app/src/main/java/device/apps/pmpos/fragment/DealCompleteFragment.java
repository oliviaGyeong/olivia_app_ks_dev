package device.apps.pmpos.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import device.apps.pmpos.R;
import device.apps.pmpos.Utils;
import device.apps.pmpos.fragment.listener.FragmentCallback;


public class DealCompleteFragment extends Fragment {

    private final String TAG = DealCompleteFragment.class.getSimpleName();

    private static boolean mHaveParam = false;

    private TextView mTextResultMsg;
    private Button mBtnOk;
    private Button mBtnPrint;

    private Context mContext;
    private FragmentCallback.MoveOtherFragmentListener mMoveFragListener;

    private String mResultMsg = "";

    public DealCompleteFragment() {
    }

    public static DealCompleteFragment newInstance() {
        mHaveParam = false;
        DealCompleteFragment fragment = new DealCompleteFragment();
        return fragment;
    }

    public static DealCompleteFragment newInstance(String param) {
        mHaveParam = true;
        DealCompleteFragment fragment = new DealCompleteFragment();
        Bundle args = new Bundle();
        args.putString(Utils.KEY_EXTRA_RESULT_MSG, param);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View DealCompleteFragmentView = inflater.inflate(R.layout.fragment_deal_complete, container, false);

        if (mHaveParam) {
            mResultMsg = getArguments().getString(Utils.KEY_EXTRA_RESULT_MSG);
        }

        mTextResultMsg = (TextView) DealCompleteFragmentView.findViewById(R.id.textResultMsg);
        mTextResultMsg.setText(mResultMsg);

        mBtnOk = (Button) DealCompleteFragmentView.findViewById(R.id.btnOk);
        mBtnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMoveFragListener.moveOtherFragment(Utils.FRAG_NUM_MAIN, false, null);
            }
        });

        mBtnPrint = (Button) DealCompleteFragmentView.findViewById(R.id.btnPrint);
        mBtnPrint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mMoveFragListener.moveOtherFragment(Utils.FRAG_NUM_RECEIPT_PRINT, false, null);
            }
        });
        mBtnPrint.setVisibility(View.GONE);

        return DealCompleteFragmentView;
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
