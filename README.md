# Yahoo Weather Data
Tired of use fuck yahoo XML apis ? This dependency is what you need

I use in my recent apps this two classes to get Yahoo API informations about the locale
temperature so, it's time this become a library...

This library is in alpha stage expect bugs and hard changes on a future. 
This library missing things like documentation, unit tests, and toons of other features, but remeber it's the first release.

Initialize the library on your Application class:

```java

  public class App extends Application {
  
    private static final String YAHOO_API_KEY = "YOUR_API_KEY";
  
    @Override public void onCreate() {
      super.onCreate();
      YahooWeatherClient.init(YAHOO_API_KEY);
    }
  
  }

```

Curently the library doesn't get your location :| so, you need do this by hand, like this snippet:

```java

  public class WeatherActivity extends Activity implements WeatherCallbacks {
  
    private YahooWeatherClient wheaterClient = new YahooWeatherClient();
    
    /* ... */
    
    @Override public void onLocationChanged(Location location) {
      wheaterClient.locationInfoForLocation(location, this);
    }
    
    @Override public void wheaterDataReceived(@NotNull LocationInfo locationInfo, @NotNull WeatherData weaterData) {
      //Use the WeatherData properties
    }
    
    @Override public void weatherDataError(@Nullable LocationInfo location) {
      //If the location info is null it's an error
      //If the location info is non null the principal woeid was not found so retry with an alternative woeid
      if (location != null && !location.getWoeids().isEmpty()) {
        //Use one of the alternatives woeids
        wheaterClient.weatherForWoied(location.getWoeids().get(0));
      }
    }
  
  }

```

The library is available on Maven Central :D Just add this to your buildscript

Gradle
---
```groovy
  dependencies {
    compile 'com.github.leonardoxh:ywheaterdata:0.2'
  }
```

Licence:
=================
```
Copyright 2015 Leonardo Rossetto

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
