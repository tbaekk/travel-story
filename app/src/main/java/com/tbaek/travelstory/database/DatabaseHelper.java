package com.tbaek.travelstory.database;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    // Database Version
    private static final int DATABASE_VERSION = 2;

    // Database Name
    private static final String DATABASE_NAME = "database_name";

    // Table Names
    private static final String DB_TABLE = "table_image";

    // column names
    private static final String COLUMN_ID    = "id";
    private static final String COLUMN_LAT   = "latitude";
    private static final String COLUMN_LNG   = "longitude";
    private static final String COLUMN_IMAGE = "image_data";

    // Table create statement
    private static final String CREATE_TABLE = "CREATE TABLE " + DB_TABLE + "("+
            COLUMN_ID  + " TEXT,"     +
            COLUMN_LAT + " DOUBLE," +
            COLUMN_LNG + " DOUBLE," +
            COLUMN_IMAGE + " BLOB);";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // creating table
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS " + DB_TABLE);

        // create new table
        onCreate(db);
    }

    public void clearAllImages() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM "+ DB_TABLE);
    }

    public Cursor getAllImages() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery( "SELECT * FROM " + DB_TABLE, null );
        return res;
    }

    public void addEntry(String id, Double lat, Double lng, byte[] image) throws SQLiteException {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_ID,    id);
        cv.put(COLUMN_LAT,   lat);
        cv.put(COLUMN_LNG,   lng);
        cv.put(COLUMN_IMAGE, image);
        db.insert(DB_TABLE, null, cv );
    }

    public void deleteEntry(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("delete from "+ DB_TABLE +" where id='" + id + "'");
    }

}
