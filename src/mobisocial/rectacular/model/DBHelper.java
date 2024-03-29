package mobisocial.rectacular.model;

import mobisocial.rectacular.App;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {
    private static final String TAG = "DBHelper";
    
    private static final String DB_NAME = "Rectacular.db";
    private static final int VERSION = 3;
    
    private Context mContext;
    
    public DBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db, MEntry.TABLE,
                MEntry.COL_ID, "INTEGER PRIMARY KEY",
                MEntry.COL_TYPE, "INTEGER NOT NULL",
                MEntry.COL_NAME, "TEXT NOT NULL",
                MEntry.COL_OWNED, "INTEGER NOT NULL",
                MEntry.COL_COUNT, "INTEGER NOT NULL",
                MEntry.COL_FOLLOWING_COUNT, "INTEGER NOT NULL",
                MEntry.COL_THUMBNAIL, "BLOB",
                MEntry.COL_METADATA, "TEXT");
        
        createTable(db, MUserEntry.TABLE,
                MUserEntry.COL_ID, "INTEGER PRIMARY KEY",
                MUserEntry.COL_ENTRY_ID, "INTEGER NOT NULL",
                MUserEntry.COL_USER_ID, "TEXT NOT NULL");
        
        createTable(db, MFollower.TABLE,
                MFollower.COL_ID, "INTEGER PRIMARY KEY",
                MFollower.COL_USER_ID, "TEXT NOT NULL",
                MFollower.COL_TYPE, "INTEGER NOT NULL",
                MFollower.COL_FEED_URI, "TEXT NOT NULL");
        
        createTable(db, MFeed.TABLE,
                MFeed.COL_ID, "INTEGER PRIMARY KEY",
                MFeed.COL_TYPE, "INTEGER NOT NULL",
                MFeed.COL_FEED_URI, "TEXT NOT NULL");
        
        createTable(db, MFollowing.TABLE,
                MFollowing.COL_ID, "INTEGER PRIMARY KEY",
                MFollowing.COL_FEED_ID, "INTEGER NOT NULL",
                MFollowing.COL_USER_ID, "TEXT NOT NULL");
        
        db.execSQL("CREATE INDEX " + MEntry.TABLE + "_type ON " +
                MEntry.TABLE + "(" + MEntry.COL_TYPE + ")");
        db.execSQL("CREATE INDEX " + MEntry.TABLE + "_type_name ON " +
                MEntry.TABLE + "(" + MEntry.COL_TYPE + "," +
                MEntry.COL_NAME + ")");
        db.execSQL("CREATE INDEX " + MEntry.TABLE + "_type_fct ON " +
                MEntry.TABLE + "(" + MEntry.COL_TYPE + "," +
                MEntry.COL_FOLLOWING_COUNT + ")");

        db.execSQL("CREATE INDEX " + MUserEntry.TABLE + "_user ON " +
                MUserEntry.TABLE + "(" + MUserEntry.COL_USER_ID + ")");
        db.execSQL("CREATE INDEX " + MUserEntry.TABLE + "_entry ON " +
                MUserEntry.TABLE + "(" + MUserEntry.COL_ENTRY_ID + ")");
        
        db.execSQL("CREATE INDEX " + MFollower.TABLE + "_type ON " +
                MFollower.TABLE + "(" + MFollower.COL_TYPE + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion >= newVersion) {
            return;
        }
        
        if (oldVersion <= 1) {
            db.execSQL("CREATE INDEX " + MEntry.TABLE + "_type_fct ON " +
                    MEntry.TABLE + "(" + MEntry.COL_TYPE + "," +
                    MEntry.COL_FOLLOWING_COUNT + ")");
            db.execSQL("CREATE INDEX " + MUserEntry.TABLE + "_entry ON " +
                    MUserEntry.TABLE + "(" + MUserEntry.COL_ENTRY_ID + ")");
        }
        
        if (oldVersion <= 2) {
            db.execSQL("ALTER TABLE " + MEntry.TABLE + " ADD COLUMN " + MEntry.COL_METADATA + " TEXT");
            // Need to start over since metadata is useful
            mContext.getSharedPreferences(App.PREFS_FILE, 0).edit()
                .putBoolean(App.PREF_APP_SETUP_COMPLETE, false).commit();
        }
        
        if (oldVersion <= 3) {
            // etc...
        }
        
        db.setVersion(VERSION);
    }
    
    private void createTable(SQLiteDatabase db, String tableName, String... cols){
        String s = "CREATE TABLE " + tableName + " (";
        for(int i = 0; i < cols.length; i += 2){
            s += cols[i] + " " + cols[i + 1];
            if(i < (cols.length - 2)){
                s += ", ";
            }
            else{
                s += " ";
            }
        }
        s += ")";
        Log.i(TAG, s);
        db.execSQL(s);
    }

}
