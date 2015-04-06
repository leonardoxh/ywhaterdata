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

import android.location.Location;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Pair;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public class YahooWheaterClient {

  private final String mWheaterUnit;
  private final XmlPullParserFactory mPullParserFactory;
  private final Callbacks mCallbacks;
  private final String mAppId;

  public YahooWheaterClient(String appId, String wheaterUnit, Callbacks callback)
      throws XmlPullParserException {
    mPullParserFactory = XmlPullParserFactory.newInstance();
    mPullParserFactory.setNamespaceAware(true);
    mWheaterUnit = wheaterUnit;
    mCallbacks = callback;
    mAppId = appId;
  }

  public void wheaterForWoied(String woeid) {
    OkHttpClient client = new OkHttpClient();
    Request.Builder request = new Request.Builder();
    request.url(buildWheaterQueryUrl(woeid, mWheaterUnit));
    client.newCall(request.get().build()).enqueue(new OnWoeidResponseListener());
  }

  private static String buildWheaterQueryUrl(String woeid, String wheaterUnit) {
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

  public void locationInfoForLocation(Location location) {
    OkHttpClient client = new OkHttpClient();
    Request.Builder request = new Request.Builder();
    request.url(buildUrl(mAppId, location));
    client.newCall(request.get().build()).enqueue(new OnLocationResponseListener());
  }

  public void locationInfoForConfig(double latitude, double longitude) {
    Location location = new Location("");
    location.setLatitude(latitude);
    location.setLongitude(longitude);
    locationInfoForLocation(location);
  }

  class OnLocationResponseListener implements Callback {

    @Override public void onFailure(Request request, IOException e) {
      mCallbacks.locationInfoError();
    }

    @Override public void onResponse(Response response) throws IOException {
      if (response.isSuccessful()) {
        new LocationInfoParserTask().executeOnExecutor(Executors.newCachedThreadPool(),
            response.body().string());
      } else {
        mCallbacks.locationInfoError();
      }
    }

  }

  class OnWoeidResponseListener implements Callback {

    @Override public void onFailure(Request request, IOException e) {
      mCallbacks.wheaterDataError();
    }

    @Override public void onResponse(Response response) throws IOException {
      if (response.isSuccessful()) {
        new WoeidParserTask().executeOnExecutor(Executors.newCachedThreadPool(),
            response.body().string());
      } else {
        mCallbacks.wheaterDataError();
      }
    }

  }

  static class LocationInfo {

    public List<String> woeids = new ArrayList<>();
    public String town;

  }

  class LocationInfoParserTask extends AsyncTask<String, Void, LocationInfo> {

    @Override protected LocationInfo doInBackground(String... strings) {
      String content = strings[0];
      LocationInfo locationInfo = new LocationInfo();
      String primaryWoeid = null;
      List<Pair<String, String>> alternativeWoeids = new ArrayList<>();
      try {
        XmlPullParser xpp = mPullParserFactory.newPullParser();
        xpp.setInput(new StringReader(content));
        boolean inWoe = false;
        boolean inTown = false;
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
          String tagName = xpp.getName();
          if (eventType == XmlPullParser.START_TAG &&
              "woeid".equals(tagName)) {
            inWoe = true;
          } else if(eventType == XmlPullParser.TEXT && inWoe) {
            primaryWoeid = xpp.getText();
          }
          if (eventType == XmlPullParser.START_TAG &&
              (tagName.startsWith("locality") || tagName.startsWith("admin"))) {
            for(int i = xpp.getAttributeCount() - 1; i >=0; i--) {
              String attrName = xpp.getAttributeName(i);
              if ("type".equals(attrName) &&
                  "Town".equals(xpp.getAttributeValue(i))) {
                inTown = true;
              } else if("woeid".equals(attrName)) {
                String woeid = xpp.getAttributeValue(i);
                if (!TextUtils.isEmpty(woeid)) {
                  alternativeWoeids.add(new Pair<>(tagName, woeid));
                }
              }
            }
          } else if(eventType == XmlPullParser.TEXT && inTown) {
            locationInfo.town = xpp.getText();
          }
          if (eventType == XmlPullParser.END_TAG) {
            inWoe = false;
            inTown = false;
          }
          eventType = xpp.next();
        }
        if (!TextUtils.isEmpty(primaryWoeid)) {
          locationInfo.woeids.add(primaryWoeid);
        }
        Collections.sort(alternativeWoeids, new Comparator<Pair<String, String>>() {

          @Override public int compare(Pair<String, String> pair1,
              Pair<String, String> pair2) {
            return pair1.first.compareTo(pair2.first);
          }

        });
        for(Pair<String, String> pair : alternativeWoeids) {
          locationInfo.woeids.add(pair.second);
        }
        if (!locationInfo.woeids.isEmpty()) {
          return locationInfo;
        }
        return null;
      } catch (IOException | XmlPullParserException e) {
        e.printStackTrace();
        return null;
      }
    }

    @Override protected void onPostExecute(LocationInfo locationInfo) {
      if (locationInfo == null) {
        mCallbacks.locationInfoError();
      } else {
        mCallbacks.locationInfoReceived(locationInfo);
      }
    }

  }

  class WoeidParserTask extends AsyncTask<String, Void, WheaterData> {

    @Override protected WheaterData doInBackground(String... strings) {
      String content = strings[0];
      WheaterData data = new WheaterData();
      try {
        XmlPullParser xpp = mPullParserFactory.newPullParser();
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
          } else if(eventType == XmlPullParser.START_TAG
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
      data.setWheaterUnit(mWheaterUnit);
      return data;
    }

    @Override protected void onPostExecute(WheaterData wheaterData) {
      if (wheaterData == null) {
        mCallbacks.wheaterDataError();
      } else {
        mCallbacks.wheaterDataReceived(wheaterData);
      }
    }

  }

  public interface Callbacks {

    void wheaterDataReceived(WheaterData data);
    void wheaterDataError();
    void locationInfoReceived(LocationInfo info);
    void locationInfoError();

  }

}
