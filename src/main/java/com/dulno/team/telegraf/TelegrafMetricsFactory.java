package com.dulno.team.telegraf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class TelegrafMetricsFactory {
  private final TelegrafConfiguration configuration;

  public TelegrafMetrics create(String node, String raw) {
    var body = TelegrafMetricBody.create(configuration.telegrafCluster(), raw);
    var cpuRatio = 100 - body.extractMetric("cpu_usage_idle", "cpu", "cpu-total");
    var totalMemory = body.extractMetric("mem_total");
    var usedMemory = body.extractMetric("mem_used");
    var memoryRatio = body.extractMetric("mem_used_percent");
    var totalDiskSpace = body.extractMetric("disk_total", "path", "/");
    var usedDiskSpace = body.extractMetric("disk_used", "path", "/");
    var diskSpaceRatio = body.extractMetric("disk_used_percent");
    return TelegrafMetrics.create(node, cpuRatio, byteToGigaByte(totalMemory),
      byteToGigaByte(usedMemory), memoryRatio, byteToGigaByte(totalDiskSpace),
      byteToGigaByte(usedDiskSpace), diskSpaceRatio);
  }

  private double byteToGigaByte(double value) {
    return value / 1024 / 1024 / 1024;
  }
}