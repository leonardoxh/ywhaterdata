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
