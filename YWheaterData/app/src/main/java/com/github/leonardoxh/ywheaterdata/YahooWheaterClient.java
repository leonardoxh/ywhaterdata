/*
 * Copyright 2015 Leonardo Rossetto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.leonardoxh.ywheaterdata;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.text.TextUtils;

import android.util.Log;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.net.HttpURLConnection;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.Map;

import static java.util.concurrent.Executors.newCachedThreadPool;

public final class YahooWheaterClient {

  public static final char WHEATER_UNIT_CELCIUS = 'c';
  public static final char WHEATER_UNIT_FAREINHART = 'f';

  private static final long DEFAULT_CONNECTION_TIMEOUT = 20L;
  private static final TimeUnit DEFAULT_CONNECTION_TIMEOUT_UNIT = TimeUnit.SECONDS;
  private static final Executor DEFAULT_EXECUTOR = newCachedThreadPool();
  private static final XmlPullParserFactory DEFAULT_PULL_PARSER = defaultXmlPullParser();
  private static final String WHEATER_METADATA = "com.github.leonardoxh.wheaterdata.YAHOO_API_KEY";

  private char wheaterUnit = WHEATER_UNIT_CELCIUS;
  private OkHttpClient okHttpClient = defaultOkHttpClient();
  private static String appId;
  private Callbacks callbacks;

  public YahooWheaterClient(String appId) {
    init(appId);
  }

  public YahooWheaterClient(Context context) {
    init(context);
  }

  public YahooWheaterClient() {
    if(appId == null) {
      Log.w(getClass().getName(), "appId == null maybe you forgot init call?");
    }
  }

  public static void init(Context context) {
    init(getAppId(context));
  }

  public static void init(String appId) {
    if(YahooWheaterClient.appId == null) {
      YahooWheaterClient.appId = appId;
    }
  }

  public void wheaterForWoied(LocationInfo locationInfo) {
    if (TextUtils.isEmpty(locationInfo.getPrimaryWoeid())) {
      callbacks.wheaterDataError(locationInfo);
      return;
    }
    Request request = new Request.Builder()
        .get()
        .url(buildWheaterQueryUrl(locationInfo.getPrimaryWoeid(), wheaterUnit))
        .build();
    okHttpClient.newCall(request).enqueue(new OnWoeidResponseListener(locationInfo));
  }

  public void locationInfoForLocation(Location location, Callbacks callbacks) {
    this.callbacks = callbacks;
    Request.Builder request = new Request.Builder();
    request.url(buildUrl(appId, location));
    okHttpClient.newCall(request.get().build()).enqueue(new OnLocationResponseListener());
  }

  public void setWheaterUnit(char wheaterUnit) {
    this.wheaterUnit = wheaterUnit;
  }

  public char getWheaterUnit() {
    return wheaterUnit;
  }

  public void setOkHttpClient(OkHttpClient okHttpClient) {
    this.okHttpClient = okHttpClient;
  }

  public void setAppId(String appId) {
    YahooWheaterClient.appId = appId;
  }

  private static String getAppId(Context context) {
    try {
      ApplicationInfo ai = context.getPackageManager()
          .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
      String apiKey = ai.metaData.getString(WHEATER_METADATA);
      if(TextUtils.isEmpty(apiKey)) {
        throw new RuntimeException("API Key == null");
      }
      return apiKey;
    } catch (PackageManager.NameNotFoundException e) {
      throw new RuntimeException("Package name not found, are the app installed?", e);
    }
  }

  private static XmlPullParserFactory defaultXmlPullParser() {
    try {
      XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
      pullParserFactory.setNamespaceAware(true);
      return pullParserFactory;
    } catch (XmlPullParserException e) {
      throw new RuntimeException("Unable to create XMLPullParserFactory", e);
    }
  }

  private static OkHttpClient defaultOkHttpClient() {
    OkHttpClient okHttpClient = new OkHttpClient();
    okHttpClient.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_UNIT);
    okHttpClient.setReadTimeout(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_UNIT);
    return okHttpClient;
  }

  private static String buildWheaterQueryUrl(String woeid, char wheaterUnit) {
    return "http://weather.yahooapis.com/forecastrss?w=" + woeid + "&u=" + wheaterUnit;
  }

  private static String buildUrl(String appId, Location location) {
    StringBuilder sb = new StringBuilder();
    sb.append("http://where.yahooapis.com/v1/places.q('");
    sb.append(location.getLatitude());
    sb.append(",");
    sb.append(location.getLongitude());
    sb.append("')");
    sb.append("?appid=");
    sb.append(appId);
    return sb.toString();
  }

  class OnLocationResponseListener implements Callback {

    @Override public void onFailure(Request request, IOException e) {
      callbacks.wheaterDataError(null);
    }

    @Override public void onResponse(Response response) throws IOException {
      if (response.isSuccessful()) {
        new LocationInfoParserTask().executeOnExecutor(DEFAULT_EXECUTOR,
            response.body().string());
      } else {
        callbacks.wheaterDataError(null);
      }
    }

  }

  class OnWoeidResponseListener implements Callback {

    final LocationInfo locationInfo;

    OnWoeidResponseListener(LocationInfo locationInfo) {
      this.locationInfo = locationInfo;
    }

    @Override public void onFailure(Request request, IOException e) {
      callbacks.wheaterDataError(locationInfo);
    }

    @Override public void onResponse(Response response) throws IOException {
      if (response.code() == HttpURLConnection.HTTP_OK) {
        new WoeidParserTask(locationInfo).executeOnExecutor(DEFAULT_EXECUTOR,
            response.body().string());
      } else {
        callbacks.wheaterDataError(locationInfo);
      }
    }

  }

  class LocationInfoParserTask extends AsyncTask<String, Void, LocationInfo> {

    @Override protected LocationInfo doInBackground(String... strings) {
      String content = strings[0];
      LocationInfo locationInfo = new LocationInfo();
      String primaryWoeid = null;
      Map<String, String> alternativeWoeids = new HashMap<>();
      try {
        XmlPullParser xpp = DEFAULT_PULL_PARSER.newPullParser();
        xpp.setInput(new StringReader(content));
        boolean inWoe = false;
        boolean inTown = false;
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
          String tagName = xpp.getName();
          if (eventType == XmlPullParser.START_TAG &&
              "woeid".equals(tagName)) {
            inWoe = true;
          } else if (eventType == XmlPullParser.TEXT && inWoe) {
            primaryWoeid = xpp.getText();
          }
          if (eventType == XmlPullParser.START_TAG &&
              (tagName.startsWith("locality") || tagName.startsWith("admin"))) {
            for (int i = xpp.getAttributeCount() - 1; i >=0; i--) {
              String attrName = xpp.getAttributeName(i);
              if ("type".equals(attrName) &&
                  "Town".equals(xpp.getAttributeValue(i))) {
                inTown = true;
              } else if ("woeid".equals(attrName)) {
                String woeid = xpp.getAttributeValue(i);
                if (!TextUtils.isEmpty(woeid)) {
                  alternativeWoeids.put(tagName, woeid);
                }
              }
            }
          } else if (eventType == XmlPullParser.TEXT && inTown) {
            locationInfo.setTown(xpp.getText());
          }
          if (eventType == XmlPullParser.END_TAG) {
            inWoe = false;
            inTown = false;
          }
          eventType = xpp.next();
        }
        if (!TextUtils.isEmpty(primaryWoeid)) {
          locationInfo.setPrimaryWoeid(primaryWoeid);
        }
        for (Map.Entry<String, String> entry : alternativeWoeids.entrySet()) {
          locationInfo.addWoeid(entry.getValue());
        }
        return locationInfo;
      } catch (IOException | XmlPullParserException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override protected void onPostExecute(LocationInfo locationInfo) {
      if (locationInfo == null) {
        callbacks.wheaterDataError(null);
      } else {
        wheaterForWoied(locationInfo);
      }
    }

  }

  class WoeidParserTask extends AsyncTask<String, Void, WheaterData> {

    final LocationInfo locationInfo;

    WoeidParserTask(LocationInfo locationInfo) {
      this.locationInfo = locationInfo;
    }

    @Override protected WheaterData doInBackground(String... strings) {
      String content = strings[0];
      WheaterData data = new WheaterData();
      try {
        XmlPullParser xpp = DEFAULT_PULL_PARSER.newPullParser();
        xpp.setInput(new StringReader(content));
        boolean hasTodayForecast = false;
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
          if (eventType == XmlPullParser.START_TAG &&
              "condition".equals(xpp.getName())) {
            for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
              if ("temp".equals(xpp.getAttributeName(i))) {
                data.setTemperature(Integer.parseInt(xpp.getAttributeValue(i)));
              } else if ("code".equals(xpp.getAttributeName(i))) {
                data.setConditionCode(Integer.parseInt(xpp.getAttributeValue(i)));
              } else if ("text".equals(xpp.getAttributeName(i))) {
                data.setConditionText(xpp.getAttributeValue(i));
              }
            }
          } else if (eventType == XmlPullParser.START_TAG
              && "forecast".equals(xpp.getName())
              && !hasTodayForecast) {
            hasTodayForecast = true;
            for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
              if ("code".equals(xpp.getAttributeName(i))) {
                data.setTodayForecastConditionCode(Integer.parseInt(xpp.getAttributeValue(i)));
              } else if ("low".equals(xpp.getAttributeValue(i))) {
                data.setLow(Integer.parseInt(xpp.getAttributeValue(i)));
              } else if ("high".equals(xpp.getAttributeValue(i))) {
                data.setHigh(Integer.parseInt(xpp.getAttributeValue(i)));
              } else if ("text".equals(xpp.getAttributeValue(i))) {
                data.setForecastText(xpp.getAttributeValue(i));
              }
            }
          } else if (eventType == XmlPullParser.START_TAG
              && "location".equals(xpp.getName())) {
            String cityOrVillage = "--";
            String region = null;
            String country = "--";
            for (int i = xpp.getAttributeCount() - 1; i >= 0; i--) {
              if ("city".equals(xpp.getAttributeName(i))) {
                cityOrVillage = xpp.getAttributeValue(i);
              } else if ("region".equals(xpp.getAttributeName(i))) {
                region = xpp.getAttributeValue(i);
              } else if ("country".equals(xpp.getAttributeName(i))) {
                country = xpp.getAttributeValue(i);
              }
            }
            if (!TextUtils.isEmpty(region)) {
              region = country;
            }
            data.setLocation(cityOrVillage + ", " + region);
          }
          eventType = xpp.next();
        }
      } catch (IOException | XmlPullParserException e) {
        e.printStackTrace();
        return null;
      }
      data.setWheaterUnit(wheaterUnit);
      return data;
    }

    @Override protected void onPostExecute(WheaterData wheaterData) {
      if (wheaterData == null) {
        callbacks.wheaterDataError(locationInfo);
      } else {
        callbacks.wheaterDataReceived(locationInfo, wheaterData);
      }
    }

  }

  public interface Callbacks {

    void wheaterDataReceived(LocationInfo locationInfo, WheaterData data);
    void wheaterDataError(LocationInfo locationInfo);

  }

}
