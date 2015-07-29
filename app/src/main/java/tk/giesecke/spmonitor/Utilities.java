package tk.giesecke.spmonitor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/** spMonitor - Utilities
 *
 * Utilities used by SplashActivity and/or spMonitor
 *
 * @author Bernd Giesecke
 * @version 0 beta July 12, 2015.
 */
public class Utilities {

	/**
	 * Scan the local subnet and return a list of IP addresses found
	 *
	 * @param subnet
	 *            Base IP address of the subnet
	 * @return <ArrayList>hosts</ArrayList>
	 *            Array list with all found IP addresses
	 */
	public static ArrayList<String> scanSubNet(String subnet){
		/** Array list to hold found IP addresses */
		ArrayList<String> hosts = new ArrayList<>();

		subnet = subnet.substring(0, subnet.lastIndexOf("."));
		subnet += ".";
		/** IP address under test */
		InetAddress inetAddress;
		for(int i=0; i<255; i++){
			try {
				inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
				if(inetAddress.isReachable(500)){
					hosts.add(inetAddress.getHostName());
					Log.d("spMonitor", inetAddress.getHostName());
				}
			} catch (IOException e) {
				Log.d("spMonitor", "Exception "+e);
				e.printStackTrace();
			}
		}

		return hosts;
	}

	/**
	 * Sends a HTTP request to an IP to test if it is the spMonitor
	 *
	 * @param ip
	 *            IP address to test
	 * @return resultToDisplay
	 *            HTTP response from the IP
	 */
	public static String checkDeviceIP(String ip) {
		/** URL including command e to be send to the spMonitor device */
		String urlString="http://"+ip+"/arduino/e"; // URL to call
		/** Response from the URL under test */
		String resultToDisplay;

		spMonitor.client.setConnectTimeout(5, TimeUnit.SECONDS); // connect timeout
		spMonitor.client.setReadTimeout(5, TimeUnit.SECONDS);    // socket timeout

		/** Request to get HTTP content from URL */
		Request request = new Request.Builder()
				.url(urlString)
				.build();

		Response response;
		try {
			response = spMonitor.client.newCall(request).execute();
			resultToDisplay = response.body().string();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		return resultToDisplay;
	}

	/**
	 * Customized alert
	 *
	 * @return <String>deviceIP</String>context
	 *            URL address of the spMonitor device
	 */
	public static String searchDeviceIP() {
		/** Instance of WiFi manager */
		WifiManager wm = (WifiManager) spMonitor.appContext.getSystemService(Context.WIFI_SERVICE);
		/** IP address assigned to this device */
		@SuppressWarnings("deprecation") String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
		/* List with all IP address found on this subnet */
		ArrayList<String> hosts = Utilities.scanSubNet(ip);
		spMonitor.deviceIP = "";
		for (int i=0; i<hosts.size(); i++) {
			ip = hosts.get(i);
			/** Result of check if spMonitor is on this IP address */
			String result = Utilities.checkDeviceIP(ip);
			if (result.startsWith("Freq")) {
				setCalValues(result);
				spMonitor.deviceIP = "http://"+ip+"/arduino/";
				return spMonitor.deviceIP;
			}
		}
		return spMonitor.deviceIP;
	}

	/**
	 * Preset calibration values with the values received from spMonitor
	 *
	 * @param result
	 *            String including the calibration values
	 */
	public static void setCalValues(String result) {
		/** Response string split by lines */
		String[] resultSplit = result.split("\r\n");
		/** Line of response string split by spaces */
		String[] calib = resultSplit[1].split(" ");
		spMonitor.calValue1 = calib[1];
		calib = resultSplit[2].split(" ");
		spMonitor.calValue2 = calib[1];
	}

	/**
	 * Check if JSON object is valid
	 *
	 * @param test
	 *            String with JSON object or array
	 * @return boolean
	 *            true if "test" us a JSON object or array
	 *            false if no JSON object or array
	 */
	public static boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			try {
				new JSONArray(test);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Customized alert
	 *
	 * @param context
	 *            Context of app
	 * @param title
	 *            Title of alert dialog
	 * @param message
	 *            Message in alert dialog
	 */
	public static void myAlert(Context context, String title, String message) {

		/** Builder for alert dialog */
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
				context);

		// set title
		alertDialogBuilder.setTitle(title);

		// set dialog message
		alertDialogBuilder
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton(context.getResources().getString(android.R.string.ok),
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								dialog.cancel();
							}
						});

		// create alert dialog
		/** Alert dialog to be shown */
		AlertDialog alertDialog = alertDialogBuilder.create();

		// show it
		alertDialog.show();
	}

	/**
	 * Start animation of refresh icon in action bar
	 */
	public static void startRefreshAnim() {
		/** Image button for refresh data status */
		ImageButton ivRefresh = (ImageButton) spMonitor.appView.findViewById(R.id.iv_update);
		/** Progressbar that will be shown instead of image button during refresh */
		ProgressBar refreshRot = (ProgressBar) spMonitor.appView.findViewById(R.id.b_tb_refresh_rot);
		ivRefresh.setVisibility(View.GONE);
		refreshRot.setVisibility(View.VISIBLE);
	}

	/**
	 * Stop animation of refresh icon in action bar
	 */
	public static void stopRefreshAnim() {
		/** Image button for refresh data status */
		ImageButton ivRefresh = (ImageButton) spMonitor.appView.findViewById(R.id.iv_update);
		/** Progressbar that will be shown instead of image button during refresh */
		ProgressBar refreshRot = (ProgressBar) spMonitor.appView.findViewById(R.id.b_tb_refresh_rot);
		ivRefresh.setVisibility(View.VISIBLE);
		refreshRot.setVisibility(View.GONE);
	}

	/**
	 * Check if the device is a tablet or a phone
	 * by checking device configuration
	 *
	 * @param context
	 *          Application context
	 * @return <boolean>isTablet</boolean>
	 *          True if device is a tablet
	 *          False if device is a phone
	 */
	public static boolean isTablet(Context context) {
		return (context.getResources().getConfiguration().screenLayout
				& Configuration.SCREENLAYOUT_SIZE_MASK)
				>= Configuration.SCREENLAYOUT_SIZE_LARGE;
	}

	/**
	 * Get min and max value of 2 series, depending on which series are
	 * visible
	 *
	 * @return <double[]>minMax</double[]>
	 *              minMax[0] = min value or 0 if no series is shown
	 *              minMax[1] = max value or 2000 if no series is shown
	 */
	public static double[] getMinMax() {
		/** Holding the min and max values from comparison result */
		double[] minMax = new double[2];
		minMax[0] = 0;
		minMax[1] = 2000;

		if (spMonitor.showSeries1 && spMonitor.showSeries2) {
			minMax[0] = Math.min(spMonitor.sensor1Series.getLowestValueY(), spMonitor.sensor2Series.getLowestValueY());
			minMax[1] = Math.max(spMonitor.sensor1Series.getHighestValueY(), spMonitor.sensor2Series.getHighestValueY());
		} else if (spMonitor.showSeries1) {
			minMax[0] = spMonitor.sensor1Series.getLowestValueY();
			minMax[1] = spMonitor.sensor1Series.getHighestValueY();
		} else {
			minMax[0] = spMonitor.sensor2Series.getLowestValueY();
			minMax[1] = spMonitor.sensor2Series.getHighestValueY();
		}
		return minMax;
	}
}
