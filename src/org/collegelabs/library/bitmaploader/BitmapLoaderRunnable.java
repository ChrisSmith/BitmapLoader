package org.collegelabs.library.bitmaploader;

import java.io.File;

import org.collegelabs.library.bitmaploader.views.AsyncImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;


/**
 * Loads a bitmap from disk, posts back to the ui to update
 */

public class BitmapLoaderRunnable implements Runnable{

	protected AsyncImageView mImageView;
	protected ICachePolicy mCachePolicy;
	protected BitmapCache mBitmapCache;
	protected String mUrl;

	public BitmapLoaderRunnable(AsyncImageView imageView, String url, ICachePolicy cachePolicy, BitmapCache bitmapCache) {
		mImageView = imageView;
		mCachePolicy = cachePolicy;
		mBitmapCache = bitmapCache;
		mUrl = url;
	}

	@Override
	public void run() {
		try{
			if(mUrl==null) return;

			Bitmap bitmap = mBitmapCache.get(mUrl);;

			boolean loadedOK = true;

			if(bitmap == null){
				File f = mCachePolicy.getFile(mUrl, true);
				String absPath = f.getAbsolutePath();
				bitmap = BitmapFactory.decodeFile(absPath);
				
				if(bitmap == null){
					if(f.exists()) f.delete();
					loadedOK = false;
				}else{
					mBitmapCache.put(mUrl, bitmap);					
				}
			}

			//this will run on the UI thread
			if(loadedOK){
				mImageView.onImageLoaded(bitmap,mUrl);
			}else{
				//load a default drawable?
			}

		}catch(Exception e){
			e.printStackTrace();
		}
	}
}