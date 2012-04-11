package org.collegelabs.library.bitmaploader.caches;

import org.collegelabs.library.bitmaploader.Constants;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class StrongBitmapCache extends LruCache<String, Bitmap>{

	//Thanks to Android Dev's http://developer.android.com/training/displaying-bitmaps/cache-bitmap.html
	public static StrongBitmapCache build(Context context){
		// Get memory class of this device, exceeding this amount will throw an
		// OutOfMemory exception.
		final int memClass = ((ActivityManager) context.getSystemService(
				Context.ACTIVITY_SERVICE)).getMemoryClass();

		// Use 1/8th of the available memory for this memory cache.
		final int cacheSize = 1024 * 1024 * memClass / 8;

		if(Constants.DEBUG){
			Log.d(Constants.TAG, "[StrongCache] memory class: "+memClass);
			Log.d(Constants.TAG, "[StrongCache] cache size: "+cacheSize+" bytes : "+(cacheSize/1024/1024)+" MB");
		}
		
		return new StrongBitmapCache(cacheSize);
	}
	
	public StrongBitmapCache(int maxSize){
		super(maxSize);
	}
	
	@Override
    protected int sizeOf(String key, Bitmap bitmap) {
        //getByteCount was added in 12
		//return bitmap.getByteCount();
		return bitmap.getRowBytes() * bitmap.getHeight();
	}	
}

