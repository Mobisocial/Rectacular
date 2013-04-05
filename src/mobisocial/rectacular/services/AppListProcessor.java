package mobisocial.rectacular.services;

import java.util.List;

import mobisocial.rectacular.App;
import mobisocial.rectacular.model.EntryManager;
import mobisocial.rectacular.model.UserEntryManager;
import mobisocial.rectacular.model.MEntry.EntryType;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.Musubi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class AppListProcessor extends ContentObserver {
    private static final String TAG = "AppListProcessor";
    
    private final Context mContext;
    private final EntryManager mEntryManager;
    private final UserEntryManager mUserEntryManager;
    private final Musubi mMusubi;
    
    public static AppListProcessor newInstance(Context context, SQLiteOpenHelper dbh) {
        HandlerThread thread = new HandlerThread("AppListProcessorThread");
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
        return new AppListProcessor(context, dbh, thread);
    }
    
    private AppListProcessor(Context context, SQLiteOpenHelper dbh, HandlerThread thread) {
        super(new Handler(thread.getLooper()));
        mContext = context;
        mEntryManager = new EntryManager(dbh);
        mUserEntryManager = new UserEntryManager(dbh);
        if (Musubi.isMusubiInstalled(context)) {
            mMusubi = Musubi.getInstance(context);
        } else {
            mMusubi = null;
        }
    }
    
    @Override
    public void onChange(boolean selfChange) {
        Log.d(TAG, "onChange");
        if (mMusubi == null) return;
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        List<DbIdentity> myIdentities = mMusubi.users(null);
        for (ApplicationInfo app : apps) {
            String name = (String)pm.getApplicationLabel(app);
            Log.d(TAG, "Installed App: " + name);
            // TODO: do something interesting with this
            for (DbIdentity ident : myIdentities) { // all owned identities
                mUserEntryManager.ensureUserEntry(
                        mEntryManager, EntryType.App, name, true, ident.getId(), true);
            }
        }
        
        // Save the state indicating that apps were fetched and saved
        mContext.getSharedPreferences(App.PREFS_FILE, 0)
            .edit().putBoolean(App.PREF_APP_SETUP_COMPLETE, true).commit();
        mContext.getContentResolver().notifyChange(App.URI_APP_SETUP_COMPLETE, null);
    }

}
