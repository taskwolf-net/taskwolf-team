package com.dulno.team.access.maintenance;

import com.dulno.core.maintenance.Maintenance;
import com.dulno.core.maintenance.MaintenanceDatabaseTable;
import com.dulno.core.maintenance.MaintenanceStatus;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@RestController
public final class MaintenanceInformationController extends TeamController {
  private final MaintenanceDatabaseTable maintenanceDatabaseTable;

  private MaintenanceInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    MaintenanceDatabaseTable maintenanceDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.maintenanceDatabaseTable = maintenanceDatabaseTable;
  }

  @RequestMapping(path = "/maintenance/find/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findMaintenance(
    HttpServletRequest request
  ) {
    return memberHasPermission(findMemberId(request), "maintenance.find")
      .thenCompose(this::findMaintenance);
  }

  private CompletableFuture<Map<String, Object>> findMaintenance(
    boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return maintenanceDatabaseTable.findMaintenanceByStatus(
      MaintenanceStatus.SCHEDULED)
      .thenCompose(scheduled -> maintenanceDatabaseTable.findMaintenanceByStatus(
        MaintenanceStatus.RUNNING)
        .thenApply(running -> Map.of("maintenance",
          Stream.concat(scheduled.stream(), running.stream())
            .sorted(Comparator.comparingLong(Maintenance::startTime))
            .map(this::assemblyMaintenanceInformation).toList())));
  }

  private Map<String, Object> assemblyMaintenanceInformation(
    Maintenance maintenance
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("id", maintenance.id());
    information.put("startTime", maintenance.startTime());
    information.put("duration", maintenance.duration());
    information.put("status", maintenance.status().toString());
    return information;
  }
}
