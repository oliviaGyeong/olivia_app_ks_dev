package device.apps.pmpos.fragment;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;

import device.apps.pmpos.R;
import device.apps.pmpos.fragment.listener.FragmentCallback;


public class MsrSwipeWaitFragment extends Fragment {

    private final String TAG = MsrSwipeWaitFragment.class.getSimpleName();

    ProgressBar mPrograssBar;
    private Button mBtnCancel;
    private FragmentCallback.PaymentCancelListener mListener;

    public MsrSwipeWaitFragment() {
    }

    public static MsrSwipeWaitFragment newInstance() {
        MsrSwipeWaitFragment fragment = new MsrSwipeWaitFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View MSReadWaitFragmentView = inflater.inflate(R.layout.fragment_msr_swipe, container, false);

        mPrograssBar = (ProgressBar) MSReadWaitFragmentView.findViewById(R.id.progressCircle);
        visibleProgrbar();

        mBtnCancel = (Button) MSReadWaitFragmentView.findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onClickPaymentCancelButton();
            }
        });
        return MSReadWaitFragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentCallback.PaymentCancelListener) {
            mListener = (FragmentCallback.PaymentCancelListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement Listener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
