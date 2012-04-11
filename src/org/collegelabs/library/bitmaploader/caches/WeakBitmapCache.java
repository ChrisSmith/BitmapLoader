package org.collegelabs.library.bitmaploader.caches;

import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

public class WeakBitmapCache {

	//TODO create an interface so weakBitmapCache can still be used,
	//wrap all calls to the LruCache
	private LruCache<String,WeakReference<Bitmap>> mBitmapCache;

	public WeakBitmapCache(){
		mBitmapCache = new WeakCache(30);
	}

	public void put(String key, Bitmap bitmap){
		mBitmapCache.put(key, new WeakReference<Bitmap>(bitmap));
	}

	public Bitmap get(String key){
		if(key == null) return null; //no null keys!
		
		Bitmap result = null;
		WeakReference<Bitmap> ref = mBitmapCache.get(key);
		if(ref != null){
			result = ref.get();
			if(result == null)
				mBitmapCache.remove(key);
		}
		return result;
	}
	
	
	private static class WeakCache extends LruCache<String,WeakReference<Bitmap>>{ 
		public WeakCache(int maxSize) {
			super(maxSize);
		}
	};
	
}
