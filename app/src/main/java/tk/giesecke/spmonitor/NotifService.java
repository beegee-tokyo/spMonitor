package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class NotifService extends IntentService {

	public NotifService() {
		super("NotifService");
	}

	@SuppressLint("InlinedApi")
	@Override
	protected void onHandleIntent(Intent intent) {
		if (intent != null) {
			/** Context of application */
			Context intentContext = getApplicationContext();
			/** Access to shared preferences of app widget */
			SharedPreferences mPrefs = intentContext.getSharedPreferences("spMonitor", 0);
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

			if (connSSID != null) {
				if (!connSSID.equalsIgnoreCase(mPrefs.getString("SSID","none"))) {
					urlString = "http://www.desire.giesecke.tk/s/l.php";
					isWAN = true;
				}
			} else {
				urlString = "http://www.desire.giesecke.tk/s/l.php";
				isWAN = true;
			}

			/** Consumption received from spMonitor device as minute average */
			Float consPowerMin = 0.0f;
			/** Solar power received from spMonitor device as minute average */
			Float solarPowerMin = 0.0f;

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
							}
						} else {
							/** JSON object containing result from server */
							JSONObject jsonValues = new JSONObject(resultToDisplay.substring(1,resultToDisplay.length()-1));

							try {
								consPowerMin = Float.parseFloat(jsonValues.getString("c"));
								solarPowerMin = Float.parseFloat(jsonValues.getString("s"));
							} catch (Exception ignore) {
								return;
							}
						}

						/** Icon for notification */
						int notifIcon;
						/** String for notification */
						String notifText;
						/** Background color for notification icon in SDK Lollipop and newer */
						int notifColor;

						notifIcon = Utilities.getNotifIcon(consPowerMin);

						if (consPowerMin > 0.0d) {
//							if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//								if (consPowerMin < 50) {
//									notifIcon = R.drawable.p0;
//								} else if (consPowerMin < 100) {
//									notifIcon = R.drawable.p50;
//								} else if (consPowerMin < 150) {
//									notifIcon = R.drawable.p100;
//								} else if (consPowerMin < 200) {
//									notifIcon = R.drawable.p150;
//								} else if (consPowerMin < 250) {
//									notifIcon = R.drawable.p200;
//								} else if (consPowerMin < 300) {
//									notifIcon = R.drawable.p250;
//								} else if (consPowerMin < 350) {
//									notifIcon = R.drawable.p300;
//								} else if (consPowerMin < 400) {
//									notifIcon = R.drawable.p350;
//								} else {
//									notifIcon = R.drawable.p400;
//								}
//							} else {
//								notifIcon = R.drawable.arrow_red_down_small;
//							}
							notifText = intentContext.getString(R.string.tv_result_txt_im) + " " +
									String.format("%.0f", Math.abs(consPowerMin)) + "W";
							notifColor = intentContext.getResources()
									.getColor(android.R.color.holo_red_light);
						} else {
//							if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//								if (consPowerMin < -400) {
//									notifIcon = R.drawable.m400;
//								} else if (consPowerMin < -350) {
//									notifIcon = R.drawable.m350;
//								} else if (consPowerMin < -300) {
//									notifIcon = R.drawable.m300;
//								} else if (consPowerMin < -250) {
//									notifIcon = R.drawable.m250;
//								} else if (consPowerMin < -200) {
//									notifIcon = R.drawable.m200;
//								} else if (consPowerMin < -150) {
//									notifIcon = R.drawable.m150;
//								} else if (consPowerMin < -100) {
//									notifIcon = R.drawable.m100;
//								} else if (consPowerMin < -500) {
//									notifIcon = R.drawable.m50;
//								} else {
//									notifIcon = R.drawable.m0;
//								}
//							} else {
//								notifIcon = R.drawable.arrow_green_up_small;
//
//							}
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

						if (mPrefs.getBoolean("notif",true)) {
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
							/** Flag if consumption is sending or receiving */
							boolean isSending = consPowerMin > 0.0d;

							/** Double for the result of solar current and consumption used at 1min updates */
							double resultPowerMin = solarPowerMin + consPowerMin;

							SolarDayDream.setNewText(intentContext, String.format("%.0f", resultPowerMin) + "W",
									String.format("%.0f", Math.abs(consPowerMin)) + "W",
									isSending,
									String.format("%.0f", solarPowerMin) + "W");
							SolarDayDream.powerVal = resultPowerMin;
							SolarDayDream.consVal = consPowerMin;
							SolarDayDream.solarVal = solarPowerMin;
						}
					} catch (Exception ignore) {
					}
				}
			}
		}
	}
}
