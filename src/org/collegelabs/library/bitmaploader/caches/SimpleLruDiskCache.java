package org.collegelabs.library.bitmaploader.caches;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.collegelabs.library.bitmaploader.Constants;
import org.collegelabs.library.utils.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

public class SimpleLruDiskCache implements DiskCache {

	//Constants
	private static final long MB = 1024 * 1024;
	private static final long HOUR = 1000 * 60 * 60;
	

	//Instance
	private Location cacheLocation = null;
	private File cacheDir = null;
	private Context mContext;
	private BroadcastReceiver mExternalStorageReceiver;
	private AtomicBoolean closed = new AtomicBoolean(false);

	private Location mRequiredLocation;
	
	/** If a file is younger that this won't be deleted, unless we pass the high water mark */
	private long MIN_EXPIRE_AGE = 24 * HOUR * 7; //one week
	
	/** Old files are eligible for deletion when the cache grows beyond this size */
	private long LOW_WATER_MARK = 3 * MB;
	/** Old and new files are eligible for deletion when the cache grows beyond this size */
	private long HIGH_WATER_MARK = 5 * MB;

	public SimpleLruDiskCache(Context ctx){
		this(ctx, true);
	}

	public SimpleLruDiskCache(Context ctx, boolean registerBroadCastReceiver){
		this(ctx, registerBroadCastReceiver, Location.Any);
	}

	public SimpleLruDiskCache(Context ctx, boolean registerBroadCastReceiver, Location requiredLocation){
		mContext = ctx; //TODO can we use .getApplicationContext(); with a broadcast receiver?
		mRequiredLocation = requiredLocation;
		if(registerBroadCastReceiver && requiredLocation != Location.Internal){
			startWatchingExternalStorage();
		}
		updateExternalStorageState();
	}
	

	public synchronized void setMinExpireAge(long hours) {	MIN_EXPIRE_AGE = hours * HOUR;  }
	public synchronized void setLowWaterMark(int mb) {	LOW_WATER_MARK = mb * MB;  }
	public synchronized void setHighWaterMark(int mb) {	HIGH_WATER_MARK = mb * MB;  }
	
	
	@Override
	public synchronized Location getCacheLocation() {
		if(closed.get()){
			Log.w(Constants.TAG,"getCacheLocation called after cache was closed");
			return null;
		}

		return cacheLocation;
	}

	@Override
	public synchronized File getCacheDirectory() {
		if(closed.get()){
			Log.w(Constants.TAG,"getCacheDirectory called after cache was closed");
			return null;
		}

		return cacheDir;
	}

	private static final Pattern replace = Pattern.compile("[^\\d\\w\\.]");
	
	@Override
	public synchronized File getFile(String fileName) {
		if(closed.get()){
			Log.w(Constants.TAG,"getFile called after cache was closed");
			return null;
		}

		if(fileName == null) throw new IllegalArgumentException("filename cannot be null");
		Matcher m = replace.matcher(fileName);
		
		String newFileName = m.replaceAll("");
		File f = new File(cacheDir, newFileName);
		//any time we are using a cached file we can update this
		//this will allow us to sort the files in the cache
		//based on how recently the file was needed
		if(f.exists()) f.setLastModified(System.currentTimeMillis());
		
		return f;
	}

	@Override
	public synchronized boolean purge() {
		if(!closed.get()){
			throw new IllegalStateException("Cache must be closed before purging files");
		}

		try{
			Queue<File> dirs = new LinkedList<File>();
			dirs.add(cacheDir);

			while(!dirs.isEmpty()){
				File directory = dirs.remove();
				String[] children = directory.list();
				if (children == null) {
					// Either dir does not exist or is not a directory
				}else{
					for (String file : children) {
						File f = new File(cacheDir,file);

						if(f.isDirectory()){
							dirs.add(f);							
						}else if(!f.isHidden()){
							//don't delete hidden files like .nomedia
							if(Constants.DEBUG) Log.d(Constants.TAG,"deleting cache file: "+file);
							f.delete();							
						}
					}
				}
			}

			return true;

		}catch(Exception e){
			Log.e(Constants.TAG,"Failed to purge cache: "+e.toString());
			e.printStackTrace();
			return false;
		}
	}


	private synchronized void updateExternalStorageState() {
		if(closed.get()){
			Log.w(Constants.TAG,"updateExternalStorageState called after cache was closed");
			return;
		}

		boolean storageStateHasChanged = false;
		boolean externalMounted = false;
		
		if(mRequiredLocation != Location.Internal){
			externalMounted = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
			//Check if External is mounted
			if(externalMounted && cacheLocation != Location.External){
				cacheDir = Utils.getInstance().getExternalCacheDir(mContext);
				cacheLocation = Location.External;
				storageStateHasChanged = true;
			}			
		}
		
		if(mRequiredLocation != Location.External){
			//If not try internal
			if(cacheDir == null || (!externalMounted && cacheLocation != Location.Internal)){
				cacheDir = mContext.getCacheDir();
				cacheLocation = Location.Internal;
				storageStateHasChanged = true;
			}
		}
		

		if(cacheDir == null) //TODO handle this
			throw new IllegalStateException("No storage available?");

		//If the state hasn't changed there is no need to continue
		if(!storageStateHasChanged) return;

		if(Constants.DEBUG){
			Log.d(Constants.TAG, "storage state changed, now: "+cacheLocation+"\n"+cacheDir.toString());
		}
		
		if(!cacheDir.exists())
			cacheDir.mkdirs();
		//check or create .nomedia file
		//prevents images from appearing in the gallery
		File nomedia = new File(cacheDir,".nomedia");
		if(!nomedia.exists()){
			try {
				nomedia.createNewFile();
			} catch (IOException e) {
				Log.e(Constants.TAG,"unable to create .nomedia file: "+e.toString());
				e.printStackTrace();
			}
		}
	}

