package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/** spMonitor - Main UI activity
 *
 * Shows life or logged data from the spMonitor
 *
 * @author Bernd Giesecke
 * @version 0.1 beta August 13, 2015.
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
	//we are going to use a handler to be able to run in our TimerTask
	private final Handler handler = new Handler();

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
	/** Command sent to the spMonitor device */
	private String thisCommand = "";
	/** XYPlot view for the current chart */
	private static GraphView currentPlot;
	/** Data series for the solar current sensor */
	public static LineGraphSeries<DataPoint> solarSeries = null;
	/** Data series for the consumption current sensor */
	public static LineGraphSeries<DataPoint> consSeries = null;
	/** Data series for the light sensor */
	private static LineGraphSeries<DataPoint> lightSeries = null;
	/** List to hold the timestamps for the chart from a log file */
	private final ArrayList<Long> timeStamps = new ArrayList<>();
	/** List to hold the measurements of the solar panel for the chart from a log file */
	private final ArrayList<Float> solarPower = new ArrayList<>();
	/** List to hold the measurement of the consumption for the chart from a log file */
	private final ArrayList<Float> consumPower = new ArrayList<>();
	/** List to hold the measurement of the light for the chart from a log file */
	private final ArrayList<Long> lightValue = new ArrayList<>();
	/** List to hold the timestamps for the chart from a log file */
	private final ArrayList<Long> timeStampsCont = new ArrayList<>();
	/** List to hold the measurements of the solar panel for the chart from a log file */
	private final ArrayList<Float> solarPowerCont = new ArrayList<>();
	/** List to hold the measurement of the consumption for the chart from a log file */
	private final ArrayList<Float> consumPowerCont = new ArrayList<>();
	/** List to hold the measurement of the light for the chart from a log file */
	private final ArrayList<Long> lightValueCont = new ArrayList<>();
	/** Counter for displayed data points */
	private double graph2LastXValue = 0d;
	/** Number of plot y values */
	private static final int plotValues = 720;

	/** Array with existing log files on the Arduino */
	private static final List<String> logFiles = new ArrayList<>();
	/** Pointer to current displayed log in logFiles array */
	private static int logFilesIndex = 0;
	/** Flag for SFTP command to get a log file*/
	private boolean isGet = false;
	/** Flag for SFTP command to fill chart with existing data*/
	private boolean isFill = false;
	/** Flag for showing a log */
	private boolean showingLog = false;

	/** Day stamp of data */
	private String dayToShow;

	/** Solar power received from spMonitor device as minute average */
	private static Float solarPowerMin = 0.0f;
	/** Solar energy generated up to now on the displayed day */
	private static float solarEnergy = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	private static Float consPowerMin = 0.0f;
	/** Consumed energy generated up to now on the displayed day */
	private static float consEnergy = 0.0f;
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
	public static boolean showSolar = true;
	/** Flag for showing consumption data */
	public static boolean showCons = true;
	/** Flag for showing light data */
	private static boolean showLight = false;
	/** Flag for calibration mode. Update every 5 secs instead of 60 seconds */
	private static boolean calModeOn = false;
	/** List for min and max values to show on the scale */
	private double[] minMaxVal;

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

		if (savedInstanceState == null) {
			if (!deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
				Utilities.startRefreshAnim();
				url = deviceIP;
				new execSftp().execute(url, "ls");
				/** Pointer to text views showing the consumed / produced energy */
				TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
				energyText.setVisibility(View.INVISIBLE);
				energyText = (TextView) findViewById(R.id.tv_solar_energy);
				energyText.setVisibility(View.INVISIBLE);
			}
		} else {
			showingLog = savedInstanceState.getBoolean("showingLog");
			if (!deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
				Utilities.startRefreshAnim();
				url = deviceIP;
				new execSftp().execute(url, "ls");
			}

			/** List to get saved time stamp values from saved instance */
			long[] timeStampsList = savedInstanceState.getLongArray("timeStampsCont");
			/** List to get saved solar current values from saved instance */
			float[] solarPowerList = savedInstanceState.getFloatArray("solarPowerCont");
			/** List to get saved consumption current values from saved instance */
			float[] consumPowerList = savedInstanceState.getFloatArray("consumPowerCont");
			/** List to get saved light values from saved instance */
			long[] lightValueList = savedInstanceState.getLongArray("lightValueCont");

			timeStampsCont.clear();
			solarPowerCont.clear();
			consumPowerCont.clear();
			lightValueCont.clear();
			for (long aTimeStampsListCont : timeStampsList != null ? timeStampsList : new long[0]) {
				timeStampsCont.add(aTimeStampsListCont);
			}
			for (float aSolarPowerListCont : solarPowerList != null ? solarPowerList : new float[0]) {
				solarPowerCont.add(aSolarPowerListCont);
			}
			for (float aConsumPowerListCont : consumPowerList != null ? consumPowerList : new float[0]) {
				consumPowerCont.add(aConsumPowerListCont);
			}
			for (long aLightValueListCont : lightValueList != null ? lightValueList : new long[0]) {
				lightValueCont.add(aLightValueListCont);
			}
			if (showingLog) {
				solarEnergy = 0f;
				consEnergy = 0f;

				/** List to get saved time stamp log values from saved instance */
				timeStampsList = savedInstanceState.getLongArray("timeStamps");
				/** List to get saved solar current log values from saved instance */
				solarPowerList = savedInstanceState.getFloatArray("solarPower");
				/** List to get saved consumption current log values from saved instance */
				consumPowerList = savedInstanceState.getFloatArray("consumPower");
				/** List to get saved light values from log saved instance */
				lightValueList = savedInstanceState.getLongArray("lightValue");

				timeStamps.clear();
				solarPower.clear();
				consumPower.clear();
				lightValue.clear();
				for (long aTimeStampsList : timeStampsList != null ? timeStampsList : new long[0]) {
					timeStamps.add(aTimeStampsList);
				}
				for (float aSolarPowerList : solarPowerList != null ? solarPowerList : new float[0]) {
					solarPower.add(aSolarPowerList);
				}
				for (float aConsumPowerList : consumPowerList != null ? consumPowerList : new float[0]) {
					consumPower.add(aConsumPowerList);
				}
				for (long aLightValueList : lightValueList != null ? lightValueList : new long[0]) {
					lightValue.add(aLightValueList);
				}

				for (int i=0; i<solarPower.size()-1; i++) {
					/** Value of solar power to calculate produced energy */
					float solarPowerVal = solarPower.get(i);
					/** Value of consumed power to calculate consumed energy */
					float consPowerVal = consumPower.get(i);
					/** Double for the result of solar current and consumption used at 1min updates */
					double resultPowerMin = solarPowerVal + consPowerVal;
					solarEnergy += solarPowerVal * 1f / 60f /1000f;
					consEnergy += resultPowerMin * 1f / 60f / 1000f;
				}

				/** Pointer to text views showing the consumed / produced energy */
				TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
				energyText.setVisibility(View.VISIBLE);
				energyText.setText("Consumed: " + String.format("%.3f", consEnergy) + "kWh");
				energyText = (TextView) findViewById(R.id.tv_solar_energy);
				energyText.setVisibility(View.VISIBLE);
				energyText.setText("Produced: " + String.format("%.3f", solarEnergy) + "kWh");

				initChart(false);
			} else {
				/** Pointer to text views showing the consumed / produced energy */
				TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
				energyText.setVisibility(View.INVISIBLE);
				energyText = (TextView) findViewById(R.id.tv_solar_energy);
				energyText.setVisibility(View.INVISIBLE);
				initChart(true);
			}

			showSolar = savedInstanceState.getBoolean("showSolar");
			showCons = savedInstanceState.getBoolean("showCons");
			showLight = savedInstanceState.getBoolean("showLight");
			calModeOn = savedInstanceState.getBoolean("calModeOn");

			/* Pointer to check box for showing/hiding solar, consumption and light graph */
			CheckBox cbShowSeries = (CheckBox)findViewById(R.id.cb_solar);
			if (showSolar) {
				cbShowSeries.setChecked(true);
			} else {
				cbShowSeries.setChecked(false);
			}
			cbShowSeries = (CheckBox)findViewById(R.id.cb_cons);
			if (showCons) {
				cbShowSeries.setChecked(true);
			} else {
				cbShowSeries.setChecked(false);
			}
			cbShowSeries = (CheckBox)findViewById(R.id.cb_light);
			if (showLight) {
				cbShowSeries.setChecked(true);
			} else {
				cbShowSeries.setChecked(false);
			}
			if (calModeOn) {
				stopTimer();
				startTimer(1, 5000);
			} else {
				stopTimer();
				startTimer(1, 60000);
			}
		}

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
	protected void onSaveInstanceState(@SuppressWarnings("NullableProblems") Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("showingLog", showingLog);
		/** temporary holder for time stamps */
		long[] timeStampsList = new long[timeStamps.size()];
		/** temporary holder for solar current */
		float[] solarPowerList = new float[solarPower.size()];
		/** temporary holder for consumption current */
		float[] consumPowerList = new float[consumPower.size()];
		/** temporary holder for light values */
		long[] lightValueList = new long[lightValue.size()];
		for (int i=0; i<timeStamps.size(); i++) {
			timeStampsList[i] = timeStamps.get(i);
		}
		for (int i=0; i<solarPower.size(); i++) {
			solarPowerList[i] = solarPower.get(i);
		}
		for (int i=0; i<consumPower.size(); i++) {
			consumPowerList[i] = consumPower.get(i);
		}
		for (int i=0; i<lightValue.size(); i++) {
			lightValueList[i] = lightValue.get(i);
		}
		savedInstanceState.putLongArray("timeStamps", timeStampsList);
		savedInstanceState.putFloatArray("solarPower", solarPowerList);
		savedInstanceState.putFloatArray("consumPower", consumPowerList);
		savedInstanceState.putLongArray("lightValue", lightValueList);

		timeStampsList = new long[timeStampsCont.size()];
		solarPowerList = new float[timeStampsCont.size()];
		consumPowerList = new float[timeStampsCont.size()];
		lightValueList = new long[timeStampsCont.size()];
		for (int i=0; i<timeStampsCont.size(); i++) {
			timeStampsList[i] = timeStampsCont.get(i);
		}
		for (int i=0; i<solarPowerCont.size(); i++) {
			solarPowerList[i] = solarPowerCont.get(i);
		}
		for (int i=0; i<consumPowerCont.size(); i++) {
			consumPowerList[i] = consumPowerCont.get(i);
		}
		for (int i=0; i<lightValueCont.size(); i++) {
			lightValueList[i] = lightValueCont.get(i);
		}
		savedInstanceState.putLongArray("timeStampsCont", timeStampsList);
		savedInstanceState.putFloatArray("solarPowerCont", solarPowerList);
		savedInstanceState.putFloatArray("consumPowerCont", consumPowerList);
		savedInstanceState.putLongArray("lightValueCont", lightValueList);

		savedInstanceState.putBoolean("showSolar", showSolar);
		savedInstanceState.putBoolean("showCons", showCons);
		savedInstanceState.putBoolean("showLight", showLight);
		savedInstanceState.putBoolean("calModeOn", calModeOn);
	}

	@Override
	public void onClick(View v) {
		url = "";
		client.setConnectTimeout(30, TimeUnit.SECONDS); // connect timeout
		client.setReadTimeout(30, TimeUnit.SECONDS);    // socket timeout
		switch (v.getId()) {
			case R.id.bt_prevLog:
				if (logFilesIndex > 0) {
					logFilesIndex--;
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					autoRefreshOn = false;
					stopTimer();
					isGet = false;
					showingLog = true;
					// Get file from Arduino via Async task
					url = deviceIP;
					Utilities.startRefreshAnim();
					// Get file from Arduino via Async task
					url = deviceIP;
					new execSftp().execute(url, "get", logFiles.get(logFilesIndex));
					/** Button to go to previous or next log */
					Button prevNextButton  = (Button) findViewById(R.id.bt_prevLog);
					if (logFilesIndex == 0) {
						prevNextButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					} else {
						prevNextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
					prevNextButton  = (Button) findViewById(R.id.bt_nextLog);
					prevNextButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
				}
				break;
			case R.id.bt_nextLog:
				if (logFilesIndex < logFiles.size()-1) {
					logFilesIndex++;
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_green_light));
					stopButton.setText(getResources().getString(R.string.start));
					autoRefreshOn = false;
					stopTimer();
					isGet = false;
					showingLog = true;
					// Get file from Arduino via Async task
					url = deviceIP;
					Utilities.startRefreshAnim();
					// Get file from Arduino via Async task
					url = deviceIP;
					new execSftp().execute(url, "get", logFiles.get(logFilesIndex));
					/** Button to go to previous or next log */
					Button nextPrevButton  = (Button) findViewById(R.id.bt_nextLog);
					if (logFilesIndex == logFiles.size()-1) {
						nextPrevButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					} else {
						nextPrevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
					}
					nextPrevButton  = (Button) findViewById(R.id.bt_prevLog);
					nextPrevButton.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
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
						graph2LastXValue = 0d;
						/** Pointer to text views showing the consumed / produced energy */
						TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
						energyText.setVisibility(View.INVISIBLE);
						energyText = (TextView) findViewById(R.id.tv_solar_energy);
						energyText.setVisibility(View.INVISIBLE);

						clearChart();
						initChart(true);
					}
					if (calModeOn) {
						startTimer(1, 5000);
					} else {
						startTimer(1, 60000);
					}
					/** Button to stop/start continuous UI refresh and switch between 5s and 60s refresh rate */
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setTextColor(getResources().getColor(android.R.color.holo_red_light));
					stopButton.setText(getResources().getString(R.string.stop));
					autoRefreshOn = true;
				}
				url = "";
				break;
			case R.id.bt_status:
				client.setConnectTimeout(2, TimeUnit.MINUTES); // connect timeout
				client.setReadTimeout(2, TimeUnit.MINUTES);    // socket timeout
				url = deviceIP + "e";
				thisCommand = "e";
				break;
			case R.id.bt_close:
				finish();
				break;
			case R.id.bt_clear:
				if (!showingLog) {
					clearChart();
				}
				url = "";
				break;
			case R.id.cb_solar:
				/** Checkbox to show or hide solar graph */
				CheckBox cbSolar = (CheckBox)findViewById(R.id.cb_solar);
				if (cbSolar.isChecked()) {
					currentPlot.addSeries(solarSeries);
					showSolar = true;
				} else {
					currentPlot.removeSeries(solarSeries);
					showSolar = false;
				}

				minMaxVal = Utilities.getMinMax();
				if (minMaxVal[0] - 10 <= 0) {
					currentPlot.getViewport().setMinY(0f);
				} else {
					currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
				}
				currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);

				currentPlot.onDataChanged(false, false);
				break;
			case R.id.cb_cons:
				/** Checkbox to show or hide consumption graph */
				CheckBox cbCons = (CheckBox)findViewById(R.id.cb_cons);
				if (cbCons.isChecked()) {
					currentPlot.removeSeries(solarSeries); // Remove and add again to have it on top
					currentPlot.addSeries(consSeries);
					currentPlot.addSeries(solarSeries);
					showCons = true;
				} else {
					currentPlot.removeSeries(consSeries);
					showCons = false;
				}

				minMaxVal = Utilities.getMinMax();
				if (minMaxVal[0] - 10 <= 0) {
					currentPlot.getViewport().setMinY(0f);
				} else {
					currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
				}
				currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);

				currentPlot.onDataChanged(false, false);
				break;
			case R.id.cb_light:
				/** Checkbox to show or hide light graph */
				CheckBox cbLight = (CheckBox)findViewById(R.id.cb_light);
				if (cbLight.isChecked()) {
					currentPlot.getSecondScale().addSeries(lightSeries);
					lightSeries.setColor(Color.GREEN);
					showLight = true;
				} else {
					currentPlot.removeSeries(lightSeries);
					// TODO Workaround for series using second scale -- cannot be removed
					lightSeries.setColor(Color.TRANSPARENT);
					showLight = false;
				}

				currentPlot.getSecondScale().setMinY(lightSeries.getLowestValueY());
				currentPlot.getSecondScale().setMaxY(lightSeries.getHighestValueY());

				currentPlot.onDataChanged(false, false);
				break;
		}

		if (!url.isEmpty()) {
			//if (autoRefreshOn) stopTimer();
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
							return resultToDisplay;
						} catch (NullPointerException en) {
							resultToDisplay = getResources().getString(R.string.err_no_device);
							return resultToDisplay;
						}
					}
				}
			}

			return resultToDisplay;
		}

		protected void onPostExecute(String result) {
			/** Length of spMonitor device response */
			int strLen = result.length();
			if ((strLen > 2) && (url.endsWith("g"))) {
				result = result.substring(0, strLen - 2);
			}
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
	 */
	private class execSftp extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			/** URL to be called */
			String urlString = params[0]; // URL to call
			/** SFTP command to be send */
			String sftpCommand = params[1]; // What to do
			/** LOG file name to get */
			String logFileName = "";
			if (sftpCommand.equalsIgnoreCase("get") || sftpCommand.equalsIgnoreCase("rm")) {
				logFileName = params[2]; // Which log file to get
			}

			/** Response from the spMonitor device or error message */
			String resultToDisplay = "";

			// Make call only if valid url is given
			if (urlString.startsWith("No")) {
				resultToDisplay = getResources().getString(R.string.err_no_device);
			} else {
				/** IP address to connect to */
				String ip[] = urlString.split("/");
				/** Sftp session instance */
				Session session = null;

				try{
					/** Jsch instance */
					JSch sshChannel = new JSch();
					session = sshChannel.getSession("root", ip[2], 22);
					session.setPassword("spMonitor");
					session.setConfig("StrictHostKeyChecking", "no");
					session.connect();

					if (session.isConnected()) {
						/** Channel for session */
						Channel channel = session.openChannel("sftp");
						channel.connect();

						if (channel.isConnected()) {
							/** SFTP channel */
							ChannelSftp sftp = (ChannelSftp) channel;

							if (sftp.isConnected()) {
								sftp.cd("/mnt/sda1/");
								if (sftpCommand.equalsIgnoreCase("get")) {
									/* Buffered reader to get file from spMonitor device */
									BufferedReader buffInput = new BufferedReader
											(new InputStreamReader(sftp.get(logFileName)));
									/* Buffer for a single line */
									String line;
									while ((line = buffInput.readLine()) != null) {
										resultToDisplay += line;
										resultToDisplay += "\n";
									}
									buffInput.close();
									isGet = true;
								// TODO add function to delete log files on the Arduino Yun
								} else if (sftpCommand.equalsIgnoreCase("rm")) {
									sftp.rm(logFileName);
									resultToDisplay = getResources().getString(R.string.log_rm_title, logFileName);
								} else if (sftpCommand.equalsIgnoreCase("ls")) {
									@SuppressWarnings("unchecked") Vector<ChannelSftp.LsEntry> list =
											sftp.ls("/mnt/sda1/*.txt");
									if (list.size() != 0) {
										logFiles.clear();
										for (int i=0; i<list.size();i++) {
											logFiles.add(list.get(i).getFilename());
										}
										if (logFiles.size() != 0) {
											Collections.sort(logFiles, new Comparator<String>() {
												@Override
												public int compare(String text1, String text2) {
													return text1.compareToIgnoreCase(text2);
												}});

											logFileName = logFiles.get(logFiles.size()-1);
											/* Buffered reader to get file from spMonitor device */
											BufferedReader buffInput = new BufferedReader
													(new InputStreamReader(sftp.get(logFileName)));
											/* Buffer for a single line */
											String line;
											while ((line = buffInput.readLine()) != null) {
												resultToDisplay += line;
												resultToDisplay += "\n";
											}
											buffInput.close();
											logFilesIndex = logFiles.size()-1;
											isFill = true;
											dayToShow = logFileName.substring(0,2) + "/" +
													logFileName.substring(3,5) + "/" +
													logFileName.substring(6,8);
										}
									} else {
										resultToDisplay = getResources().getString(R.string.noLogFile);
									}
								}
								sftp.disconnect();
							}
							channel.disconnect();
						}
						session.disconnect();
					}

				} catch(JSchException | IOException | SftpException e){
					e.printStackTrace();
					if (session != null) {
						session.disconnect();
					}
					resultToDisplay = e.getMessage();
					if (resultToDisplay.contains("EHOSTUNREACH")) {
						resultToDisplay = getApplicationContext().getString(R.string.err_linino);
					}
					return resultToDisplay;
				}
			}
			return resultToDisplay;
		}

		protected void onPostExecute(String result) {
			displayLog(result);
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
	 * Update UI with values received from spMonitor device (Arduino part)
	 *
	 * @param value
	 *                result sent by spMonitor
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
					if (thisCommand.equalsIgnoreCase("s")) {
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

								valueFields = (TextView) findViewById(R.id.tv_solar_val);
								if (calModeOn) {
									result = "s=";
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
									result += "\nc=";
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
									valueFields.setText(String.format("%.0f", solarPowerSec) + "W");
									valueFields = (TextView) findViewById(R.id.tv_cons_val);
									valueFields.setText(String.format("%.0f", resultPowerSec) + "W");
								} else {
									valueFields.setText(String.format("%.0f", solarPowerMin) + "W");
									result = "S=" + String.valueOf(solarPowerMin) + "W C=" +
											String.valueOf(consPowerMin) + "W\n";
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
								/** Temporary buffr for last read light value */
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
									/** Gregorian calendar to calculate the time stamp */
									Calendar cal = new GregorianCalendar();
									/* Set date to Jan 1st, 1970 to get smaller values for faster graph response */
									cal.set(1970, 1, 1);
									graph2LastXValue += 1d;
									/** current time in milli seconds */
									long timeInMillis = cal.getTimeInMillis();
									timeStampsCont.add(timeInMillis);
									if (calModeOn) {
										solarSeries.appendData(new DataPoint(timeInMillis,
												solarPowerSec), true, plotValues);
										solarPowerCont.add(solarPowerSec);
									} else {
										solarSeries.appendData(new DataPoint(timeInMillis,
												solarPowerMin), true, plotValues);
										solarPowerCont.add(solarPowerMin);
									}
									if (calModeOn) {
										consSeries.appendData(new DataPoint(timeInMillis,
												resultPowerSec), true, plotValues);
										consumPowerCont.add((float) resultPowerSec);
									} else {
										consSeries.appendData(new DataPoint(timeInMillis,
												resultPowerMin), true, plotValues);
										consumPowerCont.add((float) resultPowerMin);
									}
									if (calModeOn) {
										lightSeries.appendData(new DataPoint(timeInMillis,
												lightValSec), true, plotValues);
										lightValueCont.add(lightValSec);
									} else {
										lightSeries.appendData(new DataPoint(timeInMillis,
												lightValMin), true, plotValues);
										lightValueCont.add(lightValMin);
									}

									minMaxVal = Utilities.getMinMax();
									if (minMaxVal[0] - 10 <= 0) {
										currentPlot.getViewport().setMinY(0f);
									} else {
										currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
									}
									currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);
									currentPlot.getSecondScale().setMinY(lightSeries.getLowestValueY());
									currentPlot.getSecondScale().setMaxY(lightSeries.getHighestValueY());
									currentPlot.getViewport().setMinX(solarSeries.getLowestValueX());
									currentPlot.getViewport().setMaxX(solarSeries.getHighestValueX());

									if (graph2LastXValue == 2) {
										if (showCons) currentPlot.addSeries(consSeries);
										// TODO for series using second scale -- cannot be removed
										currentPlot.getSecondScale().addSeries(lightSeries);
										if (!showLight) lightSeries.setColor(Color.TRANSPARENT);
										if (showSolar) currentPlot.addSeries(solarSeries);
										if (!calModeOn) {
											stopTimer();
											startTimer(60000, 60000);
										} else {
											stopTimer();
											startTimer(5000, 5000);
										}
									}

									/** Text view to show min and max poser values */
									TextView maxPowerText = (TextView) findViewById(R.id.tv_cons_max);
									maxPowerText.setText("(" + String.format("%.0f", consSeries.getHighestValueY()) + "W)");
									maxPowerText = (TextView) findViewById(R.id.tv_solar_max);
									maxPowerText.setText("(" + String.format("%.0f", solarSeries.getHighestValueY()) + "W)");
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
	 * @param value
	 *                result sent by spMonitor
	 */
	private void displayLog(final String value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (isGet) {
					if (value.length() != 0) {
						timeStamps.clear();
						lightValue.clear();
						solarPower.clear();
						consumPower.clear();
						/** String list with single lines from received log file */
						String[] recordLines = value.split("\n");
						if (recordLines.length != 0) {
							/* Text view to show produced / consumed energy */
							TextView energyText = (TextView) findViewById(R.id.tv_cons_energy);
							energyText.setVisibility(View.VISIBLE);
							energyText = (TextView) findViewById(R.id.tv_solar_energy);
							energyText.setVisibility(View.VISIBLE);
							solarEnergy = 0f;
							consEnergy = 0f;
							for (String recordLine : recordLines) {
								/** String list with single values from a line from received log file */
								String[] valuesPerLine = recordLine.split(",");
								dayToShow = valuesPerLine[0] + "/" + valuesPerLine[1] + "/" + valuesPerLine[2];
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
								timeStamps.add(timeCal.getTimeInMillis());
								/** Light value from the log file */
								long lightVal = Long.parseLong(valuesPerLine[4]);

								lightValue.add(lightVal);
								/** Float for the result of solar current used at 1min updates */
								Float solarPowerVal = Float.parseFloat(valuesPerLine[5]);
								/** Float for the result of consumption used at 1min updates */
								Float consPowerVal = Float.parseFloat(valuesPerLine[6]);
								try {
									solarPower.add(solarPowerVal);
								} catch (NumberFormatException ignore) {
									solarPower.add(0f);
								}
								try {
									consumPower.add(solarPowerVal + consPowerVal);
								} catch (NumberFormatException ignore) {
									consumPower.add(0f);
								}

								if (valuesPerLine.length > 7) {
									try {
										solarEnergy = Float.parseFloat(valuesPerLine[7]) / 1000;
										consEnergy = Float.parseFloat(valuesPerLine[8]) / 1000;
									} catch (NumberFormatException ignore) {
									}
								}
							}

							energyText = (TextView) findViewById(R.id.tv_cons_energy);
							energyText.setText("Consumed: " + String.format("%.3f", consEnergy) + "kWh");
							energyText = (TextView) findViewById(R.id.tv_solar_energy);
							energyText.setText("Produced: " + String.format("%.3f", solarEnergy) + "kWh");
							/* Txt view to show max consumed / produced power */
							TextView maxPowerText = (TextView) findViewById(R.id.tv_cons_max);
							maxPowerText.setText("(" + String.format("%.0f", Collections.max(consumPower)) + "W)");
							maxPowerText = (TextView) findViewById(R.id.tv_solar_max);
							maxPowerText.setText("(" + String.format("%.0f", Collections.max(solarPower)) + "W)");

							initChart(false);
							Utilities.stopRefreshAnim();
						}
					}
					isGet = false;
				} else if (isFill) {
					if (value.length() != 0) {
						timeStampsCont.clear();
						lightValueCont.clear();
						solarPowerCont.clear();
						consumPowerCont.clear();
						/** String list with single lines from received log file */
						String[] recordLines = value.split("\n");
						if (recordLines.length != 0) {
							for (String recordLine : recordLines) {
								/** String list with single values from a line from received log file */
								String[] valuesPerLine = recordLine.split(",");
								dayToShow = valuesPerLine[0] + "/" + valuesPerLine[1] + "/" + valuesPerLine[2];
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
								timeStampsCont.add(timeCal.getTimeInMillis());
								/** Light value from the log file */
								long lightVal = Long.parseLong(valuesPerLine[4]);

								lightValueCont.add(lightVal);
								/** Produced solar power */
								Float solarPowerVal = Float.parseFloat(valuesPerLine[5]);
								/** Consumed power */
								Float consPowerVal = Float.parseFloat(valuesPerLine[6]);
								solarPowerCont.add(solarPowerVal);
								consumPowerCont.add(solarPowerVal + consPowerVal);
							}

							initChart(true);
							Utilities.stopRefreshAnim();
						}
					}
					isFill = false;
				} else {
					resultTextView.setText(value);
					Utilities.stopRefreshAnim();
				}
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
		thisCommand = "s";
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

		// find the temperature levels plot in the layout
		currentPlot = (GraphView) findViewById(R.id.graph);
		// setup and format sensor 1 data series
		solarSeries = new LineGraphSeries<>();
		// setup and format sensor 2 data series
		consSeries = new LineGraphSeries<>();
		// setup and format sensor 3 data series
		lightSeries = new LineGraphSeries<>();

		currentPlot.getGridLabelRenderer().setNumVerticalLabels(10);

		/** Instance of display */
		Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
		/** Orientation of the display */
		int orientation = display.getRotation();

		if (Utilities.isTablet(appContext)) {
			if (orientation == Surface.ROTATION_90
					|| orientation == Surface.ROTATION_270) {
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(5);
			} else {
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(10);
			}
		} else {
			/** Display metrics */
			DisplayMetrics metrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(metrics);
			if (orientation == Surface.ROTATION_90
					|| orientation == Surface.ROTATION_270) { // Landscape on phone
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(5);
				if (metrics.heightPixels < 481) { // On small screens we remove some check boxes
					/** Checkbox to select if series is visible or not */
					CheckBox showSeries = (CheckBox) findViewById(R.id.cb_cons);
					showSeries.setVisibility(View.INVISIBLE);
					showSeries = (CheckBox) findViewById(R.id.cb_solar);
					showSeries.setVisibility(View.INVISIBLE);
					showSeries = (CheckBox) findViewById(R.id.cb_light);
					showSeries.setVisibility(View.INVISIBLE);
				}
			} else { // Portrait on phone
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(3);
				if (metrics.widthPixels < 481) { // On small screens we remove some check boxes
					/** Checkbox to select if series is visible or not */
					CheckBox showSeries = (CheckBox) findViewById(R.id.cb_cons);
					showSeries.setVisibility(View.INVISIBLE);
					showSeries = (CheckBox) findViewById(R.id.cb_solar);
					showSeries.setVisibility(View.INVISIBLE);
					showSeries = (CheckBox) findViewById(R.id.cb_light);
					showSeries.setVisibility(View.INVISIBLE);
				}
			}
		}

		solarSeries.setColor(0xFFFFBB33);
		consSeries.setColor(0xFF33B5E5);
		lightSeries.setColor(Color.GREEN);
		solarSeries.setBackgroundColor(0x55FFBB33);
		consSeries.setBackgroundColor(0xFF33B5E5);
		solarSeries.setDrawBackground(true);
		consSeries.setDrawBackground(true);

		currentPlot.getGridLabelRenderer().setVerticalAxisTitle("Watt");
		currentPlot.getGridLabelRenderer().setVerticalAxisTitleColor(Color.WHITE);
		currentPlot.getGridLabelRenderer().setGridColor(Color.WHITE);
		currentPlot.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.BOTH);
		currentPlot.getGridLabelRenderer().setVerticalLabelsColor(Color.WHITE);
		currentPlot.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.WHITE);
		currentPlot.getGridLabelRenderer().setHorizontalLabelsColor(Color.WHITE);

		currentPlot.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
			@SuppressLint("SimpleDateFormat")
			@Override
			public String formatLabel(double value, boolean isValueX) {
				/** Simple date format to format y scale of graph */
				SimpleDateFormat dateFormat;
				if (isValueX) {
					if (showingLog || !calModeOn) {
						dateFormat = new SimpleDateFormat("HH:mm");
					} else {
						dateFormat = new SimpleDateFormat("HH:mm:ss");
					}
					/** Date to be shown as y scale of graph */
					Date d = new Date((long) (value));
					return (dateFormat.format(d));
				}
				return "" + (int) value;
			}
		});

		if (!isContinuous) {
			for (int i=0; i<solarPower.size(); i++) {
				solarSeries.appendData(new DataPoint(timeStamps.get(i), solarPower.get(i)), true, timeStamps.size());
			}
			for (int i=0; i<consumPower.size(); i++) {
				consSeries.appendData(new DataPoint(timeStamps.get(i), consumPower.get(i)), true, timeStamps.size());
			}
			for (int i= 0; i<lightValue.size(); i++) {
				lightSeries.appendData(new DataPoint(timeStamps.get(i), lightValue.get(i)), true, timeStamps.size());
			}

			minMaxVal = Utilities.getMinMax();
			if (minMaxVal[0] - 10 <= 0) {
				currentPlot.getViewport().setMinY(0f);
			} else {
				currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
			}
			currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);
			currentPlot.getSecondScale().setMinY(lightSeries.getLowestValueY());
			currentPlot.getSecondScale().setMaxY(lightSeries.getHighestValueY());
			currentPlot.getViewport().setMinX(solarSeries.getLowestValueX());
			currentPlot.getViewport().setMaxX(solarSeries.getHighestValueX());
			currentPlot.addSeries(consSeries);
			// TODO Workaround for series using second scale -- cannot be removed
			currentPlot.getSecondScale().addSeries(lightSeries);
			if (!showLight) lightSeries.setColor(Color.TRANSPARENT);
			currentPlot.addSeries(solarSeries);
		} else {
			if (timeStampsCont.size() != 0) {
				for (int i=0; i<solarPowerCont.size(); i++) {
					solarSeries.appendData(new DataPoint(timeStampsCont.get(i),
							solarPowerCont.get(i)), true, timeStampsCont.size());
				}
				for (int i=0; i<consumPowerCont.size(); i++) {
					consSeries.appendData(new DataPoint(timeStampsCont.get(i),
							consumPowerCont.get(i)), true, timeStampsCont.size());
				}
				for (int i=0; i<lightValueCont.size(); i++) {
					lightSeries.appendData(new DataPoint(timeStampsCont.get(i),
							lightValueCont.get(i)), true, timeStampsCont.size());
				}

				minMaxVal = Utilities.getMinMax();
				if (minMaxVal[0] - 10 <= 0) {
					currentPlot.getViewport().setMinY(0f);
				} else {
					currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
				}
				currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);

				if (showCons) currentPlot.addSeries(consSeries);
				// TODO Workaround for series using second scale -- cannot be removed
				currentPlot.getSecondScale().addSeries(lightSeries);
				if (!showLight) lightSeries.setColor(Color.TRANSPARENT);

				if (showSolar) currentPlot.addSeries(solarSeries);

				graph2LastXValue = timeStampsCont.size();
			} else {
				currentPlot.getViewport().setMinY(0f);
				currentPlot.getViewport().setMaxY(3000f);
			}
			currentPlot.setTitle("");
		}

		currentPlot.getViewport().setScalable(true);
		currentPlot.getViewport().setScrollable(true);
		currentPlot.getViewport().setXAxisBoundsManual(true);
		currentPlot.getViewport().setYAxisBoundsManual(true);
		currentPlot.setTitle(dayToShow);

		solarSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
			@SuppressLint("SimpleDateFormat")
			@Override
			public void onTap(Series series, DataPointInterface dataPoint) {
				/** Simple date format to show time of tapped data point */
				SimpleDateFormat dateFormat;
				if (showingLog || !calModeOn) {
					dateFormat = new SimpleDateFormat("HH:mm");
				} else {
					dateFormat = new SimpleDateFormat("HH:mm:ss");
				}
				/** Date of tapped data point */
				Date d = new Date((long) (dataPoint.getX()));
				/** Date of tapped data point as string*/
				String dateTapped = dateFormat.format(d);
				Toast.makeText(appContext, "Solar: " + String.format("%.0f", dataPoint.getY()) +
						"W at " + dateTapped, Toast.LENGTH_SHORT).show();
				/** Pointer to text view to show value */
				TextView valueFields = (TextView) findViewById(R.id.tv_solar_val);
				valueFields.setText(String.format("%.0f", dataPoint.getY()) + "W");
				valueFields = (TextView) findViewById(R.id.tv_result);
				valueFields.setText("Solar: " + String.format("%.0f", dataPoint.getY()) +
						"W at " + dateTapped);
			}
		});

		consSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
			@SuppressLint("SimpleDateFormat")
			@Override
			public void onTap(Series series, DataPointInterface dataPoint) {
				/** Simple date format to show time of tapped data point */
				SimpleDateFormat dateFormat;
				if (showingLog || !calModeOn) {
					dateFormat = new SimpleDateFormat("HH:mm");
				} else {
					dateFormat = new SimpleDateFormat("HH:mm:ss");
				}
				/** Date of tapped data point */
				Date d = new Date((long) (dataPoint.getX()));
				/** Date of tapped data point as string*/
				String dateTapped = dateFormat.format(d);
				Toast.makeText(appContext, "Consumption: " + String.format("%.0f", dataPoint.getY()) +
						"W at " + dateTapped, Toast.LENGTH_LONG).show();
				/** Pointer to text view to show value */
				TextView valueFields = (TextView) findViewById(R.id.tv_cons_val);
				valueFields.setText(String.format("%.0f", dataPoint.getY()) + "W");
				valueFields = (TextView) findViewById(R.id.tv_result);
				valueFields.setText("Consumption: " + String.format("%.0f", dataPoint.getY()) +
						"W at " + dateTapped);
			}
		});

		//lightSeries.setOnDataPointTapListener(new OnDataPointTapListener() {
		//	@Override
		//	public void onTap(Series series, DataPointInterface dataPoint) {
		//		Toast.makeText(appContext, "Sensor 3: " + String.format("%.0f", dataPoint.getY()) + "lux", Toast.LENGTH_LONG).show();
		//		/** Pointer to text view to show value */
		//		TextView valueFields = (TextView) findViewById(R.id.tv_light_value);
		//		valueFields.setText(String.format("%.0f", dataPoint.getY()) + "lux");
		//	}
		//});

		currentPlot.onDataChanged(false, false);
	}

	/**
	 * Clean up chart for a new initialization
	 */
	private void clearChart() {
		/** Gregorian calendar to create a time stamp */
		Calendar cal = new GregorianCalendar();
		cal.set(1970, 1, 1);
		/** Current time in milli seconds */
		long timeInMillis = cal.getTimeInMillis();
		timeStampsCont.add(timeInMillis);

		graph2LastXValue = 1d;

		/* New data  point to refresh the series */
		DataPoint[] values = new DataPoint[1];
		values[0] = new DataPoint(timeInMillis, solarPowerMin);
		solarSeries.resetData(values);
		values[0] = new DataPoint(timeInMillis, consPowerMin);
		consSeries.resetData(values);
		values[0] = new DataPoint(timeInMillis, lightValMin);
		lightSeries.resetData(values);
		currentPlot.onDataChanged(false, false);
	}
}
