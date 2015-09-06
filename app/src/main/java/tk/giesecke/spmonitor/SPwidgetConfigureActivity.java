package tk.giesecke.spmonitor;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;

import java.io.IOException;
import java.util.ArrayList;

/** spMonitor - SPwidgetConfigureActivity
 *
 * The configuration screen for the {@link SPwidget SPwidget} AppWidget.
 *
 * @author Bernd Giesecke
 * @version 0.2 beta August 19, 2015.
 */
public class SPwidgetConfigureActivity extends Activity implements AdapterView.OnItemClickListener {

	/** Default app widget id */
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	/** Context of the configuration */
	private Context confContext;
	/** Flag for requested widget text size */
	private boolean isTextLarge = true;
	/** Array list with available alarm names */
	private ArrayList<String> notifNames = new ArrayList<>();
	/** Array list with available alarm uri's */
	private ArrayList<String> notifUri = new ArrayList<>();
	/** Selected alarm name */
	private String notifNameSel = "";
	/** Selected alarm uri */
	private String notifUriSel = "";

	public SPwidgetConfigureActivity() {
		super();
	}

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Set the result to CANCELED.  This will cause the widget host to cancel
		// out of the widget placement if the user presses the back button.
		setResult(RESULT_CANCELED);

		setContentView(R.layout.sp_widget_config);
		findViewById(R.id.bt_cancel).setOnClickListener(mOnClickListener);
		findViewById(R.id.bt_ok).setOnClickListener(mOnClickListener);

		// Find the widget id from the intent.
		/** Intent to get bundled data */
		Intent intent = getIntent();
		/** Bundle with data */
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

		confContext = this;
		/** Radio group with radio buttons for text size selection */
		RadioGroup rgSize = (RadioGroup) findViewById(R.id.rg_size);
		rgSize.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				switch (checkedId) {
					case R.id.rb_large:
						isTextLarge = true;
						break;
					case R.id.rb_small:
						isTextLarge = false;
						break;
				}
			}
		});

		notifNames = new ArrayList<>();
		notifUri = new ArrayList<>();
		notifNames.add(getString(R.string.no_alarm_sel));
		notifUri.add("");
		notifNames.add(getString(R.string.dev_alarm_sel));
		notifUri.add("android.resource://"
				+ this.getPackageName() + "/"
				+ R.raw.alert);
		/** Index of last user selected alarm tone */
		int uriIndex = Utilities.getNotifSounds(this, notifNames, notifUri) + 2;

		/** Pointer to list view with the alarms */
		ListView lvAlarmList = (ListView) findViewById(R.id.lv_alarms);
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
				this,
				android.R.layout.simple_list_item_single_choice,
				notifNames );

		lvAlarmList.setAdapter(arrayAdapter);
		lvAlarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
			                               int pos, long id) {
				/** Instance of media player */
				MediaPlayer mMediaPlayer = new MediaPlayer();
				try {
					mMediaPlayer.setDataSource(confContext, Uri.parse(notifUri.get(pos)));
					final AudioManager audioManager = (AudioManager) confContext
							.getSystemService(Context.AUDIO_SERVICE);
					if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
						mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
						mMediaPlayer.prepare();
						mMediaPlayer.start();
					}
				} catch (IOException e) {
					if (BuildConfig.DEBUG) Log.d("spMonitor", "No alarms found");
				}
				return true;
			}
		});
		lvAlarmList.setOnItemClickListener(this);
		lvAlarmList.setItemChecked(uriIndex, true);
		lvAlarmList.setSelection(uriIndex);
	}

	/**
	 * Listener for click events
	 */
	private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			switch (v.getId()) {
				case R.id.bt_cancel:
					finish();
					break;
				case R.id.bt_ok:
					/** Context for this configuration class */
					final Context context = SPwidgetConfigureActivity.this;

					/** Access to the shared preferences */
					SharedPreferences mPrefs = getSharedPreferences("spMonitor",0);
					mPrefs.edit().putBoolean("wSizeLarge",isTextLarge).apply();

					if (!notifNameSel.equalsIgnoreCase("")) {
						mPrefs.edit().putString("alarmUri",notifUriSel).apply();
					}

					// It is the responsibility of the configuration activity to update the app widget
					/** App widget manager for this widget */
					AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
					SPwidget.updateAppWidget(context, appWidgetManager, mAppWidgetId);

					// Make sure we pass back the original appWidgetId
					/** Intent to report successful added widget */
					Intent resultValue = new Intent();
					resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
					setResult(RESULT_OK, resultValue);

					if (mPrefs.getInt("wNums",0)== 0) {
						mPrefs.edit().putInt("wNums",1).apply();
						/** Update interval in ms */
						int alarmTime = 60000;

						/** Intent for broadcast message to update widgets */
						Intent widgetIntent = new Intent(SPwidget.SP_WIDGET_UPDATE);
						/** Pending intent for broadcast message to update widgets */
						PendingIntent pendingWidgetIntent = PendingIntent.getBroadcast(
								context, 2701, widgetIntent, PendingIntent.FLAG_CANCEL_CURRENT);
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
						BroadcastReceiver mReceiver = new ScreenReceiver();
						registerReceiver(mReceiver, filter);
					}
					finish();
					break;
			}
		}
	};

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		notifNameSel = notifNames.get(position);
		notifUriSel = notifUri.get(position);
	}
}

