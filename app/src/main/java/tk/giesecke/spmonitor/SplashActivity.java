package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/** spMonitor - SplashActivity
 *
 * Shows splash screen when app is started
 * Searches for IP address of the spMonitor device
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class SplashActivity extends Activity implements View.OnClickListener {

	/** Pointer to text view to show result of device search */
	private static TextView resultTextView;
	/** Pointer to button for manual IP address entry */
	private static Button manualEntry;
	/** Pointer to text view for manual IP address entry */
	private static TextView manualEntryTxt;
	/** Delay in ms before showing main activity */
	private static final int delay = 1000;
	/** Instance of asynchronous task */
	private AsyncTask asTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		spMonitor.appContext = this;

		// Enable access to internet
		if (android.os.Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		resultTextView = (TextView) findViewById(R.id.tv_splash_status);
		manualEntry = (Button) findViewById(R.id.bt_man_ip);
		manualEntry.setOnClickListener(this);
		manualEntryTxt = (TextView) findViewById(R.id.tv_man_ip_hdr);

		// Check if device is reachable on the network

		// 1) Check if WiFi is enabled
		/** Access to connectivity manager */
		ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo wifiOn = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		wifiOn.getDetailedState();
		if (wifiOn.getDetailedState().name().equalsIgnoreCase("DISCONNECTED")) {
			resultTextView.setText(getResources().getString(R.string.no_wifi));
			manualEntry.setVisibility(View.INVISIBLE);
			manualEntryTxt.setVisibility(View.INVISIBLE);
		} else {
			/** Access to shared preferences of application*/
			spMonitor.mPrefs = getSharedPreferences("spMonitor", 0);
			spMonitor.deviceIP = spMonitor.mPrefs.getString("spMonitorIP","no IP saved");

			if (spMonitor.deviceIP.equalsIgnoreCase(getResources().getString(R.string.no_device_ip))) {
				asTask = new findArduino().execute("find_new");
			} else {
				// String list with the IP address
				String[] outValues = spMonitor.deviceIP.split("/");
				asTask = new findArduino().execute("find_registered", outValues[2]);
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.bt_man_ip:
				/** Progressbar shown during search */
				ProgressBar refreshRot = (ProgressBar) findViewById(R.id.pb_splash);
				refreshRot.setVisibility(View.INVISIBLE);

				/** Alert dialog builder to show dialog for manual IP address input */
				AlertDialog.Builder ipDialBuilder = new AlertDialog.Builder(this);
				/** Layout inflater to show dialog for manual IP address input */
				LayoutInflater ipDialInflater = getLayoutInflater();
				@SuppressLint("InflateParams") final View ipDialView = ipDialInflater.inflate(R.layout.ip_input, null);
				ipDialBuilder.setView(ipDialView);
				/** Instance of dialog */
				AlertDialog ipDialog = ipDialBuilder.create();
				ipDialog.setTitle(getString(R.string.tv_ip_header_txt));

				ipDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								/** String containing the IP address entered by the user */
								String ipBuilder;
								/** Pointer to edit text fields */
								EditText numField = (EditText) ipDialView.findViewById(R.id.et_ip_1);
								ipBuilder = numField.getText().toString();
								ipBuilder = ipBuilder + ":";
								numField = (EditText) ipDialView.findViewById(R.id.et_ip_2);
								ipBuilder = ipBuilder + numField.getText().toString() + ":";
								numField = (EditText) ipDialView.findViewById(R.id.et_ip_3);
								ipBuilder = ipBuilder + numField.getText().toString() + ":";
								numField = (EditText) ipDialView.findViewById(R.id.et_ip_4);
								ipBuilder = ipBuilder + numField.getText().toString();
								spMonitor.mPrefs.edit().putString("spMonitorIP", "http://" + ipBuilder + "/arduino/");
								updateText(getResources().getString(R.string.manual_ip, ipBuilder));
								manualEntry.setVisibility(View.INVISIBLE);
								manualEntryTxt.setVisibility(View.INVISIBLE);
								asTask.cancel(true);
								startMain();
							}
						});
				ipDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						});

				ipDialog.show();

				break;
		}
	}

	/**
	 * Async task to search for spMonitor device
	 */
	private class findArduino extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {

			/** String parameter with the command for the async task */
			String toDo = params[0]; // what do we need to do

			spMonitor.client.setConnectTimeout(1, TimeUnit.MINUTES); // connect timeout
			spMonitor.client.setReadTimeout(1, TimeUnit.MINUTES);    // socket timeout
			if (toDo.equalsIgnoreCase("find_new")) { // no info about Arduino device, search for it
				spMonitor.deviceIP = Utilities.searchDeviceIP();
				if (spMonitor.deviceIP.equalsIgnoreCase("")) {
					return "false";
				} else {
					spMonitor.mPrefs.edit().putString("spMonitorIP", spMonitor.deviceIP).apply();
					return "true";
				}
			} else if (toDo.equalsIgnoreCase("find_registered")) {
				/** String holding the IP address the spMonitor was found at last start */
				String ip = params[1]; //stored URL we need to check
				/** Result of check if spMonitor is still on same IP address */
				String result;
				for (int i=0; i<3; i++) { //try three times before giving up
					result = Utilities.checkDeviceIP(ip);
					if (result.startsWith("F ")) {
						spMonitor.deviceIP = "http://"+ip+"/arduino/";
						spMonitor.mPrefs.edit().putString("spMonitorIP", spMonitor.deviceIP).apply();
						return "true";
					}
				}
				/* spMonitor device not found on the stored IP address */
				/* First try to find in the range of the old stored IP address */
				/** Last part of IP address at which spMonitor was found at last start */
				int oldIP = Integer.parseInt(ip.substring(ip.lastIndexOf(".")+1));
				/** Subnet without last part */
				String subnet = ip.substring(0, ip.lastIndexOf("."));
				subnet += ".";
				if (oldIP > 10) oldIP = oldIP - 10;
				else oldIP = 0;
				if (oldIP > 235) oldIP = 235;
				for (int i = oldIP; i < oldIP+20; i++) {
					ip = subnet+String.valueOf(i);
					for (i=0; i<3; i++) { //try three times before giving up
						result = Utilities.checkDeviceIP(ip);
						if (result.startsWith("F ")) {
							spMonitor.deviceIP = "http://"+ip+"/arduino/";
							spMonitor.mPrefs.edit().putString("spMonitorIP", spMonitor.deviceIP).apply();
							return "true";
						}
					}
				}
				/* Still couldn't find the spMonitor device, make a full IP range scan */
				spMonitor.deviceIP = Utilities.searchDeviceIP();

				if (spMonitor.deviceIP.equalsIgnoreCase("")) {
					return "false";
				} else {
					spMonitor.mPrefs.edit().putString("spMonitorIP", spMonitor.deviceIP).apply();
					return "true";
				}
			}
			return "false";
		}

		protected void onPostExecute(String result) {

			if (result.equalsIgnoreCase("true")) {
				/** Progressbar shown during search */
				ProgressBar refreshRot = (ProgressBar) findViewById(R.id.pb_splash);
				refreshRot.setVisibility(View.INVISIBLE);

				updateText(getResources().getString(R.string.found_device, spMonitor.deviceIP));
				spMonitor.mPrefs.edit().putString("spMonitorIP", spMonitor.deviceIP);
				manualEntry.setVisibility(View.INVISIBLE);
				manualEntryTxt.setVisibility(View.INVISIBLE);
				// start main activity
				startMain();
			} else {
				/** Progressbar shown during search */
				ProgressBar refreshRot = (ProgressBar) findViewById(R.id.pb_splash);
				refreshRot.setVisibility(View.INVISIBLE);

				// wait for user to close the app or enter the IP manually
				updateText(getResources().getString(R.string.err_no_device));
				manualEntry.setVisibility(View.VISIBLE);
				manualEntryTxt.setVisibility(View.VISIBLE);
			}
		}
	}

	/**
	 * Update UI text to inform user about search result
	 *
	 * @param value
	 *            String to be displayed
	 */
	private void updateText(final String value) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				resultTextView.setText(value);
			}
		});
	}

	/**
	 * Start the main activity
	 */
	private void startMain() {
		/** Handler to start main UI with an delay of 5 second */
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				startActivity(new Intent(SplashActivity.this, spMonitor.class));
				overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
				finish();
			}
		}, delay);
	}
}
