package mobisocial.rectacular.model;

/**
 * Map users to the entries they advertised
 */
public class MUserEntry {
    public static final String TABLE = "user_entries";

    /**
     * Primary identifier
     */
    public static final String COL_ID = "_id";
    
    /**
     * Entry identifier
     */
    public static final String COL_ENTRY_ID = "entry_id";
    
    /**
     * User identifier
     */
    public static final String COL_USER_ID = "user_id";
    
    public long id;
    public Long entryId;
    public String userId;
}
