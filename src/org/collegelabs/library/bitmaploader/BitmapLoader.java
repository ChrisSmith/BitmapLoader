package org.collegelabs.library.bitmaploader;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;


public class BitmapLoader {

	/*
	 * Instance Vars
	 */
	private final ExecutorService mBitmapLoaderPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //thread per processor
	private final ExecutorService mInternetLoaderPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()); //thread per processor

	public ExecutorService getBitmapThread(){ return mBitmapLoaderPool; }
	public ExecutorService getInternetThread(){ return mInternetLoaderPool; }
	
	private BitmapCache bitmapCache;
	private ICachePolicy mCachePolicy;
	
	public BitmapLoader(Context ctx){
		this(ctx, null);
	}
	
	/**
	 * 
	 * @param uiHandler
	 * @param ctx
	 * @param cache Can be null
	 */
	public BitmapLoader(Context ctx, BitmapCache cache){
		super();
		bitmapCache = cache;
		
		if(bitmapCache == null){
			bitmapCache = new BitmapCache();
		}
		
		mCachePolicy = new BasicCachePolicy(ctx);
		
	}
	
	public void shutdownNow(){
		mInternetLoaderPool.shutdownNow();
		mBitmapLoaderPool.shutdownNow();
		mCachePolicy.close();
	}
	
	public ICachePolicy getCachePolicy(){ return mCachePolicy; }
	public BitmapCache getBitmapCache(){ return bitmapCache; }	
}
