package org.collegelabs.library.bitmaploader;

import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

import org.collegelabs.library.bitmaploader.views.AsyncImageView;

public class Request {

	protected WeakReference<AsyncImageView> mImageView;
	private String mUrl;
	private Future<?> mFuture;
	
	public Request(AsyncImageView imageView, String url) {
		if(url == null) throw new IllegalArgumentException("url can't be null");
		
		mImageView = new WeakReference<AsyncImageView>(imageView);
		mUrl = url;
	}

	public String getUrl() {
		return mUrl;
	}
	
	public AsyncImageView getImageView(){
		return mImageView.get();
	}

	public synchronized void setFuture(Future<?> future){
		mFuture = future;
	}

	public synchronized Future<?> getFuture(){
		return mFuture;
	}
	
	public synchronized void cancel(boolean mayInterruptIfRunning){
		if(mFuture != null){
			mFuture.cancel(mayInterruptIfRunning);			
		}
	}
}
