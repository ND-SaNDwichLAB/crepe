package edu.nd.crepe.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.xpath.operations.Bool;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class DatabaseManager extends SQLiteOpenHelper {

    private SQLiteDatabase db;
    private static DatabaseManager instance = null;

    public static final String COLLECTOR_TABLE = "collector";
    public static final String COLUMN_COLLECTOR_ID = "collectorId";
    public static final String COLUMN_CREATOR_USER_ID = "creatorUserId";
    public static final String COLUMN_APP_NAME = "appName";
    public static final String COLUMN_APP_PACKAGE = "appPackage";
    public static final String COLUMN_DESCRIPTION = "description";
    public static final String COLUMN_COLLECTOR_START_TIME = "collectorStartTime";
    public static final String COLUMN_COLLECTOR_END_TIME = "collectorEndTime";
    public static final String COLUMN_MODE = "mode";
    public static final String COLUMN_TARGET_SERVER_IP = "targetServerIp";
    public static final String COLUMN_COLLECTOR_STATUS = "collectorStatus";

    public static final String USER_TABLE = "user";
    public static final String COLUMN_USER_ID = "userId";
    public static final String COLUMN_USER_NAME = "userName";
    private static final String COLUMN_USER_PHOTO_URL = "userPhotoUrl";

    public static final String DATA_TABLE = "data";
    public static final String COLUMN_DATA_ID = "dataId";
    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_DATA_CONTENT = "dataContent";
    private static final String COLUMN_USER_TIME_CREATED = "userTimeCreated";
    private static final String COLUMN_USER_LAST_HEARTBEAT = "userLastHeartbeat";
    private static final String COLUMN_USER_COLLECTORS = "userCollectors";

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
            "            " + COLUMN_DESCRIPTION + " VARCHAR, " +
            "            " + COLUMN_MODE + " VARCHAR, " +
            "            " + COLUMN_TARGET_SERVER_IP + " VARCHAR, " +
            "            " + COLUMN_COLLECTOR_START_TIME + " BIGINT, " +
            "            " + COLUMN_COLLECTOR_END_TIME + " BIGINT, " +
            "            " + COLUMN_COLLECTOR_STATUS + " VARCHAR)";

    private final String createUserTableStatement = "CREATE TABLE IF NOT EXISTS " + USER_TABLE + " (" + COLUMN_USER_ID + " VARCHAR PRIMARY KEY, " +
            "            " + COLUMN_USER_NAME + " VARCHAR, " +
            "            " + COLUMN_USER_PHOTO_URL + " VARCHAR, " +
            "            " + COLUMN_USER_TIME_CREATED + " BIGINT, " +
            "            " + COLUMN_USER_LAST_HEARTBEAT + " BIGINT, " +
            "            " + COLUMN_USER_COLLECTORS + " VARCHAR)";

    private final String createDatafieldTableStatement = "CREATE TABLE IF NOT EXISTS " + DATAFIELD_TABLE + " (" + COLUMN_DATAFIELD_ID + " VARCHAR PRIMARY KEY, " +
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
    private DatabaseManager(@Nullable Context context) {
        super(context, "crepe.db", null, 1);
        this.db = this.getWritableDatabase();
    }

    // will be called the first time a database is accessed.
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // generate new tables
        sqLiteDatabase.execSQL(createCollectorTableStatement);
        sqLiteDatabase.execSQL(createUserTableStatement);
        sqLiteDatabase.execSQL(createDatafieldTableStatement);
        sqLiteDatabase.execSQL(createDataTableStatement);
        System.out.println("create table success");
    }

    // called if the database version number changes. prevents the app from crashing
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    // singleton pattern
    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseManager(context);
        }
        return instance;
    }

    // a method to add one collector to the local database
    public Boolean addOneCollector(Collector collector) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_COLLECTOR_ID, collector.getCollectorId());
        cv.put(COLUMN_CREATOR_USER_ID, collector.getCreatorUserId());
        cv.put(COLUMN_DESCRIPTION, collector.getDescription());
        cv.put(COLUMN_APP_NAME, collector.getAppName());
        cv.put(COLUMN_APP_PACKAGE, collector.getAppPackage());
        cv.put(COLUMN_MODE, collector.getMode());
        cv.put(COLUMN_TARGET_SERVER_IP, collector.getTargetServerIp());
        cv.put(COLUMN_COLLECTOR_START_TIME, collector.getCollectorStartTime());
        cv.put(COLUMN_COLLECTOR_END_TIME, collector.getCollectorEndTime());
        cv.put(COLUMN_COLLECTOR_STATUS, collector.getCollectorStatus());
        long insert = db.insert(COLLECTOR_TABLE, null, cv);
        if (insert == -1) {
            return false;
        }
        return true;
    }

    // Warning: this method will directly remove the collector from the database. The preferred method: use updateCollectorStatus to set the collector status to "deleted",
    // so that we still have a copy of the collector in the database.
    public void removeCollectorById(String collectorId) {
        // the result equals to the number of entries being deleted in the database
        int result = db.delete(COLLECTOR_TABLE, "collectorId = " + collectorId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " collectors with id " + collectorId);
        } else {
            Log.i("database", "remove collector error, current collectors: " + getAllCollectors().toString());
        }
    }

    // a method to get all collectors in the database
    public List<Collector> getAllCollectors() {
        List<Collector> collectorList = new ArrayList<>();

        String sqlString = "SELECT * FROM " + COLLECTOR_TABLE;

        Cursor cursor = db.rawQuery(sqlString, null);

        // if the cursor actually got something
        if (cursor.moveToFirst()) {
            do {
                // parse every info from cursor
                String collectorID = cursor.getString(0);
                String creatorUserID = cursor.getString(1);
                String appName = cursor.getString(2);
                String appPackage = cursor.getString(3);
                String description = cursor.getString(4);
                String mode = cursor.getString(5);
                String targetServerIP = cursor.getString(6);
                long collectorStartTime = cursor.getLong(7);
                long collectorEndTime = cursor.getLong(8);
                String collectorStatus = cursor.getString(9);
                Collector receivedCollector = new Collector(collectorID, creatorUserID, appName, appPackage, description, mode, collectorStartTime, collectorEndTime, collectorStatus);
                collectorList.add(receivedCollector);

            } while(cursor.moveToNext());
        }

        else {
            // do nothing since it's empty
            Log.i("", "The collector list is empty.");
        }
        cursor.close();
        return collectorList;
    }

    public Boolean addOneUser(User user) {
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_USER_ID, user.getUserId());
        cv.put(COLUMN_USER_NAME, user.getName());
        cv.put(COLUMN_USER_PHOTO_URL, user.getPhotoUrl());
        cv.put(COLUMN_USER_TIME_CREATED, user.getTimeCreated());
        cv.put(COLUMN_USER_LAST_HEARTBEAT, user.getLastHeartBeat());

        // convert the collectors list to json string
        Gson gson = new Gson();
        String collectorsJson = gson.toJson(user.getCollectorsForCurrentUser());
        cv.put(COLUMN_USER_COLLECTORS, collectorsJson);

        long insert = db.insert(USER_TABLE, null, cv);
        return insert != -1;
    }

    public void removeCollectorForUser(Collector collector, User user) {
        // First, you remove the collector from database
        collector.setCollectorStatus(Collector.DELETED);
        updateCollectorStatus(collector);
        // second you remove the collector from the user's list
        ArrayList<String> updatedUserCollectors = user.getCollectorsForCurrentUser();
        updatedUserCollectors.remove(collector.getCollectorId());
        user.setCollectorsForCurrentUser(updatedUserCollectors);
        // third, you update the user to the database
        updateUser(user);
    }


    public Boolean checkIfUserExists(String userId) {

        String query = "SELECT * FROM " + USER_TABLE + " WHERE " + COLUMN_USER_ID + " = \'" + userId + "\'";
        Cursor cursor = db.rawQuery(query, null);
        int cursorCount = cursor.getCount();

        cursor.close();

        if (cursorCount <= 0) {
            return false;
        } else {
            return true;
        }
    }

    public Boolean addOneUser(String userId) {
        ContentValues cv = new ContentValues();

        long timeCreated = Calendar.getInstance().getTimeInMillis();

        cv.put(COLUMN_USER_ID, userId);
        // use empty string for current user name, will change later using function updateUserName
        cv.put(COLUMN_USER_NAME, "");
        cv.put(COLUMN_USER_PHOTO_URL, "");
        cv.put(COLUMN_USER_TIME_CREATED, timeCreated);
        // use the current time for last edited
        cv.put(COLUMN_USER_LAST_HEARTBEAT, timeCreated);
        cv.put(COLUMN_USER_COLLECTORS, "");

        long insert = db.insert(USER_TABLE, null, cv);
        return insert != -1;
    }

    public String getUsername(String userId) {
        String query = "SELECT " + COLUMN_USER_NAME + " from " + USER_TABLE + " where " + COLUMN_USER_ID + "= \'" + userId + "\'";

        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int usernameColumnIndex = cursor.getColumnIndex(COLUMN_USER_NAME);
        String username = cursor.getString(usernameColumnIndex);

        cursor.close();
        return username;
    }

    public ArrayList<String> getUserCollectors(String userId) {
        String query = "SELECT " + COLUMN_USER_COLLECTORS + " from " + USER_TABLE + " where " + COLUMN_USER_ID + "= \'" + userId + "\'";

        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int collectorsColumnIndex = cursor.getColumnIndex(COLUMN_USER_COLLECTORS);
        String collectorsJson = cursor.getString(collectorsColumnIndex);

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        ArrayList<String> collectorIds = gson.fromJson(collectorsJson, type);

        cursor.close();
        return collectorIds;
    }

    // Use this function to update the database when the user set their name in the left panel
    public Boolean updateUserName(String userId, String username) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USER_NAME, username);
        // rows represent the number of rows updated
        int rows = db.update(USER_TABLE, cv,  "userId = ?" , new String[] {userId} );

        return (rows > 0);
    }

    public void removeUserById(String userId) {
        // the result equals to the number of entries being deleted

        int result = db.delete(USER_TABLE, "userId = " + userId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " users with id " + userId);
        } else {
            Log.i("database", "remove user error, current users: " + getAllUsers().toString());
        }
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        String getAllUsersQuery = "SELECT * FROM " + USER_TABLE;

        Cursor cursor = db.rawQuery(getAllUsersQuery, null);

        if(cursor.moveToFirst()) {
            do {
                String userId = cursor.getString(0);
                String userName = cursor.getString(1);
                String PhotoUrl = cursor.getString(2);
                long userTimeCreated = cursor.getLong(3);
                long userTimeLastEdited = cursor.getLong(4);
                String userCollectorsJson = cursor.getString(5);

                // convert the json string to collectors list
                Gson gson = new Gson();
                Type type = new TypeToken<ArrayList<String>>(){}.getType();
                ArrayList<String> userCollectors = gson.fromJson(userCollectorsJson, type);

                User receivedUser = new User(userId, userName, PhotoUrl, userTimeCreated, userTimeLastEdited, userCollectors);

                userList.add(receivedUser);

            } while(cursor.moveToNext());
        } else {
            Log.i("", "The user list is empty.");
        }

        cursor.close();
        return userList;
    }

    public Boolean addData(Data data) {
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATA_ID, data.getDataId());
        cv.put(COLUMN_DATAFIELD_ID, data.getDatafieldId());
        cv.put(COLUMN_USER_ID, data.getUserId());
        cv.put(COLUMN_TIMESTAMP, data.getTimestamp());
        cv.put(COLUMN_DATA_CONTENT, data.getDataContent());

        long result = db.insert(DATA_TABLE, null, cv);
        return result != -1;
    }

    public void removeDataById(String dataId) {
        // the result equals to the number of entries being deleted

        int result = db.delete(DATA_TABLE, "dataId = " + dataId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " data entries with id " + dataId);
        } else {
            Log.i("database", "remove data entry by data id error, current data entries: " + getAllData().toString());
        }
    }

    public void removeDataByDatafieldId(String datafieldId) {
        // the result equals to the number of entries being deleted

        int result = db.delete(DATA_TABLE, "datafieldId = " + datafieldId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " data entries from datafield " + datafieldId);
        } else {
            Log.i("database", "remove data entry by datafield Id error, current data entries: " + getAllData().toString());
        }
    }

    public void removeDataByUserId(String userId) {
        // the result equals to the number of entries being deleted

        int result = db.delete(DATA_TABLE, "userId = " + userId, null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " data entries from user " + userId);
        } else {
            Log.i("database", "remove data entry by user id error, current data entries: " + getAllData().toString());
        }
    }

    public List<Data> getAllData() {
        List<Data> dataList = new ArrayList<>();
        String getAllDataQuery = "SELECT * FROM " + DATA_TABLE;

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String dataId = cursor.getString(0);
                String datafieldId = cursor.getString(1);
                String userId = cursor.getString(2);
                Long timestamp = cursor.getLong(3);
                String dataContent = cursor.getString(4);

                Data receivedData = new Data(dataId, datafieldId, userId, timestamp, dataContent);

                dataList.add(receivedData);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "The data list is empty.");
        }

        cursor.close();

        return dataList;
    }

    public List<Data> getDataForCollector(Collector collector) {
        List<Data> dataList = new ArrayList<>();
        String getAllDataQuery = "SELECT * FROM " + DATA_TABLE + " WHERE " + COLUMN_COLLECTOR_ID + " = \'" + collector.getCollectorId() + "\';";

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String dataId = cursor.getString(0);
                String datafieldId = cursor.getString(1);
                String userId = cursor.getString(2);
                Long timestamp = cursor.getLong(3);
                String dataContent = cursor.getString(4);

                Data receivedData = new Data(dataId, datafieldId, userId, timestamp, dataContent);

                dataList.add(receivedData);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "Cannot find data for the specified collector ID. Try another collector instead?");
        }

        cursor.close();

        return dataList;
    }


    public Boolean addOneDatafield(Datafield datafield) {
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATAFIELD_ID, datafield.getDatafieldId());
        cv.put(COLUMN_COLLECTOR_ID, datafield.getCollectorId());
        cv.put(COLUMN_GRAPH_QUERY, datafield.getGraphQuery());
        cv.put(COLUMN_DATAFIELD_NAME, datafield.getName());
        cv.put(COLUMN_DATAFIELD_TIME_CREATED, datafield.getTimeCreated());
        cv.put(COLUMN_DATAFIELD_TIME_LAST_EDITED, datafield.getTimelastEdited());
        cv.put(COLUMN_DATAFIELD_IS_DEMONSTRATED, datafield.getDemonstrated());

        // catch exception of the insert operation
        try {
            long result = db.insert(DATAFIELD_TABLE, null, cv);
            return result != -1;
        } catch (Exception e) {
            Log.i("database", "add one datafield error: " + e.getMessage());
            return false;
        }

    }

    public void removeDatafieldById(String datafieldId) {
        // the result equals to the number of entries being deleted

        int result = db.delete(DATAFIELD_TABLE, "datafieldId = \"" + datafieldId + "\"", null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " datafield with id " + datafieldId);
        } else {
            Log.i("database", "remove datafield by id error, current datafields: " + getAllDatafields().toString());
        }
    }

    // NOTE: This method is not used in the app, because once a collector is set as "deleted", the datafields will not be queried anymore.
    public void removeDatafieldByCollectorId(String collectorId) {
        // the result equals to the number of entries being deleted

        int result = db.delete(DATAFIELD_TABLE, "collectorId = \'" + collectorId + "\'", null);
        if(result > 0) {
            Log.i("database", "successfully deleted " + result + " datafield from collector " + collectorId);
        } else {
            Log.i("database", "remove datafield by collector id error, current datafields: " + getAllDatafields().toString());
        }
    }

    public List<Datafield> getAllDatafields() {
        List<Datafield> dataList = new ArrayList<>();
        String getAllDataQuery = "SELECT * FROM " + DATAFIELD_TABLE;

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String datafieldId = cursor.getString(0);
                String collectorId = cursor.getString(1);
                String graphQuery = cursor.getString(2);
                String name = cursor.getString(3);
                Long timeCreated = cursor.getLong(4);
                Long timeLastEdited = cursor.getLong(5);
                Boolean isDemonstrated = cursor.getInt(6) != 0;
                Datafield receivedDatafield = new Datafield(datafieldId, collectorId, graphQuery, name, timeCreated, timeLastEdited, isDemonstrated);

                dataList.add(receivedDatafield);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "The datafield list is empty.");
        }

        cursor.close();

        return dataList;
    }

    public List<Datafield> getAllDatafieldsForCollector(Collector collector) {
        List<Datafield> dataList = new ArrayList<>();
        String getAllDataQuery = "SELECT * FROM " + DATAFIELD_TABLE + " WHERE " + COLUMN_COLLECTOR_ID + " = \'" + collector.getCollectorId() + "\';";

        Cursor cursor = db.rawQuery(getAllDataQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String datafieldId = cursor.getString(0);
                String collectorId = cursor.getString(1);
                String graphQuery = cursor.getString(2);
                String name = cursor.getString(3);
                Long timeCreated = cursor.getLong(4);
                Long timeLastEdited = cursor.getLong(5);
                Boolean isDemonstrated = cursor.getInt(6) != 0;
                Datafield receivedDatafield = new Datafield(datafieldId, collectorId, graphQuery, name, timeCreated, timeLastEdited, isDemonstrated);

                dataList.add(receivedDatafield);

            } while (cursor.moveToNext());
        } else {
            Log.i("", "Cannot find datafield for the specified collector ID. Try another collector instead?");
        }

        cursor.close();
        return dataList;
    }

    // update statuses for all collectors to database;
    // this is not the most efficient, but there won't be that many collectors anyways
    public void updateCollectorStatus(Collector collector) {
        String updateStatement = "UPDATE " + COLLECTOR_TABLE + " SET " + COLUMN_COLLECTOR_STATUS + " =\'" + collector.getCollectorStatus() + "\' WHERE " + COLUMN_COLLECTOR_ID + "=\'" + collector.getCollectorId() + "\'";
        Cursor c = db.rawQuery(updateStatement, null);
        c.moveToFirst();
    }


    public List<Collector> getActiveCollectors() {
        List<Collector> collectorList = new ArrayList<>();
        String getActiveCollectorsQuery = "SELECT * FROM " + COLLECTOR_TABLE + " WHERE " + COLUMN_COLLECTOR_STATUS + " = \'active\';";

        Cursor cursor = db.rawQuery(getActiveCollectorsQuery, null);

        if (cursor.moveToFirst()) {
            do {
                // parse every info from cursor
                String collectorID = cursor.getString(0);
                String creatorUserID = cursor.getString(1);
                String appName = cursor.getString(2);
                String appPackage = cursor.getString(3);
                String description = cursor.getString(4);
                String mode = cursor.getString(5);
                String targetServerIP = cursor.getString(6);
                long collectorStartTime = cursor.getLong(7);
                long collectorEndTime = cursor.getLong(8);
                String collectorStatus = cursor.getString(9);
                Collector receivedCollector = new Collector(collectorID, creatorUserID, appName, appPackage, description, mode, collectorStartTime, collectorEndTime, collectorStatus);
                collectorList.add(receivedCollector);

            } while(cursor.moveToNext());
        }

        else {
            // do nothing since it's empty
            Log.i("", "The collector list is empty.");
        }
        cursor.close();
        return collectorList;
    }

    // query to get one collector by id
    public Collector getCollectorById(String collectorId) {
        String getCollectorByIdQuery = "SELECT * FROM " + COLLECTOR_TABLE + " WHERE " + COLUMN_COLLECTOR_ID + " = \'" + collectorId + "\';";

        Cursor cursor = db.rawQuery(getCollectorByIdQuery, null);

        if (cursor.moveToFirst()) {
            // parse every info from cursor
            String collectorID = cursor.getString(0);
            String creatorUserID = cursor.getString(1);
            String appName = cursor.getString(2);
            String appPackage = cursor.getString(3);
            String description = cursor.getString(4);
            String mode = cursor.getString(5);
            String targetServerIP = cursor.getString(6);
            long collectorStartTime = cursor.getLong(7);
            long collectorEndTime = cursor.getLong(8);
            String collectorStatus = cursor.getString(9);
            Collector receivedCollector = new Collector(collectorID, creatorUserID, appName, appPackage, description, mode, collectorStartTime, collectorEndTime, collectorStatus);
            cursor.close();
            return receivedCollector;
        }

        else {
            // do nothing since it's empty
            Log.i("", "The collector list is empty.");
            cursor.close();
            return null;
        }
    }

    // check if the user is already participating in the collector
    public boolean userParticipationStatusForCollector(String userId, String collectorId) {
        ArrayList<String> userCollectors = getUserCollectors(userId);
        if (userCollectors.contains(collectorId)) {
            return true;
        } else {
            return false;
        }
    }

    public void closeDatabase() {
        if (db != null) {
            db.close();
        }
    }

    public void updateCollector(Collector updatedCollector) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_COLLECTOR_ID, updatedCollector.getCollectorId());
        cv.put(COLUMN_CREATOR_USER_ID, updatedCollector.getCreatorUserId());
        cv.put(COLUMN_DESCRIPTION, updatedCollector.getDescription());
        cv.put(COLUMN_APP_NAME, updatedCollector.getAppName());
        cv.put(COLUMN_APP_PACKAGE, updatedCollector.getAppPackage());
        cv.put(COLUMN_MODE, updatedCollector.getMode());
        cv.put(COLUMN_TARGET_SERVER_IP, updatedCollector.getTargetServerIp());
        cv.put(COLUMN_COLLECTOR_START_TIME, updatedCollector.getCollectorStartTime());
        cv.put(COLUMN_COLLECTOR_END_TIME, updatedCollector.getCollectorEndTime());
        cv.put(COLUMN_COLLECTOR_STATUS, updatedCollector.getCollectorStatus());

        int rows = db.update(COLLECTOR_TABLE, cv, "collectorId = ?", new String[]{updatedCollector.getCollectorId()});

        if (rows > 0) {
            Log.i("database", "successfully updated " + rows + " collectors with id " + updatedCollector.getCollectorId());
        } else {
            Log.i("database", "update collector error, current collectors: " + getAllCollectors().toString());
        }
    }

    public void updateDatafield(Datafield updatedDatafield) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_DATAFIELD_ID, updatedDatafield.getDatafieldId());
        cv.put(COLUMN_COLLECTOR_ID, updatedDatafield.getCollectorId());
        cv.put(COLUMN_GRAPH_QUERY, updatedDatafield.getGraphQuery());
        cv.put(COLUMN_DATAFIELD_NAME, updatedDatafield.getName());
        cv.put(COLUMN_DATAFIELD_TIME_CREATED, updatedDatafield.getTimeCreated());
        cv.put(COLUMN_DATAFIELD_TIME_LAST_EDITED, updatedDatafield.getTimelastEdited());
        cv.put(COLUMN_DATAFIELD_IS_DEMONSTRATED, updatedDatafield.getDemonstrated());

        int rows = db.update(DATAFIELD_TABLE, cv, "datafieldId = ?", new String[]{updatedDatafield.getDatafieldId()});

        if (rows > 0) {
            Log.i("database", "successfully updated " + rows + " datafields with id " + updatedDatafield.getDatafieldId());
        } else {
            Log.i("database", "update datafield error, current datafields: " + getAllDatafields().toString());
        }
    }

    public void addDatafield(Datafield addedDatafield) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_DATAFIELD_ID, addedDatafield.getDatafieldId());
        cv.put(COLUMN_COLLECTOR_ID, addedDatafield.getCollectorId());
        cv.put(COLUMN_GRAPH_QUERY, addedDatafield.getGraphQuery());
        cv.put(COLUMN_DATAFIELD_NAME, addedDatafield.getName());
        cv.put(COLUMN_DATAFIELD_TIME_CREATED, addedDatafield.getTimeCreated());
        cv.put(COLUMN_DATAFIELD_TIME_LAST_EDITED, addedDatafield.getTimelastEdited());
        cv.put(COLUMN_DATAFIELD_IS_DEMONSTRATED, addedDatafield.getDemonstrated());

        long result = db.insert(DATAFIELD_TABLE, null, cv);
        if (result != -1) {
            Log.i("database", "successfully added datafield with id " + addedDatafield.getDatafieldId());
        } else {
            Log.i("database", "add datafield error, current datafields: " + getAllDatafields().toString());
        }
    }

    public void updateUser(User currentUser) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USER_ID, currentUser.getUserId());
        cv.put(COLUMN_USER_NAME, currentUser.getName());
        cv.put(COLUMN_USER_PHOTO_URL, currentUser.getPhotoUrl());
        cv.put(COLUMN_USER_TIME_CREATED, currentUser.getTimeCreated());
        cv.put(COLUMN_USER_LAST_HEARTBEAT, currentUser.getLastHeartBeat());

        // convert the collectors list to json string
        Gson gson = new Gson();
        String collectorsJson = gson.toJson(currentUser.getCollectorsForCurrentUser());
        cv.put(COLUMN_USER_COLLECTORS, collectorsJson);

        int rows = db.update(USER_TABLE, cv, "userId = ?", new String[]{currentUser.getUserId()});

        if (rows > 0) {
            Log.i("database", "successfully updated " + rows + " users with id " + currentUser.getUserId() + " and userCollectors: " + currentUser.getCollectorsForCurrentUser().toString());
        } else {
            Log.i("database", "update user error, current users: " + getAllUsers().toString());
        }
    }
}





