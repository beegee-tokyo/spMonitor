package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
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
 * @version 0 beta July 12, 2015.
 */
public class spMonitor extends Activity implements View.OnClickListener
		, AdapterView.OnItemClickListener{

	/** Application context */
	public static Context appContext;

	public static View appView;

	private String url = "";
	public static String deviceIP = "no IP saved";
	public static final OkHttpClient client = new OkHttpClient();
	private Timer timer;
	//we are going to use a handler to be able to run in our TimerTask
	private final Handler handler = new Handler();
	/** Access to shared preferences of application*/
	public static SharedPreferences mPrefs;
	private static TextView resultTextView;
	private boolean autoRefreshOn = true;
	private String thisCommand = "";
	public static String calValue1 = "6.060606";
	public static String calValue2 = "6.060606";
	/** XYPlot view for the current chart */
	private static GraphView currentPlot;
	/** Data series for the sensor 1 */
	public static LineGraphSeries<DataPoint> sensor1Series = null;
	/** Data series for the sensor 2 */
	public static LineGraphSeries<DataPoint> sensor2Series = null;
	/** Data series for the sensor 2 */
	public static LineGraphSeries<DataPoint> sensor3Series = null;
	/** Counter for displayed data points */
	private double graph2LastXValue = 0d;
	/** Number of plot y values */
	private static final int plotValues = 720;
	/** Array with existing log files on the Arduino */
	private static final List<String> logFiles = new ArrayList<>();
	/** String with path and filename to location of log file */
	private static String restoreFile;
	/** Flag for result of file selection dialog */
	private static boolean isFileSelected = false;
	/** Flag for SFTP command */
	private boolean isGet = false;
	/** Flag for showing a log */
	private boolean showingLog = false;
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
	/** Day stamp of data */
	private String dayToShow;
	private static Float currToPower1 = 0.0f;
	private static Float currToPower2 = 0.0f;
	long lightVal = 0;
	public static boolean showSeries1 = true;
	public static boolean showSeries2 = true;
	public static boolean showSeries3 = false;

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

		resultTextView = (TextView) findViewById(R.id.tv_result);
		deviceIP = mPrefs.getString("spMonitorIP", "no IP saved");

		boolean savedStatus = false;
		if (savedInstanceState != null) {
			savedStatus = savedInstanceState.getBoolean("recreation");
			showingLog = savedInstanceState.getBoolean("showingLog");
		}

		if (!savedStatus) {
			initChart(true);

			if (!deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
				Utilities.startRefreshAnim();
				url = deviceIP;
				new execSftp().execute(url, "ls");
			}
		} else {
			if (showingLog) {
				long[] timeStampsList = savedInstanceState.getLongArray("timeStamps");
				float[] solarPowerList = savedInstanceState.getFloatArray("solarPower");
				float[] consumPowerList = savedInstanceState.getFloatArray("consumPower");
				long[] lightValueList = savedInstanceState.getLongArray("lightValue");

				timeStamps.clear();
				solarPower.clear();
				consumPower.clear();
				lightValue.clear();
				for (long aTimeStampsList : timeStampsList) {
					timeStamps.add(aTimeStampsList);
				}
				for (float aSolarPowerList : solarPowerList) {
					solarPower.add(aSolarPowerList);
				}
				for (float aConsumPowerList : consumPowerList) {
					consumPower.add(aConsumPowerList);
				}
				for (long aLightValueList : lightValueList) {
					lightValue.add(aLightValueList);
				}
				initChart(false);
			} else {
				long[] timeStampsListCont = savedInstanceState.getLongArray("timeStampsCont");
				float[] solarPowerListCont = savedInstanceState.getFloatArray("solarPowerCont");
				float[] consumPowerListCont = savedInstanceState.getFloatArray("consumPowerCont");
				long[] lightValueListCont = savedInstanceState.getLongArray("lightValueCont");
				showSeries1 = savedInstanceState.getBoolean("showSeries1");
				showSeries2 = savedInstanceState.getBoolean("showSeries2");
				showSeries3 = savedInstanceState.getBoolean("showSeries3");

				CheckBox cbShowSeries = (CheckBox)findViewById(R.id.cb_solar);
				if (showSeries1) {
					cbShowSeries.setChecked(true);
				} else {
					cbShowSeries.setChecked(false);
				}
				cbShowSeries = (CheckBox)findViewById(R.id.cb_cons);
				if (showSeries2) {
					cbShowSeries.setChecked(true);
				} else {
					cbShowSeries.setChecked(false);
				}
				cbShowSeries = (CheckBox)findViewById(R.id.cb_light);
				if (showSeries3) {
					cbShowSeries.setChecked(true);
				} else {
					cbShowSeries.setChecked(false);
				}

				timeStampsCont.clear();
				solarPowerCont.clear();
				consumPowerCont.clear();
				lightValueCont.clear();
				for (long aTimeStampsListCont : timeStampsListCont) {
					timeStampsCont.add(aTimeStampsListCont);
				}
				for (float aSolarPowerListCont : solarPowerListCont) {
					solarPowerCont.add(aSolarPowerListCont);
				}
				for (float aConsumPowerListCont : consumPowerListCont) {
					consumPowerCont.add(aConsumPowerListCont);
				}
				for (long aLightValueListCont : lightValueListCont) {
					lightValueCont.add(aLightValueListCont);
				}
				initChart(true);
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
			if (autoRefreshOn) startTimer(1);
			EditText calEditText = (EditText) findViewById(R.id.et_cal1);
			calEditText.setText(calValue1);
			calEditText = (EditText) findViewById(R.id.et_cal2);
			calEditText.setText(calValue2);
		} else {
			resultTextView.setText(getResources().getString(R.string.err_no_device));
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (autoRefreshOn) stopTimer();
	}

	@Override
	protected void onSaveInstanceState(@SuppressWarnings("NullableProblems") Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		savedInstanceState.putBoolean("recreation", true);
		savedInstanceState.putBoolean("showingLog", showingLog);
		long[] timeStampsList = new long[timeStamps.size()];
		float[] solarPowerList = new float[solarPower.size()];
		float[] consumPowerList = new float[consumPower.size()];
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

		long[] timeStampsListCont = new long[timeStampsCont.size()];
		float[] solarPowerListCont = new float[timeStampsCont.size()];
		float[] consumPowerListCont = new float[timeStampsCont.size()];
		long[] lightValueListCont = new long[timeStampsCont.size()];
		for (int i=0; i<timeStampsCont.size(); i++) {
			timeStampsListCont[i] = timeStampsCont.get(i);
		}
		for (int i=0; i<solarPowerCont.size(); i++) {
			solarPowerListCont[i] = solarPowerCont.get(i);
		}
		for (int i=0; i<consumPowerCont.size(); i++) {
			consumPowerListCont[i] = consumPowerCont.get(i);
		}
		for (int i=0; i<lightValueCont.size(); i++) {
			lightValueListCont[i] = lightValueCont.get(i);
		}
		savedInstanceState.putLongArray("timeStampsCont", timeStampsListCont);
		savedInstanceState.putFloatArray("solarPowerCont", solarPowerListCont);
		savedInstanceState.putFloatArray("consumPowerCont", consumPowerListCont);
		savedInstanceState.putLongArray("lightValueCont", lightValueListCont);

		savedInstanceState.putBoolean("showSeries1", showSeries1);
		savedInstanceState.putBoolean("showSeries2", showSeries2);
		savedInstanceState.putBoolean("showSeries3", showSeries3);
	}

	@Override
	public void onClick(View v) {
		url = "";
		client.setConnectTimeout(30, TimeUnit.SECONDS); // connect timeout
		client.setReadTimeout(30, TimeUnit.SECONDS);    // socket timeout
		ImageView activeButton = (ImageView) findViewById(R.id.iv_update);
		double[] minMaxVal;
		switch (v.getId()) {
			case R.id.bt_getLog:
				url = "";
				/** List view to add onItemClickListener */
				ListView lvFileList = getLogFileDialog();
				if (lvFileList != null) {
					lvFileList.setOnItemClickListener(this);
				}
				break;
			case R.id.bt_stop:
				if (autoRefreshOn) {
					stopTimer();
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setText(getResources().getString(R.string.start));
					autoRefreshOn = false;
				} else {
					if (showingLog) {
						showingLog = false;
						graph2LastXValue = 0d;
						//clearChart();
						initChart(true);
					}
					startTimer(1);
					Button stopButton = (Button) findViewById(R.id.bt_stop);
					stopButton.setText(getResources().getString(R.string.stop));
					autoRefreshOn = true;
				}
				url = "";
				break;
			case R.id.bt_cal1:
				EditText calEditText = (EditText) findViewById(R.id.et_cal1);
				String calVal1 = calEditText.getText().toString();
				url = deviceIP + "c1" + calVal1;
				thisCommand = "c";
				break;
			case R.id.bt_cal2:
				calEditText = (EditText)findViewById(R.id.et_cal2);
				String calVal2 = calEditText.getText().toString();
				url = deviceIP + "c2" + calVal2;
				thisCommand = "c";
				break;
			case R.id.bt_status:
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
				CheckBox cbSolar = (CheckBox)findViewById(R.id.cb_solar);
				if (cbSolar.isChecked()) {
					currentPlot.addSeries(sensor1Series);
					showSeries1 = true;
				} else {
					currentPlot.removeSeries(sensor1Series);
					showSeries1 = false;
				}

				minMaxVal = Utilities.getMinMax();
				currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
				currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);

				currentPlot.onDataChanged(false, false);
				break;
			case R.id.cb_cons:
				CheckBox cbCons = (CheckBox)findViewById(R.id.cb_cons);
				if (cbCons.isChecked()) {
					currentPlot.addSeries(sensor2Series);
					showSeries2 = true;
				} else {
					currentPlot.removeSeries(sensor2Series);
					showSeries2 = false;
				}

				minMaxVal = Utilities.getMinMax();
				currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
				currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);

				currentPlot.onDataChanged(false, false);
				break;
			case R.id.cb_light:
				CheckBox cbLight = (CheckBox)findViewById(R.id.cb_light);
				if (cbLight.isChecked()) {
					currentPlot.getSecondScale().addSeries(sensor3Series);
					sensor3Series.setColor(Color.GREEN);
					showSeries3 = true;
				} else {
					currentPlot.removeSeries(sensor3Series);
					// TODO Workaround for series using second scale -- cannot be removed
					sensor3Series.setColor(Color.TRANSPARENT);
					showSeries3 = false;
				}

				currentPlot.getSecondScale().setMinY(sensor3Series.getLowestValueY());
				currentPlot.getSecondScale().setMaxY(sensor3Series.getHighestValueY());

				currentPlot.onDataChanged(false, false);
				break;
			case R.id.iv_update:
				mPrefs = getSharedPreferences("spMonitor", 0);
				deviceIP = mPrefs.getString("spMonitorIP","no IP saved");
				if (deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
					resultTextView.setText(getResources().getString(R.string.search_device));
					deviceIP = Utilities.searchDeviceIP();
					if (deviceIP.equalsIgnoreCase("")) {
						resultTextView.setText(getResources().getString(R.string.err_no_device));
					} else {
						mPrefs.edit().putString("spMonitorIP", deviceIP).apply();
					}
				} else {
					String[] outValues = deviceIP.split("/");
					String result = Utilities.checkDeviceIP(outValues[2]);
					if (result.startsWith("Freq")) {
						resultTextView.setText(getResources().getString(R.string.found_device, outValues[2]+"\n"+result));
						deviceIP = "http://"+outValues[2]+"/arduino/";
					} else {
						resultTextView.setText(getResources().getString(R.string.search_device));
						deviceIP = Utilities.searchDeviceIP();
						if (deviceIP.equalsIgnoreCase("")) {
							resultTextView.setText(getResources().getString(R.string.err_no_device));
						} else {
							mPrefs.edit().putString("spMonitorIP", deviceIP).apply();
						}
					}
				}
				break;
		}

		if (!url.isEmpty()) {
			activeButton.setBackgroundColor(0xFF00FF00);
			if (autoRefreshOn) stopTimer();
			new callArduino().execute(url);
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		restoreFile = logFiles.get(position);
		isFileSelected = true;
	}

	private class callArduino extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			String urlString=params[0]; // URL to call
			String resultToDisplay = "";

			// Make call only if valid url is given
			if (urlString.startsWith("No")) {
				resultToDisplay = getResources().getString(R.string.err_no_device);
			} else {
				Request request = new Request.Builder()
						.url(urlString)
						.build();

				if (request != null) {
					try {
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
			int strLen = result.length();
			if ((strLen > 2) && (url.endsWith("g"))) {
				result = result.substring(0, strLen - 2);
			}
			updateText(result);
			if (timer == null) {
				if (autoRefreshOn) startTimer(5000);
			}
		}
	}

	private class execSftp extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			String urlString = params[0]; // URL to call
			String sftpCommand = params[1]; // What to do
			String logFileName = "";
			if (sftpCommand.equalsIgnoreCase("get") || sftpCommand.equalsIgnoreCase("rm")) {
				logFileName = params[2]; // Which log file to get
			}

			String resultToDisplay = "";

			// Make call only if valid url is given
			if (urlString.startsWith("No")) {
				resultToDisplay = getResources().getString(R.string.err_no_device);
			} else {
				String ip[] = urlString.split("/");
				String username = "root";
				String password = "spMonitor";
				String hostname = ip[2];
				int port = 22;
				Session session = null;

				try{
					JSch sshChannel = new JSch();
					session = sshChannel.getSession(username, hostname, port);
					session.setPassword(password);
					session.setConfig("StrictHostKeyChecking", "no");
					session.connect();

					if (session.isConnected()) {
						Channel channel = session.openChannel("sftp");
						channel.connect();

						if (channel.isConnected()) {
							ChannelSftp sftp = (ChannelSftp) channel;

							if (sftp.isConnected()) {
								if (sftpCommand.equalsIgnoreCase("get")) {
									sftp.cd("/mnt/sda1/");
									BufferedReader buffInput = new BufferedReader(new InputStreamReader(sftp.get(logFileName)));
									String line;
									while ((line = buffInput.readLine()) != null) {
										System.out.println(line);
										resultToDisplay += line;
										resultToDisplay += "\n";
									}
									buffInput.close();
									sftp.disconnect();
									isGet = true;
								} else if (sftpCommand.equalsIgnoreCase("rm")) {
									sftp.cd("/mnt/sda1/");
									sftp.rm(logFileName);
									resultToDisplay = getResources().getString(R.string.log_rm_title, logFileName);
									sftp.disconnect();
								} else if (sftpCommand.equalsIgnoreCase("ls")) {
									sftp.cd("/mnt/sda1/");
									@SuppressWarnings("unchecked") Vector<ChannelSftp.LsEntry> list = sftp.ls("/mnt/sda1/*datalog*");
									if (list.size() != 0) {
										logFiles.clear();
										for (int i=0; i<list.size();i++) {
											logFiles.add(list.get(i).getFilename());
										}
										Collections.sort(logFiles, new Comparator<String>() {
											@Override
											public int compare(String text1, String text2) {
												return text1.compareToIgnoreCase(text2);
											}
										});
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
			// TODO if log file was read, clear chart and fill with new data
			displayLog(result);
			if (timer == null) {
				if (autoRefreshOn) startTimer(5000);
			}
		}
	}

	private void updateText(final String value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				TextView valueFields;
				String result;
				ImageView activeButton = (ImageView) findViewById(R.id.iv_update);

				if (value.length() != 0) {
					if (thisCommand.equalsIgnoreCase("s")) {
						// decode JSON
						if (Utilities.isJSONValid(value)) {
							try {
								/** JSON object containing result from server */
								JSONObject jsonResult = new JSONObject(value);
								/** JSON object containing the values */
								JSONObject jsonValues = jsonResult.getJSONObject("value");

								valueFields = (TextView) findViewById(R.id.tv_sens1_value);
								currToPower1 = Float.parseFloat(jsonValues.getString("Curr1"));
								currToPower1 = currToPower1 * 230f;
								String currFromJSON = jsonValues.getString("Curr1");
								valueFields.setText(currFromJSON.substring(0,
										currFromJSON.indexOf(".") + 3) + "A " +
										String.format("%.0f", currToPower1) + "W");

								valueFields = (TextView) findViewById(R.id.tv_sens2_value);
								currToPower2 = Float.parseFloat(jsonValues.getString("Curr2"));
								currToPower2 = currToPower2 * 230f;
								currFromJSON = jsonValues.getString("Curr2");
								valueFields.setText(currFromJSON.substring(0,
										currFromJSON.indexOf(".") + 3) + "A " +
										String.format("%.0f", currToPower2) + "W");

								valueFields = (TextView) findViewById(R.id.tv_light_value);
								valueFields.setText(jsonValues.getString("Light") + "lux");
								// TODO debug only as we do not have light data at the moment
								float lightValFloat = Float.parseFloat(jsonValues.getString("Curr1")) * 230f * 46f;
								lightVal = (long) lightValFloat;
								valueFields.setText(String.valueOf(lightVal) + "lux");

								valueFields = (TextView) findViewById(R.id.tv_result);
								valueFields.setText(String.valueOf(currToPower2 - currToPower1) + "W");
								valueFields.setText(String.format("%.0f", currToPower2 - currToPower1) + "W");

								if (autoRefreshOn) {
									Calendar cal = new GregorianCalendar();
									cal.set(1970, 1, 1);
									graph2LastXValue += 1d;
									long timeInMillis = cal.getTimeInMillis();
									timeStampsCont.add(timeInMillis);
									sensor1Series.appendData(new DataPoint(timeInMillis,
											currToPower1), true, plotValues);
									solarPowerCont.add(currToPower1);
									sensor2Series.appendData(new DataPoint(timeInMillis,
											currToPower2), true, plotValues);
									consumPowerCont.add(currToPower2);
									sensor3Series.appendData(new DataPoint(timeInMillis, lightVal), true, plotValues);
									lightValueCont.add(lightVal);

									double[] minMaxVal;
									minMaxVal = Utilities.getMinMax();
									currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
									currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);
									currentPlot.getSecondScale().setMinY(sensor3Series.getLowestValueY());
									currentPlot.getSecondScale().setMaxY(sensor3Series.getHighestValueY());
									currentPlot.getViewport().setMinX(sensor1Series.getLowestValueX());
									currentPlot.getViewport().setMaxX(sensor1Series.getHighestValueX());

									if (graph2LastXValue == 2) {
										if (showSeries2) currentPlot.addSeries(sensor2Series);
										// TODO for series using second scale -- cannot be removed
										currentPlot.getSecondScale().addSeries(sensor3Series);
										if (!showSeries3) sensor3Series.setColor(Color.TRANSPARENT);
										if (showSeries1) currentPlot.addSeries(sensor1Series);
									}
								}

								Utilities.stopRefreshAnim();
								activeButton.setBackgroundColor(0xFF000000);
								return;
							} catch (Exception excError) {
								Utilities.stopRefreshAnim();
								activeButton.setBackgroundColor(0xFF000000);
								return;
							}
						}
					}
					resultTextView.setText(value);
					Utilities.stopRefreshAnim();
					activeButton.setBackgroundColor(0xFF000000);
					return;
				}
				result = "\n";
				resultTextView.setText(result);
				Utilities.stopRefreshAnim();
				activeButton.setBackgroundColor(0xFF000000);
			}
		});
	}

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
						String[] recordLines = value.split("\n");
						if (recordLines.length != 0) {
							for (String recordLine : recordLines) {
								String[] valuesPerLine = recordLine.split(",");
								dayToShow = valuesPerLine[0] + "/" + valuesPerLine[1] + "/" + valuesPerLine[2];
								String[] hourSplit = valuesPerLine[3].split(":");
								Calendar timeCal = new GregorianCalendar(
										Integer.parseInt(valuesPerLine[0]) + 2000,
										Integer.parseInt(valuesPerLine[1]),
										Integer.parseInt(valuesPerLine[2]),
										Integer.parseInt(hourSplit[0]),
										Integer.parseInt(hourSplit[1]),
										0);
								timeStamps.add(timeCal.getTimeInMillis());
								long lightVal = Long.parseLong(valuesPerLine[4]);
								// TODO debug only as we do not have light data at the moment
								float lightValFloat = Float.parseFloat(valuesPerLine[5]) * 230f * 46f;
								lightVal = (long) lightValFloat;

								lightValue.add(lightVal);
								solarPower.add(Float.parseFloat(valuesPerLine[5]) * 230);
								consumPower.add(Float.parseFloat(valuesPerLine[6]) * 230);
							}
							//clearChart();
							initChart(false);
							Utilities.stopRefreshAnim();
						}
					}
				} else {
					resultTextView.setText(value);
					Utilities.startRefreshAnim();
					ImageView activeButton = (ImageView) findViewById(R.id.iv_update);
					activeButton.setBackgroundColor(0xFF000000);
				}
			}
		});
	}

	private void startTimer(int startDelay) {
		// Start new single shot every 10 seconds
		timer = new Timer();
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
		//schedule the timer, after startDelay ms the TimerTask will run every 10000ms
		timer.schedule(timerTask, startDelay, 10000); //
	}

	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	private void autoRefresh() {
		Utilities.startRefreshAnim();
		ImageView activeButton = (ImageView) findViewById(R.id.iv_update);
		activeButton.setBackgroundColor(0xFFFF0000);

		String[] ipValues = deviceIP.split("/");
		url = "http://"+ipValues[2]+"/data/get";
		thisCommand = "s";
		new callArduino().execute(url);
	}

	private void initChart(boolean isContinuous) {

		// find the temperature levels plot in the layout
		currentPlot = (GraphView) findViewById(R.id.graph);
		// setup and format sensor 1 data series
		sensor1Series = new LineGraphSeries<>();
		// setup and format sensor 2 data series
		sensor2Series = new LineGraphSeries<>();
		// setup and format sensor 3 data series
		sensor3Series = new LineGraphSeries<>();

		currentPlot.getGridLabelRenderer().setNumVerticalLabels(10);
		if (Utilities.isTablet(appContext)) {
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			int orientation = display.getRotation();
			if (orientation == Surface.ROTATION_90
					|| orientation == Surface.ROTATION_270) {
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(5);
			} else {
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(10);
			}
		} else {
			Display display = ((WindowManager) getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
			int orientation = display.getRotation();
			if (orientation == Surface.ROTATION_90
					|| orientation == Surface.ROTATION_270) {
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(5);
			} else {
				currentPlot.getGridLabelRenderer().setNumHorizontalLabels(3);
			}
		}

		sensor1Series.setColor(Color.YELLOW);
		sensor2Series.setColor(Color.RED);
		sensor3Series.setColor(Color.GREEN);
		currentPlot.getGridLabelRenderer().setVerticalAxisTitle("Watt");

		currentPlot.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
			@SuppressLint("SimpleDateFormat")
			@Override
			public String formatLabel(double value, boolean isValueX) {
				SimpleDateFormat dateFormat;
				if (isValueX) {
					if (showingLog) {
						dateFormat = new SimpleDateFormat("HH:mm");
					} else {
						dateFormat = new SimpleDateFormat("HH:mm:ss");
					}
					Date d = new Date((long) (value));
					return (dateFormat.format(d));
				}
				return "" + (int) value;
			}
		});

		double[] minMaxVal;
		if (!isContinuous) {
			for (int i=0; i<solarPower.size(); i++) {
				sensor1Series.appendData(new DataPoint(timeStamps.get(i), solarPower.get(i)),true,timeStamps.size());
			}
			for (int i=0; i<consumPower.size(); i++) {
				sensor2Series.appendData(new DataPoint(timeStamps.get(i), consumPower.get(i)),true,timeStamps.size());
			}
			for (int i= 0; i<lightValue.size(); i++) {
				sensor3Series.appendData(new DataPoint(timeStamps.get(i), lightValue.get(i)), true, timeStamps.size());
			}
			currentPlot.setTitle(dayToShow);

			minMaxVal = Utilities.getMinMax();
			currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
			currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);
			currentPlot.getSecondScale().setMinY(sensor3Series.getLowestValueY());
			currentPlot.getSecondScale().setMaxY(sensor3Series.getHighestValueY());
			currentPlot.getViewport().setMinX(sensor1Series.getLowestValueX());
			currentPlot.getViewport().setMaxX(sensor1Series.getHighestValueX());
			currentPlot.getViewport().setXAxisBoundsManual(true);
			currentPlot.getViewport().setYAxisBoundsManual(true);
			currentPlot.getViewport().setScalable(true);
			currentPlot.getViewport().setScrollable(true);
			currentPlot.addSeries(sensor2Series);
			// TODO Workaround for series using second scale -- cannot be removed
			currentPlot.getSecondScale().addSeries(sensor3Series);
			if (!showSeries3) sensor3Series.setColor(Color.TRANSPARENT);
			currentPlot.addSeries(sensor1Series);
		} else {
			if (timeStampsCont.size() != 0) {
				for (int i=0; i<solarPowerCont.size(); i++) {
					sensor1Series.appendData(new DataPoint(timeStampsCont.get(i), solarPowerCont.get(i)), true, timeStampsCont.size());
				}
				for (int i=0; i<consumPowerCont.size(); i++) {
					sensor2Series.appendData(new DataPoint(timeStampsCont.get(i), consumPowerCont.get(i)),true,timeStampsCont.size());
				}
				for (int i=0; i<lightValueCont.size(); i++) {
					sensor3Series.appendData(new DataPoint(timeStampsCont.get(i), lightValueCont.get(i)),true,timeStampsCont.size());
				}

				minMaxVal = Utilities.getMinMax();
				currentPlot.getViewport().setMinY(minMaxVal[0] - 10f);
				currentPlot.getViewport().setMaxY(minMaxVal[1] + 10f);

				if (showSeries2) currentPlot.addSeries(sensor2Series);
				// TODO Workaround for series using second scale -- cannot be removed
				currentPlot.getSecondScale().addSeries(sensor3Series);
				if (!showSeries3) sensor3Series.setColor(Color.TRANSPARENT);
				if (showSeries1) currentPlot.addSeries(sensor1Series);

				graph2LastXValue = timeStampsCont.size();
			} else {
				currentPlot.getViewport().setMinY(0f);
				currentPlot.getViewport().setMaxY(3000f);
			}
			currentPlot.getViewport().setXAxisBoundsManual(true);
			currentPlot.getViewport().setYAxisBoundsManual(true);
			currentPlot.getViewport().setScalable(false);
			currentPlot.getViewport().setScrollable(false);
			currentPlot.setTitle("");
		}
		sensor1Series.setOnDataPointTapListener(new OnDataPointTapListener() {
			@Override
			public void onTap(Series series, DataPointInterface dataPoint) {
				Toast.makeText(appContext, "Sensor 1: " + dataPoint.getY() + "W", Toast.LENGTH_LONG).show();
				TextView valueFields = (TextView) findViewById(R.id.tv_sens1_value);
				valueFields.setText(String.valueOf(dataPoint.getY()) + "W");
			}
		});
		sensor2Series.setOnDataPointTapListener(new OnDataPointTapListener() {
			@Override
			public void onTap(Series series, DataPointInterface dataPoint) {
				Toast.makeText(appContext, "Sensor 2: " + dataPoint.getY() + "W", Toast.LENGTH_LONG).show();
				TextView valueFields = (TextView) findViewById(R.id.tv_sens2_value);
				valueFields.setText(String.valueOf(dataPoint.getY()) + "W");
			}
		});
		sensor3Series.setOnDataPointTapListener(new OnDataPointTapListener() {
			@Override
			public void onTap(Series series, DataPointInterface dataPoint) {
				Toast.makeText(appContext, "Sensor 3: " + dataPoint.getY() + "lux", Toast.LENGTH_LONG).show();
				TextView valueFields = (TextView) findViewById(R.id.tv_light_value);
				valueFields.setText(String.valueOf(dataPoint.getY()) + "lux");
			}
		});

		currentPlot.onDataChanged(false, false);
	}

	/**
	 * Clean up temperature chart for a new initialization
	 */
	private void clearChart() {
		Calendar cal = new GregorianCalendar();
		cal.set(1970, 1, 1);
		long timeInMillis = cal.getTimeInMillis();
		timeStampsCont.add(timeInMillis);

		graph2LastXValue = 1d;

		DataPoint[] values = new DataPoint[1];
		values[0] = new DataPoint(timeInMillis,currToPower1);
		sensor1Series.resetData(values);
		values[0] = new DataPoint(timeInMillis,currToPower2);
		sensor2Series.resetData(values);
		values[0] = new DataPoint(timeInMillis,lightVal);
		sensor3Series.resetData(values);
		//currentPlot.removeAllSeries();
	}

	/**
	 * Show dialog with available log files
	 */
	private ListView getLogFileDialog() {
		if (logFiles.size() != 0) {
			/** Builder for restore file selection dialog */
			AlertDialog.Builder fileListBuilder = new AlertDialog.Builder(appContext);
			/** Inflater for restore file selection dialog */
			LayoutInflater fileListInflater = (LayoutInflater) appContext.getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			/** View for restore file selection dialog */
			@SuppressLint("InflateParams") View fileListView = fileListInflater.inflate(R.layout.log_selection, null);
			fileListBuilder.setView(fileListView);
			/** Pointer to restore file selection dialog */
			AlertDialog fileList = fileListBuilder.create();
			fileList.setTitle(appContext.getString(R.string.sbGetLog));

			fileList.setButton(AlertDialog.BUTTON_POSITIVE, this.getString(R.string.bGet),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							if (isFileSelected) {
								// stop continuous display in chart
								Button stopButton = (Button) findViewById(R.id.bt_stop);
								stopButton.setText(getResources().getString(R.string.start));
								autoRefreshOn = false;
								stopTimer();
								isGet = false;
								showingLog = true;

								Utilities.startRefreshAnim();
								// Get file from Arduino via Async task
								url = deviceIP;
								new execSftp().execute(url, "get", restoreFile);
								isFileSelected = false;
								dialog.dismiss();
							} else {
								Utilities.myAlert(appContext, getString(R.string.errorAlertTitle),
										getString(R.string.noFileSelected));
							}
						}
					});

			fileList.setButton(AlertDialog.BUTTON_NEGATIVE, this.getString(android.R.string.cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Utilities.startRefreshAnim();
							// Get list of files from Arduino via Async task
							url = deviceIP;
							new execSftp().execute(url, "ls", restoreFile);
							isFileSelected = false;
							dialog.dismiss();
						}
					});

			/** Pointer to list view with the files */
			/* Pointer to list view with the files */
			ListView lvFileList = (ListView) fileListView.findViewById(R.id.lv_FileList);
			final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
					appContext,
					android.R.layout.simple_list_item_single_choice,
					logFiles );

			lvFileList.setAdapter(arrayAdapter);
			lvFileList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
				public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				                               int pos, long id) {
					restoreFile = logFiles.get(pos);
					Utilities.startRefreshAnim();
					// Delete selected log file from Arduino
					url = deviceIP;
					new execSftp().execute(url, "rm", restoreFile);
					logFiles.remove(pos);
					arrayAdapter.notifyDataSetChanged();
					return true;
				}
			});

			fileList.show();
			return lvFileList;

		} else {
			Utilities.myAlert(appContext,
					appContext.getResources().getString(R.string.errorAlertTitle),
					appContext.getResources().getString(R.string.noLogFile));
		}
		return null;
	}
}
