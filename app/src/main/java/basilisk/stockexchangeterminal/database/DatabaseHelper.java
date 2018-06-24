package basilisk.stockexchangeterminal.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "appstore.db"; // название бд
    private static final int SCHEMA = 1; // версия базы данных

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, SCHEMA);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(
                "CREATE TABLE " + AlertPrice.TABLE + "(" +
                        AlertPrice.COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                        AlertPrice.COL_ACTIVE +" INTEGER DEFAULT 0," +
                        AlertPrice.COL_ALERT +" INTEGER DEFAULT 0," +
                        AlertPrice.COL_CURR_PAIR +" TEXT," +
                        AlertPrice.COL_LOWER +" REAL," +
                        AlertPrice.COL_HIGHER +" REAL" +
                        ")"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + AlertPrice.TABLE);
        onCreate(sqLiteDatabase);
    }
}
