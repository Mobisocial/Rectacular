package mobisocial.rectacular.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "AppInstallReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            Log.d(TAG, "no data");
            return;
        }
        Log.d(TAG, "uri: " + intent.getData().toString());
        
        // TODO: do something with this information
    }

}
