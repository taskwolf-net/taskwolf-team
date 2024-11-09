package com.dulno.team.prometheus;

import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@RequiredArgsConstructor(staticName = "create")
public class PrometheusMetricBody {
  private final String raw;

  public double extractFilesystemMetric(String metricName, String filesystem) {
    return extractMetric(line -> line.startsWith(metricName) &&
      line.contains("mountpoint=\"" + filesystem + "\""));
  }

  public double extractMetric(String metricName) {
    return extractMetric(line -> line.startsWith(metricName));
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
