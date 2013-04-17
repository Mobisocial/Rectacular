package mobisocial.rectacular.fragments;

import mobisocial.rectacular.App;
import mobisocial.rectacular.R;
import mobisocial.rectacular.model.EntryManager;
import mobisocial.rectacular.model.MEntry;
import mobisocial.rectacular.model.MEntry.EntryType;
import mobisocial.rectacular.util.SimpleCursorLoader;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
    
    private static final long DEFAULT_LIST_LIMIT = 10;
    
    private FragmentActivity mActivity;
    
    private EntryManager mEntryManager;
    
    private EntryListCursorAdapter mEntries;
    private ListView mEntryView;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mActivity = getActivity();
        
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
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
        // TODO Auto-generated method stub
        
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
