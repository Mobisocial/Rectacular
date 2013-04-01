package mobisocial.rectacular.social;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import mobisocial.rectacular.App;
import mobisocial.rectacular.model.EntryManager;
import mobisocial.rectacular.model.FollowerManager;
import mobisocial.rectacular.model.MEntry;
import mobisocial.rectacular.model.MEntry.EntryType;
import mobisocial.rectacular.model.MFollower;
import mobisocial.socialkit.musubi.DbFeed;
import mobisocial.socialkit.musubi.DbIdentity;
import mobisocial.socialkit.musubi.DbObj;
import mobisocial.socialkit.musubi.Musubi;
import mobisocial.socialkit.obj.MemObj;

public class SocialClient {
    private static final String TAG = "SocialClient";
    
    private static final String TYPE = "type";
    private static final String ENTRIES = "entries";
    private static final String NAME = "name";
    private static final String OWNED = "owned";
    private static final String COUNT = "count";
    
    private static final String HELLO = "hello";
    private static final String TO = "to";
    
    private final Musubi mMusubi;
    private final SQLiteOpenHelper mDatabaseSource;
    private final EntryManager mEntryManager;
    private final FollowerManager mFollowerManager;
    
    public SocialClient(Musubi musubi, Context context) {
        mMusubi = musubi;
        mDatabaseSource = App.getDatabaseSource(context);
        mEntryManager = new EntryManager(mDatabaseSource);
        mFollowerManager = new FollowerManager(mDatabaseSource);
    }
    
    /**
     * Sends a collection of entries to entry followers
     * @param entries Collection of entries, and whether or not they're owned
     * @param counts Number of times each entry appears
     * @param followers The followers to contact
     * @param type Type of the entry
     */
    public void postToFollowers(
            Map<String, Boolean> entries, Map<String, Long> counts,
            List<MFollower> followers, EntryType type) {
        JSONObject json = new JSONObject();
        try {
            json.put(TYPE, type.name());
            JSONArray arr = new JSONArray();
            for (Map.Entry<String, Boolean> entry : entries.entrySet()) {
                // Single entry
                JSONObject jsonEntry = new JSONObject();
                jsonEntry.put(NAME, entry.getKey());
                jsonEntry.put(OWNED, entry.getValue());
                jsonEntry.put(COUNT, counts.get(entry.getKey()));
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
            feed.postObj(new MemObj("rectacular", json));
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
        feed.postObj(new MemObj("rectacular", json));
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
            handleEntries(obj, json.optJSONArray(ENTRIES), EntryType.valueOf(json.optString(TYPE)));
        }
    }
    
    /**
     * Handle a "hello" DbObj
     * @param obj The original DbObj
     * @param type EntryType of the content
     */
    private void handleHello(DbObj obj, EntryType type) {
        mFollowerManager.ensureFollower(
                type, obj.getSender().getId(), obj.getContainingFeed().getUri());
        List<MEntry> entries = mEntryManager.getEntries(type);
        Map<String, Boolean> owned = new HashMap<String, Boolean>();
        Map<String, Long> counts = new HashMap<String, Long>();
        for (MEntry entry : entries) {
            if (entry.owned || entry.followingCount > 0L) {
                owned.put(entry.name, entry.owned);
                long extra = entry.owned ? 1 : 0;
                // friend count + me
                counts.put(entry.name, entry.followingCount + extra);
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
    }
    
    /**
     * Handle a collection of entries sent here. This includes notifying the user's followers as well.
     * @param obj The original DbObj
     * @param entries JSONArray of entries
     * @param type EntryType of the content
     */
    private void handleEntries(DbObj obj, JSONArray entries, EntryType type) {
        // TODO: implement
    }
}
