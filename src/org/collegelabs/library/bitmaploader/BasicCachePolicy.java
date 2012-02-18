package org.collegelabs.library.bitmaploader;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.collegelabs.library.utils.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.util.Log;

public class BasicCachePolicy implements ICachePolicy {

	public static String TAG = "";
	public static boolean DEBUG = false;
	
	//Constants
	private static final long MB = 1024 * 1024;
	private static final long HOUR = 1000 * 60 * 60;
	

	//Instance
	private Location cacheLocation = null;
	private File cacheDir = null;
	private Context mContext;
	private BroadcastReceiver mExternalStorageReceiver;
	private AtomicBoolean closed = new AtomicBoolean(false);

	
	/** If a file is younger that this won't be deleted, unless we pass the high water mark */
	private long MIN_EXPIRE_AGE = 24 * HOUR * 7; //one week
	
	/** Old files are eligible for deletion when the cache grows beyond this size */
	private long LOW_WATER_MARK = 3 * MB;
	/** Old and new files are eligible for deletion when the cache grows beyond this size */
	private long HIGH_WATER_MARK = 5 * MB;

	public BasicCachePolicy(Context ctx){
		mContext = ctx;
		startWatchingExternalStorage();
	}

	public synchronized void setMinExpireAge(long hours) {	MIN_EXPIRE_AGE = hours * HOUR;  }
	public synchronized void setLowWaterMark(int mb) {	LOW_WATER_MARK = mb * MB;  }
	public synchronized void setHighWaterMark(int mb) {	HIGH_WATER_MARK = mb * MB;  }
	
	
	@Override
	public synchronized Location getCacheLocation() {
		if(closed.get()){
			Log.w(TAG,"getCacheLocation called after cache was closed");
			return null;
		}

		return cacheLocation;
	}

	@Override
	public synchronized File getCacheDirectory() {
		if(closed.get()){
			Log.w(TAG,"getCacheDirectory called after cache was closed");
			return null;
		}

		return cacheDir;
	}

	@Override
	public synchronized File getFile(String fileName, boolean updateTimestamp) {
		if(closed.get()){
			Log.w(TAG,"getFile called after cache was closed");
			return null;
		}

		String newFileName = fileName.replaceAll("[^\\d\\w\\.]", "");
		File f = new File(cacheDir, newFileName);
		//any time we are using a cached file we can update this
		//this will allow us to sort the files in the cache
		//based on how recently the file was needed
		if(updateTimestamp && f.exists()) 
			f.setLastModified(System.currentTimeMillis());
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
							Log.d(TAG,"deleting cache file: "+file);
							f.delete();							
						}
					}
				}
			}

			return true;

		}catch(Exception e){
			Log.e(TAG,"Failed to purge cache: "+e.toString());
			e.printStackTrace();
			return false;
		}
	}


	private synchronized void updateExternalStorageState() {
		if(closed.get()){
			Log.w(TAG,"updateExternalStorageState called after cache was closed");
			return;
		}

		boolean storageStateHasChanged = false;

		boolean externalMounted = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
		//Check if External is mounted
		if(externalMounted && cacheLocation != Location.External){
			cacheDir = Utils.getInstance().getExternalCacheDir(mContext);
			cacheLocation = Location.External;
			storageStateHasChanged = true;
		}
		//If not try internal
		if(cacheDir == null || (!externalMounted && cacheLocation != Location.Internal)){
			cacheDir = mContext.getCacheDir();
			cacheLocation = Location.Internal;
			storageStateHasChanged = true;
		}

		if(cacheDir == null)
			throw new IllegalStateException("No storage available?");

		//If the state hasn't changed there is no need to continue
		if(!storageStateHasChanged) return;

		if(!cacheDir.exists())
			cacheDir.mkdirs();
		//check or create .nomedia file
		//prevents images from appearing in the gallery
		File nomedia = new File(cacheDir,".nomedia");
		if(!nomedia.exists()){
			try {
				nomedia.createNewFile();
			} catch (IOException e) {
				Log.e(TAG,"unable to create .nomedia file: "+e.toString());
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
		updateExternalStorageState();
	}


	@Override
	public synchronized void close(){
		mContext.unregisterReceiver(mExternalStorageReceiver);
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
			Log.e(TAG,"failed to sweep cache");
			return false;
		}
		
		long dirSizeBytes = result.length;
		long dirSizeMB = dirSizeBytes / (1024 * 1024);
		if(DEBUG) Log.d(TAG,"cache size: "+dirSizeBytes+" Bytes ("+dirSizeMB+" MB)");

		if(dirSizeBytes < LOW_WATER_MARK){
			if(DEBUG) Log.d(TAG,"below low water mark, nothing to do");
			return true;
		}
		
		Iterator<File> iter = result.olderFiles.descendingIterator();
		File f;
		while(dirSizeBytes > LOW_WATER_MARK && iter.hasNext()){
			f = iter.next();
			dirSizeBytes -= f.length();
			if(DEBUG) Log.d(TAG,"sweeping (old): "+f.toString()+" : "+f.length());
			f.delete();
		}

		if(dirSizeBytes < HIGH_WATER_MARK){
			if(DEBUG) Log.d(TAG,"below high water mark, nothing else to do");
			return true;
		}
		
		iter = result.newerFiles.descendingIterator();
		while(dirSizeBytes > HIGH_WATER_MARK && iter.hasNext()){
			f = iter.next();
			dirSizeBytes -= f.length();
			if(DEBUG) Log.d(TAG,"sweeping (new): "+f.toString()+" : "+f.length());
			f.delete();
		}
		
		if(DEBUG) Log.d(TAG,"done sweeping");
		return true;
	}

	/**
	 * 
	 * @param root
	 * @return Size of directory in Bytes
	 */
	private Tuple getDirectorySize(File root){
		long size = 0l;
		
		final Comparator<File> fileAgeComparator = new Comparator<File>() {
			@Override
			public int compare(File lhs, File rhs) {
				int res = (int) (lhs.lastModified() - rhs.lastModified());
				if(res == 0) res = (int) (lhs.length() - rhs.length());
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
			Log.e(TAG,"Failed to get dir size: "+e.toString());
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
