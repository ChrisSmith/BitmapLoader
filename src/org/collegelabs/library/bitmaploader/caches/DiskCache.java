package org.collegelabs.library.bitmaploader.caches;

import java.io.File;

public interface DiskCache {

	public enum Location{
		Internal,
		External
	}
	
	public DiskCache.Location getCacheLocation();
	
	public File getCacheDirectory();
	
	/**
	 * Maps filename to a File object.
	 * 
	 * Returns a file even if it doesn't exist,
	 * This just uses the correct mapping with the current 
	 * state of external/internal storage.
	 */
	public File getFile(String fileName);
	
	/**
	 * Evict all files from the cache
	 */
	public boolean purge();
	
	/**
	 * 
	 * @return
	 */
	public boolean sweep();
	
	/**
	 * Always disconnect from the DiskCache so it has a chance to 
	 * cleanup after itself
	 */
	public void disconnect();

}
