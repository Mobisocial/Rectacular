package mobisocial.rectacular;

import mobisocial.rectacular.model.DBHelper;
import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class App extends Application {
    // Shared preferences constants
    public static final String PREFS_FILE = "rectacular_prefs";
    public static final String PREF_APP_SETUP_COMPLETE = "app_setup_complete";
    
    // Content URIs
    private static final String URI_SCHEME = "content://";
    private static final String URI_AUTHORITY = "mobisocial.rectacular.db";
    public static final Uri URI_APP_SETUP_COMPLETE = Uri.parse(
            URI_SCHEME + URI_AUTHORITY + "/app_setup_complete");
    
    private SQLiteOpenHelper mDatabaseSource;
    
    public static SQLiteOpenHelper getDatabaseSource(Context c) {
        Context appAsContext = c.getApplicationContext();
        return ((App)appAsContext).getDatabaseSource();
    }
    
    public synchronized SQLiteOpenHelper getDatabaseSource() {
        if (mDatabaseSource == null) {
            mDatabaseSource = new DBHelper(getApplicationContext());
        }
        return mDatabaseSource;
    }
}
