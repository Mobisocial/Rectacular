package mobisocial.rectacular.social;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import mobisocial.rectacular.App;
import mobisocial.rectacular.model.EntryManager;
import mobisocial.rectacular.model.FeedManager;
import mobisocial.rectacular.model.FollowerManager;
import mobisocial.rectacular.model.FollowingManager;
import mobisocial.rectacular.model.MEntry;
import mobisocial.rectacular.model.MEntry.EntryType;
import mobisocial.rectacular.model.MFeed;
import mobisocial.rectacular.model.MFollower;
import mobisocial.rectacular.model.MFollowing;
import mobisocial.rectacular.model.MUserEntry;
import mobisocial.rectacular.model.UserEntryManager;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

public class SocialClient {
    private static final String TAG = "SocialClient";
    
    public static final String HELLO_TYPE = "rectacular_hello";
    public static final String ENTRIES_TYPE = "rectacular";
    
    private static final String TYPE = "type";
    private static final String ENTRIES = "entries";
    private static final String NAME = "name";
    private static final String OWNED = "owned";
    private static final String OWNERS = "owners";
    private static final String EXTRA = "extra";
    
    private static final String HELLO = "hello";
    private static final String TO = "to";
    
    private final Musubi mMusubi;
    private final Context mContext;
    private final SQLiteOpenHelper mDatabaseSource;
    private final EntryManager mEntryManager;
    private final UserEntryManager mUserEntryManager;
    private final FeedManager mFeedManager;
    private final FollowerManager mFollowerManager;
    private final FollowingManager mFollowingManager;
    
    public SocialClient(Musubi musubi, Context context) {
        mMusubi = musubi;
        mContext = context;
        mDatabaseSource = App.getDatabaseSource(mContext);
        mEntryManager = new EntryManager(mDatabaseSource);
        mUserEntryManager = new UserEntryManager(mDatabaseSource);
        mFeedManager = new FeedManager(mDatabaseSource);
        mFollowerManager = new FollowerManager(mDatabaseSource);
        mFollowingManager = new FollowingManager(mDatabaseSource);
    }
    
