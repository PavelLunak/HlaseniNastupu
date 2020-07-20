package cz.stodva.hlaseninastupu.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cz.stodva.hlaseninastupu.listeners.OnDatabaseClearedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemDeletedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsCountCheckedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnMaxIdCheckedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportAddedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.AppUtils;


public class DataSource implements AppConstants {

    private SQLiteDatabase database;
    Context context;
    private DbHelper dbHelper;


    public DataSource(Context context) {
        Log.d(LOG_TAG, "DataSource - CONSTRUCTOR");
        dbHelper = new DbHelper(context);
        this.context = context;
    }

    public void open() throws SQLException {
        if (database != null) {
            return;
        }

        try {
            database = dbHelper.getWritableDatabase();
        } catch (NullPointerException e) {
            e.printStackTrace();
            dbHelper = new DbHelper(context);
            database = dbHelper.getWritableDatabase();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        if (dbHelper != null) dbHelper.close();
        dbHelper = null;
    }

    public void clearTable(OnDatabaseClearedListener listener) {
        if (database == null) open();
        database.delete(DbHelper.TABLE_REPORTS, null, null);
        if (listener != null) listener.onDatabaseCleared();
    }

    public String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

    public void addReport(final Report report, final OnReportAddedListener listener) {
        Log.d(LOG_TAG, "DataSource - addReport(report: " + report.toString() + ")" + LOG_UNDERLINED);

        if (report == null) {
            if (listener != null) listener.onReportAdded(null);
            return;
        }

        open();
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_TYPE, report.getMessageType());
        values.put(DbHelper.COLUMN_TIME, String.valueOf(report.getTime()));
        values.put(DbHelper.COLUMN_SENT, String.valueOf(report.getSentTime()));
        values.put(DbHelper.COLUMN_DELIVERED, String.valueOf(report.getDeliveryTime()));
        values.put(DbHelper.COLUMN_REQUEST_CODE, String.valueOf(report.getAlarmRequestCode()));
        values.put(DbHelper.COLUMN_ERROR_REQUEST_CODE, String.valueOf(report.getRequestCodeForErrorAlarm()));
        values.put(DbHelper.COLUMN_IS_ERROR_ALERT, report.isErrorAlert() ? "1" : "0");
        values.put(DbHelper.COLUMN_MESSAGE, report.getMessage());
        values.put(DbHelper.COLUMN_IS_FAILED, report.isFailed() ? "1" : "0");
        values.put(DbHelper.COLUMN_IS_DELIVERED, report.isDelivered() ? "1" : "0");

        long insertId = database.insert(DbHelper.TABLE_REPORTS, null, values);

        final Cursor cursor = database.query(
                DbHelper.TABLE_REPORTS,
                null,
                DbHelper.COLUMN_ID + " = " + insertId,
                null,
                null,
                null,
                null);

        cursor.moveToFirst();
        final Report r = cursorToReport(cursor);
        if (listener != null) listener.onReportAdded(r);

        if (r != null) Log.d(LOG_TAG, LOG_TAB + "added report: " + r.toString());
        else Log.d(LOG_TAG, LOG_TAB + "NULL");

        cursor.close();
    }

    public void getReportById(int reportId, OnReportLoadedListener listener) {
        Log.d(LOG_TAG, "DataSource - getReportById(reportId: " + reportId + ")" + LOG_UNDERLINED);

        open();
        Report toReturn = null;
        String selectQuery = "SELECT  * FROM " + DbHelper.TABLE_REPORTS + " WHERE " + DbHelper.COLUMN_ID + " = '" + reportId + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) toReturn = cursorToReport(cursor);
        cursor.close();

        if (toReturn != null) Log.d(LOG_TAG, LOG_TAB + "report by ID: " + toReturn.toString());
        else Log.d(LOG_TAG, LOG_TAB + "NULL");

