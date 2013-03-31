package mobisocial.rectacular.model;

import mobisocial.rectacular.model.MEntry.EntryType;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;

public class FeedManager extends ManagerBase {
    private static final String[] STANDARD_FIELDS = new String[] {
        MFeed.COL_ID,
        MFeed.COL_TYPE,
        MFeed.COL_FEED_URI
    };
    
    private static final int _id = 0;
    private static final int type = 1;
    private static final int feedUri = 2;
    
    private SQLiteStatement sqlInsertFeed;

    public FeedManager(SQLiteDatabase db) {
        super(db);
    }
    
    public FeedManager(SQLiteOpenHelper databaseSource) {
        super(databaseSource);
    }
    
    /**
     * Add a new following feed
     * @param feed Valid MFeed object
     */
    public void insertFeed(MFeed feed) {
        SQLiteDatabase db = initializeDatabase();
        if (sqlInsertFeed == null) {
            synchronized(this) {
                if (sqlInsertFeed == null) {
                    StringBuilder sql = new StringBuilder()
                        .append("INSERT INTO ").append(MFeed.TABLE)
                        .append("(")
                        .append(MFeed.COL_TYPE).append(",")
                        .append(MFeed.COL_FEED_URI)
                        .append(") VALUES (?,?)");
                    sqlInsertFeed = db.compileStatement(sql.toString());
                }
            }
        }
        synchronized(sqlInsertFeed) {
            bindField(sqlInsertFeed, type, feed.type.ordinal());
            bindField(sqlInsertFeed, feedUri, feed.feedUri);
            feed.id = sqlInsertFeed.executeInsert();
        }
    }
    
    /**
     * Get a following feed of a given type
     * @param type EntryType of the desired entry
     * @return MFeed object
     */
    public MFeed getFeed(EntryType type) {
        SQLiteDatabase db = initializeDatabase();
        String table = MFeed.TABLE;
        String[] columns = STANDARD_FIELDS;
        String selection = MFeed.COL_TYPE + "=?";
        String[] selectionArgs = new String[] { Integer.toString(type.ordinal()) };
        String groupBy = null, having = null, orderBy = null;
        Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        try {
            if (c.moveToFirst()) {
                return fillInStandardFields(c);
            } else {
                return null;
            }
        } finally {
            c.close();
        }
    }
    
    /**
     * Get a following feed for a given feed URI
     * @param feedUri Uri of the desired feed
     * @return MFeed object
     */
    public MFeed getFeed(Uri feedUri) {
        SQLiteDatabase db = initializeDatabase();
        String table = MFeed.TABLE;
        String[] columns = STANDARD_FIELDS;
        String selection = MFeed.COL_FEED_URI + "=?";
        String[] selectionArgs = new String[] { feedUri.toString() };
        String groupBy = null, having = null, orderBy = null;
        Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        try {
            if (c.moveToFirst()) {
                return fillInStandardFields(c);
            } else {
                return null;
            }
        } finally {
            c.close();
        }
    }
    
    /**
     * Delete a feed of a given type
     * @param type EntryType of the desired feed
     * @return true if rows were deleted, false otherwise
     */
    public boolean deleteFeed(EntryType type) {
        SQLiteDatabase db = initializeDatabase();
        String table = MFeed.TABLE;
        String whereClause = MFeed.COL_TYPE + "=?";
        String[] whereArgs = new String[] { Integer.toString(type.ordinal()) };
        return db.delete(table, whereClause, whereArgs) > 0;
    }
    
    private MFeed fillInStandardFields(Cursor c) {
        MFeed feed = new MFeed();
        feed.id = c.getLong(_id);
        feed.type = EntryType.values()[(int) c.getLong(type)];
        feed.feedUri = Uri.parse(c.getString(feedUri));
        return feed;
    }

    @Override
    public void close() {
        if (sqlInsertFeed != null) {
            sqlInsertFeed.close();
        }
    }

}
