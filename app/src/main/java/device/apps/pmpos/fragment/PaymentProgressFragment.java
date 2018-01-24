package device.apps.pmpos.fragment;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;

import device.apps.pmpos.R;
import device.apps.pmpos.Utils;
import device.apps.pmpos.fragment.listener.FragmentCallback;

public class PaymentProgressFragment extends Fragment {

    private final String TAG = PaymentProgressFragment.class.getSimpleName();

    private static boolean mHaveRdPktBuf = false;

    ProgressBar mPrograssBar;

    private Context mContext;

    private FragmentCallback.MoveOtherFragmentListener mMoveFragListener;

    public PaymentProgressFragment() {
    }

    public static PaymentProgressFragment newInstance() {
        mHaveRdPktBuf = false;
        PaymentProgressFragment fragment = new PaymentProgressFragment();
        return fragment;
    }

    public static PaymentProgressFragment newInstance(byte[] recvRdPktBuf) {
        PaymentProgressFragment fragment = new PaymentProgressFragment();

        if (recvRdPktBuf == null) {
            mHaveRdPktBuf = false;
        } else if ((recvRdPktBuf != null) && (recvRdPktBuf.length > 0)) {
            mHaveRdPktBuf = true;
            Bundle args = new Bundle();
            args.putByteArray(Utils.KEY_EXTRA_IC_READER_PACKET_BUFFER, recvRdPktBuf);
            fragment.setArguments(args);
        }

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View PaymentProgressFragmentView = inflater.inflate(R.layout.fragment_payment_progress, container, false);
        mPrograssBar = (ProgressBar) PaymentProgressFragmentView.findViewById(R.id.progressCircle);
        visibleProgrbar();
        return PaymentProgressFragmentView;
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
        invisibleProgrbar();
        mMoveFragListener = null;
    }

    private void visibleProgrbar() {
        mPrograssBar.setVisibility(View.VISIBLE);
        ObjectAnimator animation = ObjectAnimator.ofInt(mPrograssBar, "progress", 0, 1000);
        animation.setInterpolator (new DecelerateInterpolator());
        animation.start ();
    }

    private void invisibleProgrbar() {
        mPrograssBar.clearAnimation();
        mPrograssBar.setVisibility(View.INVISIBLE);
    }
}