        if (listener != null) listener.onReportLoaded(toReturn);
    }

    public void getReportByMaxId(final OnReportLoadedListener listener) {
        Log.d(LOG_TAG, "DataSource - getReportByMaxId()" + LOG_UNDERLINED);

        getMaxId(new OnMaxIdCheckedListener() {
            @Override
            public void onMaxIdChecked(int maxId) {
                Report toReturn = null;
                String selectQuery = "SELECT  * FROM " + DbHelper.TABLE_REPORTS + " WHERE " + DbHelper.COLUMN_ID + " = '" + maxId + "'";
                Cursor cursor = database.rawQuery(selectQuery, null);
                if (cursor.moveToFirst()) toReturn = cursorToReport(cursor);
                cursor.close();

                if (toReturn != null) Log.d(LOG_TAG, LOG_TAB + "report with max ID: " + toReturn.toString());
                else Log.d(LOG_TAG, LOG_TAB + "NULL");

                if (listener != null) listener.onReportLoaded(toReturn);
            }
        });
    }

    public void getMaxId(OnMaxIdCheckedListener listener) {
        Log.d(LOG_TAG, "DataSource - getMaxId()" + LOG_UNDERLINED);

        open();
        int maxId = 0;
        Cursor cursor = database.rawQuery("select max(id) as id from " + DbHelper.TABLE_REPORTS, null);
        if (cursor.moveToFirst()) maxId = cursor.getInt(0);
        cursor.close();

        Log.d(LOG_TAG, LOG_TAB + "max ID: " + maxId);

        if (listener != null) listener.onMaxIdChecked(maxId);
    }

    public void getReportByTime(long reportTime, OnReportLoadedListener listener) {
        Log.d(LOG_TAG, "DataSource - getReportByTime()" + LOG_UNDERLINED);

        open();
        Report toReturn = null;
        String selectQuery = "SELECT  * FROM " + DbHelper.TABLE_REPORTS + " WHERE " + DbHelper.COLUMN_TIME + " = '" + reportTime + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) toReturn = cursorToReport(cursor);
        cursor.close();

        if (toReturn != null) Log.d(LOG_TAG, LOG_TAB + "report by time: " + toReturn.toString());
        else Log.d(LOG_TAG, LOG_TAB + "NULL");

        if (listener != null) listener.onReportLoaded(toReturn);
    }

    public void updateReportValue(int reportId, String[] valueTypes, long[] values, OnReportUpdatedListener listener) {
        Log.d(LOG_TAG, "DataSource - updateReportValue(reportId: " + reportId + ")" + LOG_UNDERLINED);

        if (reportId < 0) {
            if (listener != null) listener.onReportUpdated(null);
            return;
        }

        if (valueTypes == null) listener.onReportUpdated(null);
        if (values == null) listener.onReportUpdated(null);
        if (valueTypes.length != values.length) listener.onReportUpdated(null);

        if (database == null) open();

        ContentValues cv = new ContentValues();

        for (int i = 0; i < valueTypes.length; i ++) {
            cv.put(valueTypes[i], String.valueOf(values[i]));
        }

        int rowsUpdated = database.update(DbHelper.TABLE_REPORTS, cv, DbHelper.COLUMN_ID + " = ?", new String[] {String.valueOf(reportId)});

        final Cursor cursor = database.query(
                DbHelper.TABLE_REPORTS,
                null,
                DbHelper.COLUMN_ID + " = " + reportId,
                null,
                null,
                null,
                null);

        cursor.moveToFirst();
        final Report r = cursorToReport(cursor);

        if (r != null) Log.d(LOG_TAG, LOG_TAB + "updated report: " + r.toString());
        else Log.d(LOG_TAG, LOG_TAB + "NULL");

        if (listener != null) listener.onReportUpdated(r);
    }

    public void updateReportMessage(int reportId, String message, OnReportUpdatedListener listener) {
        Log.d(LOG_TAG, "DataSource - updateRMessage()" + LOG_UNDERLINED);

        if (reportId < 0) {
            if (listener != null) listener.onReportUpdated(null);
            return;
        }

        if (database == null) open();

        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_MESSAGE, message);

        int rowsUpdated = database.update(DbHelper.TABLE_REPORTS, values, DbHelper.COLUMN_ID + " = ?", new String[] {String.valueOf(reportId)});

        final Cursor cursor = database.query(
                DbHelper.TABLE_REPORTS,
                null,
                DbHelper.COLUMN_ID + " = " + reportId,
                null,
                null,
                null,
                null);

        cursor.moveToFirst();
        final Report r = cursorToReport(cursor);

        if (r != null) Log.d(LOG_TAG, LOG_TAB + "updated report: " + r.toString());
        else Log.d(LOG_TAG, LOG_TAB + "NULL");

        if (listener != null) listener.onReportUpdated(r);
    }

    public void removeItem(int itemId, OnItemDeletedListener listener) {
        Log.d(LOG_TAG, "DataSource - removeItem(itemId: " + itemId + ")" + LOG_UNDERLINED);
        open();
        int rowsRemoved = database.delete(
                DbHelper.TABLE_REPORTS,
                DbHelper.COLUMN_ID + " = ?",
                new String[] {String.valueOf(itemId)});

        if (listener != null) listener.onItemDeleted();
    }

    public void getAllItems(OnItemsLoadedListener listener) {
        Log.d(LOG_TAG, "DataSource - getAllItems()" + LOG_UNDERLINED);
        open();
        ArrayList<Report> toReturn = new ArrayList<>();
        Report report;
        Cursor cursor;

        try {
            cursor = database.query(
                    DbHelper.TABLE_REPORTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    DbHelper.COLUMN_TIME + " DESC");

            cursor.moveToFirst();

            for(int i = 0, count = cursor.getCount(); i < count; i ++) {
                report = cursorToReport(cursor);
                toReturn.add(report);
                if(cursor.isLast()) break;
                cursor.moveToNext();
            }

            cursor.close();
            if (listener != null) listener.onItemsLoaded(toReturn);
        } catch (Exception e) {
            if (listener != null) listener.onItemsLoaded(toReturn);
        }
    }

    // Získání všech hlášení, která čekají na odeslání nebo doručení
    public void getPageWaitingItems(int offset, int itemsPerPage, OnItemsLoadedListener listener) {
        Log.d(LOG_TAG, "DataSource - getAllItems()" + LOG_UNDERLINED);
        open();
        ArrayList<Report> toReturn = new ArrayList<>();
        Report report;
        Cursor cursor;

        try {
            cursor = database.query(
                    DbHelper.TABLE_REPORTS,
                    null,
                    DbHelper.COLUMN_SENT + " = " + ("" + WAITING) + " OR " + DbHelper.COLUMN_DELIVERED + " = " + ("" + WAITING),
                    null,
                    null,
                    null,
                    DbHelper.COLUMN_TIME + " DESC",
                    "" + offset + ", " + itemsPerPage);

            cursor.moveToFirst();

            for(int i = 0, count = cursor.getCount(); i < count; i ++) {
                report = cursorToReport(cursor);
                toReturn.add(report);
                if(cursor.isLast()) break;
                cursor.moveToNext();
            }

            cursor.close();
            if (listener != null) listener.onItemsLoaded(toReturn);
        } catch (Exception e) {
            if (listener != null) listener.onItemsLoaded(toReturn);
        }
    }

    // Nebude načítat hlášení čekající na odeslání nebo doručení, tak jsou načítána zvlášť metodou getWaitingItems()
    public void getPage(int offset, int itemsPerPage, OnItemsLoadedListener listener) {
        Log.d(LOG_TAG, "DataSource - getAllItems()" + LOG_UNDERLINED);
        Log.d(LOG_TAG_PAGES, "DataSource - getAllItems()");
        Log.d(LOG_TAG_PAGES, "offset: " + offset);
        open();
        ArrayList<Report> toReturn = new ArrayList<>();
        Report report;
        Cursor cursor;

        try {
            cursor = database.query(
                    DbHelper.TABLE_REPORTS,
                    null,
                    null,
                    null,
                    null,
                    null,
                    DbHelper.COLUMN_TIME + " DESC",
                    "" + offset + ", " + itemsPerPage);

            cursor.moveToFirst();

            for(int i = 0, count = cursor.getCount(); i < count; i ++) {
                report = cursorToReport(cursor);
                toReturn.add(report);
                if(cursor.isLast()) break;
                cursor.moveToNext();
            }

            cursor.close();
            if (listener != null) listener.onItemsLoaded(toReturn);
        } catch (Exception e) {
            if (listener != null) listener.onItemsLoaded(toReturn);
        }
    }

    private Report cursorToReport(Cursor cursor) {
        Log.d(LOG_TAG, "DataSource - cursorToReport()" + LOG_UNDERLINED);
        Report report = new Report();

        report.setId(cursor.getInt(0));
        report.setMessageType(cursor.getInt(1));
        report.setTime(Long.parseLong(cursor.getString(2)));
        report.setSentTime(Long.parseLong(cursor.getString(3)));
        report.setDeliveryTime(Long.parseLong(cursor.getString(4)));
        report.setAlarmRequestCode(Integer.parseInt(cursor.getString(5)));
        report.setRequestCodeForErrorAlarm(Integer.parseInt(cursor.getString(6)));
        report.setErrorAlert(cursor.getString(7).equals("1"));
        report.setMessage(cursor.getString(8));
        report.setFailed(cursor.getString(9).equals("1"));
        report.setDelivered(cursor.getString(10).equals("1"));

        if (report != null) Log.d(LOG_TAG, LOG_TAB + report.toString());
        else Log.d(LOG_TAG, LOG_TAB + "NULL");

        return report;
    }

    public void getCount(boolean onlyWaiting, OnItemsCountCheckedListener listener) {
        if (onlyWaiting) getWaitingItemsCount(listener);
        else getItemsCount(listener);
    }

    public void getItemsCount(OnItemsCountCheckedListener listener) {
        Log.d(LOG_TAG, "DataSource - getItemsCount()" + LOG_UNDERLINED);

        open();
        int toReturn = 0;
        String selectQuery = "SELECT COUNT(*) AS pocet FROM " + DbHelper.TABLE_REPORTS;
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) toReturn = cursor.getInt(0);
        cursor.close();

        Log.d(LOG_TAG, LOG_TAB + "count: " + toReturn);

        if (listener != null) listener.onItemsCountChecked(toReturn);
    }

    public void getWaitingItemsCount(OnItemsCountCheckedListener listener) {
        open();
        int toReturn = 0;
        String selectQuery = "SELECT COUNT(*) AS pocet FROM " + DbHelper.TABLE_REPORTS + " WHERE " + DbHelper.COLUMN_SENT + " = " + WAITING;
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) toReturn = cursor.getInt(0);
        cursor.close();

        if (listener != null) listener.onItemsCountChecked(toReturn);
    }
}
