package mobisocial.rectacular.services;

import java.util.LinkedList;
import java.util.List;

import mobisocial.rectacular.App;
import mobisocial.rectacular.model.EntryManager;
import mobisocial.rectacular.model.FollowerManager;
import mobisocial.rectacular.model.MEntry;
import mobisocial.rectacular.model.MEntry.EntryType;
import mobisocial.rectacular.model.UserEntryManager;
import mobisocial.rectacular.social.Entry;
import mobisocial.rectacular.social.SocialClient;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.Musubi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.Log;

public class AppInstallReceiver extends BroadcastReceiver {
    private static final String TAG = "AppInstallReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getData() == null) {
            Log.d(TAG, "no data");
            return;
        }
        Uri appUri = intent.getData();
        Log.d(TAG, "uri: " + appUri.toString());
        
        String packageName = appUri.getSchemeSpecificPart();
        String name = getApplicationName(context, packageName, PackageManager.GET_UNINSTALLED_PACKAGES);
        if (name == null) {
            return;
        }
        Log.d(TAG, "app name: " + name);
        
        if (!Musubi.isMusubiInstalled(context)) {
            return;
        }
        
        // Save the app
        Musubi musubi = Musubi.getInstance(context);
        List<DbIdentity> myIdentities = musubi.users(null);
        EntryManager em = new EntryManager(App.getDatabaseSource(context));
        UserEntryManager uem = new UserEntryManager(App.getDatabaseSource(context));
        for (DbIdentity ident : myIdentities) {
            uem.ensureUserEntry(em, EntryType.App, name, true, ident.getId(), true);
        }
        
        // Let followers know
        MEntry dbEntry = em.getEntry(EntryType.App, name);
        if (dbEntry == null) {
            return; // should never happen
        }
        FollowerManager fm = new FollowerManager(App.getDatabaseSource(context));
        SocialClient sc = new SocialClient(musubi, context);
        Entry entry = sc.dbEntryToEntry(dbEntry);
        List<Entry> entries = new LinkedList<Entry>();
        entries.add(entry);
        sc.postToFollowers(entries, fm.getFollowers(EntryType.App), EntryType.App, null);
    }
    
    private String getApplicationName(Context context, String data, int flag) {
        final PackageManager pckManager = context.getPackageManager();
        ApplicationInfo applicationInformation;
        try {
            applicationInformation = pckManager.getApplicationInfo(data, flag);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInformation = null;
        }
        final String applicationName = (String) (applicationInformation != null ? pckManager.getApplicationLabel(applicationInformation) : null);

        return applicationName;
    }

}
