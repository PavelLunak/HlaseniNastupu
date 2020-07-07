package cz.stodva.hlaseninastupu.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cz.stodva.hlaseninastupu.listeners.OnItemDeletedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsCountCheckedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportAddedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.objects.Report;


public class DataSource {

    private SQLiteDatabase database;
    Context context;
    private DbHelper dbHelper;


    public DataSource(Context context) {
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

    public void clearTable() {
        if (database == null) open();
        database.delete(DbHelper.TABLE_REPORTS, null, null);
    }

    public String formatDate(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return dateFormat.format(date);
    }

    public void addReport(final Report report, final OnReportAddedListener listener) {
        if (report == null) {
            if (listener != null) listener.onReportAdded(null);
            return;
        }

        open();
        ContentValues values = new ContentValues();
        values.put(DbHelper.COLUMN_TYPE, report.getMessageType());
        values.put(DbHelper.COLUMN_TIME, String.valueOf(report.getTime()));
        values.put(DbHelper.COLUMN_SENT, String.valueOf(report.getSendingTime()));
        values.put(DbHelper.COLUMN_DELIVERED, String.valueOf(report.getDeliveryTime()));
        values.put(DbHelper.COLUMN_IS_ALARM, report.isAlarm() ? "1" : "0");

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
        cursor.close();
    }

    public void getReportById(int reportId, OnReportLoadedListener listener) {
        open();
        Report toReturn = null;
        String selectQuery = "SELECT  * FROM " + DbHelper.TABLE_REPORTS + " WHERE " + DbHelper.COLUMN_ID + " = '" + reportId + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) toReturn = cursorToReport(cursor);
        cursor.close();
        if (listener != null) listener.onReportLoaded(toReturn);
    }

    public void getReportByTime(long reportTime, OnReportLoadedListener listener) {
        open();
        Report toReturn = null;
        String selectQuery = "SELECT  * FROM " + DbHelper.TABLE_REPORTS + " WHERE " + DbHelper.COLUMN_TIME + " = '" + reportTime + "'";
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) toReturn = cursorToReport(cursor);
        cursor.close();
        if (listener != null) listener.onReportLoaded(toReturn);
    }

    public void updateReportValue(int reportId, String valueType, long value, OnReportUpdatedListener listener) {
        if (reportId < 0) {
            if (listener != null) listener.onReportUpdated(null);
            return;
        }

        if (database == null) open();

        ContentValues values = new ContentValues();
        values.put(valueType, String.valueOf(value));

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
        if (listener != null) listener.onReportUpdated(r);
    }

    public void removeItem(int itemId, OnItemDeletedListener listener) {
        open();
        int rowsRemoved = database.delete(DbHelper.TABLE_REPORTS, DbHelper.COLUMN_ID + " = ?", new String[] {String.valueOf(itemId)});
        if (listener != null) listener.onItemDeleted();
    }

    public void getAllItems(OnItemsLoadedListener listener) {
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
                    DbHelper.COLUMN_ID + " ASC");

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
        Report report = new Report();

        report.setId(cursor.getInt(0));
        report.setMessageType(cursor.getInt(1));
        report.setTime(Long.parseLong(cursor.getString(2)));
        report.setSentTime(Long.parseLong(cursor.getString(3)));
        report.setDeliveryTime(Long.parseLong(cursor.getString(4)));
        report.setAlarm(cursor.getString(5).equals("1"));

        return report;
    }

    public void getItemsCount(OnItemsCountCheckedListener listener) {
        open();
        int toReturn = 0;
        String selectQuery = "SELECT COUNT(*) AS pocet FROM " + DbHelper.TABLE_REPORTS;
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) toReturn = cursor.getInt(0);
        cursor.close();
        if (listener != null) listener.onItemsCountChecked(toReturn);
    }
}
