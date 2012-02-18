package org.collegelabs.library.bitmaploader.views;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.Future;

import org.collegelabs.library.bitmaploader.BitmapCache;
import org.collegelabs.library.bitmaploader.BitmapLoader;
import org.collegelabs.library.bitmaploader.BitmapLoaderRunnable;
import org.collegelabs.library.bitmaploader.ICachePolicy;
import org.collegelabs.library.bitmaploader.InternetBitmapRunnable;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class AsyncImageView extends ImageView {

	public AsyncImageView(Context context) {
		this(context, null);
	}

	public AsyncImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	private String mUrl = null;

	public String getImageUrl(){
		return mUrl;
	}

	public void setImageUrl(String url, BitmapLoader loader){
		if(mUrl == null || url == null || !mUrl.equals(url)){
			mUrl = url;
			loadUrl(loader);
		}
	}

	private WeakReference<Future<?>> mRequest = null;

	private void cancelCurrentRequest(){
		Future<?> request = (mRequest != null) ? mRequest.get() : null;
		if(request != null){
			request.cancel(true);
		}
		mRequest = null;
	}

	private void loadUrl(BitmapLoader loader){
		cancelCurrentRequest();
		
		BitmapCache bitmapCache = loader.getBitmapCache();
		ICachePolicy mCachePolicy = loader.getCachePolicy();
		
		Bitmap bitmap = bitmapCache.get(mUrl);
		if(bitmap!=null){
			setImageBitmap(bitmap);
			return;
		}

		setImageBitmap(defaultBitmap);

		if(mUrl == null) return;
		
		File file = mCachePolicy.getFile(mUrl, true);

		SourceType source = file.exists() ? SourceType.Disk : SourceType.Network;
		mRequest = new WeakReference<Future<?>>(doLoadUrl(loader, source, mUrl));
	}
	
	public enum SourceType{
		Network,
		Disk,
	}
	
	protected Future<?> doLoadUrl(BitmapLoader loader, SourceType source, String url){
		switch(source){
		case Network:{
			return loader.getInternetThread().submit(new InternetBitmapRunnable(this, url, loader.getCachePolicy(), loader.getBitmapCache()));	
		}
		case Disk:{
			return loader.getBitmapThread().submit(new BitmapLoaderRunnable(this, url, loader.getCachePolicy(), loader.getBitmapCache()));		
		}
		default:
			Log.w("","unknown source type: "+source);
			return null;
		}			
	}
	

	private Bitmap defaultBitmap = null;

	public void setDefaultBitmap(Bitmap bitmap){
		defaultBitmap = bitmap;
	}

	private long mDelay = 300l; //delay before posting bitmap to UI thread
	public void setDelay(long delayInMs){
		mDelay = delayInMs;
	}

	
	public void onImageLoaded(final Bitmap bitmap, final String pUrl){
		AsyncImageView.this.postDelayed(new Runnable() {
			@Override public void run() {
				if((mUrl == null && pUrl == null) || (mUrl != null && mUrl.equals(pUrl))){

					Resources resources = getResources();
					Drawable[] layers = {new BitmapDrawable(resources, defaultBitmap), new BitmapDrawable(resources,bitmap)};
					TransitionDrawable transition = new TransitionDrawable(layers);
					//					transition.setCrossFadeEnabled(true);
					transition.startTransition(300);
					setImageDrawable(transition);
				}
			}
		}, mDelay);
	}
}
