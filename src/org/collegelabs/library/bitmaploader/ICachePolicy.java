package org.collegelabs.library.bitmaploader;

import java.io.File;

public interface ICachePolicy {

	public enum Location{
		Internal,
		External,
		Either
	}
	
	public ICachePolicy.Location getCacheLocation();
	public File getCacheDirectory();
	
	
	
	
	/*
	 * Return a file even if it doesn't exist,
	 * This just uses the correct mapping with the current 
	 * state of external/internal storage
	 * 
	 * Useful for FileOutputStream when saveFile can't be used
	 *
	 * Increase the LastModified param? if the file exists
	 * Does an access value exist?
	 */
	public File getFile(String fileName, boolean updateTimestamp);
	
	/*
	 * Delete everything
	 */
	public boolean purge();
	
	
	public boolean sweep();
	
	/*
	 * Treat CachePolicies like files, always call close()
	 */
	public void close();

}
