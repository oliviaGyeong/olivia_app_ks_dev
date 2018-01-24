package device.apps.pmpos.fragment.sign;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class BitmapConvertor{

	private int mDataWidth;
	private byte mRawBitmapData[];
	private byte[] mDataArray;
	private static final String TAG = "BitmapConvertor";
//	private ProgressDialog mPd;
	private Context mContext;
	private int mWidth, mHeight;
	private String mStatus;
	private String mFileName;
	
	
	public BitmapConvertor(Context context) {
		mContext = context;
	}

/**
 * Converts the input image to 1bpp-monochrome bitmap
 * @param inputBitmap : Bitmpa to be converted
 * @param fileName : Save-As filename
 * @return :  Returns a String. Success when the file is saved on memory card or error.
 */
    public String convertBitmap(Bitmap inputBitmap, String fileName){
		
    	mWidth = inputBitmap.getWidth();
    	mHeight = inputBitmap.getHeight();
    	mFileName = fileName;
    	mDataWidth=((mWidth+31)/32)*4*8;
    	mDataArray = new byte[(mDataWidth * mHeight)];
    	mRawBitmapData = new byte[(mDataWidth * mHeight) / 8];
    	ConvertInBackground convert = new ConvertInBackground();
    	convert.execute(inputBitmap);
    	return mStatus;
    	
    }

	

	private void convertArgbToGrayscale(Bitmap bmpOriginal, int width, int height){
    	int pixel;
    	int k = 0;
    	int B=0,G=0,R=0;
    	try{
    	for(int x = 0; x < height; x++) {
            for(int y = 0; y < width; y++, k++) {
                // get one pixel color
                pixel = bmpOriginal.getPixel(y, x);
                
                // retrieve color of all channels
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                // take conversion up to one single value by calculating pixel intensity.
                R = G = B = (int)(0.299 * R + 0.587 * G + 0.114 * B);
                // set new pixel color to output bitmap
                if (R < 128) {
					mDataArray[k] = 0;
				} else {
					mDataArray[k] = 1;
				}
            }
            if(mDataWidth>width){
				for(int p=width;p<mDataWidth;p++,k++){
					mDataArray[k]=1;
				}
			}
        }
    	}catch (Exception e) {
    		Log.e(TAG, e.toString());
		}
    }
    
    private void createRawMonochromeData(){
    	int length = 0;
    	for (int i = 0; i < mDataArray.length; i = i + 8) {
			byte first = mDataArray[i];
			for (int j = 0; j < 7; j++) {
				byte second = (byte) ((first << 1) | mDataArray[i + j]);
				first = second;
			}
			mRawBitmapData[length] = first;
			length++;
		}
    }
    
    private String saveImage(String fileName, int width, int height) {
    	FileOutputStream fileOutputStream;
    	BMPFile bmpFile = new BMPFile();
		//File file = new File(Environment.getExternalStorageDirectory(), fileName + ".bmp");
		File file = new File(mContext.getFilesDir(), fileName);
		try {
			file.createNewFile();
			fileOutputStream = new FileOutputStream(file);
		} catch (IOException e) {
			return "Memory Access Denied";
		}
		bmpFile.saveBitmap(fileOutputStream, mRawBitmapData, width, height);
		return "Success";
    }

    class ConvertInBackground extends AsyncTask<Bitmap, String, Void>{

		@Override
		protected Void doInBackground(Bitmap... params) {
			convertArgbToGrayscale(params[0], mWidth, mHeight);
	    	createRawMonochromeData();
	    	mStatus = saveImage(mFileName, mWidth, mHeight);
			return null;
		}
    	
    }
}
