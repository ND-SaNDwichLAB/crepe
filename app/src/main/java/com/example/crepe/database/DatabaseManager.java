package com.example.crepe.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {

    public static final String COLLECTOR_TABLE = "collector";
    public static final String COLUMN_COLLECTOR_ID = "collectorId";
    public static final String COLUMN_CREATOR_USER_ID = "creatorUserId";
    public static final String COLUMN_APP_NAME = "appName";
    public static final String COLUMN_APP_PACKAGE = "appPackage";
    public static final String COLUMN_NAME = "name";
    public static final String COLUMN_COLLECTOR_START_TIME = "collectorStartTime";
    public static final String COLUMN_COLLECTOR_END_TIME = "collectorEndTime";
    public static final String COLUMN_COLLECTOR_GRAPH_QUERY = "collectorGraphQuery";
    public static final String COLUMN_COLLECTOR_APP_DATA_FIELDS = "collectorAppDataFields";
    public static final String COLUMN_MODE = "mode";
    public static final String COLUMN_TARGET_SERVER_IP = "targetServerIp";
    public static final String COLUMN_COLLECTOR_STATUS = "collectorStatus";

    public static final String USER_TABLE = "usertable";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_USER_NAME = "userName";

    public static final String DATA_TABLE = "data";
    public static final String COLUMN_DATA_ID = "dataId";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_DATA_CONTENT = "dataContent";
    private static final String COLUMN_USER_TIME_CREATED = "userTimeCreated";
    private static final String COLUMN_USER_LAST_TIME_EDITED = "userTimeLastEdited";

    private static final String DATAFIELD_TABLE = "datafield";
    private static final String COLUMN_DATAFIELD_ID = "datafieldId";
    private static final String COLUMN_GRAPH_QUERY = "graphQuery";
    private static final String COLUMN_DATAFIELD_NAME = "datafieldName";
    private static final String COLUMN_DATAFIELD_TIME_CREATED = "datafieldTimeCreated";
    private static final String COLUMN_DATAFIELD_TIME_LAST_EDITED = "datafieldTimeLastEdited";
    private static final String COLUMN_DATAFIELD_IS_DEMONSTRATED = "datafieldIsDemonstrated";


    private static final List<String> tableList = new ArrayList<>(Arrays.asList(COLLECTOR_TABLE, USER_TABLE, DATA_TABLE, DATAFIELD_TABLE));

    // Create table statements
    private final String createCollectorTableStatement = "CREATE TABLE IF NOT EXISTS " + COLLECTOR_TABLE + " (" + COLUMN_COLLECTOR_ID + " VARCHAR PRIMARY KEY, " +
            "            " + COLUMN_CREATOR_USER_ID + " VARCHAR, " +
            "            " + COLUMN_APP_NAME + " VARCHAR, " +
            "            " + COLUMN_APP_PACKAGE + " VARCHAR, " +
            "            " + COLUMN_NAME + " VARCHAR, " +
            "            " + COLUMN_MODE + " VARCHAR, " +
            "            " + COLUMN_TARGET_SERVER_IP + " VARCHAR, " +
            "            " + COLUMN_COLLECTOR_START_TIME + " BIGINT, " +
            "            " + COLUMN_COLLECTOR_END_TIME + " BIGINT, " +
            "            " + COLUMN_COLLECTOR_GRAPH_QUERY + " VARCHAR, " +
            "            " + COLUMN_COLLECTOR_APP_DATA_FIELDS + " VARCHAR, " +
            "            " + COLUMN_COLLECTOR_STATUS + " VARCHAR)";

    private final String createUserTableStatement = "CREATE TABLE IF NOT EXISTS " + USER_TABLE + " (" + COLUMN_USER_ID + " VARCHAR PRIMARY KEY, " +
            "            " + COLUMN_USER_NAME + " VARCHAR, " +
            "            " + COLUMN_USER_TIME_CREATED + " BIGINT, " +
            "            " + COLUMN_USER_LAST_TIME_EDITED + " BIGINT);";

    private final String createDataFieldTableStatement = "CREATE TABLE IF NOT EXISTS " + DATAFIELD_TABLE + " (" + COLUMN_DATAFIELD_ID + " VARCHAR PRIMARY KEY, " +
            "            " + COLUMN_COLLECTOR_ID + " VARCHAR, " +
            "            " + COLUMN_GRAPH_QUERY + " VARCHAR, " +
            "            " + COLUMN_DATAFIELD_NAME + " VARCHAR, " +
            "            " + COLUMN_DATAFIELD_TIME_CREATED + " BIGINT, " +
            "            " + COLUMN_DATAFIELD_TIME_LAST_EDITED + " BIGINT, " +
            "            " + COLUMN_DATAFIELD_IS_DEMONSTRATED + " BOOLEAN, " +
            "            " + "FOREIGN KEY(" + COLUMN_COLLECTOR_ID + ") REFERENCES " + COLLECTOR_TABLE + "(" + COLUMN_COLLECTOR_ID + "));";

    private final String createDataTableStatement = "CREATE TABLE IF NOT EXISTS " + DATA_TABLE + " (" + COLUMN_DATA_ID + " VARCHAR PRIMARY KEY, " +
            "            " + COLUMN_DATAFIELD_ID + " VARCHAR, " +
            "            " + COLUMN_USER_ID + " VARCHAR, " +
            "            " + COLUMN_TIMESTAMP + " BIGINT, " +
            "            " + COLUMN_DATA_CONTENT + " VARCHAR, " +
            "            " + "FOREIGN KEY(" + COLUMN_DATAFIELD_ID + ") REFERENCES " + DATAFIELD_TABLE + "(" + COLUMN_DATAFIELD_ID + "), " +
            "            " + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + USER_TABLE + "(" + COLUMN_USER_ID + "));" ;


    // constructor
    public DatabaseManager(@Nullable Context context) {
        super(context, "crepe.db", null, 1);
    }

    // will be called the first time a database is accessed.
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // generate new tables
        sqLiteDatabase.execSQL(createCollectorTableStatement);
        sqLiteDatabase.execSQL(createUserTableStatement);
        sqLiteDatabase.execSQL(createDataFieldTableStatement);
        sqLiteDatabase.execSQL(createDataTableStatement);
        System.out.println("create table success");
    }

    // called if the database version number changes. prevents the app from crashing
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    // clear all 4 tables in the database
    public void clearDatabase(Context c) {
        SQLiteDatabase db = this.getWritableDatabase();

        for(String tableName: tableList) {
            db.delete(tableName, "1", null);
        }
        Toast.makeText(c, "Clear database success!", Toast.LENGTH_SHORT).show();
        db.close();
    }

    public Boolean addOneCollector(Collector collector) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_COLLECTOR_ID, collector.getCollectorId());
        cv.put(COLUMN_CREATOR_USER_ID, collector.getCreatorUserId());
        cv.put(COLUMN_NAME, collector.getDescription());
        cv.put(COLUMN_APP_NAME, collector.getAppName());
        cv.put(COLUMN_APP_PACKAGE, collector.getAppPackage());
        cv.put(COLUMN_MODE, collector.getMode());
        cv.put(COLUMN_TARGET_SERVER_IP, collector.getTargetServerIp());
        cv.put(COLUMN_COLLECTOR_START_TIME, collector.getCollectorStartTime());
        cv.put(COLUMN_COLLECTOR_END_TIME, collector.getCollectorEndTime());
        cv.put(COLUMN_COLLECTOR_STATUS, collector.getCollectorStatus());
        cv.put(COLUMN_COLLECTOR_APP_DATA_FIELDS,  collector.getDataFieldsToJson());
        long insert = db.insert(COLLECTOR_TABLE, null, cv);
        if (insert == -1) {
            return false;
        }
        db.close();
        return true;
    }

    public void removeCollectorById(String collectorId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // the result equals to the number of entries being deleted

        int result = db.delete(COLLECTOR_TABLE, "collectorId = " + collectorId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " collectors with id " + collectorId);
        } else {
            Log.i("database", "remove collector error, current collectors: " + getAllCollectors().toString());
        }
        db.close();
    }

    // a method to get all collectors in the database
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
                String appPackage = cursor.getString(3);
                String name = cursor.getString(4);
                String mode = cursor.getString(5);
                String targetServerIP = cursor.getString(6);
                long collectorStartTime = cursor.getLong(7);
                long collectorEndTime = cursor.getLong(8);
                String collectorGraphQuery = cursor.getString(9);
                String collectorAppDataFields = cursor.getString(10);
                String collectorStatus = cursor.getString(11);
                List<Pair<String, String>> dataFields = stringToListOfPairs(collectorAppDataFields);
                Collector receivedCollector = new Collector(collectorID, creatorUserID, appName, appPackage, name, mode, collectorStartTime, collectorEndTime, dataFields, collectorStatus);
                collectorList.add(receivedCollector);

            } while(cursor.moveToNext());
        }

        else {
            // do nothing since it's empty
            Log.i("", "The collector list is empty.");
        }
        cursor.close();
        db.close();
        return collectorList;
    }

    public Boolean addOneUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_USER_ID, user.getUserId());
        cv.put(COLUMN_USER_NAME, user.getName());
        cv.put(COLUMN_USER_TIME_CREATED, user.getTimeCreated());
        cv.put(COLUMN_USER_LAST_TIME_EDITED, user.getTimeLastEdited());

        long insert = db.insert(USER_TABLE, null, cv);
        db.close();
        return insert != -1;
    }

    public Boolean checkIfUserExists(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_USER_ID + " = \"" + userId + "\"";
        Cursor cursor = db.rawQuery(query, null);
        int cursorCount = cursor.getCount();

        db.close();
        cursor.close();

        if (cursorCount <= 0) {
            return false;
        } else {
            return true;
        }
    }

    public Boolean addOneUser(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        long timeCreated = Calendar.getInstance().getTimeInMillis();

        cv.put(COLUMN_USER_ID, userId);
        // use empty string for current user name, will change later using function updateUserName
        cv.put(COLUMN_USER_NAME, "");
        cv.put(COLUMN_USER_TIME_CREATED, timeCreated);
        // use the current time for last edited
        cv.put(COLUMN_USER_LAST_TIME_EDITED, timeCreated);

        long insert = db.insert(USER_TABLE, null, cv);
        db.close();
        return insert != -1;
    }

    public String getUsername(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COLUMN_USER_NAME + " from " + USER_TABLE + " where " + COLUMN_USER_ID + "= \"" + userId + "\"";

        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int usernameColumnIndex = cursor.getColumnIndex(COLUMN_USER_NAME);
        String username = cursor.getString(usernameColumnIndex);

        db.close();
        cursor.close();
        return username;
    }

    // Use this function to update the database when the user set their name in the left panel
    public Boolean updateUserName(String userId, String username) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USER_NAME, username);
        // rows represent the number of rows updated
        int rows = db.update(USER_TABLE, cv,  "userId = ?" , new String[] {userId} );

        db.close();
        return (rows > 0);
    }

    public void removeUserById(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // the result equals to the number of entries being deleted

        int result = db.delete(USER_TABLE, "userId = " + userId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " users with id " + userId);
        } else {
            Log.i("database", "remove user error, current users: " + getAllUsers().toString());
        }
        db.close();
    }

    public List<User> getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<User> userList = new ArrayList<>();
        String getAllUsersQuery = "SELECT * FROM " + USER_TABLE;

        Cursor cursor = db.rawQuery(getAllUsersQuery, null);

        if(cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(0);
                String userName = cursor.getString(1);
                long userTimeCreated = cursor.getLong(2);
                long userTimelastEdited = cursor.getLong(3);

                User receivedUser = new User(userId, userName, userTimeCreated, userTimelastEdited);

                userList.add(receivedUser);

            } while(cursor.moveToNext());
        } else {
            Log.i("", "The user list is empty.");
        }

        cursor.close();
        db.close();
        return userList;
    }

    public Boolean addData(Data data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATA_ID, data.getDataId());
        cv.put(COLUMN_DATAFIELD_ID, data.getDataFieldId());
        cv.put(COLUMN_USER_ID, data.getUserId());
        cv.put(COLUMN_TIMESTAMP, data.getTimestamp());
        cv.put(COLUMN_DATA_CONTENT, data.getDataContent());

        long result = db.insert(DATA_TABLE, null, cv);
        db.close();
        return result != -1;
    }

    public void removeDataById(String dataId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // the result equals to the number of entries being deleted

        int result = db.delete(DATA_TABLE, "dataId = " + dataId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " data entries with id " + dataId);
        } else {
            Log.i("database", "remove data entry by data id error, current data entries: " + getAllData().toString());
        }
        db.close();
    }

    public void removeDataByDatafieldId(String datafieldId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // the result equals to the number of entries being deleted

        int result = db.delete(DATA_TABLE, "datafieldId = " + datafieldId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " data entries from datafield " + datafieldId);
        } else {
            Log.i("database", "remove data entry by datafield Id error, current data entries: " + getAllData().toString());
        }
        db.close();
    }

    public void removeDataByUserId(String userId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // the result equals to the number of entries being deleted

        int result = db.delete(DATA_TABLE, "userId = " + userId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " data entries from user " + userId);
        } else {
            Log.i("database", "remove data entry by user id error, current data entries: " + getAllData().toString());
        }
        db.close();
    }

    public List<Data> getAllData() {
        List<Data> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String getAllDataQuery = "SELECT * FROM " + DATA_TABLE;

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String dataId = cursor.getString(0);
                String dataFieldId = cursor.getString(1);
                String userId = cursor.getString(2);
                Long timestamp = cursor.getLong(3);
                String dataContent = cursor.getString(4);

                Data receivedData = new Data(dataId, dataFieldId, userId, timestamp, dataContent);

                dataList.add(receivedData);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "The data list is empty.");
        }

        db.close();
        cursor.close();

        return dataList;
    }

    public List<Data> getDataForCollector(Collector collector) {
        List<Data> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String getAllDataQuery = "SELECT * FROM " + DATA_TABLE + " WHERE " + COLUMN_COLLECTOR_ID + " = \"" + collector.getCollectorId() + "\";";

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String dataId = cursor.getString(0);
                String dataFieldId = cursor.getString(1);
                String userId = cursor.getString(2);
                Long timestamp = cursor.getLong(3);
                String dataContent = cursor.getString(4);

                Data receivedData = new Data(dataId, dataFieldId, userId, timestamp, dataContent);

                dataList.add(receivedData);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "Cannot find data for the specified collector ID. Try another collector instead?");
        }

        db.close();
        cursor.close();

        return dataList;
    }


    public Boolean addOneDataField(Datafield dataField) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATAFIELD_ID, dataField.getDataFieldId());
        cv.put(COLUMN_COLLECTOR_ID, dataField.getCollectorId());
        cv.put(COLUMN_GRAPH_QUERY, dataField.getGraphQuery());
        cv.put(COLUMN_DATAFIELD_NAME, dataField.getName());
        cv.put(COLUMN_DATAFIELD_TIME_CREATED, dataField.getTimeCreated());
        cv.put(COLUMN_DATAFIELD_TIME_LAST_EDITED, dataField.getTimelastEdited());
        cv.put(COLUMN_DATAFIELD_IS_DEMONSTRATED, dataField.getDemonstrated());

        // catch exception of the insert operation
        try {
            long result = db.insert(DATAFIELD_TABLE, null, cv);
            db.close();
            return result != -1;
        } catch (Exception e) {
            Log.i("database", "add one datafield error: " + e.getMessage());
            return false;
        }

    }

    public void removeDatafieldById(String datafieldId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // the result equals to the number of entries being deleted

        int result = db.delete(DATAFIELD_TABLE, "datafieldId = " + datafieldId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " datafield with id " + datafieldId);
        } else {
            Log.i("database", "remove datafield by id error, current datafields: " + getAllDatafields().toString());
        }
        db.close();
    }
    public void removeDatafieldByCollectorId(String collectorId) {
        SQLiteDatabase db = this.getWritableDatabase();
        // the result equals to the number of entries being deleted

        int result = db.delete(DATAFIELD_TABLE, "collectorId = " + collectorId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " datafield from collector " + collectorId);
        } else {
            Log.i("database", "remove datafield by collector id error, current datafields: " + getAllDatafields().toString());
        }
        db.close();
    }

    public List<Datafield> getAllDatafields() {
        List<Datafield> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String getAllDataQuery = "SELECT * FROM " + DATAFIELD_TABLE;

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String dataFieldId = cursor.getString(0);
                String collectorId = cursor.getString(1);
                String graphQuery = cursor.getString(2);
                String name = cursor.getString(3);
                Long timeCreated = cursor.getLong(4);
                Long timeLastEdited = cursor.getLong(5);
                Boolean isDemonstrated = cursor.getInt(6) != 0;
                Datafield receivedDatafield = new Datafield(dataFieldId, collectorId, graphQuery, name, timeCreated, timeLastEdited, isDemonstrated);

                dataList.add(receivedDatafield);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "The datafield list is empty.");
        }

        db.close();
        cursor.close();

        return dataList;
    }

    public List<Datafield> getDatafieldForCollector(Collector collector) {
        List<Datafield> dataList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String getAllDataQuery = "SELECT * FROM " + DATAFIELD_TABLE + " WHERE " + COLUMN_COLLECTOR_ID + " = \"" + collector.getCollectorId() + "\";";

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String dataFieldId = cursor.getString(0);
                String collectorId = cursor.getString(1);
                String graphQuery = cursor.getString(2);
                String name = cursor.getString(3);
                Long timeCreated = cursor.getLong(4);
                Long timeLastEdited = cursor.getLong(5);
                Boolean isDemonstrated = cursor.getInt(6) != 0;
                Datafield receivedDatafield = new Datafield(dataFieldId, collectorId, graphQuery, name, timeCreated, timeLastEdited, isDemonstrated);

                dataList.add(receivedDatafield);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "Cannot find datafield for the specified collector ID. Try another collector instead?");
        }

        db.close();
        cursor.close();
        return dataList;
    }

    // update statuses for all collectors to database;
    // yeah this is not the most efficient, but there won't be that many collectors anyways
    public void updateCollectorStatus(Collector collector) {
        String updateStatement = "UPDATE " + COLLECTOR_TABLE + " SET " + COLUMN_COLLECTOR_STATUS + " =\'" + collector.getCollectorStatus() + "\' WHERE " + COLUMN_COLLECTOR_ID + "=" + collector.getCollectorId();
        Cursor c = getWritableDatabase().rawQuery(updateStatement, null);
        c.moveToFirst();
        c.close();
    }

    public List<Pair<String,String>> stringToListOfPairs(String string) {
        List<Pair<String,String>> list = new ArrayList<>();
        string = string.substring(1, string.length()-1);
        String[] pairs = string.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            String key = keyValue[0];
            String value = keyValue[1];
            list.add(new Pair<>(key, value));
        }
        return list;

    }

    // a function that retrieve collectors that has active status
    public List<Collector> getActiveCollectors() {
        List<Collector> collectorList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String getActiveCollectorsQuery = "SELECT * FROM " + COLLECTOR_TABLE + " WHERE " + COLUMN_COLLECTOR_STATUS + " = \"active\";";

        Cursor cursor = db.rawQuery(getActiveCollectorsQuery, null);

        if (cursor.moveToFirst()) {
            do {
                // parse every info from cursor
                String collectorID = cursor.getString(0);
                String creatorUserID = cursor.getString(1);
                String appName = cursor.getString(2);
                String appPackage = cursor.getString(3);
                String name = cursor.getString(4);
                String mode = cursor.getString(5);
                String targetServerIP = cursor.getString(6);
                long collectorStartTime = cursor.getLong(7);
                long collectorEndTime = cursor.getLong(8);
                String collectorGraphQuery = cursor.getString(9);
                String collectorAppDataFields = cursor.getString(10);
                String collectorStatus = cursor.getString(11);
                List<Pair<String, String>> dataFields = stringToListOfPairs(collectorAppDataFields);
                Collector receivedCollector = new Collector(collectorID, creatorUserID, appName, appPackage, name, mode, collectorStartTime, collectorEndTime, dataFields, collectorStatus);
                collectorList.add(receivedCollector);

            } while(cursor.moveToNext());
        }

        else {
            // do nothing since it's empty
            Log.i("", "The collector list is empty.");
        }
        cursor.close();
        db.close();
        return collectorList;
    }
}





