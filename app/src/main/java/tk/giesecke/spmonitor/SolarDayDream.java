package tk.giesecke.spmonitor;

import java.util.Random;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Point;
import android.os.Build;
import android.service.dreams.DreamService;
import android.view.ViewPropertyAnimator;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

/**
 * This class is the implementation of a DreamService. When activated, a
 * TextView will repeatedly, move from the left to the right of screen, at a
 * random y-value.
 * <p/>
 * Daydreams are only available on devices running API v17+.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
public class SolarDayDream extends DreamService {

	private static final TimeInterpolator sInterpolator = new LinearInterpolator();

	private final AnimatorListener mAnimListener = new AnimatorListenerAdapter() {

		@Override
		public void onAnimationEnd(Animator animation) {
			if (isDayDreaming) {
				// Start animation again
				startTextViewScrollAnimation();
			}
		}
	};

	private final Random mRandom = new Random();
	private final Point mPointSize = new Point();

	private static TextView mDreamTextPower;
	private static TextView mDreamTextCons;
	private static TextView mDreamTextSolar;

	private ViewPropertyAnimator mAnimatorPower;

	/** Consumption power received from spMonitor device as minute average */
	public static double powerVal = 0.0f;
	/** Power received from spMonitor device as minute average */
	public static Float consVal = 0.0f;
	/** Solar power received from spMonitor device as minute average */
	public static Float solarVal = 0.0f;

	public static boolean isDayDreaming = false;

	@Override
	public void onAttachedToWindow() {
		super.onAttachedToWindow();

		// Exit dream upon user touch?
		setInteractive(false);

		// Hide system UI?
		setFullscreen(true);

		// Keep screen at full brightness?
		setScreenBright(false);

		// Set the content view, just like you would with an Activity.
		setContentView(R.layout.solar_daydream);

		mDreamTextPower = (TextView) findViewById(R.id.dream_power);
		mDreamTextCons = (TextView) findViewById(R.id.dream_cons);
		mDreamTextSolar = (TextView) findViewById(R.id.dream_solar);
	}

	@Override
	public void onDreamingStarted() {
		super.onDreamingStarted();

//		/** Context of application */
//		Context intentContext = getApplicationContext();
//		Utilities.startStopUpdates(intentContext, true);

		startTextViewScrollAnimation();

		isDayDreaming = true;
	}

	@Override
	public void onDreamingStopped() {
		super.onDreamingStopped();

//		/** Context of application */
//		Context intentContext = getApplicationContext();
//		/** Access to shared preferences of app widget */
//		SharedPreferences mPrefs = intentContext.getSharedPreferences("spMonitor", 0);
//		if (!(mPrefs.getBoolean("notif",true)) &&
//				(mPrefs.getInt("wNums",0) == 0) &&
//				spMonitor.syncServiceReceiver == null) {
//			Utilities.startStopUpdates(intentContext, false);
//		}

		mAnimatorPower.cancel();

		isDayDreaming = false;
	}

	private void startTextViewScrollAnimation() {
		/** Context of day dream */
		Context ddContext = getApplicationContext();

		// Refresh Size of Window
		getWindowManager().getDefaultDisplay().getSize(mPointSize);

		final int windowWidth = mPointSize.x;
		final int windowHeight = mPointSize.y;

		// Move TextView so it's moved all the way to the left
		mDreamTextPower.setTranslationX(-mDreamTextPower.getWidth());
		mDreamTextCons.setTranslationX(-mDreamTextCons.getWidth());
		mDreamTextSolar.setTranslationX(-mDreamTextSolar.getWidth());

		// Move TextView to random y value
		final int yRangePower = windowHeight -
				mDreamTextPower.getHeight() -
				mDreamTextCons.getHeight() -
				mDreamTextSolar.getHeight();
		mDreamTextPower.setTranslationY(mRandom.nextInt(yRangePower));

		mDreamTextCons.setTranslationY(mDreamTextPower.getTranslationY() + mDreamTextPower.getHeight());

		mDreamTextSolar.setTranslationY(mDreamTextCons.getTranslationY() + mDreamTextCons.getHeight());

		// Create an Animator and keep a reference to it
		mAnimatorPower = mDreamTextPower.animate().translationX(windowWidth)
				.setDuration(30000)
				.setListener(mAnimListener)
				.setInterpolator(sInterpolator);
		mDreamTextCons.animate().translationX(windowWidth)
				.setDuration(30000)
				.setInterpolator(sInterpolator);
		mDreamTextSolar.animate().translationX(windowWidth)
				.setDuration(30000)
				.setInterpolator(sInterpolator);

		// Set initial values if first time
		if (consVal == 0) {
			/** Instance of DataBaseHelper */
			DataBaseHelper dbHelper = new DataBaseHelper(ddContext, DataBaseHelper.DATABASE_NAME);
			/** Instance of data base */
			SQLiteDatabase dataBase = dbHelper.getReadableDatabase();

			/** Cursor with data from the database */
			Cursor newDataSet = DataBaseHelper.getLastRow(dataBase);
			newDataSet.moveToFirst();

			consVal = newDataSet.getFloat(6);
			solarVal = newDataSet.getFloat(5);
			powerVal = (double) (solarVal + consVal);

			newDataSet.close();
			dataBase.close();
			dbHelper.close();
		}
		setNewText(getApplicationContext(), String.format("%.0f", powerVal) + "W",
				String.format("%.0f", Math.abs(consVal)) + "W",
				consVal > 0.0d,
				String.format("%.0f", solarVal) + "W");

		// Start the animation
		mAnimatorPower.start();
	}

	private static void setNewText(Context context, String power, String cons, boolean isSending, String solar) {
		String viewText = "Power now = " + power;
		mDreamTextPower.setText(viewText);

		if (isSending) {
			viewText = "Importing = " + cons;
			mDreamTextCons.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
		} else {
			viewText = "Exporting = " + cons;
			mDreamTextCons.setTextColor(context.getResources().getColor(android.R.color.holo_green_light));
		}
		mDreamTextCons.setText(viewText);

		viewText = "SolarPV = " + solar;
		mDreamTextSolar.setText(viewText);
	}
}
