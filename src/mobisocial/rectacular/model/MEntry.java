package mobisocial.rectacular.model;

/**
 * Generic way to store the fact that a user has broadcasted his state.
 */

public class MEntry {
    public static final String TABLE = "entries";
    
    /**
     * Primary identifier
     */
    public static final String COL_ID = "_id";
    
    /**
     * The type of the entity
     */
    public static final String COL_TYPE = "ent_type";
    
    /**
     * The name of the entity
     */
    public static final String COL_NAME = "ent_name";
    
    /**
     * Flag as to whether the current user has this
     */
    public static final String COL_OWNED = "ent_owned";
    
    /**
     * Entity ownership count
     */
    public static final String COL_COUNT = "ent_count";
    
    /**
     * Count among following
     */
    public static final String COL_FOLLOWING_COUNT = "ent_following_count";
    
    /**
     * Optional thumbnail
     */
    public static final String COL_THUMBNAIL = "thumbnail";
    
    public long id;
    public EntryType type;
    public String name;
    public Boolean owned;
    public Long count;
    public Long followingCount;
    public byte[] thumbnail;
    
    public enum EntryType {
        App,
        Movie,
        Song,
        Album,
        Restaurant
        // always add, never remove
    }
}