    /**
     * Sends a collection of entries to entry followers
     * @param entries List of entries
     * @param followers The followers to contact
     * @param type Type of the entry
     * @param exclude A user to exclude, if any
     */
    public void postToFollowers(List<Entry> entries, List<MFollower> followers, EntryType type, String exclude) {
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE, type.name());
            JSONArray arr = new JSONArray();
            for (Entry entry : entries) {
                // Single entry
                JSONObject jsonEntry = entryToJson(entry);
                arr.put(jsonEntry);
            }
            json.put(ENTRIES, arr);
        } catch(JSONException e) {
            Log.e(TAG, "json issue with post to followers", e);
            return;
        }
        Log.d(TAG, "sending json: " + json.toString());
        for (MFollower follower : followers) {
            DbFeed feed = mMusubi.getFeed(follower.feedUri);
            feed.postObj(new MemObj(ENTRIES_TYPE, json));
        }
    }
    
    /**
     * Let the people you follow know that you exist
     * @param feedUri Uri of the feed to reach those you follow
     * @param recipients List of String recipient identifiers
     * @param type EntryType of the followed content
     */
    public void sendHello(Uri feedUri, List<String> recipients, EntryType type) {
        JSONObject json = new JSONObject();
        try {
            json.put(HELLO, true);
            json.put(TYPE, type.name());
            JSONArray to = new JSONArray();
            for (String recipient : recipients) {
                to.put(recipient);
            }
            json.put(TO, to);
        } catch (JSONException e) {
            Log.e(TAG, "json issue with hello post", e);
            return;
        }
        Log.d(TAG, "sending json: " + json.toString());
        DbFeed feed = mMusubi.getFeed(feedUri);
        feed.postObj(new MemObj(HELLO_TYPE, json));
    }
    
    /**
     * Handle an incoming Musubi object
     * @param obj DbObj for this app
     */
    public void handleIncomingObj(DbObj obj) {
        JSONObject json = obj.getJson();
        if (json == null) {
            Log.d(TAG, "no json");
            return;
        }
        if (json.has(HELLO) && json.has(TYPE) && json.has(TO) && !obj.getSender().isOwned()) {
            JSONArray to = json.optJSONArray(TO);
            for (int i = 0; i < to.length(); i++) {
                String rec = to.optString(i);
                DbIdentity recipient = mMusubi.userForGlobalId(
                        obj.getContainingFeed().getUri(), rec);
                if (recipient != null && recipient.isOwned()) {
                    // only handle one request to me, since there's only 1 me
                    handleHello(obj, EntryType.valueOf(json.optString(TYPE)));
                    break;
                }
            }
        } else if (json.has(TYPE) && json.has(ENTRIES)) {
            Uri feedUri = obj.getContainingFeed().getUri();
            MFeed feed = mFeedManager.getFeed(feedUri);
            // only process entries sent to my own feeds for the type
            if (feed != null && feed.type.ordinal() == json.optInt(TYPE)) {
                handleEntries(obj, json.optJSONArray(ENTRIES), EntryType.valueOf(json.optString(TYPE)));
            }
        }
    }
    
    /**
     * Handle a "hello" DbObj by sending back my and my friends' content
     * @param obj The original DbObj
     * @param type EntryType of the content
     */
    private void handleHello(DbObj obj, EntryType type) {
        // Add the follower if not already known
        mFollowerManager.ensureFollower(
                type, obj.getSender().getId(), obj.getContainingFeed().getUri());
        
        // Populate the entries (mine and my friends' only)
        List<MEntry> dbEntries = mEntryManager.getOneLevelEntries(type);
        List<Entry> entries = new LinkedList<Entry>();
        for (MEntry dbEntry : dbEntries) { // TODO: this is inefficient
            if (dbEntry.followingCount > 0L) {
                entries.add(dbEntryToEntry(dbEntry));
            }
        }
        
        // Add a single follower to notify (the hello sender)
        List<MFollower> followers = new LinkedList<MFollower>();
        SQLiteDatabase db = mDatabaseSource.getWritableDatabase();
        try {
            db.beginTransaction();
            MFollower follower = mFollowerManager.getFollower(type, obj.getSender().getId());
            if (follower != null) {
                followers.add(follower);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        
        postToFollowers(entries, followers, type, null);
    }
    
    /**
     * Handle a collection of entries sent here. This includes notifying my followers as well.
     * @param obj The original DbObj
     * @param dbEntries JSONArray of entries
     * @param type EntryType of the content
     */
    private void handleEntries(DbObj obj, JSONArray dbEntries, EntryType type) {
        // TODO: finish this
        List<Entry> entries = new LinkedList<Entry>();
        try {
            for (int i = 0; i < dbEntries.length(); i++) {
                entries.add(jsonToEntry(dbEntries.getJSONObject(i)));
            }
        } catch (JSONException e) {
            Log.e(TAG, "json entry parse error", e);
            return;
        }
        
        // get the followers
        MFeed typeFeed = mFeedManager.getFeed(type);
        Set<String> following = new HashSet<String>();
        if (typeFeed != null) {
            Set<MFollowing> dbFollowings = mFollowingManager.getFollowing(typeFeed.id);
            for (MFollowing dbFollowing : dbFollowings) {
                following.add(dbFollowing.userId);
            }
        }
        
        // ensure that we track all owners
        List<Entry> outgoing = new LinkedList<Entry>();
        SQLiteDatabase db = mDatabaseSource.getWritableDatabase();
        for (Entry entry : entries) {
            for (String owner : entry.owners) {
                mUserEntryManager.ensureUserEntry(
                        mEntryManager, type, entry.name, false, owner, following.contains(owner));
            }
            
            // add to report list if owned by direct following
            if (entry.owned) {
                MEntry dbEntry = null;
                try {
                    // unfortunately need a transaction because of weak consistency
                    db.beginTransaction();
                    dbEntry = mEntryManager.getEntry(type, entry.name);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                // next level ownership must be relative to me
                entry.owned = (dbEntry != null) ? dbEntry.owned : false;
                outgoing.add(entry);
            }
        }
        
        mContext.getContentResolver().notifyChange(App.URI_NEW_CONTENT, null);
        
        // notify all followers
        postToFollowers(outgoing, mFollowerManager.getFollowers(type), type, obj.getSender().getId());
    }
    
    /**
     * Converts an Entry object into a sendable JSONObject
     * @param entry Entry object with fields completed
     * @return JSONObject
     */
    private JSONObject entryToJson(Entry entry) {
        JSONObject result = new JSONObject();
        try {
            result.put(TYPE, entry.type.ordinal());
            result.put(NAME, entry.name);
            result.put(OWNED, entry.owned);
            JSONArray owners = new JSONArray();
            for (String owner : entry.owners) {
                owners.put(owner);
            }
            result.put(OWNERS, owners);
            if (entry.extra != null) {
                result.put(EXTRA, Base64.encodeToString(entry.extra, 0));
            }
        } catch (JSONException e) {
            Log.e(TAG, "error creating json", e);
        }
        return result;
    }
    
    /**
     * Converts a received JSONObject into an Entry
     * @param json JSONObject received
     * @return Entry object
     */
    private Entry jsonToEntry(JSONObject json) {
        try {
            Entry result = new Entry();
            result.type = EntryType.values()[json.getInt(TYPE)];
            result.name = json.getString(NAME);
            result.owned = json.getBoolean(OWNED);
            JSONArray owners = json.getJSONArray(OWNERS);
            result.owners = new HashSet<String>();
            for (int i = 0; i < owners.length(); i++) {
                result.owners.add(owners.getString(i));
            }
            if (json.has(EXTRA)) {
                result.extra = Base64.decode(json.getString(EXTRA), 0);
            }
            return result;
        } catch (JSONException e) {
            Log.w(TAG, "error parsing json");
            return null;
        }
    }
    
    /**
     * Convert a database-backed MEntry into an Entry
     * @param dbEntry MEntry object
     * @return Entry object
     */
    public Entry dbEntryToEntry(MEntry dbEntry) {
        Entry entry = new Entry();
        entry.type = dbEntry.type;
        entry.name = dbEntry.name;
        entry.owned = dbEntry.owned;
        if (dbEntry.thumbnail != null) {
            entry.extra = dbEntry.thumbnail;
        }
        List<MUserEntry> userEntries = mUserEntryManager.getUserEntries(dbEntry.id);
        entry.owners = new HashSet<String>();
        for (MUserEntry userEntry : userEntries) {
            entry.owners.add(userEntry.userId);
        }
        return entry;
    }
}
