package org.collegelabs.library.bitmaploader;

import java.io.File;
import java.io.IOException;

import org.collegelabs.library.bitmaploader.BitmapLoader.SourceType;
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

	protected BitmapLoader mBitmaploader;
	protected DiskCache mDiskCache;
	protected StrongBitmapCache mBitmapCache;
	protected Request mRequest;
	
	public LoadDiskBitmap(BitmapLoader loader, Request request) {
		if(loader == null || request == null) throw new IllegalArgumentException("args can't be null");
		
		mBitmaploader = loader;
		mDiskCache = loader.getCachePolicy();
		mBitmapCache = loader.getBitmapCache();
		mRequest = request;
	}

	@Override
	public void run() {
		final String mUrl = mRequest.getUrl();
		
		try{
			if(Constants.DEBUG) Log.d(Constants.TAG, "[LoadDiskBitmap] running: "+mUrl);
			
			Bitmap bitmap = mBitmapCache.get(mUrl);
			
			boolean loadedOK = true;

			if(bitmap == null){
				File f = mDiskCache.getFile(mUrl);
				if(!f.exists()){
					if(Constants.DEBUG) Log.d(Constants.TAG, "[LoadDiskBitmap] requeue on network thread: "+mUrl);
					
					mBitmaploader.dispatch(mRequest, SourceType.Network);
					return;
				}
				
				if(!f.isFile()){
					throw new IOException("Not a file: "+f.getAbsolutePath());
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

			AsyncImageView imageView = mRequest.getImageView();
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