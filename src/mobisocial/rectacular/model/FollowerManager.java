package mobisocial.rectacular.model;

import java.util.ArrayList;
import java.util.List;

import mobisocial.rectacular.model.MEntry.EntryType;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;

public class FollowerManager extends ManagerBase {
    private static final String[] STANDARD_FIELDS = new String[] {
        MFollower.COL_ID,
        MFollower.COL_USER_ID,
        MFollower.COL_TYPE,
        MFollower.COL_FEED_URI
    };
    
    private static final int _id = 0;
    private static final int userId = 1;
    private static final int type = 2;
    private static final int feedUri = 3;
    
    private SQLiteStatement sqlInsertFollower;
    private SQLiteStatement sqlUpdateFollower;

    public FollowerManager(SQLiteDatabase db) {
        super(db);
    }
    
    public FollowerManager(SQLiteOpenHelper databaseSource) {
        super(databaseSource);
    }
    
    /**
     * Add a follower
     * @param follower Details about the follower
     */
    public void insertFollower(MFollower follower) {
        SQLiteDatabase db = initializeDatabase();
        if (sqlInsertFollower == null) {
            synchronized(this) {
                if (sqlInsertFollower == null) {
                    StringBuilder sql = new StringBuilder()
                        .append("INSERT INTO ").append(MFollower.TABLE)
                        .append("(")
                        .append(MFollower.COL_USER_ID).append(",")
                        .append(MFollower.COL_TYPE).append(",")
                        .append(MFollower.COL_FEED_URI)
                        .append(") VALUES (?,?,?)");
                    sqlInsertFollower = db.compileStatement(sql.toString());
                }
            }
        }
        synchronized(sqlInsertFollower) {
            bindField(sqlInsertFollower, userId, follower.userId);
            bindField(sqlInsertFollower, type, follower.type.ordinal());
            bindField(sqlInsertFollower, feedUri, follower.feedUri);
            follower.id = sqlInsertFollower.executeInsert();
        }
    }
    
    /**
     * Update a follower entry
     * @param follower MFollower object
     */
    public void updateFollower(MFollower follower) {
        SQLiteDatabase db = initializeDatabase();
        if (sqlUpdateFollower == null) {
            synchronized(this) {
                if (sqlUpdateFollower == null) {
                    StringBuilder sql = new StringBuilder()
                        .append("UPDATE ").append(MFollower.TABLE)
                        .append(" SET ")
                        .append(MFollower.COL_USER_ID).append("=?,")
                        .append(MFollower.COL_TYPE).append("=?,")
                        .append(MFollower.COL_FEED_URI).append("=?")
                        .append(" WHERE ").append(MFollower.COL_ID).append("=?");
                    sqlUpdateFollower = db.compileStatement(sql.toString());
                }
            }
        }
        synchronized(sqlUpdateFollower) {
            bindField(sqlUpdateFollower, userId, follower.userId);
            bindField(sqlUpdateFollower, type, follower.type.ordinal());
            bindField(sqlUpdateFollower, feedUri, follower.feedUri);
            bindField(sqlUpdateFollower, 4, follower.id);
            sqlUpdateFollower.executeUpdateDelete();
        }
    }
    
    /**
     * Get a follower or insert one if none exists.
     * @param type EntryType of the follower
     * @param userId String that identifies the follower
     * @param feedUri Uri to reach the follower
     * @return MFollower object
     */
    public MFollower ensureFollower(EntryType type, String userId, Uri feedUri) {
        SQLiteDatabase db = initializeDatabase();
        db.beginTransaction();
        try {
            MFollower follower = getFollower(type, userId);
            if (follower != null && !follower.feedUri.equals(feedUri)) {
                follower.feedUri = feedUri;
                updateFollower(follower);
            } else if (follower == null) {
                follower = new MFollower();
                follower.type = type;
                follower.userId = userId;
                follower.feedUri = feedUri;
                insertFollower(follower);
            }
            db.setTransactionSuccessful();
            return follower;
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Get all followers of a given type
     * @param type EntryType of followers
     * @return List of MFollower objects
     */
    public List<MFollower> getFollowers(EntryType type) {
        SQLiteDatabase db = initializeDatabase();
        String table = MFollower.TABLE;
        String[] columns = STANDARD_FIELDS;
        String selection = MFollower.COL_TYPE + "=?";
        String[] selectionArgs = new String[] {
                Integer.toString(type.ordinal())
        };
        String groupBy = null, having = null, orderBy = null;
        Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        try {
            List<MFollower> followers = new ArrayList<MFollower>();
            while (c.moveToNext()) {
                followers.add(fillInStandardFields(c));
            }
            return followers;
        } finally {
            c.close();
        }
    }
    
    /**
     * Determine if we know how to reach a follower
     * @param type EntryType the follower cares about
     * @param userId Unique user identifier
     * @return MFollower object
     */
    public MFollower getFollower(EntryType type, String userId) {
        SQLiteDatabase db = initializeDatabase();
        String table = MFollower.TABLE;
        String[] columns = STANDARD_FIELDS;
        String selection = MFollower.COL_TYPE + "=? AND " + MFollower.COL_USER_ID + "=?";
        String[] selectionArgs = new String[] {
                Integer.toString(type.ordinal()), userId
        };
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
    
    private MFollower fillInStandardFields(Cursor c) {
        MFollower follower = new MFollower();
        follower.id = c.getLong(_id);
        follower.userId = c.getString(userId);
        follower.type = EntryType.values()[(int) c.getLong(type)];
        follower.feedUri = Uri.parse(c.getString(feedUri));
        return follower;
    }

    @Override
    public void close() {
        if (sqlInsertFollower != null) {
            sqlInsertFollower.close();
        }
        if (sqlUpdateFollower != null) {
            sqlUpdateFollower.close();
        }
    }

}
