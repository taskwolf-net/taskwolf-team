package com.dulno.team.prometheus;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class PrometheusMetricsFactory {
  private final PrometheusConfiguration configuration;

  public PrometheusMetrics create(String node, String raw) {
    var body = PrometheusMetricBody.create(raw);
    var totalMemory = body.extractMetric("node_memory_MemTotal_bytes");
    var availableMemory = body.extractMetric("node_memory_MemAvailable_bytes");
    var usedMemory = totalMemory - availableMemory;
    var totalDiskSpace = body.extractFilesystemMetric("node_filesystem_size_bytes",
      configuration.rootMountPath());
    var freeDiskSpace = body.extractFilesystemMetric("node_filesystem_free_bytes",
      configuration.rootMountPath());
    var availableDiskSpace = body.extractFilesystemMetric(
      "node_filesystem_avail_bytes", configuration.rootMountPath());
    var usedDiskSpace = totalDiskSpace - freeDiskSpace;
    return PrometheusMetrics.create(node, byteToGigaByte(totalMemory),
      byteToGigaByte(availableMemory), byteToGigaByte(usedMemory),
      usedMemory / totalMemory, byteToGigaByte(totalDiskSpace),
      byteToGigaByte(freeDiskSpace), byteToGigaByte(availableDiskSpace),
      byteToGigaByte(usedDiskSpace), byteToGigaByte(usedDiskSpace / totalDiskSpace));
  }

  private double byteToGigaByte(double value) {
    return value / 1024 / 1024 / 1024;
  }
}