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

import java.util.ArrayList;
import java.util.List;

public final class LocationInfo {

  private String primaryWoeid;
  private List<String> woeids = new ArrayList<>();
  private String town;

  public String getPrimaryWoeid() {
    return primaryWoeid;
  }

  public String getTown() {
    return town;
  }

  public List<String> getWoeids() {
    return woeids;
  }

  void addWoeid(String woeid) {
    this.woeids.add(woeid);
  }

  void addWoeids(List<String> woeids) {
    this.woeids.addAll(woeids);
  }

  void setPrimaryWoeid(String woeid) {
    this.primaryWoeid = woeid;
  }

  void setTown(String town) {
    this.town = town;
  }

}
