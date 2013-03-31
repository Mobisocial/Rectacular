package mobisocial.rectacular.model;

import mobisocial.rectacular.model.MEntry.EntryType;
import android.net.Uri;

/**
 * Track followers and how to reach them
 */

public class MFollower {
    public static final String TABLE = "followers";
    
    /**
     * Primary identifier
     */
    public static final String COL_ID = "_id";
    
    /**
     * User identifier
     */
    public static final String COL_USER_ID = "user_id";
    
    /**
     * Type follower listens for
     */
    public static final String COL_TYPE = "followed_type";
    
    /**
     * Feed uri to send updates
     */
    public static final String COL_FEED_URI = "feed_uri";
    
    public long id;
    public String userId;
    public EntryType type;
    public Uri feedUri;
}
