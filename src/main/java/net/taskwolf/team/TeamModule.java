package net.taskwolf.team;

import net.taskwolf.core.CoreModule;
import net.taskwolf.core.log.Log;
import net.taskwolf.core.module.Module;
import net.taskwolf.core.module.ModuleDescription;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.module.ModuleLoadPriority;
import net.taskwolf.team.command.GroupCommand;
import net.taskwolf.team.command.PermissionCommand;
import net.taskwolf.team.command.TeamCommand;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;

@ModuleDescription(name = "team", version = "1.0.0-SNAPSHOT",
  priority = ModuleLoadPriority.NEUTRAL)
public final class TeamModule extends Module {
  private Log log;
  private TeamDatabaseTable teamDatabaseTable;
  private GroupDatabaseTable groupDatabaseTable;

  public TeamModule(CoreModule coreModule) {
    super(coreModule);
  }

  @Override
  public void enable() throws Exception {
    log = coreModule().log().subLog("Team");
    teamDatabaseTable = TeamDatabaseTable.create(coreModule().databaseConnection(),
      coreModule().databaseKeyspace());
    groupDatabaseTable = GroupDatabaseTable.create(coreModule().databaseConnection(),
      coreModule().databaseKeyspace());
    registerCommand(TeamCommand.create(log, coreModule().userDatabaseTable(),
      teamDatabaseTable));
    registerCommand(GroupCommand.create(log, groupDatabaseTable));
    registerCommand(PermissionCommand.create(log, teamDatabaseTable,
      groupDatabaseTable));
  }

  @Override
  public void disable() {

  }

  @Override
  public ModuleInformation moduleInformation() {
    return ModuleInformation.create("Team", "", "", ModuleInformation.Type.HIDDEN);
  }
}
