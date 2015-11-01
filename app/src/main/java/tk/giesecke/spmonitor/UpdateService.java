package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Update service for notifications and widgets
 * a service on a separate handler thread.
 */
public class UpdateService extends IntentService {

	public UpdateService() {
		super("UpdateService");
	}

	@SuppressLint("InlinedApi")
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			if (BuildConfig.DEBUG) Log.d("spUpdate", "UpdateStart");

			if(spMonitor.isCommunicating) {
				if (BuildConfig.DEBUG) Log.d("spUpdate", "Update skipped");
				return;
			}
			/** Context of application */
			Context intentContext = getApplicationContext();
			/** Access to shared preferences of app widget */
			SharedPreferences mPrefs = intentContext.getSharedPreferences("spMonitor", 0);

			// Update only if either
			// notification is active
			// or widget is active
			// or daydream is active
			if ((mPrefs.getBoolean("notif",true))
					|| (mPrefs.getInt("wNums",0) != 0)
					|| (SolarDayDream.isDayDreaming))
			{
				/** Flag for WAN connection */
				boolean isWAN = false;

				/** URL of the spMonitor device */
				String deviceIP = mPrefs.getString("spMonitorIP", "no IP saved");

				// Check if we are connected to home network
				/** SSID current connected or NULL if none */
				String connSSID = Utilities.getSSID(intentContext);

				/** String list with parts of the URL */
				String[] ipValues = deviceIP.split("/");
				/** String with the URL to get the data */
				String urlString = "http://"+ipValues[2]+"/data/get"; // URL to call

				/** Response from the spMonitor device or error message */
				String resultToDisplay = "";
				/** A HTTP client to access the spMonitor device */
				OkHttpClient client = new OkHttpClient();

				client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
				client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout

				if ((connSSID == null) || (!connSSID.equalsIgnoreCase(mPrefs.getString("SSID","none")))) {
					isWAN = true;
					urlString = "http://www.desire.giesecke.tk/s/l.php";
				}

				/** Consumption received from spMonitor device as minute average */
				Float consPowerMin;
				/** Solar power received from spMonitor device as minute average */
				Float solarPowerMin;

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
						return;
					}
				}

				if (!resultToDisplay.equalsIgnoreCase("")) {
					// decode JSON
					if (Utilities.isJSONValid(resultToDisplay)) {
						try {
							if (!isWAN) {
								/** JSON object containing result from server */
								JSONObject jsonResult = new JSONObject(resultToDisplay);
								/** JSON object containing the values */
								JSONObject jsonValues = jsonResult.getJSONObject("value");

								try {
									consPowerMin = Float.parseFloat(jsonValues.getString("C"));
									solarPowerMin = Float.parseFloat(jsonValues.getString("S"));
								} catch (Exception ignore) {
									consPowerMin = 0.0f;
									solarPowerMin = 0.0f;
								}
							} else {
								/** JSON object containing result from server */
								JSONObject jsonValues = new JSONObject(resultToDisplay.substring(1,resultToDisplay.length()-1));

								try {
									consPowerMin = Float.parseFloat(jsonValues.getString("c"));
									solarPowerMin = Float.parseFloat(jsonValues.getString("s"));
								} catch (Exception ignore) {
									consPowerMin = 0.0f;
									solarPowerMin = 0.0f;
								}
							}
						} catch (Exception ignore) {
							consPowerMin = 0.0f;
							solarPowerMin = 0.0f;
						}

						if (mPrefs.getBoolean("notif",true)) {
							if (BuildConfig.DEBUG) Log.d("spUpdate", "Update Notification");
							/** Icon for notification */
							int notifIcon;
							/** String for notification */
							String notifText;
							/** Background color for notification icon in SDK Lollipop and newer */
							int notifColor;

							notifIcon = Utilities.getNotifIcon(consPowerMin);

							if (consPowerMin > 0.0d) {
								notifText = intentContext.getString(R.string.tv_result_txt_im) + " " +
										String.format("%.0f", Math.abs(consPowerMin)) + "W";
								notifColor = intentContext.getResources()
										.getColor(android.R.color.holo_red_light);
							} else {
								notifText = intentContext.getString(R.string.tv_result_txt_ex) + " " +
										String.format("%.0f", Math.abs(consPowerMin)) + "W";
								notifColor = intentContext.getResources()
										.getColor(android.R.color.holo_green_light);
								if (consPowerMin < -200.0d) {
									/** Uri of selected alarm */
									String selUri = mPrefs.getString("alarmUri","");
									if (!selUri.equalsIgnoreCase("")) {
										NotificationCompat.Builder builder = new NotificationCompat.Builder(intentContext)
												.setContentTitle(intentContext.getString(R.string.app_name))
												.setContentIntent(PendingIntent.getActivity(intentContext, 0,
														new Intent(intentContext, spMonitor.class), 0))
												.setContentText(intentContext.getString(R.string.notif_export,
														String.format("%.0f", Math.abs(consPowerMin)),
														Utilities.getCurrentTime()))
												.setAutoCancel(true)
												.setSound(Uri.parse(selUri))
												.setDefaults(Notification.FLAG_ONLY_ALERT_ONCE)
												.setPriority(NotificationCompat.PRIORITY_DEFAULT)
												.setVisibility(Notification.VISIBILITY_PUBLIC)
												.setWhen(System.currentTimeMillis())
												.setSmallIcon(android.R.drawable.ic_dialog_info);

										Notification notification = builder.build();
										NotificationManager notificationManager =
												(NotificationManager) intentContext.getSystemService(Context.NOTIFICATION_SERVICE);
										notificationManager.notify(0, notification);
									}
								} else {
									// Instance of notification manager to cancel the existing notification */
									NotificationManager nMgr = (NotificationManager) intentContext.getSystemService(Context.NOTIFICATION_SERVICE);
									nMgr.cancel(0);
								}
							}

							/* Pointer to notification builder for export/import arrow */
							NotificationCompat.Builder builder1 = new NotificationCompat.Builder(intentContext)
									.setContentTitle(intentContext.getString(R.string.app_name))
									.setContentIntent(PendingIntent.getActivity(intentContext, 0, new Intent(intentContext, spMonitor.class), 0))
									.setAutoCancel(false)
									.setPriority(NotificationCompat.PRIORITY_DEFAULT)
									.setVisibility(Notification.VISIBILITY_PUBLIC)
									.setWhen(System.currentTimeMillis());
						/* Pointer to notification manager for export/import arrow */
							NotificationManager notificationManager1 = (NotificationManager) intentContext.getSystemService(Context.NOTIFICATION_SERVICE);

							builder1.setSmallIcon(notifIcon);
							builder1.setContentText(notifText);
							builder1.setTicker(String.format("%.0f", Math.abs(consPowerMin)) + "W");
							if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
								builder1.setColor(notifColor);
							}
						/* Pointer to notification for export/import arrow */
							Notification notification1 = builder1.build();
							notificationManager1.notify(1, notification1);
						} else {
							// Instance of notification manager to cancel the existing notification */
							NotificationManager nMgr = (NotificationManager) intentContext.getSystemService(Context.NOTIFICATION_SERVICE);
							nMgr.cancel(1);
						}

						if (SolarDayDream.isDayDreaming) {
							if (BuildConfig.DEBUG) Log.d("spUpdate", "Update DayDream");
							SolarDayDream.powerVal = (double) (solarPowerMin + consPowerMin);
							SolarDayDream.consVal = consPowerMin;
							SolarDayDream.solarVal = solarPowerMin;
						}

						if (mPrefs.getInt("wNums",0) != 0) {
							if (BuildConfig.DEBUG) Log.d("spUpdate", "Update Widget");
							/** App widget manager for all widgets of this app */
							AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(intentContext);
							/** Component name of this widget */
							ComponentName thisAppWidget = new ComponentName(intentContext.getPackageName(),
									SPwidget.class.getName());
							/** List of all active widgets */
							int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

							for (int appWidgetId : appWidgetIds) {
								SPwidget.updateAppWidget(intentContext,appWidgetManager,appWidgetId,solarPowerMin,consPowerMin);
							}
						}
					}
				}
			}
		}
	}
}
