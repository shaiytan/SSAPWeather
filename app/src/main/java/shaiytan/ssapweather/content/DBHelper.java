package shaiytan.ssapweather.content;

import android.content.Context;
import android.database.sqlite.*;


/**
 * Created by Shaiytan on 23.06.2017.
 */

public class DBHelper extends SQLiteOpenHelper
{
    private static final String CREATE_TABLE_SCRIPT = "CREATE TABLE weather(" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "datetime INTEGER NOT NULL," +
            "description TEXT NOT NULL," +
            "temperature REAL NOT NULL," +
            "humidity REAL NOT NULL," +
            "icon TEXT NOT NULL);";
    private static final String DBNAME="forecast.db";
    private static final int DBVERSION=1;
    private static final String DROP_TABLE_SCRIPT = "DROP TABLE IF EXISTS weather;";
    public DBHelper(Context context) {
            super(context, DBNAME, null, DBVERSION);
        }
    @Override
    public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_SCRIPT);
        }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_SCRIPT);
        onCreate(db);
    }
}
