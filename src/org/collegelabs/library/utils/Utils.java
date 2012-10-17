package org.collegelabs.library.utils;

import java.io.File;
import java.io.IOException;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

/*
 * A collection of random methods that may be called staticly
 * from multiple different classes 
 * This class will also use java's lazy class loading to allow
 * applications to be run in multiple versions of the 
 * Android SDK without causing 'class not found' exceptions
 */
public abstract class Utils {

	protected static Utils INSTANCE = null;

	public static Utils getInstance() {

		final int sdkVersion = Build.VERSION.SDK_INT;

		if(INSTANCE==null)
			if(sdkVersion >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
				INSTANCE = new ICSUtils();
			}else if(sdkVersion >= Build.VERSION_CODES.HONEYCOMB){
				INSTANCE = new HoneyCombUtils();
			}else if(sdkVersion >= Build.VERSION_CODES.GINGERBREAD){
				INSTANCE = new GingerbreadUtils();
			}else if(sdkVersion >= Build.VERSION_CODES.FROYO){
				INSTANCE = new FroyoUtils();
			}else if(sdkVersion >= Build.VERSION_CODES.ECLAIR){
				INSTANCE = new EclairUtils();
			}else if(sdkVersion >= Build.VERSION_CODES.DONUT){
				INSTANCE = new DonutUtils();
			}else{
				throw new IllegalStateException("Unsupported SDK version: "+sdkVersion);
			}
		
		return INSTANCE;
	}

	// Prevent instantiation from other classes
	protected Utils() {}

	/*
	 * Get the external Cache path
	 */
	public abstract File getExternalCacheDir(Context c);  

	public abstract void enableStrictMode();


	@TargetApi(4)
	private static class DonutUtils extends Utils{
		
		@Override
		public File getExternalCacheDir(Context c) {
			final String AndroidDataDir = Environment.getExternalStorageDirectory().getPath()+"/Android/data/";
			final String AppCacheDir = AndroidDataDir + c.getPackageName() + "/cache/";
			
			File mExternalCacheDir = new File(AppCacheDir);
			
			if (!mExternalCacheDir.exists()) {
				try {
					(new File(AndroidDataDir,".nomedia")).createNewFile();
				} catch (IOException e) {
				}
				if (!mExternalCacheDir.mkdirs()) {
					Log.w("", "Unable to create external cache directory");
					return null;
				}
			}
			return mExternalCacheDir;

		}

		@Override
		public void enableStrictMode() {
			//NO OP
		}
	}

	@TargetApi(5)
	private static class EclairUtils extends DonutUtils{

	}

	@TargetApi(8)
	private static class FroyoUtils extends EclairUtils{
		public File getExternalCacheDir(Context c){
			return c.getExternalCacheDir();
		}
	}


	@TargetApi(9)
	private static class GingerbreadUtils extends FroyoUtils{		
		@Override
		public void enableStrictMode(){

			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
			.detectDiskReads()
			.detectDiskWrites()
			.detectNetwork()
			.penaltyLog()
//			 .penaltyDialog()
			.detectAll()
			.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			.detectLeakedSqlLiteObjects()
			.penaltyLog()
			//.penaltyDeath()
			.build());

		}
	}

	@TargetApi(11)
	private static class HoneyCombUtils extends GingerbreadUtils {
		@Override
		public void enableStrictMode(){

			StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
			.detectDiskReads()
			.detectDiskWrites()
			.detectNetwork()   
			.penaltyLog()
			.penaltyFlashScreen() //Added this in Honeycomb
			.detectAll()
			.build());
			StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
			.detectLeakedSqlLiteObjects()
			.detectLeakedClosableObjects()
			.penaltyLog()
			.build());
		}
	}

	
	@TargetApi(14)
	private static class ICSUtils extends HoneyCombUtils {
	
	}


}
