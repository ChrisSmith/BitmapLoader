package org.collegelabs.library.bitmaploader;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.collegelabs.library.bitmaploader.caches.DiskCache;
import org.collegelabs.library.bitmaploader.caches.StrongBitmapCache;
import org.collegelabs.library.bitmaploader.views.AsyncImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class LoadNetworkBitmap implements Runnable{

	protected DiskCache mCachePolicy;
	protected StrongBitmapCache mBitmapCache;
	protected String mUrl;
	protected Request mRequest;
	
	public LoadNetworkBitmap(BitmapLoader loader, Request request) {
		this(loader.getCachePolicy(), loader.getBitmapCache(), request);
	}
	
	public LoadNetworkBitmap(DiskCache cachePolicy, StrongBitmapCache bitmapCache, Request request) {
			mCachePolicy = cachePolicy;
			mBitmapCache = bitmapCache;
			mUrl = request.getUrl();
			mRequest = request;
	}

	@Override
	public void run() {
		try {
			if(Constants.DEBUG) Log.d(Constants.TAG, "[LoadNetworkBitmap] running: "+mUrl);
			
			File file = mCachePolicy.getFile(mUrl);
			Bitmap bitmap = null;
			
			boolean decodedOK = true;
			
			if(!file.exists()){
				HttpGet httpRequest = new HttpGet(mUrl);
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response = (HttpResponse) httpclient.execute(httpRequest);
				HttpEntity entity = response.getEntity();
				BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity); 
				InputStream instream = bufHttpEntity.getContent();	    	
				bitmap = BitmapFactory.decodeStream(instream);

				if(bitmap != null){	
				
					file.createNewFile();
	
					BufferedOutputStream os = 
						new BufferedOutputStream(new FileOutputStream(file), 1024);
					bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
					os.flush();
					os.close();
				}else{
					decodedOK = false;
				}
				
			}else{
				bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
				if(bitmap == null){
					decodedOK = false;
					if(Constants.DEBUG) Log.e(Constants.TAG, 
							"[LoadNetworkBitmap] bad image file, decode failed so its getting deleted: "+mUrl);
						
					file.delete(); //known to exist, don't need to check again
				}
			}
			
			AsyncImageView imageView = mRequest.getImageView();
			if(decodedOK){
				//Memory cache isn't required (So you can prefetch without loading them)
				if(mBitmapCache != null) mBitmapCache.put(mUrl, bitmap);
				if(imageView != null) imageView.asyncCompleted(bitmap, mUrl);
				else if(Constants.DEBUG) Log.w(Constants.TAG, "[LoadNetworkBitmap] imageview is null");
			}else{
				if(imageView != null) imageView.asyncFailed(mUrl);
				else if(Constants.DEBUG) Log.w(Constants.TAG, "[LoadNetworkBitmap] imageview is null");
			}

		} catch (UnknownHostException e) {
			//dns lookup failed, likely no internet connection
			if(Constants.DEBUG) e.printStackTrace();
			Log.e(Constants.TAG, "[LoadNetworkBitmap] : "+mUrl+" : "+e.toString());
		} catch (ClientProtocolException e) {
			if(Constants.DEBUG) e.printStackTrace();
			Log.e(Constants.TAG, "[LoadNetworkBitmap] : "+mUrl+" : "+e.toString());
		} catch (IOException e) {
			if(Constants.DEBUG) e.printStackTrace();
			Log.e(Constants.TAG, "[LoadNetworkBitmap] : "+mUrl+" : "+e.toString());
		} catch(Throwable e){
			Log.e(Constants.TAG, "[LoadNetworkBitmap] : "+mUrl+" : "+e.toString());
			if(Constants.DEBUG) e.printStackTrace();
		}
	}

}