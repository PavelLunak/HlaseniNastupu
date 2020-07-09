package cz.stodva.hlaseninastupu.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;


public class DbHelper extends SQLiteOpenHelper {

    public static final String TABLE_REPORTS = "reports_list";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TYPE = "message_type";
    public static final String COLUMN_TIME = "report_time";
    public static final String COLUMN_SENT = "sending_time";
    public static final String COLUMN_DELIVERED = "delivery_time";
    public static final String COLUMN_REQUEST_CODE = "request_code";
    public static final String COLUMN_ERROR_REQUEST_CODE = "error_request_code";
    public static final String COLUMN_IS_ERROR_ALERT = "error_alert";
    public static final String COLUMN_MESSAGE = "message";

    private static final String DATABASE_NAME = "reports.db";
    private static final int DATABASE_VERSION = 5;

    private static final String TABLE_DEVICES_CREATE = "CREATE TABLE "
            + TABLE_REPORTS
            + " ( " + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + COLUMN_TYPE + " TINYINT, "
            + COLUMN_TIME + " VARCHAR (100), "
            + COLUMN_SENT + " VARCHAR (100), "
            + COLUMN_DELIVERED + " VARCHAR (100), "
            + COLUMN_REQUEST_CODE + " VARCHAR (100), "
            + COLUMN_ERROR_REQUEST_CODE + " VARCHAR (100), "
            + COLUMN_IS_ERROR_ALERT + " VARCHAR (1), "
            + COLUMN_MESSAGE + " VARCHAR (200) "
            + ");";

    public Context context;


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(TABLE_DEVICES_CREATE);
    }

    public static boolean doesDatabaseExist(Context context, String dbName) {
        File dbFile = context.getDatabasePath(dbName);
        return dbFile.exists();
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_REPORTS);
        onCreate(db);
    }
}
