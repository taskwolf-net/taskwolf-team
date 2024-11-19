package com.dulno.team.telegraf;

import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Accessors(fluent = true)
@RequiredArgsConstructor(staticName = "create")
public final class TelegrafMetrics implements Comparable<TelegrafMetrics> {
  private final String node;
  private final double cpuRatio;
  private final double totalMemory;
  private final double usedMemory;
  private final double memoryRatio;
  private final double totalDiskSpace;
  private final double usedDiskSpace;
  private final double diskSpaceRatio;

  public Map<String, Object> assemblyInformation() {
    var information = Maps.<String, Object>newHashMap();
    information.put("node", node);
    information.put("cpuRatio", cpuRatio);
    information.put("totalMemory", totalMemory);
    information.put("usedMemory", usedMemory);
    information.put("memoryRatio", memoryRatio);
    information.put("totalDiskSpace", totalDiskSpace);
    information.put("usedDiskSpace", usedDiskSpace);
    information.put("diskSpaceRatio", diskSpaceRatio);
    return information;
  }

  @Override
  public int compareTo(TelegrafMetrics other) {
    var points = 0;
    if (cpuRatio > other.cpuRatio()) {
      points++;
    }
    if (memoryRatio > other.memoryRatio()) {
      points++;
    }
    if (diskSpaceRatio > other.diskSpaceRatio()) {
      points++;
    }
    if (points <= 1) {
      return -1;
    }
    return 1;
  }
}