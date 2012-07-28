package org.collegelabs.library.bitmaploader;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import org.collegelabs.library.bitmaploader.caches.DiskCache;
import org.collegelabs.library.bitmaploader.caches.StrongBitmapCache;
import org.collegelabs.library.bitmaploader.views.AsyncImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


/**
 * Loads a bitmap from disk, posts back to the UI to update
 */

public class LoadDiskBitmap implements Runnable{

	protected WeakReference<AsyncImageView> mImageView;
	protected DiskCache mDiskCache;
	protected StrongBitmapCache mBitmapCache;
	protected String mUrl;

	public LoadDiskBitmap(AsyncImageView imageView, String url, DiskCache diskCache, StrongBitmapCache bitmapCache) {
		if(url == null) throw new IllegalArgumentException("url can't be null");
		if(diskCache == null) throw new IllegalArgumentException("diskCache can't be null");
		
		mImageView = new WeakReference<AsyncImageView>(imageView);
		mDiskCache = diskCache;
		mBitmapCache = bitmapCache;
		mUrl = url;
	}

	@Override
	public void run() {
		try{
			
			if(Constants.DEBUG) Log.d(Constants.TAG, "[LoadDiskBitmap] running: "+mUrl);
			
			Bitmap bitmap = mBitmapCache.get(mUrl);;
			
			boolean loadedOK = true;

			if(bitmap == null){
				File f = mDiskCache.getFile(mUrl);
				if(!f.exists() || !f.isFile()){
					throw new IOException("Doesn't exist or not a file: "+f.getAbsolutePath());
				}
				
				String absPath = f.getAbsolutePath();
				bitmap = BitmapFactory.decodeFile(absPath);
				
				if(bitmap == null){
					if(f.exists()){
						if(Constants.DEBUG) Log.e(Constants.TAG, 
							"[LoadDiskBitmap] bad image file, decode failed so its getting deleted: "+mUrl);
						f.delete();
					}
					
					loadedOK = false;
				}else{
					if(Constants.DEBUG) Log.d(Constants.TAG,  "[LoadDiskBitmap] put into cache: "+mUrl);
					mBitmapCache.put(mUrl, bitmap);
				}
			}

			AsyncImageView imageView = mImageView.get();
			if(Constants.DEBUG) Log.d(Constants.TAG,  "[LoadDiskBitmap] post back: "+mUrl+" loadedOk: "+loadedOK);

			if(loadedOK){
				if(imageView != null) imageView.asyncCompleted(bitmap,mUrl);
				else if(Constants.DEBUG) Log.w(Constants.TAG, "[LoadDiskBitmap] imageview is null "+mUrl);
			}else{
				if(imageView != null) imageView.asyncFailed(mUrl);
				else if(Constants.DEBUG) Log.w(Constants.TAG, "[LoadDiskBitmap] imageview is null "+mUrl);
			}

		}catch(Exception e){
			Log.e(Constants.TAG, "[LoadDiskBitmap] : "+mUrl+" : "+e.toString());
			if(Constants.DEBUG) e.printStackTrace();
		}catch(Throwable e){
			Log.e(Constants.TAG, "[LoadDiskBitmap] : "+mUrl+" : "+e.toString());
			if(Constants.DEBUG) e.printStackTrace();
		}
	}
}