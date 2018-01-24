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

/*
*   Activity가 MSR 모듈로부터 받은 IC 응답전문의 응답코드
*   - 00 : 서명입력화면으로 이동.
*   - 08 외: IC read fail화면으로 이동.
*   결제취소버튼: (새로 정의된) 결제취소 packet을 PaymentCancelListener로 처리.
* */

public class ICReadWaitFragment extends Fragment {

    private final String TAG = ICReadWaitFragment.class.getSimpleName();

    ProgressBar mPrograssBar;

    private Button mBtnCancel;

    private FragmentCallback.PaymentCancelListener mListener;

    public ICReadWaitFragment() {

    }

    public static ICReadWaitFragment newInstance() {
        ICReadWaitFragment fragment = new ICReadWaitFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View ICReadWaitFragmentView = inflater.inflate(R.layout.fragment_ic_read, container, false);

        mPrograssBar = (ProgressBar) ICReadWaitFragmentView.findViewById(R.id.progressCircle);
        visibleProgrbar();

        mBtnCancel = (Button) ICReadWaitFragmentView.findViewById(R.id.btnCancel);
        mBtnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onClickPaymentCancelButton();
            }
        });
        return ICReadWaitFragmentView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentCallback.PaymentCancelListener) {
            mListener = (FragmentCallback.PaymentCancelListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement PaymentCancelListener");
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
//        animation.setDuration (); //in milliseconds
        animation.setInterpolator (new DecelerateInterpolator());
        animation.start ();
    }

    private void invisibleProgrbar() {
        mPrograssBar.clearAnimation();
        mPrograssBar.setVisibility(View.INVISIBLE);
    }

}
