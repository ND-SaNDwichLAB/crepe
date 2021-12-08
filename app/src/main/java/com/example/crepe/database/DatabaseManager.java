package com.example.crepe.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {

    public static final String COLLECTOR_TABLE = "collector";
    public static final String COLUMN_COLLECTOR_ID = "collectorID";
    public static final String COLUMN_CREATOR_USER_ID = "creatorUserID";
    public static final String COLUMN_APP_NAME = "appName";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_TIME_CREATED = "timeCreated";
    public static final String COLUMN_TIME_LAST_EDITED = "timeLastEdited";
    public static final String COLUMN_MODE = "mode";
    public static final String COLUMN_TARGET_SERVER_IP = "targetServerIP";

    public DatabaseManager(@Nullable Context context) {
        super(context, "crepe.db", null, 1);
    }

    // will be called the first time a database is accessed.
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // generate a new table
        String createTableStatement = "CREATE TABLE " + COLLECTOR_TABLE + " (" + COLUMN_COLLECTOR_ID + " VARCHAR PRIMARY KEY, " +
                "            " + COLUMN_CREATOR_USER_ID + " VARCHAR, " +
                "            " + COLUMN_APP_NAME + " VARCHAR, " +
                "            " + COLUMN_NAME + " VARCHAR, " +
                "            " + COLUMN_TIME_CREATED + " BIGINT NOT NULL, " +
                "            " + COLUMN_TIME_LAST_EDITED + " BIGINT, " +
                "            " + COLUMN_MODE + " VARCHAR, " +
                "            " + COLUMN_TARGET_SERVER_IP + " VARCHAR)";

        sqLiteDatabase.execSQL(createTableStatement);
    }

    // called if the database version number changes. prevents the app from crashing
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public Boolean addOne(Collector collector) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_COLLECTOR_ID, collector.getCollectorID());
        cv.put(COLUMN_CREATOR_USER_ID, collector.getCreatorUserID());
        cv.put(COLUMN_NAME, collector.getName());
        cv.put(COLUMN_APP_NAME, collector.getAppName());
        cv.put(COLUMN_MODE, collector.getMode());
        cv.put(COLUMN_TIME_CREATED, collector.getTimeCreated());
        cv.put(COLUMN_TIME_LAST_EDITED, collector.getTimeLastEdited());
        cv.put(COLUMN_TARGET_SERVER_IP, collector.getTargetServerIP());

        long insert = db.insert(COLLECTOR_TABLE, null, cv);

        if (insert == -1) {
            return false;
        }
        else {
            return true;
        }
    }

    public List<Collector> getAllCollectors() {
        List<Collector> collectorList = new ArrayList<>();

        String sqlString = "SELECT * FROM " + COLLECTOR_TABLE;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(sqlString, null);

        // if the cursor actually got something
        if (cursor.moveToFirst()) {
            do {
                // parse every info from cursor
                String collectorID = cursor.getString(0);
                String creatorUserID = cursor.getString(1);
                String appName = cursor.getString(2);
                String name = cursor.getString(3);
                long timeCreated = cursor.getLong(4);
                long timeLastEdited = cursor.getLong(5);
                String mode = cursor.getString(6);
                String targetServerIP = cursor.getString(7);

                Collector receivedCollector = new Collector(collectorID, creatorUserID, appName, name, timeCreated, timeLastEdited, mode, targetServerIP);
                collectorList.add(receivedCollector);

            } while(cursor.moveToNext());
        }

        else {
            // do nothing since it's empty
        }
        cursor.close();
        db.close();
        return collectorList;
    }
}
