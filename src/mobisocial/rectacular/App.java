package mobisocial.rectacular;

import mobisocial.rectacular.model.DBHelper;
import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteOpenHelper;

public class App extends Application {
    private SQLiteOpenHelper mDatabaseSource;
    
    public static SQLiteOpenHelper getDatabaseSource(Context c) {
        Context appAsContext = c.getApplicationContext();
        return ((App)appAsContext).getDatabaseSource();
    }
    
    public synchronized SQLiteOpenHelper getDatabaseSource() {
        if (mDatabaseSource == null) {
            mDatabaseSource = new DBHelper(getApplicationContext());
        }
        return mDatabaseSource;
    }
}
