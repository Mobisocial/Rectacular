package mobisocial.rectacular.model;

import java.util.LinkedList;
import java.util.List;

import mobisocial.rectacular.model.MEntry.EntryType;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

public class UserEntryManager extends ManagerBase {
    private static final String[] STANDARD_FIELDS = new String[] {
        MUserEntry.COL_ID,
        MUserEntry.COL_ENTRY_ID,
        MUserEntry.COL_USER_ID
    };
    
    private static final int _id = 0;
    private static final int entryId = 1;
    private static final int userId = 2;
    
    private SQLiteStatement sqlInsertUserEntry;
    
    public UserEntryManager(SQLiteDatabase db) {
        super(db);
    }
    
    public UserEntryManager(SQLiteOpenHelper databaseSource) {
        super(databaseSource);
    }
    
    /**
     * Insert a user-entry pair
     * @param userEntry A valid user-entry pair
     */
    public void insertUserEntry(MUserEntry userEntry) {
        SQLiteDatabase db = initializeDatabase();
        if (sqlInsertUserEntry == null) {
            synchronized(this) {
                if (sqlInsertUserEntry == null) {
                    StringBuilder sql = new StringBuilder()
                        .append("INSERT INTO ").append(MUserEntry.TABLE)
                        .append("(")
                        .append(MUserEntry.COL_ENTRY_ID).append(",")
                        .append(MUserEntry.COL_USER_ID)
                        .append(") VALUES (?,?)");
                    sqlInsertUserEntry = db.compileStatement(sql.toString());
                }
            }
        }
        synchronized(sqlInsertUserEntry) {
            bindField(sqlInsertUserEntry, entryId, userEntry.entryId);
            bindField(sqlInsertUserEntry, userId, userEntry.userId);
            userEntry.id = sqlInsertUserEntry.executeInsert();
        }
    }
    
    /**
     * Insert a user entry if one does not already exist
     * @param em EntryManager for updating state
     * @param entryId long identifying an MEntry
     * @param userId String identifying a user
     * @param isFollowing Whether or not this user is someone followed
     * @return MUserEntry object
     */
    public MUserEntry ensureUserEntry(EntryManager em, long entryId, String userId, boolean isFollowing) {
        SQLiteDatabase db = initializeDatabase();
        db.beginTransaction();
        try {
            MUserEntry userEntry = getUserEntry(entryId, userId);
            if (userEntry == null) {
                userEntry = new MUserEntry();
                userEntry.entryId = entryId;
                userEntry.userId = userId;
                insertUserEntry(userEntry);
                em.updateCount(entryId, true, isFollowing);
            }
            db.setTransactionSuccessful();
            return userEntry;
        } finally {
            db.endTransaction();
        }
    }
    
    /**
     * Insert an entry and user entry if one or both does not exist.
     * @param em EntryManager for updating state
     * @param type EntryType of the entry
     * @param name String name of the entry
     * @param setOwned Whether or not this entry should be set as owned
     * @param userId String identifier of the user
     * @param isFollowing Whether or not this user is someone followed
     * @return MUserEntry object
     */
    public MUserEntry ensureUserEntry(
            EntryManager em, EntryType type, String name, boolean setOwned,
            String userId, boolean isFollowing) {
        MEntry entry = em.ensureEntry(type, name, false, false, setOwned);
        return ensureUserEntry(em, entry.id, userId, isFollowing);
    }
    
    /**
     * Return a single user-entry pair if it exists
     * @param entryId A valid MEntry id
     * @param userId A valid Musubi user id
     * @return MUserEntry object
     */
    public MUserEntry getUserEntry(long entryId, String userId) {
        SQLiteDatabase db = initializeDatabase();
        String table = MUserEntry.TABLE;
        String[] columns = STANDARD_FIELDS;
        String selection = MUserEntry.COL_ENTRY_ID + "=? AND " + MUserEntry.COL_USER_ID + "=?";
        String[] selectionArgs = new String[]{ Long.toString(entryId), userId };
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
     * Get all users for an entry
     * @param entryId MEntry id
     * @return List of MUserEntry objects
     */
    public List<MUserEntry> getUserEntries(long entryId) {
        SQLiteDatabase db = initializeDatabase();
        String table = MUserEntry.TABLE;
        String[] columns = STANDARD_FIELDS;
        String selection = MUserEntry.COL_ENTRY_ID + "=?";
        String[] selectionArgs = new String[]{ Long.toString(entryId) };
        String groupBy = null, having = null, orderBy = null;
        Cursor c = db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
        try {
            List<MUserEntry> entries = new LinkedList<MUserEntry>();
            while (c.moveToNext()) {
                entries.add(fillInStandardFields(c));
            }
            return entries;
        } finally {
            c.close();
        }
    }
    
    private MUserEntry fillInStandardFields(Cursor c) {
        MUserEntry userEntry = new MUserEntry();
        userEntry.id = c.getLong(_id);
        userEntry.entryId = c.getLong(entryId);
        userEntry.userId = c.getString(userId);
        return userEntry;
    }

    @Override
    public void close() {
        if (sqlInsertUserEntry != null) {
            sqlInsertUserEntry.close();
        }
    }

}
