package basilisk.stockexchangeterminal.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class DatabaseAdapter {
    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DatabaseAdapter(Context context){
        dbHelper = new DatabaseHelper(context.getApplicationContext());
    }

    public DatabaseAdapter open(){
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close(){
        dbHelper.close();
    }

    private Cursor getAllEntries(){
        String[] columns = new String[] {
                AlertPrice.COL_ID,
                AlertPrice.COL_ACTIVE,
                AlertPrice.COL_ALERT,
                AlertPrice.COL_CURR_PAIR,
                AlertPrice.COL_LOWER,
                AlertPrice.COL_HIGHER
        };
        return  database.query(AlertPrice.TABLE, columns, null, null, null, null, null);
    }

    public List<AlertPrice> getListAlertPrice(){
        ArrayList<AlertPrice> listAlertPrice = new ArrayList<>();
        Cursor cursor = getAllEntries();
        if(cursor.moveToFirst()){
            do{
                int id = cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ID));
                boolean active = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ACTIVE))) == 1;
                boolean alert = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ALERT))) == 1;
                String currencyPair = cursor.getString(cursor.getColumnIndex(AlertPrice.COL_CURR_PAIR));
                float lower = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_LOWER));
                float higher = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_HIGHER));
                listAlertPrice.add(new AlertPrice(id, active, alert, currencyPair, lower, higher));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return listAlertPrice;
    }

    private Cursor getEntries(String selection, String[] selectionArgs){
        String[] columns = new String[] {
                AlertPrice.COL_ID,
                AlertPrice.COL_ACTIVE,
                AlertPrice.COL_ALERT,
                AlertPrice.COL_CURR_PAIR,
                AlertPrice.COL_LOWER,
                AlertPrice.COL_HIGHER
        };
        return  database.query(AlertPrice.TABLE, columns, selection, selectionArgs, null, null, null);
    }

    public List<AlertPrice> getListAlertPrice(String selection, String[] selectionArgs){
        ArrayList<AlertPrice> listAlertPrice = new ArrayList<>();
        Cursor cursor = getEntries(selection, selectionArgs);
        if(cursor.moveToFirst()){
            do{
                int id = cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ID));
                boolean active = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ACTIVE))) == 1;
                boolean alert = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ALERT))) == 1;
                String currencyPair = cursor.getString(cursor.getColumnIndex(AlertPrice.COL_CURR_PAIR));
                float lower = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_LOWER));
                float higher = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_HIGHER));
                listAlertPrice.add(new AlertPrice(id, active, alert, currencyPair, lower, higher));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return listAlertPrice;
    }
    public AlertPrice getAlertPrice(long id){
        AlertPrice alertPrice = null;
        String query = String.format("SELECT * FROM %s WHERE %s=?", AlertPrice.TABLE, AlertPrice.COL_ID);
        Cursor cursor = database.rawQuery(query, new String[]{ String.valueOf(id)});
        if(cursor.moveToFirst()){
            boolean active = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ACTIVE))) == 1;
            boolean alert = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ALERT))) == 1;
            String currencyPair = cursor.getString(cursor.getColumnIndex(AlertPrice.COL_CURR_PAIR));
            float lower = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_LOWER));
            float higher = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_HIGHER));
            alertPrice = new AlertPrice(id, active, alert, currencyPair, lower, higher);
        }
        cursor.close();
        return alertPrice;
    }

    public AlertPrice getAlertPrice(String currencyPair){
        AlertPrice alertPrice = null;
        String query = String.format("SELECT * FROM %s WHERE %s=?", AlertPrice.TABLE, AlertPrice.COL_CURR_PAIR);
        Cursor cursor = database.rawQuery(query, new String[]{currencyPair});
        if(cursor.moveToFirst()){
            long id = cursor.getLong(cursor.getColumnIndex(AlertPrice.COL_ID));
            boolean active = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ACTIVE))) == 1;
            boolean alert = (cursor.getInt(cursor.getColumnIndex(AlertPrice.COL_ALERT))) == 1;
            float lower = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_LOWER));
            float higher = cursor.getFloat(cursor.getColumnIndex(AlertPrice.COL_HIGHER));
            alertPrice = new AlertPrice(id, active, alert, currencyPair, lower, higher);
        }
        cursor.close();
        return alertPrice;
    }
    public long delete(long id){
        String whereClause = "_id = ?";
        String[] whereArgs = new String[]{String.valueOf(id)};
        return database.delete(AlertPrice.TABLE, whereClause, whereArgs);
    }

    public long insert(AlertPrice alertPrice){
        ContentValues cv = new ContentValues();
        cv.put(AlertPrice.COL_ACTIVE, alertPrice.isActive());
        cv.put(AlertPrice.COL_ALERT, alertPrice.isAlert());
        cv.put(AlertPrice.COL_CURR_PAIR, alertPrice.getCurrencyPair());
        cv.put(AlertPrice.COL_LOWER, alertPrice.getLower());
        cv.put(AlertPrice.COL_HIGHER, alertPrice.getHigher());
        return database.insert(AlertPrice.TABLE, null, cv);
    }

    public long update(AlertPrice alertPrice){
        String whereClause = AlertPrice.COL_ID + "=" + String.valueOf(alertPrice.getId());
        ContentValues cv = new ContentValues();
        cv.put(AlertPrice.COL_ACTIVE, alertPrice.isActive());
        cv.put(AlertPrice.COL_ALERT, alertPrice.isAlert());
        cv.put(AlertPrice.COL_CURR_PAIR, alertPrice.getCurrencyPair());
        cv.put(AlertPrice.COL_LOWER, alertPrice.getLower());
        cv.put(AlertPrice.COL_HIGHER, alertPrice.getHigher());
        return database.update(AlertPrice.TABLE, cv, whereClause, null);
    }

    public long getCount(){
        return DatabaseUtils.queryNumEntries(database, AlertPrice.TABLE);
    }
}
