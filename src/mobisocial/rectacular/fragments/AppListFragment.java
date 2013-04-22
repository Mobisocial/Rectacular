package mobisocial.rectacular.fragments;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import mobisocial.rectacular.App;
import mobisocial.rectacular.R;
import mobisocial.rectacular.model.EntryManager;
import mobisocial.rectacular.model.MEntry;
import mobisocial.rectacular.model.MEntry.EntryType;
import mobisocial.rectacular.util.SimpleCursorLoader;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class AppListFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<Cursor>, OnItemClickListener {
    // TODO: this can be generalized
    private static final String TAG = "AppListFragment";
    
    private static final long DEFAULT_LIST_LIMIT = 10;
    
    private FragmentActivity mActivity;
    
    private EntryManager mEntryManager;
    
    private EntryListCursorAdapter mEntries;
    private ListView mEntryView;
    
    private Map<View, String> mMetadata;
    private Map<View, String> mNames;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = getActivity();
        
        mMetadata = new HashMap<View, String>();
        mNames = new HashMap<View, String>();
        
        View v = inflater.inflate(R.layout.top_list, container, false);
        
        mEntryManager = new EntryManager(App.getDatabaseSource(mActivity));
        
        mEntryView = (ListView)v.findViewById(R.id.entry_list);
        mEntryView.setOnItemClickListener(this);
        
        mActivity.getSupportLoaderManager().initLoader(0, null, this);
        mActivity.getContentResolver().registerContentObserver(App.URI_NEW_CONTENT, false, new ContentObserver(new Handler(getActivity().getMainLooper())) {
            @Override
            public void onChange(boolean selfChange) {
                mActivity.getSupportLoaderManager().restartLoader(0, null, AppListFragment.this);
            }
        });
        
        return v;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mActivity.getSupportLoaderManager().restartLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        EntryType type = EntryType.App;
        return new EntryLoader(mActivity, type);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (mEntries == null) {
            mEntries = new EntryListCursorAdapter(mActivity, cursor);
        } else {
            mEntries.changeCursor(cursor);
        }
        mEntryView.setAdapter(mEntries);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
    }
    
    private class EntryListCursorAdapter extends CursorAdapter {
        public EntryListCursorAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }
        
        @Override
        public View newView(Context context, Cursor c, ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(context);
            View v = inflater.inflate(R.layout.top_list_item, parent, false);
            bindView(v, context, c);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView nameText = (TextView)view.findViewById(R.id.entry_title);
            TextView detailsText = (TextView)view.findViewById(R.id.entry_subtitle);
            @SuppressWarnings("unused")
            ImageView icon = (ImageView)view.findViewById(R.id.entry_image);
            
            MEntry entry = mEntryManager.fillInStandardFields(cursor);
            nameText.setText(entry.name);
            long total = entry.count - (entry.owned ? 1 : 0);
            long friendTotal = entry.followingCount - (entry.owned ? 1 : 0);
            detailsText.setText("Used by " + total + " others (following " + friendTotal + ")");
            
            if (entry.metadata != null) {
                mMetadata.put(view, entry.metadata);
            }
            mNames.put(view, entry.name);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // If we have metadata, use that directly, else search by name
        String metadata = mMetadata.get(view);
        String name = mNames.get(view);
        Uri marketUri = null;
        if (metadata != null) {
            marketUri = Uri.parse("market://details?id=" + metadata);
        } else {
            try {
                String query = URLEncoder.encode(name, "UTF-8");
                marketUri = Uri.parse("market://search?q=" + query);
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "bad encoding", e);
                return;
            }
        }
        Intent market = new Intent(Intent.ACTION_VIEW, marketUri);
        mActivity.startActivity(market);
    }
    
    public static class EntryLoader extends SimpleCursorLoader {
        private EntryManager mEntryManager;
        private EntryType mType;
        
        public EntryLoader(Context context, EntryType type) {
            super(context);
            SQLiteOpenHelper helper = App.getDatabaseSource(context);
            mEntryManager = new EntryManager(helper);
            mType = type;
        }

        @Override
        public Cursor loadInBackground() {
            Cursor c = mEntryManager.getDiscoveredTopEntriesCursor(mType, DEFAULT_LIST_LIMIT);
            c.setNotificationUri(getContext().getContentResolver(), App.URI_NEW_CONTENT);
            return c;
        }
    }

}
