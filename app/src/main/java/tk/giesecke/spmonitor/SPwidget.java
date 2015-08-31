package tk.giesecke.spmonitor;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.widget.RemoteViews;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** spMonitor - SPwidget
 *
 * Implementation of App Widget functionality.
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class SPwidget extends AppWidgetProvider {

	/** broadcast signature for widget update */
	public static final String SP_WIDGET_UPDATE = "SP_WIDGET_UPDATE";

	@Override
	public void onReceive(@NonNull Context context, @NonNull Intent intent) {
		//*******************************************************
		// Receiver for the widget (click on widget, update service,
		// disable, ... we handle only the update requests here
		//*******************************************************

		super.onReceive(context, intent);

		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
		/** Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(context.getPackageName(),
				SPwidget.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		/** Remote views of the widgets */
		//RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sp_widget);

		if (SP_WIDGET_UPDATE.equals(intent.getAction())) {
			//for (int appWidgetId : appWidgetIds) {
			//	appWidgetManager.updateAppWidget(appWidgetId, views);
			//}

			onUpdate(context, appWidgetManager, appWidgetIds);
		}
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		// There may be multiple widgets active, so update all of them
		for (int appWidgetId : appWidgetIds) {
			updateAppWidget(context, appWidgetManager, appWidgetId);
		}
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		// When the user deletes the widget, delete the preference associated with it.
	}

	@Override
	public void onEnabled(Context context) {
		// Enter relevant functionality for when the first widget is created
	}

	@Override
	public void onDisabled(Context context) {
		/** Instance of the shared preferences */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor",0);
		mPrefs.edit().putInt("wNums",0).apply();
		/** Intent to start scheduled update of the widgets */
		Intent intent = new Intent(SPwidget.SP_WIDGET_UPDATE);
		/** Pending intent for broadcast message to update widgets */
		PendingIntent pendingIntent = PendingIntent.getBroadcast(
				context, 2701, intent, PendingIntent.FLAG_CANCEL_CURRENT);
		/** Alarm manager for scheduled widget updates */
		AlarmManager alarmManager = (AlarmManager) context.getSystemService
				(Context.ALARM_SERVICE);
		alarmManager.cancel(pendingIntent);
	}

	@SuppressLint("InlinedApi")
	static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
	                            int appWidgetId) {

		if (android.os.Build.VERSION.SDK_INT > 9) {
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		/** RemoteViews object */
		RemoteViews views;
		/** Access to shared preferences of app widget */
		SharedPreferences mPrefs = context.getSharedPreferences("spMonitor", 0);
		/** Flag for WAN connection */
		boolean isWAN = false;
		if (mPrefs.getBoolean("wSizeLarge",true)) {
			views = new RemoteViews(context.getPackageName(), R.layout.sp_widget_large);
		} else {
			views = new RemoteViews(context.getPackageName(), R.layout.sp_widget);
		}

		// Create an Intent to launch MainActivity
		/** Intent to start app if widget is pushed */
		Intent intent1 = new Intent(context, SplashActivity.class);
		intent1.putExtra("appWidgetId", appWidgetId);
		// Creating a pending intent, which will be invoked when the user
		// clicks on the widget
		/** Pending intent to start app if widget is pushed */
		PendingIntent pendingIntent1 = PendingIntent.getActivity(context, 0,
				intent1, PendingIntent.FLAG_UPDATE_CURRENT);
		//  Attach an on-click listener to the battery icon
		views.setOnClickPendingIntent(R.id.rlWidget1, pendingIntent1);

		/** URL of the spMonitor device */
		String deviceIP = mPrefs.getString("spMonitorIP", "no IP saved");

		// Check if we are connected to home network
		/** SSID current connected or NULL if none */
		String connSSID = Utilities.getSSID(context);

		/** String list with parts of the URL */
		String[] ipValues = deviceIP.split("/");
		/** String with the URL to get the data */
		String urlString = "http://"+ipValues[2]+"/data/get"; // URL to call

		/** Response from the spMonitor device or error message */
		String resultToDisplay = "";
		/** A HTTP client to access the spMonitor device */
		OkHttpClient client = new OkHttpClient();

		if (connSSID != null) {
			if (!connSSID.equalsIgnoreCase(mPrefs.getString("SSID",""))) {
				urlString = "http://www.desire.giesecke.tk/s/l.php";
				client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
				client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout
				isWAN = true;
			}
		} else {
			urlString = "http://www.desire.giesecke.tk/s/l.php";
			client.setConnectTimeout(5, TimeUnit.MINUTES); // connect timeout
			client.setReadTimeout(5, TimeUnit.MINUTES);    // socket timeout
			isWAN = true;
		}

		/** Solar power received from spMonitor device as minute average */
		Float solarPowerMin = 0.0f;
		/** Consumption received from spMonitor device as minute average */
		Float consPowerMin = 0.0f;

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
				views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().
						getString(R.string.widgetCommError1));
				views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().
						getString(R.string.widgetCommError2));
				views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().
						getString(R.string.widgetCommError3));
				// Instruct the widget manager to update the widget
				appWidgetManager.updateAppWidget(appWidgetId, views);
				return;
			}
		}

		if (resultToDisplay.equalsIgnoreCase("")) {
			views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().
					getString(R.string.widgetCommError1));
			views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().
					getString(R.string.widgetCommError2));
			views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().
					getString(R.string.widgetCommError3));
			// Instruct the widget manager to update the widget
			appWidgetManager.updateAppWidget(appWidgetId, views);
			return;
		} else {
			// decode JSON
			if (Utilities.isJSONValid(resultToDisplay)) {
				try {
					if (!isWAN) {
						/** JSON object containing result from server */
						JSONObject jsonResult = new JSONObject(resultToDisplay);
						/** JSON object containing the values */
						JSONObject jsonValues = jsonResult.getJSONObject("value");

						try {
							solarPowerMin = Float.parseFloat(jsonValues.getString("S"));
							consPowerMin = Float.parseFloat(jsonValues.getString("C"));
						} catch (Exception ignore) {
							views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().
									getString(R.string.widgetCommError1));
							views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().
									getString(R.string.widgetCommError2));
							views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().
									getString(R.string.widgetCommError3));
						}
					} else {
						/** JSON object containing result from server */
						JSONObject jsonValues = new JSONObject(resultToDisplay.substring(1,resultToDisplay.length()-1));

						try {
							solarPowerMin = Float.parseFloat(jsonValues.getString("s"));
							consPowerMin = Float.parseFloat(jsonValues.getString("c"));
						} catch (Exception ignore) {
							views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().
									getString(R.string.widgetCommError1));
							views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().
									getString(R.string.widgetCommError2));
							views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().
									getString(R.string.widgetCommError3));
							// Instruct the widget manager to update the widget
							appWidgetManager.updateAppWidget(appWidgetId, views);
							return;
						}
					}

					/** Double for the result of solar current and consumption used at 1min updates */
					double resultPowerMin = solarPowerMin + consPowerMin;

					views.setTextViewText(R.id.tv_widgetRow1Value, String.format("%.0f", resultPowerMin) + "W");
					views.setTextViewText(R.id.tv_widgetRow2Value, String.format("%.0f", Math.abs(consPowerMin)) + "W");
					views.setTextViewText(R.id.tv_widgetRow3Value, String.format("%.0f", solarPowerMin) + "W");

					/** Icon for notification */
					int notifIcon;
					/** String for notification */
					String notifText;
					/** Background color for notification icon in SDK Lollipop and newer */
					int notifColor;

					if (consPowerMin > 0.0d) {
						views.setTextColor(R.id.tv_widgetRow2Value, context.getResources()
								.getColor(android.R.color.holo_red_light));
						if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							notifIcon = R.drawable.arrow_down_small;
						} else {
							notifIcon = R.drawable.arrow_red_down_small;

						}
						notifText = context.getString(R.string.tv_result_txt_im) + " " +
								String.format("%.0f", Math.abs(consPowerMin)) + "W";
						notifColor = context.getResources()
								.getColor(android.R.color.holo_red_light);
					} else {
						views.setTextColor(R.id.tv_widgetRow2Value, context.getResources()
								.getColor(android.R.color.holo_green_light));
						if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
							notifIcon = R.drawable.arrow_up_small;
						} else {
							notifIcon = R.drawable.arrow_green_up_small;

						}
						notifText = context.getString(R.string.tv_result_txt_ex) + " " +
								String.format("%.0f", Math.abs(consPowerMin)) + "W";
						notifColor = context.getResources()
								.getColor(android.R.color.holo_green_light);
						if (consPowerMin < -200.0d) {
							/** Uri of selected alarm */
							String selUri = mPrefs.getString("alarmUri","");
							if (!selUri.equalsIgnoreCase("")) {
								// Instance of notification manager to cancel the existing notification */
								NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
								nMgr.cancel(0);

								NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
										.setContentTitle(context.getString(R.string.app_name))
										.setContentIntent(PendingIntent.getActivity(context, 0,
												new Intent(context, spMonitor.class), 0))
										.setContentText(context.getString(R.string.notif_export,
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
										(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
								notificationManager.notify(0, notification);
							}
						}
					}

					// Instance of notification manager to cancel the existing notification */
					NotificationManager nMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
					nMgr.cancel(1);

					/* Pointer to notification builder for export/import arrow */
					NotificationCompat.Builder builder1 = new NotificationCompat.Builder(context)
							.setContentTitle(context.getString(R.string.app_name))
							.setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, spMonitor.class), 0))
							.setAutoCancel(false)
							.setSound(Uri.parse("android.resource://"
									+ context.getPackageName() + "/"
									+ R.raw.silent))
							.setPriority(NotificationCompat.PRIORITY_DEFAULT)
							.setVisibility(Notification.VISIBILITY_PUBLIC)
							.setWhen(System.currentTimeMillis());
					/* Pointer to notification manager for export/import arrow */
					NotificationManager notificationManager1 = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

					builder1.setSmallIcon(notifIcon);
					builder1.setContentText(notifText);
					builder1.setTicker(String.format("%.0f", Math.abs(consPowerMin)) + "W");
					if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
						builder1.setColor(notifColor);
					}
					/* Pointer to notification for export/import arrow */
					Notification notification1 = builder1.build();
					notificationManager1.notify(1, notification1);

				} catch (Exception ignore) {
					views.setTextViewText(R.id.tv_widgetRow1Value, context.getResources().
							getString(R.string.widgetCommError1));
					views.setTextViewText(R.id.tv_widgetRow2Value, context.getResources().
							getString(R.string.widgetCommError2));
					views.setTextViewText(R.id.tv_widgetRow3Value, context.getResources().
							getString(R.string.widgetCommError3));
				}
			}
		}
		// Instruct the widget manager to update the widget
		appWidgetManager.updateAppWidget(appWidgetId, views);
	}
}

