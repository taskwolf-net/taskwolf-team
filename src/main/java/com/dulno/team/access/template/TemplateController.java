package com.dulno.team.access.template;

import com.dulno.core.access.DulnoRequestBody;
import com.dulno.core.template.*;
import com.dulno.team.access.TeamController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.google.common.collect.Lists;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    var body = DulnoRequestBody.of(payload, response);
    memberHasPermission(findMemberId(request), "template.create")
      .thenAccept(hasPermission -> templateDatabaseTable.generateAvailableTemplateId()
        .thenAccept(id -> createTemplate(id, body.getString("englishName"),
          body.getString("englishDescription"), body.getString("germanName"),
          body.getString("germanDescription"), body.getObject("trigger"),
          body.getObjectList("actions"), body.getString("accessType"), hasPermission)));
  }

  private void createTemplate(
    UUID templateId, String englishName, String englishDescription,
    String germanName, String germanDescription, DulnoRequestBody triggerData,
    List<DulnoRequestBody> actionData, String accessType, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var actions = Lists.<TemplateAction>newArrayList();
    for (int i = 0; i < actionData.size(); i++) {
      var action = actionData.get(i);
      actions.add(TemplateAction.create(i, action.getString("module"),
        action.getString("type")));
    }
    var trigger = TemplateTrigger.create(triggerData.getString("module"),
      triggerData.getString("type"));
    templateDatabaseTable.insertTemplate(Template.create(templateId, trigger,
      actions, englishName, englishDescription, germanName, germanDescription,
      TemplateAccessType.valueOf(accessType)));
  }

  @RequestMapping(path = "/template/update/", method = RequestMethod.POST)
  public void updateTemplate(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
    var templateId = body.getUUID("template");
    memberHasPermission(findMemberId(request), "template.update")
      .thenAccept(hasPermission -> updateTemplate(templateId,
        body.getString("englishName"), body.getString("englishDescription"),
        body.getString("germanName"), body.getString("germanDescription"),
        body.getObject("trigger"), body.getObjectList("actions"),
        body.getString("accessType"), hasPermission));
  }

  private void updateTemplate(
    UUID templateId, String englishName, String englishDescription,
    String germanName, String germanDescription, DulnoRequestBody triggerData,
    List<DulnoRequestBody> actionData, String accessType, boolean hasPermission
  ) {
    if (!hasPermission) {
      return;
    }
    var actions = Lists.<TemplateAction>newArrayList();
    for (int i = 0; i < actionData.size(); i++) {
      var action = actionData.get(i);
      actions.add(TemplateAction.create(i, action.getString("module"),
        action.getString("type")));
    }
    var trigger = TemplateTrigger.create(triggerData.getString("module"),
      triggerData.getString("type"));
    templateDatabaseTable.updateTemplate(Template.create(templateId, trigger,
      actions, englishName, englishDescription, germanName, germanDescription,
      TemplateAccessType.valueOf(accessType)));
  }

  @RequestMapping(path = "/template/delete/", method = RequestMethod.POST)
  public void deleteTemplate(
    HttpServletRequest request, @RequestBody String payload,
    HttpServletResponse response
  ) {
    var body = DulnoRequestBody.of(payload, response);
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
