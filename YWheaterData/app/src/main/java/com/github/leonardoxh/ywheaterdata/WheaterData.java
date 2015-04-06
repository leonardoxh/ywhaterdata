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

public final class WheaterData {

  private static final int INVALID_TEMPERATURE = Integer.MIN_VALUE;
  private static final int INVALID_CONDITION = -1;

  private String conditionText;
  private String forecastText;
  private String location;
  private String wheaterUnit;
  private int low = INVALID_TEMPERATURE;
  private int high = INVALID_TEMPERATURE;
  private int temperature = INVALID_TEMPERATURE;
  private int conditionCode = INVALID_CONDITION;
  private int todayForecastConditionCode = INVALID_CONDITION;

  public void setConditionText(String conditionText) {
    this.conditionText = conditionText;
  }

  public void setForecastText(String forecastText) {
    this.forecastText = forecastText;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public void setTemperature(int temperature) {
    this.temperature = temperature;
  }

  public void setConditionCode(int conditionCode) {
    this.conditionCode = conditionCode;
  }

  public void setTodayForecastConditionCode(int todayForecastConditionCode) {
    this.todayForecastConditionCode = todayForecastConditionCode;
  }

  public void setHigh(int high) {
    this.high = high;
  }

  public void setLow(int low) {
    this.low = low;
  }

  public void setWheaterUnit(String wheaterUnit) {
    this.wheaterUnit = wheaterUnit;
  }

  public String getLocation() {
    return location;
  }

  public String getConditionText() {
    return conditionText;
  }

  public int getConditionCode() {
    return conditionCode;
  }

  public String getForecastText() {
    return forecastText;
  }

  public int getLow() {
    return low;
  }

  public int getHigh() {
    return high;
  }

  public int getTemperature() {
    return temperature;
  }

  public int getTodayForecastConditionCode() {
    return todayForecastConditionCode;
  }

  public String getWheaterUnit() {
    return wheaterUnit;
  }

}
