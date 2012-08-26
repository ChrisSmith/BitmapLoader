package org.collegelabs.library.bitmaploader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.collegelabs.library.bitmaploader.caches.DiskCache;
import org.collegelabs.library.bitmaploader.caches.SimpleLruDiskCache;
import org.collegelabs.library.bitmaploader.caches.StrongBitmapCache;

import android.content.Context;


public class BitmapLoader {

	/*
	 * Instance Vars
	 */
	private final ExecutorService mBitmapLoaderPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //thread per processor
	private final ExecutorService mInternetLoaderPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //thread per processor
			
	public ExecutorService getBitmapThread(){ return mBitmapLoaderPool; }
	public ExecutorService getInternetThread(){ return mInternetLoaderPool; }
	
	private StrongBitmapCache mBitmapCache;
	private DiskCache mDiskCache;
	
	/*
	 * If the user supplies an DiskCache then they are responsible for closing it 
	 */
	private boolean autoCloseDiskCache = false;
	
	public BitmapLoader(Context ctx){
		this(ctx, null, null);
	}
	
	/**
	 * 
	 * @param uiHandler
	 * @param ctx
	 * @param cache Can be null
	 */
	public BitmapLoader(Context ctx, StrongBitmapCache cache, DiskCache diskCache){
		super();
		mBitmapCache = cache;
		mDiskCache = diskCache;
		
		if(mBitmapCache == null){
			mBitmapCache = StrongBitmapCache.build(ctx);
		}
		
		if(mDiskCache == null){
			mDiskCache = new SimpleLruDiskCache(ctx);
			autoCloseDiskCache = true;
		}
	}
	
	public void shutdownNow(){
		mInternetLoaderPool.shutdownNow();
		mBitmapLoaderPool.shutdownNow();
		if(autoCloseDiskCache) mDiskCache.disconnect();
	}
	
	public void dispatch(Request request, SourceType source){
		synchronized (request) {
			Future<?> future = null;
			
			switch(source){
			case Network:
				future = mInternetLoaderPool.submit(new LoadNetworkBitmap(this, request));	
				break;
			case Disk:
				future = mBitmapLoaderPool.submit(new LoadDiskBitmap(this, request));
				break;
			}
			request.setFuture(future);
		}
	}
	
	public DiskCache getCachePolicy(){ return mDiskCache; }
	public StrongBitmapCache getBitmapCache(){ return mBitmapCache; }	
	
	/** Possible locations to load the image from */
	public enum SourceType{
		Network,
		Disk,
	}
}
