package org.collegelabs.library.bitmaploader.views;

import java.lang.ref.WeakReference;

import org.collegelabs.library.bitmaploader.BitmapLoader;
import org.collegelabs.library.bitmaploader.BitmapLoader.SourceType;
import org.collegelabs.library.bitmaploader.Constants;
import org.collegelabs.library.bitmaploader.R;
import org.collegelabs.library.bitmaploader.Request;
import org.collegelabs.library.bitmaploader.caches.StrongBitmapCache;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

public class AsyncImageView extends ImageView {
	
	/** Current String representation of a URL to load the image from */
	private String mUrl = "";

	/** 
	 * Need to keep track of the current request
	 */
	private WeakReference<Request> mRequest = null;

	/** Bitmap to draw while the real one is loading */
	private Bitmap defaultBitmap = null;

	/** Delay before posting bitmap to UI thread */
	private long mDelay = 300l; 
	
	/** Boolean indicating if the request finished loading yet */
	private boolean isLoaded = false;
	
	//The view's default handler won't work correctly if it isn't attached
	//to its parent view (or something like that)
	//Use our own handler to ensure messages aren't dropped
	private Handler mHandler = new Handler();
	
	private WeakReference<Runnable> mLastMessage = null;
	
	private boolean mBlockLayout, mIsFixedSize;
	
	/*
	 * Inherited Constructors
	 */
	public AsyncImageView(Context context) {
		this(context, null);
	}
	
	public AsyncImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public AsyncImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.AsyncImage);

		mIsFixedSize = array.getBoolean(R.styleable.AsyncImage_isFixedSize, false);

		array.recycle();
	}

	/*
	 * Public
	 */
	
	public void setImageUrl(String url, BitmapLoader loader){
		if(url == null) throw new IllegalArgumentException("url cannot be null");
		if(loader == null) throw new IllegalArgumentException("loader can't be null");
		
		if(Constants.DEBUG) Log.d(Constants.TAG, "[AsyncImageView] set url: "+url);
		
		if(!mUrl.equals(url)){
			loadUrl(loader, url);
		}else{
			if(Constants.DEBUG) Log.w(Constants.TAG, "[AsyncImageView] urls match, not loading: "+url);
		}
	}
	
	
	/**
	 * Called upon completion of asyncLoadBitmap(BitmapLoader, SourceType, String)
	 * 
	 * @param bitmap The final bitmap to display
	 * @param pUrl The url representing this bitmap
	 */
	public void asyncCompleted(final Bitmap bitmap, final String pUrl){
		if(Constants.DEBUG) Log.d(Constants.TAG, "[AsyncImageView] asyncCompleted called: "+pUrl);
		
		if(pUrl == null) throw new IllegalArgumentException("pUrl can't be null");
		
		Runnable r = new Runnable() {
			@Override public void run() {
				if(!mUrl.equals(pUrl)){
					if(Constants.DEBUG) Log.w(Constants.TAG, "[AsyncImageView] race condition! mUrl != pUrl: "+mUrl+", "+pUrl);
					return;
				}
				
				if(Constants.DEBUG) Log.d(Constants.TAG, "[AsyncImageView] asyncCompleted set img: "+pUrl);
				
				isLoaded = true;
				Resources resources = getResources();
				Drawable[] layers = {new BitmapDrawable(resources, defaultBitmap), new BitmapDrawable(resources,bitmap)};
				TransitionDrawable transition = new TransitionDrawable(layers);
				transition.startTransition(300);
				setImageDrawable(transition);
				
				if(mListener!=null) mListener.onStateChanged(AsyncImageView.this, IStateChangeListener.State.LOADING_COMPLETED);
			}
		};
		
		mHandler.postDelayed(r, mDelay);
		mLastMessage = new WeakReference<Runnable>(r);
	}

	/**
	 * 
	 * @param pUrl
	 */
	public void asyncFailed(final String pUrl){
		if(Constants.DEBUG) Log.w(Constants.TAG, "[AsyncImageView] asyncFailed: "+pUrl);
		
		if(pUrl == null) throw new IllegalArgumentException("pUrl can't be null");
		//Should already be the defaultDrawable, TODO add a failure drawable

		if(mListener!=null) mListener.onStateChanged(AsyncImageView.this, IStateChangeListener.State.LOADING_FAILED);
	}
	
	public String getImageUrl(){
		return mUrl;
	}
	
	public void setDefaultBitmap(Bitmap bitmap){
		defaultBitmap = bitmap;
		if(!isLoaded){
			setImageBitmap(defaultBitmap);
		}
	}

	public void setDelay(long delayInMs){
		mDelay = delayInMs;
	}
	
	//Blocking layout to improve performance
	//Only set mIsFixedSize = true when the bounds of this image view 
	//won't change and the parent respects those bounds (LinearLayout using weights won't)
	//https://plus.google.com/113058165720861374515/posts/iTk4PjgeAWX
	@Override
    public void setImageDrawable(Drawable drawable) {
        blockLayoutIfPossible();
        super.setImageDrawable(drawable);
        mBlockLayout = false;
    }

    @Override
    public void requestLayout() {
        if (!mBlockLayout) {
            super.requestLayout();
        }
    }

    private void blockLayoutIfPossible() {
        if (mIsFixedSize) {
            mBlockLayout = true;
        }
    }
    
    public void setIsFixedSize(boolean isFixedSize){
    	mIsFixedSize = isFixedSize;
    }
    
	/*
	 * Private
	 */
	
	private void cancelCurrentRequest(String oldUrl){
		if(Constants.DEBUG) Log.w(Constants.TAG, "[AsyncImageView] cancelCurrentRequest: "+oldUrl);
		
		Request request = (mRequest != null) ? mRequest.get() : null;
		if(request != null){
			request.cancel(true);
		}

		if(mLastMessage != null){
			Runnable last = mLastMessage.get();
			mHandler.removeCallbacks(last);			
		}
		
		mRequest = null;
		isLoaded = false;
	}

	private void loadUrl(BitmapLoader loader, String newUrl){
		cancelCurrentRequest(mUrl);
		
		mUrl = newUrl;
		
		StrongBitmapCache bitmapCache = loader.getBitmapCache();
		
		Bitmap bitmap = bitmapCache.get(mUrl);
		
		if(bitmap!=null){
			isLoaded = true;
			setImageBitmap(bitmap);
			if(Constants.DEBUG) Log.d(Constants.TAG,"[AsyncImageView] Cache hit: "+mUrl);
			if(mListener!=null) mListener.onStateChanged(AsyncImageView.this, IStateChangeListener.State.LOADING_COMPLETED);
			return;
		}

		if(Constants.DEBUG) Log.d(Constants.TAG,"[AsyncImageView] Cache miss: "+mUrl);
		
		setImageBitmap(defaultBitmap);

		Request request = new Request(this, mUrl);
		loader.dispatch(request, SourceType.Disk);
		
		mRequest = new WeakReference<Request>(request);
		
		if(mListener!=null) mListener.onStateChanged(AsyncImageView.this, IStateChangeListener.State.LOADING_STARTED);
	}
	
	private IStateChangeListener mListener=null;
	public void setStateChangeListener(IStateChangeListener listener){
		mListener = listener;
	}
	
	public static interface IStateChangeListener{
		public static enum State{
			LOADING_STARTED,
			LOADING_COMPLETED,
			LOADING_FAILED
		}
		
		public void onStateChanged(AsyncImageView view, IStateChangeListener.State newState);
	}
}
