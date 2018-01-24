package device.apps.pmpos.fragment.sign;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import device.apps.pmpos.Utils;

public class TouchDrawer extends View {
    private static final String TAG = TouchDrawer.class.getSimpleName();

    private static final float TOUCH_TOLERANCE = 4;
    private static final int COUNT_OF_VALID_LINE_POINT = 10;//40;

    public static final int TEST_RESULT_INITIALIZE = -1;
    public static final int TEST_RESULT_SUCCESS = 1000;

    private final int ORIGINAL_BITMAP_WIDTH = 128;
    private final int ORIGINAL_BITMAP_HEIGHT = 64;

    private Canvas mCanvas;
    private Bitmap mBitmap;
    private Paint mBitmapPaint;

    private Paint mPaintPen;
    private Path mPenPath;

    private int mDisplayWidth = 0;
    private int mDisplayHeight = 0;
    private int mValidLinePointCount = 0;
    private int mResult = TEST_RESULT_INITIALIZE;

    private final Object mTestResultCallbackLock = new Object();
    
    private TouchTestResultCallback mTestResultCallback;

    private float mX, mY;

    private Context mContext;

    public TouchDrawer(Context context) {
        super(context);

        mContext = context;
        init();
    }

    public final void testResult(TouchTestResultCallback cb) {
        synchronized (mTestResultCallbackLock) {
            mTestResultCallback = cb;
        }
    }

    private void init() {
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        mDisplayWidth = displayMetrics.widthPixels; // 720
        mDisplayHeight = displayMetrics.heightPixels; // 1280

        if (Utils.DEBUG_INFO) Log.w(TAG, "mDisplayWidth= "+mDisplayWidth+", mDisplayHeight= "+mDisplayHeight);

        mPaintPen = new Paint();
        mPaintPen.setAntiAlias(true);
        mPaintPen.setDither(true);
        mPaintPen.setColor(Color.BLACK);
        mPaintPen.setStyle(Paint.Style.STROKE);
        mPaintPen.setStrokeJoin(Paint.Join.ROUND);
        mPaintPen.setStrokeCap(Paint.Cap.ROUND);
        mPaintPen.setStrokeWidth(5);

        mPenPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        setBackgroundColor(Color.WHITE);
    }

    public void clear() {
        if (mCanvas != null) {
            mCanvas.drawColor(0, PorterDuff.Mode.CLEAR);
            invalidate();
        }
        mValidLinePointCount = 0;
    }

    public void saveBitmapToFile(String fileName) {
        Bitmap resizeBitmap = Bitmap.createScaledBitmap(mBitmap, ORIGINAL_BITMAP_WIDTH, ORIGINAL_BITMAP_HEIGHT, false);

        BitmapConvertor bmpConvertor = new BitmapConvertor(mContext);
        bmpConvertor.convertBitmap(resizeBitmap, fileName);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }

        if (Utils.DEBUG_INFO) Log.w(TAG, "w= "+w+", h= "+h); // 640, 320
        if (Utils.DEBUG_INFO) Log.w(TAG, "oldw= "+oldw+", oldh= "+oldh); // 0, 0

        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_4444);
        mCanvas = new Canvas(mBitmap);

        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        canvas.drawPath(mPenPath, mPaintPen);
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mBitmap != null && !mBitmap.isRecycled()) {
            mBitmap.recycle();
            mBitmap = null;
        }
        super.onDetachedFromWindow();
    }

//    boolean signTouchDownFlag = false;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        updateTestSuccess();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchBegin(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
                RectF bounds = new RectF(0, 0, 0, 0);
                mPenPath.computeBounds(bounds, true);
                touchEnd();
//                mResult = TEST_RESULT_SUCCESS;
                mResult = TEST_RESULT_INITIALIZE;
                break;
        }
        return true;
    }

    private void touchBegin(float x, float y) {
        mPenPath.reset();
        mPenPath.moveTo(x, y);
        mX = x;
        mY = y;
        invalidate();
    }

    private void touchMove(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE ||
                dy >= TOUCH_TOLERANCE) {
            mPenPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
            mX = x;
            mY = y;
        }
        invalidate();
    }

    private void touchEnd() {
        mPenPath.lineTo(mX, mY);
        mCanvas.drawPath(mPenPath, mPaintPen);
        mPenPath.reset();
        invalidate();
    }

    private void updateTestSuccess() {
        mValidLinePointCount++;
        if (mValidLinePointCount > COUNT_OF_VALID_LINE_POINT) {
            mValidLinePointCount = 0;
            mResult = TEST_RESULT_SUCCESS;
            onTouchTestResult(mResult);
        }
    }

    private void onTouchTestResult(int result) {
        if (mTestResultCallback != null) {
            mTestResultCallback.onTouchTestResult(result);
        }
    }

    public interface TouchTestResultCallback {
        void onTouchTestResult(int result);
    }
}
