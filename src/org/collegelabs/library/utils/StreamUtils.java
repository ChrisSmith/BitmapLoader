package org.collegelabs.library.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class StreamUtils {

	public static String ReadInputStream(InputStream in) throws IOException {
		StringBuffer stream = new StringBuffer();
		byte[] b = new byte[1024];
		for (int n; (n = in.read(b)) != -1;) {
			stream.append(new String(b, 0, n));
		}
		return stream.toString();
	}
	
	public static void copyStream(InputStream is, OutputStream os)  throws IOException {
		byte[] buf = new byte[1024];
		int numRead;
		while ( (numRead = is.read(buf) ) >= 0) {
			os.write(buf, 0, numRead);
		}
	}

	public static void downloadUrl(File file, URL url) throws IOException{

		if(!file.exists()){ 
			file.createNewFile();
		}

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();

		try{
			InputStream is = null;

			if(connection.getResponseCode() >= HttpURLConnection.HTTP_BAD_REQUEST){
				is = connection.getErrorStream();
			}else{
				is = connection.getInputStream();
			}

			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file));
			StreamUtils.copyStream(is,os); 

			os.flush();
			os.close();	
			is.close();

		}finally{
			connection.disconnect();
		}

	}
}
