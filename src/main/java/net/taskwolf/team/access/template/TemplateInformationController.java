package net.taskwolf.team.access.template;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import net.taskwolf.core.CoreModule;
import net.taskwolf.core.action.ActionInformation;
import net.taskwolf.core.module.ModuleInformation;
import net.taskwolf.core.template.Template;
import net.taskwolf.core.template.TemplateDatabaseTable;
import net.taskwolf.core.trigger.TriggerInformation;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import net.taskwolf.team.member.TeamMember;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
public final class TemplateInformationController extends TeamController {
  private final TemplateDatabaseTable templateDatabaseTable;
  private final CoreModule coreModule;

  private TemplateInformationController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    TemplateDatabaseTable templateDatabaseTable, CoreModule coreModule
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.templateDatabaseTable = templateDatabaseTable;
    this.coreModule = coreModule;
  }

  @RequestMapping(path = "/templates/all/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findAllTemplates(
    HttpServletRequest request
  ) {
    return findMember(request)
      .thenCompose(member -> memberHasPermission(member.id(), "templates.find")
        .thenCompose(hasPermission -> findAllTemplates(member, hasPermission)));
  }

  private CompletableFuture<Map<String, Object>> findAllTemplates(
    TeamMember member, boolean hasPermission
  ) {
    if (!hasPermission) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    return templateDatabaseTable.findAllTemplates()
      .thenApply(templates -> Map.of("templates", templates.stream()
        .map(template -> assemblyTemplateInformation(member, template))
        .toList()));
  }

  private Map<String, Object> assemblyTemplateInformation(
    TeamMember member, Template template
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("name", template.translateName(member.language()));
    information.put("description", template.translateDescription(member.language()));
    information.put("accessType", template.accessType());
    information.put("modules", template.modules());
    var triggerInformation = Maps.<String, Object>newHashMap();
    var triggerModule = template.trigger().module();
    triggerInformation.put("module", triggerModule);
    triggerInformation.put("moduleLogo", coreModule.findModuleInformation(triggerModule)
      .map(ModuleInformation::logo).orElse(""));
    var triggerType = template.trigger().type();
    triggerInformation.put("type", triggerType);
    triggerInformation.put("typeDescription", coreModule.translate(member.language(),
      coreModule.findTriggerInformation(triggerModule, triggerType)
        .map(TriggerInformation::description).orElse("")));
    information.put("trigger", triggerInformation);
    var actionsInformation = Lists.<Map<String, Object>>newArrayList();
    for (var action : template.actions()) {
      var actionInformation = Maps.<String, Object>newHashMap();
      var actionModule = action.module();
      actionInformation.put("actionIndex", action.actionIndex());
      actionInformation.put("module", action.module());
      actionInformation.put("moduleLogo", coreModule.findModuleInformation(actionModule)
        .map(ModuleInformation::logo).orElse(""));
      var actionType = action.type();
      actionInformation.put("type", action.type());
      actionInformation.put("typeDescription", coreModule.translate(member.language(),
        coreModule.findActionInformation(actionModule, actionType)
          .map(ActionInformation::description).orElse("")));
      actionsInformation.add(actionInformation);
    }
    information.put("actions", actionsInformation);
    return information;
  }
}
