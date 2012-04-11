Description
=======
BitmapLoader is an Android Library project that makes it easier to asynchronously load over HTTP. The library also caches the requests to either external or internal storage depending on what is available.

Sample
=======
~~~~~~ java
//onCreate()
BitmapLoader mBitmapLoader  = new BitmapLoader(this);
AsyncImageView mImageView = (AsyncImageView) findViewById(R.id.imageView1);

//Whenever you need to load an image
mImageView.setImageUrl("http://somedomain.com/awesomeimage.jpg", bitmapLoader);
~~~~~~


Sample Project
=======
A complete sample is over at [BitmapLoaderDemo](https://github.com/ChrisSmith/BitmapLoaderDemo)

Simple right? Thats how things should be. Please file issues and feature requests, I'm only getting started with this.


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
