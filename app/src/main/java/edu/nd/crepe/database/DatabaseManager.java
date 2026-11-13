package edu.nd.crepe.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Local SQLite storage for collectors, users, datafields and collected data.
 *
 * The manager is a process-wide singleton that is shared by the UI and by the accessibility
 * service, so it never caches a {@link SQLiteDatabase} handle: every operation asks the
 * {@link SQLiteOpenHelper} for the current one, which transparently re-opens the database if it
 * was closed. All queries are parameterized.
 */
public class DatabaseManager extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseManager";
    private static final String DATABASE_NAME = "crepe.db";
    private static final int DATABASE_VERSION = 1;

    private static DatabaseManager instance = null;

    private static final Type STRING_LIST_TYPE = new TypeToken<ArrayList<String>>() {}.getType();

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
            "            " + "FOREIGN KEY(" + COLUMN_USER_ID + ") REFERENCES " + USER_TABLE + "(" + COLUMN_USER_ID + "));";


    // constructor
    private DatabaseManager(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // will be called the first time a database is accessed.
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        // generate new tables
        sqLiteDatabase.execSQL(createCollectorTableStatement);
        sqLiteDatabase.execSQL(createUserTableStatement);
        sqLiteDatabase.execSQL(createDatafieldTableStatement);
        sqLiteDatabase.execSQL(createDataTableStatement);
        Log.i(TAG, "created tables");
    }

    // called if the database version number changes. Nothing to migrate yet (version 1 is the only schema).
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade from " + oldVersion + " to " + newVersion + ": no migration required");
    }

    // singleton pattern
    public static synchronized DatabaseManager getInstance(Context context) {
        if (instance == null) {
            // always hold the application context, never an activity
            Context appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
            instance = new DatabaseManager(appContext);
        }
        return instance;
    }

    /**
     * Always go through the helper so that a database that was closed is transparently re-opened.
     */
    private SQLiteDatabase db() {
        return getWritableDatabase();
    }

    // ---------------------------------------------------------------------------------------
    // Row <-> object helpers
    // ---------------------------------------------------------------------------------------

    @Nullable
    private static String getString(Cursor cursor, String column) {
        int index = cursor.getColumnIndexOrThrow(column);
        return cursor.isNull(index) ? null : cursor.getString(index);
    }

    private static long getLong(Cursor cursor, String column) {
        return cursor.getLong(cursor.getColumnIndexOrThrow(column));
    }

    private static Collector readCollector(Cursor cursor) {
        return new Collector(
                getString(cursor, COLUMN_COLLECTOR_ID),
                getString(cursor, COLUMN_CREATOR_USER_ID),
                getString(cursor, COLUMN_APP_NAME),
                getString(cursor, COLUMN_APP_PACKAGE),
                getString(cursor, COLUMN_DESCRIPTION),
                getString(cursor, COLUMN_MODE),
                getString(cursor, COLUMN_TARGET_SERVER_IP),
                getLong(cursor, COLUMN_COLLECTOR_START_TIME),
                getLong(cursor, COLUMN_COLLECTOR_END_TIME),
                getString(cursor, COLUMN_COLLECTOR_STATUS));
    }

    private static ContentValues collectorValues(Collector collector) {
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
        return cv;
    }

    private static ArrayList<String> parseCollectorIds(@Nullable String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<String> ids = new Gson().fromJson(json, STRING_LIST_TYPE);
            return ids != null ? ids : new ArrayList<String>();
        } catch (JsonParseException e) {
            Log.w(TAG, "Could not parse stored collector id list, treating it as empty", e);
            return new ArrayList<>();
        }
    }

    private static User readUser(Cursor cursor) {
        return new User(
                getString(cursor, COLUMN_USER_ID),
                getString(cursor, COLUMN_USER_NAME),
                getString(cursor, COLUMN_USER_PHOTO_URL),
                getLong(cursor, COLUMN_USER_TIME_CREATED),
                getLong(cursor, COLUMN_USER_LAST_HEARTBEAT),
                parseCollectorIds(getString(cursor, COLUMN_USER_COLLECTORS)));
    }

    private static ContentValues userValues(User user) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USER_ID, user.getUserId());
        cv.put(COLUMN_USER_NAME, user.getName());
        cv.put(COLUMN_USER_PHOTO_URL, user.getPhotoUrl());
        cv.put(COLUMN_USER_TIME_CREATED, user.getTimeCreated());
        cv.put(COLUMN_USER_LAST_HEARTBEAT, user.getLastHeartBeat());
        // convert the collectors list to json string
        cv.put(COLUMN_USER_COLLECTORS, new Gson().toJson(user.getCollectorsForCurrentUser()));
        return cv;
    }

    private static Data readData(Cursor cursor) {
        return new Data(
                getString(cursor, COLUMN_DATA_ID),
                getString(cursor, COLUMN_DATAFIELD_ID),
                getString(cursor, COLUMN_USER_ID),
                getLong(cursor, COLUMN_TIMESTAMP),
                getString(cursor, COLUMN_DATA_CONTENT));
    }

    private static Datafield readDatafield(Cursor cursor) {
        int demonstratedIndex = cursor.getColumnIndexOrThrow(COLUMN_DATAFIELD_IS_DEMONSTRATED);
        return new Datafield(
                getString(cursor, COLUMN_DATAFIELD_ID),
                getString(cursor, COLUMN_COLLECTOR_ID),
                getString(cursor, COLUMN_GRAPH_QUERY),
                getString(cursor, COLUMN_DATAFIELD_NAME),
                getLong(cursor, COLUMN_DATAFIELD_TIME_CREATED),
                getLong(cursor, COLUMN_DATAFIELD_TIME_LAST_EDITED),
                cursor.getInt(demonstratedIndex) != 0);
    }

    private static ContentValues datafieldValues(Datafield datafield) {
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_DATAFIELD_ID, datafield.getDatafieldId());
        cv.put(COLUMN_COLLECTOR_ID, datafield.getCollectorId());
        cv.put(COLUMN_GRAPH_QUERY, datafield.getGraphQuery());
        cv.put(COLUMN_DATAFIELD_NAME, datafield.getName());
        cv.put(COLUMN_DATAFIELD_TIME_CREATED, datafield.getTimeCreated());
        cv.put(COLUMN_DATAFIELD_TIME_LAST_EDITED, datafield.getTimelastEdited());
        cv.put(COLUMN_DATAFIELD_IS_DEMONSTRATED, datafield.getDemonstrated());
        return cv;
    }

    private List<Collector> queryCollectors(@Nullable String selection, @Nullable String[] selectionArgs) {
        List<Collector> collectorList = new ArrayList<>();
        try (Cursor cursor = db().query(COLLECTOR_TABLE, null, selection, selectionArgs, null, null, null)) {
            while (cursor.moveToNext()) {
                collectorList.add(readCollector(cursor));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to query collectors", e);
        }
        return collectorList;
    }

    private List<Datafield> queryDatafields(@Nullable String selection, @Nullable String[] selectionArgs) {
        List<Datafield> datafieldList = new ArrayList<>();
        try (Cursor cursor = db().query(DATAFIELD_TABLE, null, selection, selectionArgs, null, null, null)) {
            while (cursor.moveToNext()) {
                datafieldList.add(readDatafield(cursor));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to query datafields", e);
        }
        return datafieldList;
    }

    private int delete(String table, String column, @Nullable String value) {
        if (value == null) {
            Log.w(TAG, "Refusing to delete from " + table + " with a null " + column);
            return 0;
        }
        try {
            return db().delete(table, column + " = ?", new String[]{value});
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to delete from " + table + " where " + column + " = " + value, e);
            return 0;
        }
    }

    // ---------------------------------------------------------------------------------------
    // Collectors
    // ---------------------------------------------------------------------------------------

    // a method to add one collector to the local database
    public Boolean addOneCollector(Collector collector) {
        if (collector == null || collector.getCollectorId() == null) {
            Log.w(TAG, "Cannot add a collector without an id");
            return false;
        }
        try {
            return db().insert(COLLECTOR_TABLE, null, collectorValues(collector)) != -1;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to add collector " + collector.getCollectorId(), e);
            return false;
        }
    }

    // Warning: this method will directly remove the collector from the database. The preferred method: use updateCollectorStatus to set the collector status to "deleted",
    // so that we still have a copy of the collector in the database.
    public void removeCollectorById(String collectorId) {
        // the result equals to the number of entries being deleted in the database
        int result = delete(COLLECTOR_TABLE, COLUMN_COLLECTOR_ID, collectorId);
        if (result > 0) {
            Log.i(TAG, "successfully deleted " + result + " collectors with id " + collectorId);
        } else {
            Log.w(TAG, "remove collector error: no collector with id " + collectorId);
        }
    }

    // a method to get all collectors in the database
    public List<Collector> getAllCollectors() {
        return queryCollectors(null, null);
    }

    public List<Collector> getActiveCollectors() {
        return queryCollectors(COLUMN_COLLECTOR_STATUS + " = ?", new String[]{Collector.ACTIVE});
    }

    // query to get one collector by id, returns null if there is no such collector
    @Nullable
    public Collector getCollectorById(String collectorId) {
        if (collectorId == null) {
            return null;
        }
        List<Collector> matches = queryCollectors(COLUMN_COLLECTOR_ID + " = ?", new String[]{collectorId});
        return matches.isEmpty() ? null : matches.get(0);
    }

    // update the status of one collector in the database
    public void updateCollectorStatus(Collector collector) {
        if (collector == null || collector.getCollectorId() == null) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_COLLECTOR_STATUS, collector.getCollectorStatus());
        try {
            db().update(COLLECTOR_TABLE, cv, COLUMN_COLLECTOR_ID + " = ?", new String[]{collector.getCollectorId()});
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to update status of collector " + collector.getCollectorId(), e);
        }
    }

    public void updateCollector(Collector updatedCollector) {
        if (updatedCollector == null || updatedCollector.getCollectorId() == null) {
            return;
        }
        try {
            int rows = db().update(COLLECTOR_TABLE, collectorValues(updatedCollector), COLUMN_COLLECTOR_ID + " = ?", new String[]{updatedCollector.getCollectorId()});
            if (rows > 0) {
                Log.i(TAG, "successfully updated " + rows + " collectors with id " + updatedCollector.getCollectorId());
            } else {
                Log.w(TAG, "update collector error: no collector with id " + updatedCollector.getCollectorId());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to update collector " + updatedCollector.getCollectorId(), e);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Users
    // ---------------------------------------------------------------------------------------

    public Boolean addOneUser(User user) {
        if (user == null || user.getUserId() == null) {
            Log.w(TAG, "Cannot add a user without an id");
            return false;
        }
        try {
            return db().insert(USER_TABLE, null, userValues(user)) != -1;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to add user " + user.getUserId(), e);
            return false;
        }
    }

    public Boolean addOneUser(String userId) {
        if (userId == null) {
            return false;
        }
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

        try {
            return db().insert(USER_TABLE, null, cv) != -1;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to add user " + userId, e);
            return false;
        }
    }

    public void removeCollectorForUser(Collector collector, User user) {
        if (collector == null || user == null) {
            return;
        }
        // First, mark the collector as deleted in the database
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
        if (userId == null) {
            return false;
        }
        try (Cursor cursor = db().query(USER_TABLE, new String[]{COLUMN_USER_ID}, COLUMN_USER_ID + " = ?", new String[]{userId}, null, null, null)) {
            return cursor.getCount() > 0;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to check if user " + userId + " exists", e);
            return false;
        }
    }

    // returns null if there is no such user
    @Nullable
    public String getUsername(String userId) {
        if (userId == null) {
            return null;
        }
        try (Cursor cursor = db().query(USER_TABLE, new String[]{COLUMN_USER_NAME}, COLUMN_USER_ID + " = ?", new String[]{userId}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursor.isNull(0) ? null : cursor.getString(0);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to get the name of user " + userId, e);
        }
        return null;
    }

    // never returns null: an unknown user, or a user without collectors, yields an empty list
    @NonNull
    public ArrayList<String> getUserCollectors(String userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        try (Cursor cursor = db().query(USER_TABLE, new String[]{COLUMN_USER_COLLECTORS}, COLUMN_USER_ID + " = ?", new String[]{userId}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return parseCollectorIds(cursor.isNull(0) ? null : cursor.getString(0));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to get the collectors of user " + userId, e);
        }
        return new ArrayList<>();
    }

    // Use this function to update the database when the user set their name in the left panel
    public Boolean updateUserName(String userId, String username) {
        if (userId == null) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put(COLUMN_USER_NAME, username);
        try {
            // rows represent the number of rows updated
            int rows = db().update(USER_TABLE, cv, COLUMN_USER_ID + " = ?", new String[]{userId});
            return rows > 0;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to update the name of user " + userId, e);
            return false;
        }
    }

    public void removeUserById(String userId) {
        // the result equals to the number of entries being deleted
        int result = delete(USER_TABLE, COLUMN_USER_ID, userId);
        if (result > 0) {
            Log.i(TAG, "successfully deleted " + result + " users with id " + userId);
        } else {
            Log.w(TAG, "remove user error: no user with id " + userId);
        }
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<>();
        try (Cursor cursor = db().query(USER_TABLE, null, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                userList.add(readUser(cursor));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to query users", e);
        }
        return userList;
    }

    public void updateUser(User currentUser) {
        if (currentUser == null || currentUser.getUserId() == null) {
            return;
        }
        try {
            int rows = db().update(USER_TABLE, userValues(currentUser), COLUMN_USER_ID + " = ?", new String[]{currentUser.getUserId()});
            if (rows > 0) {
                Log.i(TAG, "successfully updated " + rows + " users with id " + currentUser.getUserId());
            } else {
                Log.w(TAG, "update user error: no user with id " + currentUser.getUserId());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to update user " + currentUser.getUserId(), e);
        }
    }

    // check if the user is already participating in the collector
    public boolean userParticipationStatusForCollector(String userId, String collectorId) {
        return getUserCollectors(userId).contains(collectorId);
    }

    // ---------------------------------------------------------------------------------------
    // Data
    // ---------------------------------------------------------------------------------------

    public Boolean addData(Data data) {
        if (data == null || data.getDataId() == null) {
            Log.w(TAG, "Cannot add data without an id");
            return false;
        }
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_DATA_ID, data.getDataId());
        cv.put(COLUMN_DATAFIELD_ID, data.getDatafieldId());
        cv.put(COLUMN_USER_ID, data.getUserId());
        cv.put(COLUMN_TIMESTAMP, data.getTimestamp());
        cv.put(COLUMN_DATA_CONTENT, data.getDataContent());

        try {
            return db().insert(DATA_TABLE, null, cv) != -1;
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to add data " + data.getDataId(), e);
            return false;
        }
    }

    public void removeDataById(String dataId) {
        // the result equals to the number of entries being deleted
        int result = delete(DATA_TABLE, COLUMN_DATA_ID, dataId);
        if (result > 0) {
            Log.i(TAG, "successfully deleted " + result + " data entries with id " + dataId);
        } else {
            Log.w(TAG, "remove data entry by data id error: no data with id " + dataId);
        }
    }

    public void removeDataByDatafieldId(String datafieldId) {
        // the result equals to the number of entries being deleted
        int result = delete(DATA_TABLE, COLUMN_DATAFIELD_ID, datafieldId);
        if (result > 0) {
            Log.i(TAG, "successfully deleted " + result + " data entries from datafield " + datafieldId);
        } else {
            Log.w(TAG, "remove data entry by datafield id error: no data for datafield " + datafieldId);
        }
    }

    public void removeDataByUserId(String userId) {
        // the result equals to the number of entries being deleted
        int result = delete(DATA_TABLE, COLUMN_USER_ID, userId);
        if (result > 0) {
            Log.i(TAG, "successfully deleted " + result + " data entries from user " + userId);
        } else {
            Log.w(TAG, "remove data entry by user id error: no data for user " + userId);
        }
    }

    public List<Data> getAllData() {
        List<Data> dataList = new ArrayList<>();
        try (Cursor cursor = db().query(DATA_TABLE, null, null, null, null, null, null)) {
            while (cursor.moveToNext()) {
                dataList.add(readData(cursor));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to query data", e);
        }
        return dataList;
    }

    // the data table has no collector id of its own, so go through the datafields of the collector
    public List<Data> getDataForCollector(Collector collector) {
        List<Data> dataList = new ArrayList<>();
        if (collector == null || collector.getCollectorId() == null) {
            return dataList;
        }
        String sql = "SELECT d.* FROM " + DATA_TABLE + " d INNER JOIN " + DATAFIELD_TABLE + " f ON d." + COLUMN_DATAFIELD_ID + " = f." + COLUMN_DATAFIELD_ID
                + " WHERE f." + COLUMN_COLLECTOR_ID + " = ?";
        try (Cursor cursor = db().rawQuery(sql, new String[]{collector.getCollectorId()})) {
            while (cursor.moveToNext()) {
                dataList.add(readData(cursor));
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to query data for collector " + collector.getCollectorId(), e);
        }
        return dataList;
    }

    // ---------------------------------------------------------------------------------------
    // Datafields
    // ---------------------------------------------------------------------------------------

    public Boolean addOneDatafield(Datafield datafield) {
        if (datafield == null || datafield.getDatafieldId() == null) {
            Log.w(TAG, "Cannot add a datafield without an id");
            return false;
        }
        try {
            return db().insert(DATAFIELD_TABLE, null, datafieldValues(datafield)) != -1;
        } catch (RuntimeException e) {
            Log.e(TAG, "add one datafield error: " + e.getMessage(), e);
            return false;
        }
    }

    public void addDatafield(Datafield addedDatafield) {
        if (addOneDatafield(addedDatafield)) {
            Log.i(TAG, "successfully added datafield with id " + addedDatafield.getDatafieldId());
        } else {
            Log.w(TAG, "add datafield error: could not add datafield " + (addedDatafield != null ? addedDatafield.getDatafieldId() : null));
        }
    }

    public void removeDatafieldById(String datafieldId) {
        // the result equals to the number of entries being deleted
        int result = delete(DATAFIELD_TABLE, COLUMN_DATAFIELD_ID, datafieldId);
        if (result > 0) {
            Log.i(TAG, "successfully deleted " + result + " datafield with id " + datafieldId);
        } else {
            Log.w(TAG, "remove datafield by id error: no datafield with id " + datafieldId);
        }
    }

    // NOTE: This method is not used in the app, because once a collector is set as "deleted", the datafields will not be queried anymore.
    public void removeDatafieldByCollectorId(String collectorId) {
        // the result equals to the number of entries being deleted
        int result = delete(DATAFIELD_TABLE, COLUMN_COLLECTOR_ID, collectorId);
        if (result > 0) {
            Log.i(TAG, "successfully deleted " + result + " datafield from collector " + collectorId);
        } else {
            Log.w(TAG, "remove datafield by collector id error: no datafield for collector " + collectorId);
        }
    }

    public List<Datafield> getAllDatafields() {
        return queryDatafields(null, null);
    }

    public List<Datafield> getAllDatafieldsForCollector(Collector collector) {
        if (collector == null || collector.getCollectorId() == null) {
            return new ArrayList<>();
        }
        return queryDatafields(COLUMN_COLLECTOR_ID + " = ?", new String[]{collector.getCollectorId()});
    }

    public void updateDatafield(Datafield updatedDatafield) {
        if (updatedDatafield == null || updatedDatafield.getDatafieldId() == null) {
            return;
        }
        try {
            int rows = db().update(DATAFIELD_TABLE, datafieldValues(updatedDatafield), COLUMN_DATAFIELD_ID + " = ?", new String[]{updatedDatafield.getDatafieldId()});
            if (rows > 0) {
                Log.i(TAG, "successfully updated " + rows + " datafields with id " + updatedDatafield.getDatafieldId());
            } else {
                Log.w(TAG, "update datafield error: no datafield with id " + updatedDatafield.getDatafieldId());
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Failed to update datafield " + updatedDatafield.getDatafieldId(), e);
        }
    }

    /**
     * Closes the underlying database. Do not call this from a single screen: the manager is shared
     * with the accessibility service, which keeps using it after the UI is gone. The database is
     * re-opened on demand by the next operation.
     */
    public void closeDatabase() {
        close();
    }
}
