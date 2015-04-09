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
import android.text.TextUtils;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

final class Utils {

  private static final long DEFAULT_CONNECTION_TIMEOUT = 20L;
  private static final TimeUnit DEFAULT_CONNECTION_TIMEOUT_UNIT = TimeUnit.SECONDS;
  private static final String WHEATER_METADATA =
      "com.github.leonardoxh.wheaterdata.YAHOO_API_KEY";

  static XmlPullParserFactory defaultXmlPullParser() {
    try {
      XmlPullParserFactory pullParserFactory = XmlPullParserFactory.newInstance();
      pullParserFactory.setNamespaceAware(true);
      return pullParserFactory;
    } catch (XmlPullParserException e) {
      throw new RuntimeException("Unable to create XMLPullParserFactory", e);
    }
  }

  static OkHttpClient defaultOkHttpClient() {
    OkHttpClient okHttpClient = new OkHttpClient();
    okHttpClient.setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_UNIT);
    okHttpClient.setReadTimeout(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_UNIT);
    okHttpClient.setWriteTimeout(DEFAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT_UNIT);
    return okHttpClient;
  }

  static String getApiKey(Context context) {
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

}
