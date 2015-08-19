package tk.giesecke.spmonitor;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/** spMonitor - Main UI activity
 *
 * Shows life or logged data from the spMonitor
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class spMonitor extends Activity implements View.OnClickListener {

	/** Access to shared preferences of application*/
	public static SharedPreferences mPrefs;
	/** Application context */
	public static Context appContext;
	/** The view of the main UI */
	public static View appView;
	/** The timer to refresh the UI */
	private Timer timer;
	/**we are going to use a handler to be able to run in our TimerTask */
	private final Handler handler = new Handler();
	/** Flag for communication active */
	public static boolean isCommunicating = false;

	/** The url to access the spMonitor device */
	private String url = "";
	/** The ip address to access the spMonitor device */
	public static String deviceIP = "no IP saved";
	/** A HTTP client to access the spMonitor device */
	public static final OkHttpClient client = new OkHttpClient();

	/** Pointer to text view for results */
	private static TextView resultTextView;
	/** Flag if UI auto refresh is on or off */
	private boolean autoRefreshOn = true;

	/** MPAndroid chart view for the current chart */
	private static LineChart lineChart;
	/** LineData for the plot */
	private LineData plotData;
	/** List to hold the timestamps for the chart from a log file */
	private static final ArrayList<String> timeSeries = new ArrayList<>();
	/** List to hold the measurements of the solar panel for the chart from a log file */
	private static final ArrayList<Entry> solarSeries = new ArrayList<>();
	/** List to hold the measurement of the consumption for the chart from a log file */
	private static final ArrayList<Entry> consSeries = new ArrayList<>();
	/** List to hold the measurement of the light for the chart from a log file */
	private static final ArrayList<Entry> lightSeries = new ArrayList<>();
	/** List for zero line in the chart */
	private static final ArrayList<Entry> zeroSeries = new ArrayList<>();
	/** List to hold the timestamps for a continuously updated chart */
	public static final ArrayList<String> timeStamps = new ArrayList<>();
	/** List to hold the measurements of the solar panel for a continuously updated chart */
	public static final ArrayList<Float> solarPower = new ArrayList<>();
	/** List to hold the measurement of the consumption for a continuously updated chart */
	public static final ArrayList<Float> consumPower = new ArrayList<>();
	/** List to hold the measurement of the light for a continuously updated chart */
	public static final ArrayList<Long> lightValue = new ArrayList<>();
	/** List to hold the timestamps for a chart from logged data */
	public static final ArrayList<String> timeStampsCont = new ArrayList<>();
	/** List to hold the measurements of the solar panel for a chart from logged data */
	public static final ArrayList<Float> solarPowerCont = new ArrayList<>();
	/** List to hold the measurement of the consumption for a chart from logged data */
	public static final ArrayList<Float> consumPowerCont = new ArrayList<>();
	/** List to hold the measurement of the light for a chart from logged data */
	public static final ArrayList<Long> lightValueCont = new ArrayList<>();
	/** Line data set for solar data */
	private LineDataSet solar;
	/** Line data set for consumption data */
	private LineDataSet cons;
	/** Line data set for light data */
	private LineDataSet light;

	/** Array with existing log dates on the Arduino */
	private static final List<String> logDates = new ArrayList<>();
	/** Pointer to current displayed log in logDates array */
	private static int logDatesIndex = 0;
	/** Flag for showing a log */
	public static boolean showingLog = false;

	/** Day stamp of data */
	public static String dayToShow;

	/** Solar power received from spMonitor device as minute average */
	private static Float solarPowerMin = 0.0f;
	/** Solar energy generated up to now on the displayed day */
	public static float solarEnergy = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	private static Float consPowerMin = 0.0f;
	/** Consumed energy generated up to now on the displayed day */
	public static float consEnergy = 0.0f;
	/** Light received from spMonitor device as minute average */
	private long lightValMin = 0;
	/** Solar power received from spMonitor device as minute average */
	private static Float lastSolarPowerMin = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	private static Float lastConsPowerMin = 0.0f;
	/** Light received from spMonitor device as minute average */
	private long lastLightValMin = 0;
	/** Solar power received from spMonitor device as 5 seconds average */
	private static Float solarPowerSec = 0.0f;
	/** Consumption received from spMonitor device as 5 seconds average */
	private static Float consPowerSec = 0.0f;
	/** Light received from spMonitor device as 5 seconds average */
	private long lightValSec = 0;
	/** Flag for showing solar power data */
	private static boolean showSolar = true;
	/** Flag for showing consumption data */
	private static boolean showCons = true;
	/** Flag for showing light data */
	private static boolean showLight = false;
	/** Flag for calibration mode. Update every 5 secs instead of 60 seconds */
	private static boolean calModeOn = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sp_monitor);

		// Enable access to internet
		if (android.os.Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		appContext = this;
		appView = getWindow().getDecorView().findViewById(android.R.id.content);

		mPrefs = getSharedPreferences("spMonitor", 0);
		resultTextView = (TextView) findViewById(R.id.tv_result);
		deviceIP = mPrefs.getString("spMonitorIP", "no IP saved");

		if (!deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
			Utilities.startRefreshAnim();
			new syncDBtoDB().execute();
		} else {
			resultTextView.setText(getString(R.string.err_no_device));
		}

		/** Pointer to text views showing the consumed / produced energy */
		TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
		energyText.setVisibility(View.INVISIBLE);
		energyText = (TextView) findViewById(R.id.tv_solar_energy);
		energyText.setVisibility(View.INVISIBLE);

		/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
		Button btStop = (Button) findViewById(R.id.bt_stop);
		if (showingLog) {
			btStop.setTextColor(getResources().getColor(android.R.color.holo_green_light));
			btStop.setText(getResources().getString(R.string.start));
		}
		btStop.setOnLongClickListener(new View.OnLongClickListener() {
			public boolean onLongClick(View v) {
				calModeOn = !calModeOn;
				if (calModeOn) {
					resultTextView.setText(getString(R.string.fast_mode_on));
					stopTimer();
					startTimer(1, 5000);
				} else {
					resultTextView.setText(getString(R.string.fast_mode_off));
					stopTimer();
					startTimer(1, 60000);
				}
				/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
				Button stopButton = (Button) findViewById(R.id.bt_stop);
				stopButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
				stopButton.setText(getResources().getString(R.string.stop));
				autoRefreshOn = true;
				return true;
			}
		});

	}

	@Override
	public void onResume() {
		super.onResume();
		if (!deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
			if (timer == null) {
				if (calModeOn) {
					if (autoRefreshOn) startTimer(1, 5000);
				} else {
					if (autoRefreshOn) startTimer(1, 60000);
				}
			}
		} else {
			resultTextView.setText(getResources().getString(R.string.err_no_device));
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		stopTimer();
	}

	@Override
	public void onClick(View v) {
		url = "";
		client.setConnectTimeout(30, TimeUnit.SECONDS); // connect timeout
		client.setReadTimeout(30, TimeUnit.SECONDS);    // socket timeout
		/** Button to go to previous  log */
		Button prevButton  = (Button) findViewById(R.id.bt_prevLog);
		/** Button to go to next log */
		Button nextButton  = (Button) findViewById(R.id.bt_nextLog);

		if (isCommunicating) return;

		switch (v.getId()) {
			case R.id.bt_prevLog:
				if (logDatesIndex > 0) {
					Utilities.startRefreshAnim();
					logDatesIndex--;
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					autoRefreshOn = false;
					stopTimer();
					showingLog = true;
					// Get data from data base
					/** String list with requested date info */
					String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
					/** Instance of DataBaseHelper */
					DataBaseHelper dbHelper = new DataBaseHelper(appContext);
					/** Instance of data base */
					SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

					/** Cursor with new data from the database */
					Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
							Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
					Utilities.fillSeries(newDataSet);
					initChart(false);
					newDataSet.close();
					dataBase.close();
					dbHelper.close();

					if (logDatesIndex == 0) {
						prevButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					} else {
						prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
					nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					Utilities.stopRefreshAnim();
				}
				break;
			case R.id.bt_nextLog:
				if (logDatesIndex < logDates.size()-1) {
					Utilities.startRefreshAnim();
					logDatesIndex++;
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					autoRefreshOn = false;
					stopTimer();
					showingLog = true;
					// Get data from data base
					/** String list with requested date info */
					String[] requestedDate = logDates.get(logDatesIndex).substring(0, 8).split("-");
					/** Instance of DataBaseHelper */
					DataBaseHelper dbHelper = new DataBaseHelper(appContext);
					/** Instance of data base */
					SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

					/** Cursor with new data from the database */
					Cursor newDataSet = DataBaseHelper.getDay(dataBase, Integer.parseInt(requestedDate[2]),
							Integer.parseInt(requestedDate[1]), Integer.parseInt(requestedDate[0]));
					Utilities.fillSeries(newDataSet);
					initChart(false);
					newDataSet.close();
					dataBase.close();
					dbHelper.close();

					if (logDatesIndex == logDates.size()-1) {
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					} else {
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
					prevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					Utilities.stopRefreshAnim();
				}
				break;
			case R.id.bt_stop:
				if (autoRefreshOn) {
					stopTimer();
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					autoRefreshOn = false;
				} else {
					if (showingLog) {
						showingLog = false;
						Utilities.startRefreshAnim();
						new syncDBtoDB().execute();

						/** Pointer to text views showing the consumed / produced energy */
						TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
						energyText.setVisibility(View.INVISIBLE);
						energyText = (TextView) findViewById(R.id.tv_solar_energy);
						energyText.setVisibility(View.INVISIBLE);

						logDatesIndex = logDates.size()-1;
						nextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					} else {
						if (calModeOn) {
							startTimer(1, 5000);
						} else {
							startTimer(1, 60000);
						}
					}
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					stopButton.setText(getResources().getString(R.string.stop));
					autoRefreshOn = true;
				}
				break;
			case R.id.bt_status:
				client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
				client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout
				url = deviceIP + "e";
				break;
			case R.id.bt_close:
				stopTimer();
				finish();
				break;
			case R.id.bt_sync:
				if (!showingLog) {
					Utilities.startRefreshAnim();
					new syncDBtoDB().execute();
				}
				break;
			case R.id.cb_solar:
				/** Checkbox to show or hide solar graph */
				CheckBox cbSolar = (CheckBox)findViewById(R.id.cb_solar);
				if (cbSolar.isChecked()) {
					solar.setColor(0xFFFFBB33);
					solar.setCircleColor(0xFFFFBB33);
					solar.setHighLightColor(0xFFFFBB33);
					solar.setFillColor(0xAAFFBB33);
					showSolar = true;
				} else {
					solar.setColor(Color.TRANSPARENT);
					solar.setCircleColor(Color.TRANSPARENT);
					solar.setHighLightColor(Color.TRANSPARENT);
					solar.setFillColor(Color.TRANSPARENT);
					showSolar = false;
				}
				// let the chart know it's data has changed
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
				break;
			case R.id.cb_cons:
				/** Checkbox to show or hide consumption graph */
				CheckBox cbCons = (CheckBox)findViewById(R.id.cb_cons);
				if (cbCons.isChecked()) {
					cons.setColor(0xFF33B5E5);
					cons.setCircleColor(0xFF33B5E5);
					cons.setHighLightColor(0xFF33B5E5);
					cons.setFillColor(0xAA33B5E5);
					showCons = true;
				} else {
					cons.setColor(Color.TRANSPARENT);
					cons.setCircleColor(Color.TRANSPARENT);
					cons.setHighLightColor(Color.TRANSPARENT);
					cons.setFillColor(Color.TRANSPARENT);
					showCons = false;
				}
				// let the chart know it's data has changed
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
				break;
			case R.id.cb_light:
				/** Checkbox to show or hide light graph */
				CheckBox cbLight = (CheckBox)findViewById(R.id.cb_light);
				if (cbLight.isChecked()) {
					light.setColor(Color.GREEN);
					light.setCircleColor(Color.GREEN);
					light.setHighLightColor(Color.GREEN);
					light.setFillColor(Color.GREEN);
					showLight = true;
				} else {
					light.setColor(Color.TRANSPARENT);
					light.setCircleColor(Color.TRANSPARENT);
					light.setHighLightColor(Color.TRANSPARENT);
					light.setFillColor(Color.TRANSPARENT);
					showLight = false;
				}
				// let the chart know it's data has changed
				lineChart.notifyDataSetChanged();
				lineChart.invalidate();
				break;
		}

		if (!url.isEmpty()) {
			new callArduino().execute(url);
		}
	}

	/**
	 * Async task class to contact Arduino part of the spMonitor device
	 */
	private class callArduino extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			/** URL to be called */
			String urlString=params[0]; // URL to call
			/** Response from the spMonitor device or error message */
			String resultToDisplay = "";

			// Make call only if valid url is given
			if (urlString.startsWith("No")) {
				resultToDisplay = getResources().getString(R.string.err_no_device);
			} else {
				/** Request to spMonitor device */
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/** Response from spMonitor device */
						Response response = client.newCall(request).execute();
						if (response != null) {
							resultToDisplay = response.body().string();
						}
					} catch (IOException e) {
						e.printStackTrace();
						resultToDisplay = e.getMessage();
						try {
							if (resultToDisplay.contains("EHOSTUNREACH")) {
								resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
							}
							if (resultToDisplay.equalsIgnoreCase("")) {
								resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
							}
							return resultToDisplay;
						} catch (NullPointerException en) {
							resultToDisplay = getResources().getString(R.string.err_no_device);
							return resultToDisplay;
						}
					}
				}
			}

			if (resultToDisplay.equalsIgnoreCase("")) {
				resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
			}
			return resultToDisplay;
		}

		protected void onPostExecute(String result) {
			updateUI(result);
			if (timer == null) {
				if (calModeOn) {
					if (autoRefreshOn) startTimer(5000, 5000);
				} else {
					if (autoRefreshOn) startTimer(5000, 60000);
				}
			}
		}
	}

	/**
	 * Async task class to contact Linino part of the spMonitor device
	 * and sync spMonitor database with local Android database
	 */
	private class syncDBtoDB extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			/** String list with parts of the URL */
			String[] ipValues = deviceIP.split("/");
			/** URL to be called */
			String urlString = "http://"+ipValues[2]+"/sd/spMonitor/query.php"; // URL to call

			/** Response from the spMonitor device or error message */
			String resultToDisplay = getResources().getString(R.string.filesSyncFail);

			// Check for last entry in the local database
			/** Instance of DataBaseHelper */
			DataBaseHelper dbHelper = new DataBaseHelper(appContext);
			/** Instance of data base */
			SQLiteDatabase dataBase = dbHelper.getReadableDatabase();
			/** Cursor with data from database */
			Cursor dbCursor = DataBaseHelper.getLastRow(dataBase);
			if (dbCursor.getCount() != 0) { // local database not empty, need to sync only missing
				dbCursor.moveToFirst();

				int lastMinute =  dbCursor.getInt(4);
				int lastHour = dbCursor.getInt(3);
				int lastDay = dbCursor.getInt(2);

				urlString += "?date=" + dbCursor.getString(0); // add year
				urlString += "-" + ("00" +
						dbCursor.getString(1)).substring(dbCursor.getString(1).length()); // add month
				//urlString += "-" + dbCursor.getString(1); // add month
				urlString += "-" + ("00" +
						String.valueOf(lastDay))
						.substring(String.valueOf(lastDay).length()); // add day
				//urlString += "-" + dbCursor.getString(2); // add day
				urlString += "-" + ("00" +
						String.valueOf(lastHour))
						.substring(String.valueOf(lastHour).length()); // add hour
				//urlString += "-" + dbCursor.getString(3); // add hour
				urlString += ":" + ("00" +
						String.valueOf(lastMinute))
						.substring(String.valueOf(lastMinute).length()); // add minute
				//urlString += ":" + String.valueOf(lastMinute); // add minute
				urlString += "&get=all";
			} // else {} local database is empty, need to sync all data
			dbCursor.close();
			dataBase.close();
			dbHelper.close();
			// Make call only if valid url is given
			if (urlString.startsWith("No")) {
				resultToDisplay = getResources().getString(R.string.err_no_device);
			} else {
				/** Request to spMonitor device */
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
						/** Response from spMonitor device */
						Response response = client.newCall(request).execute();
						if (response != null) {
							resultToDisplay = response.body().string();
						}
					} catch (IOException e) {
						e.printStackTrace();
						resultToDisplay = e.getMessage();
						try {
							if (resultToDisplay.contains("EHOSTUNREACH")) {
								resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
							}
							if (resultToDisplay.equalsIgnoreCase("")) {
								resultToDisplay = getApplicationContext().getString(R.string.err_arduino);
							}
							return resultToDisplay;
						} catch (NullPointerException en) {
							resultToDisplay = getResources().getString(R.string.err_no_device);
							return resultToDisplay;
						}
					}
					if (Utilities.isJSONValid(resultToDisplay)) {
						try {
							/** JSON array with the data received from spMonitor device */
							JSONArray jsonFromDevice = new JSONArray(resultToDisplay);
							/** Instance of DataBaseHelper */
							dbHelper = new DataBaseHelper(appContext);
							/** Instance of data base */
							dataBase = dbHelper.getWritableDatabase();

							// Get received data into local database
							// skip first data record from device, it is already in the database
							for (int i=1; i<jsonFromDevice.length(); i++) {
								/** JSONObject with a single record */
								JSONObject jsonRecord = jsonFromDevice.getJSONObject(i);
								String record = jsonRecord.getString("d");
								record = record.replace("-",",");
								record += ","+jsonRecord.getString("l");
								record += ","+jsonRecord.getString("s");
								record += ","+jsonRecord.getString("c");
								DataBaseHelper.addDay(dataBase, record);
							}
							resultToDisplay = getResources().getString(R.string.filesSynced);
						} catch (JSONException e) {
							resultToDisplay = getResources().getString(R.string.filesSyncFail);
							dataBase.close();
							dbHelper.close();
						}
						dataBase.close();
						dbHelper.close();
					} else {
						resultToDisplay = getResources().getString(R.string.filesSyncFail);
					}
				}
			}

			return resultToDisplay;
		}

		protected void onPostExecute(String result) {
			updateSynced(result);
		}
	}

	/**
	 * Update UI with values received from spMonitor device (Arduino part)
	 *
	 * @param value
	 *        result sent by spMonitor
	 */
	private void updateUI(final String value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				/** Pointer to text views to be updated */
				TextView valueFields;
				/* String with results received from spMonitor device */
				String result;

				if (value.length() != 0) {
					if (url.endsWith("get")) {
						// decode JSON
						if (Utilities.isJSONValid(value)) {
							try {
								/** JSON object containing result from server */
								JSONObject jsonResult = new JSONObject(value);
								/** JSON object containing the values */
								JSONObject jsonValues = jsonResult.getJSONObject("value");

								try {
									solarPowerMin = Float.parseFloat(jsonValues.getString("S"));
									lastSolarPowerMin = solarPowerMin;
								} catch (Exception excError) {
									solarPowerMin = lastSolarPowerMin;
								}
								/** Temporary buffer for last read power value */
								Float oldPower = solarPowerSec;
								try {
									solarPowerSec = Float.parseFloat(jsonValues.getString("sr"));
								} catch (Exception excError) {
									solarPowerSec = oldPower;
								}
								try {
									consPowerMin = Float.parseFloat(jsonValues.getString("C"));
									lastConsPowerMin = consPowerMin;
								} catch (Exception excError) {
									consPowerMin = lastConsPowerMin;
								}
								oldPower = consPowerSec;
								try {
									consPowerSec = Float.parseFloat(jsonValues.getString("cr"));
								} catch (Exception excError) {
									consPowerSec = oldPower;
								}

								/** Double for the result of solar current and consumption used at 1min updates */
								double resultPowerMin = solarPowerMin + consPowerMin;
								/** Double for the result of solar current and consumption used at 5sec updates */
								double resultPowerSec = solarPowerSec + consPowerSec;

								result = "S=" + String.valueOf(solarPowerMin) + "W ";
								result += "s=";
								try {
									result += jsonValues.getString("s");
								} catch (Exception excError) {
									result += "---";
								}
								result += "A sv=";
								try {
									result += jsonValues.getString("sv");
								} catch (Exception excError) {
									result += "---";
								}
								result += "V sr=";
								try {
									result += jsonValues.getString("sr");
								} catch (Exception excError) {
									result += "---";
								}
								result += "W sa=";
								try {
									result += jsonValues.getString("sa");
								} catch (Exception excError) {
									result += "---";
								}
								result += "W sp=";
								try {
									result += jsonValues.getString("sp");
								} catch (Exception excError) {
									result += "---";
								}
								result += "\nC=" + String.valueOf(consPowerMin) + "W c=";
								try {
									result += jsonValues.getString("c");
								} catch (Exception excError) {
									result += "---";
								}
								result += "A cv=";
								try {
									result += jsonValues.getString("cv");
								} catch (Exception excError) {
									result += "---";
								}
								result += "V cr=";
								try {
									result += jsonValues.getString("cr");
								} catch (Exception excError) {
									result += "---";
								}
								result += "W ca=";
								try {
									result += jsonValues.getString("ca");
								} catch (Exception excError) {
									result += "---";
								}
								result += "W cp=";
								try {
									result += jsonValues.getString("cp") + "\n";
								} catch (Exception excError) {
									result += "---" + "\n";
								}

								valueFields = (TextView) findViewById(R.id.tv_solar_val);
								if (calModeOn) {
									valueFields.setText(String.format("%.0f", solarPowerSec) + "W");
									valueFields = (TextView) findViewById(R.id.tv_cons_val);
									valueFields.setText(String.format("%.0f", resultPowerSec) + "W");
								} else {
									valueFields.setText(String.format("%.0f", solarPowerMin) + "W");
									valueFields = (TextView) findViewById(R.id.tv_cons_val);
									valueFields.setText(String.format("%.0f", resultPowerMin) + "W");
								}
								resultTextView.setText(result);

								valueFields = (TextView) findViewById(R.id.tv_result_txt);
								if (calModeOn) {
									if (consPowerSec > 0.0d) {
										valueFields.setText(getString(R.string.tv_result_txt_im));
										valueFields = (TextView) findViewById(R.id.tv_result_val);
										valueFields.setTextColor(getResources()
												.getColor(android.R.color.holo_red_light));
									} else {
										valueFields.setText(getString(R.string.tv_result_txt_ex));
										valueFields = (TextView) findViewById(R.id.tv_result_val);
										valueFields.setTextColor(getResources()
												.getColor(android.R.color.holo_green_light));
									}
									valueFields.setText(String.format("%.0f", Math.abs(consPowerSec)) + "W");
								} else {
									if (consPowerMin > 0.0d) {
										valueFields.setText(getString(R.string.tv_result_txt_im));
										valueFields = (TextView) findViewById(R.id.tv_result_val);
										valueFields.setTextColor(getResources()
												.getColor(android.R.color.holo_red_light));
									} else {
										valueFields.setText(getString(R.string.tv_result_txt_ex));
										valueFields = (TextView) findViewById(R.id.tv_result_val);
										valueFields.setTextColor(getResources()
												.getColor(android.R.color.holo_green_light));
									}
									valueFields.setText(String.format("%.0f", Math.abs(consPowerMin)) + "W");
								}

								try {
									lightValMin = Long.parseLong(jsonValues.getString("L"));
									lastLightValMin = lightValMin;
								} catch (Exception excError) {
									lightValMin = lastLightValMin;
								}
								/** Temporary buffer for last read light value */
								long oldLight = lightValSec;
								try {
									lightValSec = Long.parseLong(jsonValues.getString("l"));
								} catch (Exception excError) {
									lightValSec = oldLight;
								}
								valueFields = (TextView) findViewById(R.id.tv_light_value);
								if (calModeOn) {
									valueFields.setText(String.valueOf(lightValSec) + "lux");
								} else {
									valueFields.setText(String.valueOf(lightValMin) + "lux");
								}

								if (autoRefreshOn) {
									/** Current time as string */
									String nowTime = Utilities.getCurrentTime();
									plotData.addXValue(nowTime);
									timeStampsCont.add(nowTime);
									if (calModeOn) {
										solarSeries.add(new Entry(solarPowerSec, solarSeries.size()));
										solarPowerCont.add(solarPowerSec);
										consSeries.add(new Entry(consPowerSec, consSeries.size()));
										consumPowerCont.add(consPowerSec);
										lightSeries.add(new Entry(lightValSec, lightSeries.size()));
										lightValueCont.add(lightValSec);
									} else {
										solarSeries.add(new Entry(solarPowerMin, solarSeries.size()));
										solarPowerCont.add(solarPowerMin);
										consSeries.add(new Entry(consPowerMin, consSeries.size()));
										consumPowerCont.add(consPowerMin);
										lightSeries.add(new Entry(lightValMin, lightSeries.size()));
										lightValueCont.add(lightValMin);
									}
									zeroSeries.add(new Entry(0, zeroSeries.size()));

									/** Text view to show min and max poser values */
									TextView maxPowerText = (TextView) findViewById(R.id.tv_cons_max);
									maxPowerText.setText("(" + String.format("%.0f",
											Collections.max(consumPowerCont)) + "W)");
									maxPowerText = (TextView) findViewById(R.id.tv_solar_max);
									maxPowerText.setText("(" + String.format("%.0f",
											Collections.max(solarPowerCont)) + "W)");

									// let the chart know it's data has changed
									lineChart.notifyDataSetChanged();
									lineChart.invalidate();
								}

								Utilities.stopRefreshAnim();
								return;
							} catch (Exception excError) {
								Utilities.stopRefreshAnim();
								return;
							}
						}
					}
					resultTextView.setText(value);
					Utilities.stopRefreshAnim();
					return;
				}
				result = "\n";
				resultTextView.setText(result);
				Utilities.stopRefreshAnim();
			}
		});
	}

	/**
	 * Update UI with values received from spMonitor device (Linino part)
	 *
 	 * @param result
	 *        result sent by spMonitor
	 */
	private void updateSynced(final String result) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				resultTextView.setText(result);

				if (!showingLog) {
					/** Today split into 3 integers for the database query */
					int[] todayDate = Utilities.getCurrentDate();
					/** Instance of DataBaseHelper */
					DataBaseHelper dbHelper = new DataBaseHelper(appContext);
					/** Instance of data base */
					SQLiteDatabase dataBase = dbHelper.getReadableDatabase();
					/** Cursor with new data from the database */
					Cursor newDataSet = DataBaseHelper.getDay(dataBase, todayDate[2],
							todayDate[1], todayDate[0] - 2000);
					Utilities.fillSeries(newDataSet);
					newDataSet.close();
					logDates.clear();
					/** List with years in the database */
					ArrayList<Integer> yearsAvail = DataBaseHelper.getEntries(dataBase, "year", 0, 0);
					for (int year = 0; year < yearsAvail.size(); year++) {
						/** List with months of year in the database */
						ArrayList<Integer> monthsAvail = DataBaseHelper.getEntries(dataBase, "month", 0, yearsAvail.get(year));
						for (int month = 0; month < monthsAvail.size(); month++) {
							/** List with days of month of year in the database */
							ArrayList<Integer> daysAvail = DataBaseHelper.getEntries(dataBase, "day", monthsAvail.get(month), yearsAvail.get(year));
							for (int day = 0; day < daysAvail.size(); day++) {
								logDates.add(("00" + String.valueOf(yearsAvail.get(year)))
										.substring(String.valueOf(yearsAvail.get(year)).length()) +
										"-" + ("00" + String.valueOf(monthsAvail.get(month)))
										.substring(String.valueOf(monthsAvail.get(month)).length()) +
										"-" + ("00" + String.valueOf(daysAvail.get(day)))
										.substring(String.valueOf(daysAvail.get(day)).length()));
							}
						}
					}
					logDatesIndex = logDates.size() - 1;

					dataBase.close();
					dbHelper.close();

					if (timer == null) {
						if (calModeOn) {
							if (autoRefreshOn) startTimer(5000, 5000);
						} else {
							if (autoRefreshOn) startTimer(5000, 60000);
						}
					}
					initChart(true);
				}
				Utilities.stopRefreshAnim();
			}
		});
	}

	/**
	 * Start recurring timer with given delay and repeat time
	 *
	 * @param startDelay
	 *                delay in milli seconds before starting the timer
	 * @param repeatTime
	 *                repeat time in milli seconds
	 */
	private void startTimer(int startDelay, long repeatTime) {
		// Start new single shot every 10 seconds
		timer = new Timer();
		/** Timer task for UI update */
		TimerTask timerTask = new TimerTask() {
			public void run() {
				//use a handler to run a toast that shows the current timestamp
				handler.post(new Runnable() {
					public void run() {
						autoRefresh();
					}
				});
			}
		};
		//schedule the timer, after startDelay ms the TimerTask will run every repeatTime ms
		timer.schedule(timerTask, startDelay, repeatTime); //
	}

	/**
	 * Stop recurring timer if it is running
	 */
	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	/**
	 * Start async task to get last measurements from spMonitor
	 */
	private void autoRefresh() {
		Utilities.startRefreshAnim();
		/** String list with parts of the URL */
		String[] ipValues = deviceIP.split("/");
		url = "http://"+ipValues[2]+"/data/get";
		new callArduino().execute(url);
	}

	/**
	 * Initialize chart to show solar power, consumption and light values
	 *
	 * @param isContinuous
	 *          Flag for display mode
	 *          true = continuous display of data received from spMonitor
	 *          false = display content of a log file
	 */
	private void initChart(boolean isContinuous) {

		// Pointer to the chart in the layout
		lineChart = (LineChart) findViewById(R.id.graph);

		timeSeries.clear();
		solarSeries.clear();
		consSeries.clear();
		lightSeries.clear();
		zeroSeries.clear();
		if (!isContinuous) {
			for (int i=0; i<timeStamps.size(); i++) {
				timeSeries.add(timeStamps.get(i));
			}
			for (int i=0; i<solarPower.size(); i++) {
				solarSeries.add(new Entry(solarPower.get(i), i));
			}
			for (int i=0; i<consumPower.size(); i++) {
				consSeries.add(new Entry(consumPower.get(i), i));
			}
			for (int i= 0; i<lightValue.size(); i++) {
				lightSeries.add(new Entry(lightValue.get(i), i));
			}
			for (int i= 0; i<timeStamps.size(); i++) {
				zeroSeries.add(new Entry(0, i));
			}
		} else {
			if (timeStampsCont.size() != 0) {
				for (int i=0; i<timeStampsCont.size(); i++) {
					timeSeries.add(timeStampsCont.get(i));
				}
				for (int i=0; i<solarPowerCont.size(); i++) {
					solarSeries.add(new Entry(solarPowerCont.get(i), i));
				}
				for (int i=0; i<consumPowerCont.size(); i++) {
					consSeries.add(new Entry(consumPowerCont.get(i), i));
				}
				for (int i= 0; i<lightValueCont.size(); i++) {
					lightSeries.add(new Entry(lightValueCont.get(i), i));
				}
				for (int i= 0; i<timeStampsCont.size(); i++) {
					zeroSeries.add(new Entry(0, i));
				}
			}
		}
		/** Line data set for solar data */
		solar = new LineDataSet(solarSeries, "Solar");
		/** Line data set for consumption data */
		cons = new LineDataSet(consSeries, "Consumption");
		/** Line data set for light data */
		light = new LineDataSet(lightSeries, "Light");
		/** Line data set for zero line */
		LineDataSet zero = new LineDataSet(zeroSeries, "");

		solar.setLineWidth(1.75f);
		solar.setCircleSize(0f);
		if (showSolar) {
			solar.setColor(0xFFFFBB33);
			solar.setCircleColor(0xFFFFBB33);
			solar.setHighLightColor(0xFFFFBB33);
			solar.setFillColor(0xAAFFBB33);
		} else {
			solar.setColor(Color.TRANSPARENT);
			solar.setCircleColor(Color.TRANSPARENT);
			solar.setHighLightColor(Color.TRANSPARENT);
			solar.setFillColor(Color.TRANSPARENT);
		}
		solar.setDrawValues(false);
		solar.setDrawFilled(true);

		cons.setLineWidth(1.75f);
		cons.setCircleSize(0f);
		if (showCons) {
			cons.setColor(0xFF33B5E5);
			cons.setCircleColor(0xFF33B5E5);
			cons.setHighLightColor(0xFF33B5E5);
			cons.setFillColor(0xAA33B5E5);
		} else {
			cons.setColor(Color.TRANSPARENT);
			cons.setCircleColor(Color.TRANSPARENT);
			cons.setHighLightColor(Color.TRANSPARENT);
			cons.setFillColor(Color.TRANSPARENT);
		}
		cons.setDrawValues(false);
		cons.setDrawValues(false);
		cons.setDrawFilled(true);
		cons.setAxisDependency(YAxis.AxisDependency.LEFT);

		light.setLineWidth(1.75f);
		light.setCircleSize(0f);
		if (showLight) {
			light.setColor(Color.GREEN);
			light.setCircleColor(Color.GREEN);
			light.setHighLightColor(Color.GREEN);
		} else {
			light.setColor(Color.TRANSPARENT);
			light.setCircleColor(Color.TRANSPARENT);
			light.setHighLightColor(Color.TRANSPARENT);
		}
		light.setDrawValues(false);
		light.setAxisDependency(YAxis.AxisDependency.RIGHT);

		zero.setLineWidth(1.75f);
		zero.setCircleSize(0f);
		zero.setColor(Color.RED);
		zero.setCircleColor(Color.RED);
		zero.setHighLightColor(Color.RED);
		zero.setDrawValues(false);

		/** Data set with data for the 4 plots */
		ArrayList<LineDataSet> dataSets = new ArrayList<>();
		dataSets.add(zero);
		dataSets.add(solar);
		dataSets.add(cons);
		dataSets.add(light);

		/** Data object with the data set and the y values */
		plotData = new LineData(timeSeries, dataSets);

		lineChart.setBackgroundColor(Color.BLACK);
		lineChart.setDrawGridBackground(false);
		lineChart.setTouchEnabled(true);
		lineChart.setDragEnabled(true);
		lineChart.setAutoScaleMinMaxEnabled(true);
		lineChart.setData(plotData);

		TextView chartTitle = (TextView) findViewById(R.id.tv_plotTitle);
		chartTitle.setText(dayToShow);

		/** Instance of left y axis */
		YAxis lYAx = lineChart.getAxisLeft();
		lYAx.setEnabled(true);
		lYAx.setTextColor(Color.WHITE);
		lYAx.setStartAtZero(false);
		lYAx.setSpaceTop(1);
		lYAx.setSpaceBottom(1);

		/** Instance of right y axis */
		YAxis rYAx = lineChart.getAxisRight();
		rYAx.setEnabled(true);
		rYAx.setTextColor(Color.WHITE);
		rYAx.setStartAtZero(false);
		rYAx.setSpaceTop(1);
		rYAx.setSpaceBottom(1);

		/** Instance of x axis */
		XAxis xAx = lineChart.getXAxis();
		xAx.setEnabled(true);
		xAx.setTextColor(Color.WHITE);
		xAx.setPosition(XAxis.XAxisPosition.BOTTOM);

		lineChart.getLegend().setEnabled(false);

		// create a custom MarkerView (extend MarkerView) and specify the layout
		// to use for it
		/** Instance of custom marker view handler */
		CustomMarkerView mv = new CustomMarkerView(appContext);
		lineChart.setMarkerView(mv);

		// set the marker to the chart
		lineChart.setMarkerView(mv);

		// let the chart know it's data has changed
		lineChart.notifyDataSetChanged();
		lineChart.invalidate();

	}

	/**
	 * Show time, consumption and solar power when user touches a data point
	 *
	 */
	public class CustomMarkerView extends MarkerView {

		/** Pointer to text view for time */
		private final TextView tvMarkerTime;
		/** Pointer to text view for consumption */
		private final TextView tvMarkerCons;
		/** Pointer to text view for solar power */
		private final TextView tvMarkerSolar;

		public CustomMarkerView(Context context) {
			super(context, R.layout.plot_marker);
			/** Text view for time in marker */
			tvMarkerTime = (TextView) findViewById(R.id.tv_marker_time);
			/** Text view for consumption in marker */
			tvMarkerCons = (TextView) findViewById(R.id.tv_marker_cons);
			/** Text view for solar power in marker */
			tvMarkerSolar = (TextView) findViewById(R.id.tv_marker_solar);
		}

		// callbacks every time the MarkerView is redrawn, can be used to update the
		// content (user-interface)
		@Override
		public void refreshContent(Entry e, Highlight highlight) {
			/** Index for the series at the touched data point */
			int dataIndex = highlight.getXIndex();
			/** Entry with data of solar power at given index */
			Entry touchSolar = solarSeries.get(dataIndex);
			/** Entry with data of consumption at given index */
			Entry touchCons = consSeries.get(dataIndex);

			tvMarkerTime.setText(timeSeries.get(dataIndex));
			tvMarkerCons.setText((Float.toString(touchCons.getVal())+"W"));
			tvMarkerSolar.setText((Float.toString(touchSolar.getVal())+"W"));
		}

		@Override
		public int getXOffset() {
			// this will center the marker-view horizontally
			return -(getWidth() / 2);
		}

		@Override
		public int getYOffset() {
			// this will cause the marker-view to be above the selected value
			return -getHeight();
		}
	}
}
