package com.dulno.team.maintenance;

import com.dulno.core.maintenance.Maintenance;
import com.dulno.core.maintenance.MaintenanceDatabaseTable;
import com.dulno.core.maintenance.MaintenanceStatus;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Singleton
@RequiredArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({@Inject}))
public final class TeamMaintenanceSchedule {
  private final MaintenanceDatabaseTable maintenanceDatabaseTable;
  private final ScheduledExecutorService executorService =
    Executors.newScheduledThreadPool(1);
  private ScheduledFuture<?> scheduler;

  private static final int RESET_INTERVAL = 1000 * 60;
  private static final TimeUnit RESET_TIME_UNIT = TimeUnit.MILLISECONDS;

  public void start() {
    scheduler = executorService.scheduleAtFixedRate(this::execute,
      calculateInitialDelay(), RESET_INTERVAL, RESET_TIME_UNIT);
    execute();
  }

  private long calculateInitialDelay() {
    var current = LocalDateTime.now();
    return Duration.between(current, current.plusMinutes(1)
      .truncatedTo(ChronoUnit.MINUTES)).toMillis() + 1000 * 30;
  }

  private void execute() {
    maintenanceDatabaseTable.findMaintenanceByStatus(MaintenanceStatus.SCHEDULED)
      .thenAccept(this::runMaintenance);
  }

  private void runMaintenance(List<Maintenance> maintenanceList) {
    for (var maintenance : maintenanceList) {
      if (System.currentTimeMillis() + 1000 * 60 >= maintenance.startTime()) {
        maintenanceDatabaseTable.updateMaintenanceStatus(maintenance,
          MaintenanceStatus.RUNNING);
      }
    }
  }

  public void stop() {
    scheduler.cancel(false);
  }
}
