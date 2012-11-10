Description
=======
BitmapLoader is an Android Library project that makes it easier to asynchronously load over HTTP, especially inside of ListViews. The library also caches the requests in memory and to external/internal storage.

Projects Using It
=======
- [onTour Free](https://play.google.com/store/apps/details?id=collegelabs.onTour.free)
- [onTour Pro](https://play.google.com/store/apps/details?id=collegelabs.onTour.paid)
- [Album Tracker](https://play.google.com/store/apps/details?id=org.collegelabs.albumtracker)


Sample
=======

Create a single bitmap cache accessible via the application object. 
You don't have to do this in the Application, but it helps prevent you from using too much memory
~~~~~~ java
public class Application extends android.app.Application {

    private StrongBitmapCache mBitmapCache;
	
    @Override
    public void onCreate(){
		super.onCreate();
		mBitmapCache = StrongBitmapCache.build(this);		
	}

    @Override
    public void onLowMemory (){
		super.onLowMemory();
		mBitmapCache.evictAll();
	}
	
	public StrongBitmapCache getBitmapCache(){ 
		return mBitmapCache; 
	}
}
~~~~~~

In an Activity or Adapter
~~~~~~ java
//Create a BitmapLoader (this is a Thread Pool used to load images from the network and disk cache)
BitmapLoader mBitmapLoader = new BitmapLoader(this, mBitmapCache, new SimpleLruDiskCache(this));

//Use the AsyncImageView class instead of ImageView in your layout
AsyncImageView mImageView = (AsyncImageView) findViewById(R.id.imageView1);

//Whenever you need to load an image, (like bindView for a ListView Adapter) just call setImageUrl.
//This does no disk/network on the main thread and if the bitmap is in the 
//LRU cache it is set immediately, otherwise it is done asynchronously 
mImageView.setImageUrl("http://somedomain.com/awesomeimage.jpg", bitmapLoader);
~~~~~~


Sample Project
=======
A complete sample is over at [BitmapLoaderDemo](https://github.com/ChrisSmith/BitmapLoaderDemo)

Hope you find this useful, please file issues and feature requests.


License
=======

    Copyright 2012 Chris Smith

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
