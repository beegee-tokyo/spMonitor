package tk.giesecke.spmonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

/** spMonitor - Main UI activity
 *
 * Shows life or logged data from the spMonitor
 *
 * @author Bernd Giesecke
 * @version 0.1 beta August 13, 2015.
 */
class DataBaseHelper extends SQLiteOpenHelper {

	/** Name of the database */
	private static final String DATABASE_NAME="spMonitor";
	/** Name of the table */
	private static final String TABLE_NAME = "s";

	public DataBaseHelper(Context context) {
		super(context, DATABASE_NAME, null, 1);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		db.execSQL("CREATE TABLE " + TABLE_NAME + " (" +
				"year INTEGER, month INTEGER, day INTEGER, hour INTEGER, minute INTEGER, " +
				"solar DOUBLE, cons DOUBLE, light LONG);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

		db.execSQL("DROP TABLE IF EXISTS "+ TABLE_NAME);
		onCreate(db);
	}

	/**
	 * Add an entry to the database
	 *
	 * @param db
	 *            pointer to database
	 * @param recordLine
	 *            String with a record
	 *            format: yy,mm,dd,hh:mm,light,solar,consumption
	 *            e.g.: "15,08,13,13:54,35000,613.456,-120.22"
	 */
	private static void addDay(SQLiteDatabase db, String recordLine) {

		/* Parse the string into its single values */
		String[] valuesPerLine = recordLine.split(",");
		/** String list with hour & minute values */
		String[] hourSplit = valuesPerLine[3].split(":");

		/** ContentValues to hold the measured and calculated values to be added to the database */
		ContentValues values = new ContentValues(14);
		values.put("year", Integer.parseInt(valuesPerLine[0]));
		values.put("month", Integer.parseInt(valuesPerLine[1]));
		values.put("day", Integer.parseInt(valuesPerLine[2]));
		values.put("hour", Integer.parseInt(hourSplit[0]));
		values.put("minute", Integer.parseInt(hourSplit[1]));
		values.put("solar", Double.parseDouble(valuesPerLine[5]));
		values.put("cons", Double.parseDouble(valuesPerLine[6]));
		values.put("light", Long.parseLong(valuesPerLine[4]));

		db.insert(TABLE_NAME, null, values);
	}

	/**
	 * Read data of day "dayNumber" and returns the data as a cursor
	 *
	 * @param db
	 *            pointer to database
	 * @param dayNumber
	 *            the day we want to read (1-31)
	 * @param monthSelected
	 *            the month we want to read from
	 * @param yearSelected
	 *            the year we want to read from
	 * @return <code>Cursor</code> dayStamp
	 *            Cursor with all database entries matching with dayNumber
	 *            Entry per minute is
	 *            cursor[0] = year stamp
	 *            cursor[1] = month stamp
	 *            cursor[2] = day stamp
	 *            cursor[3] = hour stamp
	 *            cursor[4] = minute stamp
	 *            cursor[5] = sensor power
	 *            cursor[6] = consumed power
	 *            cursor[7] = light value
	 */
	public static Cursor getDay(SQLiteDatabase db, int dayNumber, int monthSelected, int yearSelected) {
		return db.rawQuery("select * from " + TABLE_NAME + " where day= " + String.valueOf(dayNumber) + " and month= "
				+ String.valueOf(monthSelected) + " and year= " + String.valueOf(yearSelected), null);
	}

	/**
	 * Get specific row values from the database,
	 * e.g. all years stored in the database or
	 *      all months of a year stored in the database or
	 *      all days in a month of a year stored in the database
	 *
	 * @param db
	 *            pointer to database
	 * @param requestField
	 *            requested row from db
	 *            "year" returns all year values found
	 *            "month" returns all month values found in year <code>requestLimiter</code>
	 *            "day" returns all day values found in month <code>requestLimiter</code>
	 * @param requestLimiterMonth
	 *            limiter for request
	 *            unused if requestField is "year"
	 *            unused if requestField is "month"
	 *            month if requestField is "day"
	 * @param requestLimiterYear
	 *            limiter for request
	 *            unused if requestField is "year"
	 *            year if requestField is "month"
	 *            year if requestField is "day"
	 *
	 * @return <code>ArrayList<Integer></code>
	 *            array list with all entries found
	 */
	public static ArrayList<Integer> getEntries(SQLiteDatabase db,
	                                   String requestField, int requestLimiterMonth, int requestLimiterYear) {

		/** Array list holding the found values */
		ArrayList<Integer> returnArray = new ArrayList<>();

		/** Limiter for row search */
		String queryRequest = "select distinct " + requestField + " from " + TABLE_NAME;
		if (requestField.equalsIgnoreCase("day")) {
			queryRequest += " where month = " + String.valueOf(requestLimiterMonth) +
					" and year = " + String.valueOf(requestLimiterYear);
		} else if (requestField.equalsIgnoreCase("month")) {
			queryRequest += " where year = " + String.valueOf(requestLimiterYear);
		}

		/** Cursor holding the records of a day */
		Cursor allRows = db.rawQuery(queryRequest, null);

		allRows.moveToFirst();
		for (int i=0; i<allRows.getCount(); i++) {
			returnArray.add(allRows.getInt(0));
			allRows.moveToNext();
		}
		allRows.close();
		return returnArray;
	}

	/**
	 * Deletes all entries where day is deleteDay in month deleteMonth from year deleteYear
	 *
	 * @param db
	 *            pointer to database
	 * @param deleteDay
	 *            day to be removed from database
	 * @param deleteMonth
	 *            month where day should be deleted
	 * @param deleteYear
	 *            year where day should be deleted
	 */
	public static void deleteDay(SQLiteDatabase db, int deleteDay, int deleteMonth, int deleteYear) {
		db.delete(TABLE_NAME, "day=" + deleteDay + " AND month=" + deleteMonth +
				" AND year=" + deleteYear, null);
	}

	/**
	 * Add records from file fileName into database
	 *
	 * @param db
	 *            pointer to database
	 * @param fileName
	 *            pointer to database
	 * @param sftp
	 *            sftp channel used to read the file
	 * @param updatePlot
	 *            true -> use data to update plot series
	 *            false -> update only data base
	 */
	public static void syncFileToDB(SQLiteDatabase db, String fileName, ChannelSftp sftp, boolean updatePlot ) {
		/** Buffered reader to get file from spMonitor device */
		BufferedReader buffInput;
		try {
			buffInput = new BufferedReader
					(new InputStreamReader(sftp.get(fileName)));
		} catch (SftpException e) {
			return;
		}
		/* Buffer for a file */
		String fileBuffer = "";
		/* Buffer for a single line */
		String line;
		try {
			while ((line = buffInput.readLine()) != null) {
				fileBuffer += line;
				fileBuffer += "\n";
			}
			buffInput.close();
		} catch (IOException e) {
			return;
		}

		if (updatePlot) {
			spMonitor.timeStampsCont.clear();
			spMonitor.lightValueCont.clear();
			spMonitor.solarPowerCont.clear();
			spMonitor.consumPowerCont.clear();
		}

		/** String list with single lines from received log file */
		String[] recordLines = fileBuffer.split("\n");
		if (recordLines.length != 0) {
			for (String recordLine : recordLines) {
				addDay(db, recordLine);
				if (updatePlot) {
					/** String list with single values from a line from received log file */
					String[] valuesPerLine = recordLine.split(",");
					spMonitor.dayToShow = valuesPerLine[0] + "/" + valuesPerLine[1] + "/" + valuesPerLine[2];
					/** String list with hour & minute values */
					String[] hourSplit = valuesPerLine[3].split(":");
					/** Gregorian calender to calculate the time stamp */
					Calendar timeCal = new GregorianCalendar(
							1970,
							1,
							1,
							Integer.parseInt(hourSplit[0]),
							Integer.parseInt(hourSplit[1]),
							0);
					spMonitor.timeStampsCont.add(timeCal.getTimeInMillis());
					/** Light value from the log file */
					long lightVal = Long.parseLong(valuesPerLine[4]);

					spMonitor.lightValueCont.add(lightVal);
					/** Produced solar power */
					Float solarPowerVal = Float.parseFloat(valuesPerLine[5]);
					/** Consumed power */
					Float consPowerVal = Float.parseFloat(valuesPerLine[6]);
					spMonitor.solarPowerCont.add(solarPowerVal);
					spMonitor.consumPowerCont.add(solarPowerVal + consPowerVal);
				}
			}
		}
	}
}
