package com.dulno.team.prometheus;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class PrometheusMetrics {
  private final String node;
  private final double totalMemory;
  private final double availableMemory;
  private final double usedMemory;
  private final double memoryRatio;
  private final double totalDiskSpace;
  private final double freeDiskSpace;
  private final double availableDiskSpace;
  private final double usedDiskSpace;
  private final double diskSpaceRatio;

  public Map<String, Object> assemblyInformation() {
    var information = Maps.<String, Object>newHashMap();
    information.put("node", node);
    information.put("totalMemory", totalMemory);
    information.put("availableMemory", availableMemory);
    information.put("usedMemory", usedMemory);
    information.put("memoryRatio", memoryRatio);
    information.put("totalDiskSpace", totalDiskSpace);
    information.put("freeDiskSpace", freeDiskSpace);
    information.put("availableDiskSpace", availableDiskSpace);
    information.put("usedDiskSpace", usedDiskSpace);
    information.put("diskSpaceRatio", diskSpaceRatio);
    return information;
  }
}