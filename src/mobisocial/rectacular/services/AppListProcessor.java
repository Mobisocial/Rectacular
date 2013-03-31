package mobisocial.rectacular.services;

import java.util.List;

import mobisocial.rectacular.model.EntryManager;
import mobisocial.rectacular.model.FollowerManager;
import mobisocial.rectacular.model.UserEntryManager;
import mobisocial.rectacular.model.MEntry.EntryType;
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
    @SuppressWarnings("unused")
    private final FollowerManager mFollowerManager;
    @SuppressWarnings("unused")
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
        mFollowerManager = new FollowerManager(dbh);
        if (Musubi.isMusubiInstalled(context)) {
            mMusubi = Musubi.getInstance(context);
        } else {
            mMusubi = null;
        }
    }
    
    @Override
    public void onChange(boolean selfChange) {
        Log.d(TAG, "onChange");
        PackageManager pm = mContext.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        for (ApplicationInfo app : apps) {
            String name = (String)pm.getApplicationLabel(app);
            Log.d(TAG, "Installed App: " + name);
            // TODO: do something interesting with this
            mUserEntryManager.ensureUserEntry(mEntryManager, EntryType.App, name, true, "", true);
        }
    }

}
