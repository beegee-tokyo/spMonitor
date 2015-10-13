package tk.giesecke.spmonitor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.widget.ImageView;

public class AnimatedView extends ImageView{
	private final Context mContext;
	private int x = -1;
	private int y = -1;
	private int xVelocity = 10;
	private int yVelocity = 5;
	private final Handler h;

	public AnimatedView(Context context, AttributeSet attrs)  {
		super(context, attrs);
		mContext = context;
		h = new Handler();
	}

	private final Runnable r = new Runnable() {
		@Override
		public void run() {
			invalidate();
		}
	};

	protected void onDraw(Canvas c) {
		//noinspection deprecation
		BitmapDrawable ball = (BitmapDrawable) mContext.getResources().getDrawable(R.mipmap.splash);

		if (x<0 && y <0) {
			x = this.getWidth()/2;
			y = this.getHeight()/2;
		} else if (ball != null){
			x += xVelocity;
			y += yVelocity;
			if ((x > this.getWidth() - ball.getBitmap().getWidth()) || (x < 0)) {
				xVelocity = xVelocity*-1;
			}
			if ((y > this.getHeight() - ball.getBitmap().getHeight()) || (y < 0)) {
				yVelocity = yVelocity*-1;
			}
		}
		if (ball != null) {
			c.drawBitmap(ball.getBitmap(), x, y, null);
		}
		int FRAME_RATE = 30;
		h.postDelayed(r, FRAME_RATE);
	}
}
