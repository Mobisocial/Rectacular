package mobisocial.rectacular.services;

import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

public class MessageReceiver extends BroadcastReceiver {
    private static final String TAG = "MessageReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Log.d(TAG, "no intent");
        }
        
        Log.d(TAG, "message received: " + intent);
        
        Uri objUri = intent.getParcelableExtra("objUri");
        if (objUri == null) {
            Log.d(TAG, "no object found");
            return;
        }
        
        Musubi musubi = Musubi.forIntent(context, intent);
        DbObj obj = musubi.objForUri(objUri);
        if (obj.getSender().isOwned()) {
            return; // TODO: maybe do something else with messages I send
        }
        
        // TODO: process this message
    }

}
