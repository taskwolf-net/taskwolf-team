package net.taskwolf.team.access.template;

import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.taskwolf.core.access.TaskwolfRequestBody;
import net.taskwolf.core.template.*;
import net.taskwolf.team.access.TeamController;
import net.taskwolf.team.group.GroupDatabaseTable;
import net.taskwolf.team.member.TeamDatabaseTable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.util.List;
import java.util.UUID;

@RestController
public final class TemplateController extends TeamController {
  private final TemplateDatabaseTable templateDatabaseTable;

  private TemplateController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable,
    TemplateDatabaseTable templateDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.templateDatabaseTable = templateDatabaseTable;
  }

  @RequestMapping(path = "/template/create/", method = RequestMethod.POST)
  public void createTemplate(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "template.create")
      .thenAccept(hasPermission -> templateDatabaseTable.generateAvailableTemplateId()
        .thenAccept(id -> createTemplate(id, body.getObject("names"),
          body.getObject("descriptions"), body.getObject("trigger"),
          body.getObjectList("actions"), body.getString("accessType"), hasPermission)));
  }

  private void createTemplate(
    UUID templateId, TaskwolfRequestBody nameData,
    TaskwolfRequestBody descriptionData, TaskwolfRequestBody triggerData,
    List<TaskwolfRequestBody> actionData, String accessType, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var names = Lists.newArrayList(nameData.getString("english"),
      nameData.getString("german"));
    var descriptions = Lists.newArrayList(descriptionData.getString("english"),
      descriptionData.getString("german"));
    var actions = Lists.<TemplateAction>newArrayList();
    for (int i = 0; i < actionData.size(); i++) {
      var action = actionData.get(i);
      actions.add(TemplateAction.create(i, action.getString("module"),
        action.getString("type")));
    }
    var trigger = TemplateTrigger.create(triggerData.getString("module"),
      triggerData.getString("type"));
    templateDatabaseTable.insertTemplate(Template.create(templateId, trigger,
      actions, names, descriptions, TemplateAccessType.valueOf(accessType)));
  }

  @RequestMapping(path = "/template/update/", method = RequestMethod.POST)
  public void updateTemplate(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var templateId = body.getUUID("template");
    memberHasPermission(findMemberId(request), "template.update")
      .thenAccept(hasPermission -> updateTemplate(templateId,
        body.getObject("names"), body.getObject("descriptions"),
        body.getObject("trigger"), body.getObjectList("actions"),
        body.getString("accessType"), hasPermission));
  }

  private void updateTemplate(
    UUID templateId, TaskwolfRequestBody nameData,
    TaskwolfRequestBody descriptionData, TaskwolfRequestBody triggerData,
    List<TaskwolfRequestBody> actionData, String accessType, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var names = Lists.newArrayList(nameData.getString("english"),
      nameData.getString("german"));
    var descriptions = Lists.newArrayList(descriptionData.getString("english"),
      descriptionData.getString("german"));
    var actions = Lists.<TemplateAction>newArrayList();
    for (int i = 0; i < actionData.size(); i++) {
      var action = actionData.get(i);
      actions.add(TemplateAction.create(i, action.getString("module"),
        action.getString("type")));
    }
    var trigger = TemplateTrigger.create(triggerData.getString("module"),
      triggerData.getString("type"));
    templateDatabaseTable.updateTemplate(Template.create(templateId, trigger,
      actions, names, descriptions, TemplateAccessType.valueOf(accessType)));
  }

  @RequestMapping(path = "/template/delete/", method = RequestMethod.POST)
  public void deleteTemplate(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = TaskwolfRequestBody.of(payload, response);
    var templateId = body.getUUID("template");
    memberHasPermission(findMemberId(request), "template.delete")
      .thenAccept(hasPermission -> templateDatabaseTable.templateExists(templateId)
        .thenAccept(exists -> deleteTemplate(templateId, hasPermission, exists)));
  }

  private void deleteTemplate(UUID templateId, boolean hasPermission, boolean exists) {
    if (!hasPermission || !exists) {
      return;
    }
    templateDatabaseTable.deleteTemplate(templateId);
  }
}
