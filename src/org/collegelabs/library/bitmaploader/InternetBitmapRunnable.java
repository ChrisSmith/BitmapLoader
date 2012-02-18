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
import org.collegelabs.library.bitmaploader.views.AsyncImageView;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class InternetBitmapRunnable implements Runnable{

	protected AsyncImageView mImageView;
	protected ICachePolicy mCachePolicy;
	protected BitmapCache mBitmapCache;
	protected String mUrl;
	
	public InternetBitmapRunnable(AsyncImageView imageView, String url, ICachePolicy cachePolicy, BitmapCache bitmapCache) {
			mImageView = imageView;
			mCachePolicy = cachePolicy;
			mBitmapCache = bitmapCache;
			mUrl = url;
	}

	@Override
	public void run() {
		try {
			
			File file = mCachePolicy.getFile(mUrl, true);
			Bitmap bitmap = null;
			
			boolean decodedOK = true;
			
			if(!file.exists()){
				HttpGet httpRequest = new HttpGet(mUrl);
				HttpClient httpclient = new DefaultHttpClient();
				HttpResponse response;
				response = (HttpResponse) httpclient.execute(httpRequest);
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
					file.delete(); //known to exist, don't need to check again
				}
			}
			
			if(decodedOK){
				if(mBitmapCache != null) mBitmapCache.put(mUrl, bitmap);
				if(mImageView != null) mImageView.onImageLoaded(bitmap, mUrl);
			}else{
				
			}

		} catch (UnknownHostException e) {
			//dns lookup failed, likely no internet connection
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}