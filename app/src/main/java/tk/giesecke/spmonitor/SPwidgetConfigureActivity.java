package tk.giesecke.spmonitor;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;

/**
 * The configuration screen for the {@link SPwidget SPwidget} AppWidget.
 */
public class SPwidgetConfigureActivity extends Activity {

	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	public SPwidgetConfigureActivity() {
		super();
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Set the result to CANCELED.  This will cause the widget host to cancel
		// out of the widget placement if the user presses the back button.
		setResult(RESULT_CANCELED);

//		setContentView(R.layout.spwidget_configure);
//		mAppWidgetText = (EditText) findViewById(R.id.appwidget_text);
//		findViewById(R.id.add_button).setOnClickListener(mOnClickListener);

		// Find the widget id from the intent.
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
		}

		// If this activity was started with an intent without an app widget ID, finish with an error.
		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}

//		mAppWidgetText.setText(loadTitlePref(SPwidgetConfigureActivity.this, mAppWidgetId));

		// It is the responsibility of the configuration activity to update the app widget
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		SPwidget.updateAppWidget(this, appWidgetManager, mAppWidgetId);

		// Make sure we pass back the original appWidgetId
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
		setResult(RESULT_OK, resultValue);

		SharedPreferences mPrefs = getSharedPreferences("spMonitor",0);
		if (mPrefs.getInt("wNums",0)== 0) {
			mPrefs.edit().putInt("wNums",1).apply();
			/** Update interval in ms */
			int alarmTime = 60000;

			/** Intent for broadcast message to update widgets */
			Intent widgetIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
			/** Pending intent for broadcast message to update widgets */
			PendingIntent pendingWidgetIntent = PendingIntent.getBroadcast(
					this, 2701, widgetIntent, PendingIntent.FLAG_CANCEL_CURRENT);
			/** Alarm manager for scheduled widget updates */
			AlarmManager alarmManager = (AlarmManager) getSystemService
					(Context.ALARM_SERVICE);
			alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
					System.currentTimeMillis() + 10000,
					alarmTime, pendingWidgetIntent);

			/** IntentFilter to receive Screen on/off broadcast msgs */
			IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
			filter.addAction(Intent.ACTION_SCREEN_OFF);
			/** BroadcastReceiver to receive Screen on/off broadcast msgs */
			ScreenReceiver.screenOnOffReceiver = new ScreenReceiver();
			registerReceiver(ScreenReceiver.screenOnOffReceiver, filter);
		}
		finish();
	}
}

