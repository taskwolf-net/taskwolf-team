package com.dulno.team.telegraf;

import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor(staticName = "create")
public class TelegrafMetricBody {
  private final String cluster;
  private final String raw;

  public double extractMetric(
    String metricName, String secondaryFilterKey, String secondaryFilterValue
  ) {
    return extractMetric(line -> line.startsWith(metricName) &&
      line.contains("cluster=\"" + cluster + "\"") &&
      line.contains(secondaryFilterKey + "=\"" + secondaryFilterValue + "\""));
  }

  public double extractMetric(String metricName) {
    return extractMetric(line -> line.startsWith(metricName) &&
      line.contains("cluster=\"" + cluster + "\""));
  }

  public double extractMetric(Function<String, Boolean> finder) {
    for (var line : raw.split("\n")) {
      if (finder.apply(line)) {
        var parts = line.split(" ");
        if (parts.length == 2) {
          return Double.parseDouble(parts[1]);
        }
      }
    }
    return -1;
  }
}
