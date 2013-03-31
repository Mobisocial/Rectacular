package mobisocial.rectacular.model;

import android.net.Uri;
import mobisocial.rectacular.model.MEntry.EntryType;

/**
 * Feeds for each type of content
 */
public class MFeed {
    public static final String TABLE = "my_feeds";
    
    /**
     * Primary identifier
     */
    public static final String COL_ID = "_id";
    
    /**
     * Followed content type
     */
    public static final String COL_TYPE = "content_type";
    
    /**
     * Feed URI to reach followed users
     */
    public static final String COL_FEED_URI = "feed_uri";
    
    public long id;
    public EntryType type;
    public Uri feedUri;
}
