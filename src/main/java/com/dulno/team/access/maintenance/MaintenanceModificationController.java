package com.dulno.team.access.maintenance;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.maintenance.Maintenance;
import com.dulno.core.maintenance.MaintenanceDatabaseTable;
import com.dulno.core.maintenance.MaintenanceStatus;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.UUID;

@RestController
public final class MaintenanceModificationController extends TeamController {
  private final MaintenanceDatabaseTable maintenanceDatabaseTable;

  private MaintenanceModificationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    MaintenanceDatabaseTable maintenanceDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.maintenanceDatabaseTable = maintenanceDatabaseTable;
  }

  @RequestMapping(path = "/maintenance/create/", method = RequestMethod.POST)
  public void createMaintenance(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "maintenance.create")
      .thenAccept(hasPermission -> createMaintenance(body.getString("description"),
        body.getLong("startTime"), body.getLong("duration"), hasPermission));
  }

  private void createMaintenance(
    String description, long startTime, long duration, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    maintenanceDatabaseTable.generateAvailableMaintenanceId().thenAccept(id ->
      maintenanceDatabaseTable.insertMaintenance(id, description, startTime,
        duration, MaintenanceStatus.SCHEDULED));
  }

  @RequestMapping(path = "/maintenance/finish/", method = RequestMethod.POST)
  public void finishMaintenance(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var maintenanceId = body.getUUID("maintenance");
    memberHasPermission(findMemberId(request), "maintenance.finish")
      .thenAccept(hasPermission -> maintenanceDatabaseTable.maintenanceExists(maintenanceId)
        .thenAccept(exists -> finishMaintenance(maintenanceId, hasPermission, exists)));
  }

  private void finishMaintenance(
    UUID maintenanceId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    maintenanceDatabaseTable.findMaintenance(maintenanceId)
      .thenAccept(this::finishMaintenance);
  }

  private void finishMaintenance(Maintenance maintenance) {
    maintenanceDatabaseTable.updateMaintenanceStatus(maintenance,
      MaintenanceStatus.COMPLETED);
  }

  @RequestMapping(path = "/maintenance/delete/", method = RequestMethod.POST)
  public void deleteMaintenance(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var maintenanceId = body.getUUID("maintenance");
    memberHasPermission(findMemberId(request), "maintenance.delete")
      .thenAccept(hasPermission -> maintenanceDatabaseTable.maintenanceExists(maintenanceId)
        .thenAccept(exists -> deleteMaintenance(maintenanceId, hasPermission, exists)));
  }

  private void deleteMaintenance(
    UUID maintenanceId, boolean hasPermission, boolean exists
  ) {
    if (!hasPermission || !exists) {
      return;
    }
    maintenanceDatabaseTable.findMaintenance(maintenanceId)
      .thenAccept(this::deleteMaintenance);
  }

  private void deleteMaintenance(Maintenance maintenance) {
    maintenanceDatabaseTable.deleteMaintenance(maintenance.id());
  }
}
