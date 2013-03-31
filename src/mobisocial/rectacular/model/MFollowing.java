package mobisocial.rectacular.model;

/**
 * Track users we follow
 */
public class MFollowing {
    public static final String TABLE = "following";
    
    /**
     * Primary identifier
     */
    public static final String COL_ID = "_id";
    
    /**
     * Feed identifier
     */
    public static final String COL_FEED_ID = "feed_id";
    
    /**
     * User identifier
     */
    public static final String COL_USER_ID = "user_id";
    
    public long id;
    public Long feedId;
    public String userId;
}
