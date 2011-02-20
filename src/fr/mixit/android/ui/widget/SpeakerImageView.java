package fr.mixit.android.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import fr.mixit.android.R;

public class SpeakerImageView extends View {
	
	private Bitmap mBitmap;
	private Paint mRect = new Paint();
	private Paint mFill = new Paint();
	private PaintFlagsDrawFilter setFilter = new PaintFlagsDrawFilter(0, Paint.FILTER_BITMAP_FLAG);
	private PaintFlagsDrawFilter remFilter = new PaintFlagsDrawFilter(Paint.FILTER_BITMAP_FLAG, 0);

	public SpeakerImageView(Context context) {
		super(context);
		init(context);
	}

	public SpeakerImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public SpeakerImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	protected void init(Context context) {
		final Resources res = context.getResources();
		mBitmap = BitmapFactory.decodeResource(res, R.drawable.speaker_thumbnail);

		mRect.setStyle(Paint.Style.STROKE);
		mRect.setColor(Color.BLACK);
		mRect.setStrokeWidth(1);
		mRect.setShadowLayer(2.0f, 2.0f, 2.0f, 0xff000000);
		
		mFill.setStyle(Paint.Style.FILL);
		mFill.setColor(Color.WHITE);
	}
	
	public void setImage(Bitmap image) {
		mBitmap = image;
		invalidate();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		canvas.drawRect(0, 0, getMeasuredWidth() - 5, getMeasuredHeight() - 5, mRect);
		canvas.drawRect(1, 1, getMeasuredWidth() - 5, getMeasuredHeight() - 5, mFill);

		Matrix matrix = new Matrix();
		matrix.setScale(((float) getMeasuredWidth() - 10) / mBitmap.getWidth(),
				((float) getMeasuredHeight() - 10) / mBitmap.getHeight());
		matrix.postTranslate(3.0f, 3.0f);
		canvas.setDrawFilter(setFilter);
		canvas.drawBitmap(mBitmap, matrix, null);
		canvas.setDrawFilter(remFilter);
		
		canvas.restore();
	}

}
