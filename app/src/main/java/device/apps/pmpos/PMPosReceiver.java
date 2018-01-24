package device.apps.pmpos;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class PMPosReceiver extends BroadcastReceiver {
    private final String TAG = PMPosReceiver.class.getSimpleName();
    public static Context mContext;
    public static Intent ServiceIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(Utils.ACTION_RECEIVE_DATA_FROM_DAEMON)) {
            mContext = context;
            if (intent != null) {
                ServiceIntent = new Intent(context, PMPosHelper.class);
                ServiceIntent.setData(intent.getData());
                context.startService(ServiceIntent);

                Utils.showToastMessage(mContext, "","Start PM-POS Service");
            }
        }
    }
}