	private synchronized void startWatchingExternalStorage() {
		mExternalStorageReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				updateExternalStorageState();
			}
		};
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_REMOVED);
		filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
		filter.addAction(Intent.ACTION_MEDIA_SHARED);
		filter.addDataScheme("file");
		mContext.registerReceiver(mExternalStorageReceiver, filter);
	}


	@Override
	public synchronized void disconnect(){
		if(mExternalStorageReceiver != null){
			mContext.unregisterReceiver(mExternalStorageReceiver);
		}
		mContext = null;
		closed.set(true);
	}

	@Override
	public boolean sweep() {
		if(!closed.get()){
			throw new IllegalStateException("Cache must be closed before sweeping files");
		}
		
		Tuple result = getDirectorySize(cacheDir);
		if(result == null){
			Log.e(Constants.TAG,"failed to sweep cache");
			return false;
		}
		
		long dirSizeBytes = result.length;
		long dirSizeMB = dirSizeBytes / (1024 * 1024);
		if(Constants.DEBUG) Log.d(Constants.TAG,"cache size: "+dirSizeBytes+" Bytes ("+dirSizeMB+" MB)");

		if(dirSizeBytes < LOW_WATER_MARK){
			if(Constants.DEBUG) Log.d(Constants.TAG,"below low water mark, nothing to do");
			return true;
		}
		
		File f;
		Iterator<File> iter = result.olderFiles.iterator();
		while(dirSizeBytes > LOW_WATER_MARK && iter.hasNext()){
			f = iter.next();
			dirSizeBytes -= f.length();
			if(Constants.DEBUG) Log.d(Constants.TAG,"sweeping (old): "+f.toString()+" : "+f.length());
			f.delete();
		}

		if(dirSizeBytes < HIGH_WATER_MARK){
			if(Constants.DEBUG) Log.d(Constants.TAG,"below high water mark, nothing else to do");
			return true;
		}
		
		iter = result.newerFiles.iterator();
		while(dirSizeBytes > HIGH_WATER_MARK && iter.hasNext()){
			f = iter.next();
			dirSizeBytes -= f.length();
			if(Constants.DEBUG) Log.d(Constants.TAG,"sweeping (new): "+f.toString()+" : "+f.length());
			f.delete();
		}
		
		if(Constants.DEBUG) Log.d(Constants.TAG,"done sweeping");
		return true;
	}

	/**
	 * 
	 * @param root
	 * @return Size of directory in Bytes
	 */
	private Tuple getDirectorySize(File root){
		long size = 0l;
		
		//Reversed direction of comparator to return results 
		//Oldest(smallest) date and largest file first
		final Comparator<File> fileAgeComparator = new Comparator<File>() {
			@Override
			public int compare(File lhs, File rhs) {
				int res = (int) (lhs.lastModified() - rhs.lastModified());
				if(res == 0) res = (int) (rhs.length() - lhs.length());
				return res;
			}
		};
		
		TreeSet<File> olderFiles = new TreeSet<File>(fileAgeComparator);
		TreeSet<File> newFiles = new TreeSet<File>(fileAgeComparator);
		long EXPIRE_POINT = System.currentTimeMillis() - MIN_EXPIRE_AGE;
		
		try{
			Queue<File> dirs = new LinkedList<File>();
			dirs.add(root);

			while(!dirs.isEmpty()){
				File directory = dirs.remove();
				String[] children = directory.list();
				if (children == null) {
					// Either dir does not exist or is not a directory
				}else{
					for (String file : children) {
						File f = new File(directory,file);

						if(f.isDirectory()){
							dirs.add(f);							
						}else{
							size += f.length();
							if(f.lastModified() < EXPIRE_POINT)
								olderFiles.add(f);
							else 
								newFiles.add(f);
						}
					}
				}
			}

			return new Tuple(olderFiles, newFiles, size);
		}catch(Exception e){
			Log.e(Constants.TAG,"Failed to get dir size: "+e.toString());
			e.printStackTrace();
			return null;
		}
	}
	
	
	
	private static class Tuple{
		public TreeSet<File> olderFiles;
		public TreeSet<File> newerFiles;
		public long length;
		
		public Tuple(TreeSet<File> set1, TreeSet<File> set2, long len){
			this.olderFiles = set1;
			this.newerFiles = set2;
			this.length = len;
		}
	}
}
