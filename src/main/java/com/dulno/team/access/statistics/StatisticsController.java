package com.dulno.team.access.statistics;

import com.google.common.collect.Maps;
import jakarta.servlet.http.HttpServletRequest;
import com.dulno.core.ticket.TicketDatabaseTable;
import com.dulno.team.access.TeamController;
import com.dulno.team.access.question.QuestionInformationController;
import com.dulno.team.group.GroupDatabaseTable;
import com.dulno.team.member.TeamDatabaseTable;
import com.dulno.team.member.TeamMember;
import com.dulno.team.member.TeamWorkDatabaseTable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.security.Key;
import java.text.SimpleDateFormat;
import java.time.Year;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
public final class StatisticsController extends TeamController {
  private final TicketDatabaseTable ticketDatabaseTable;
  private final QuestionInformationController teamEmailInformationController;
  private final TeamDatabaseTable teamDatabaseTable;
  private final TeamWorkDatabaseTable teamWorkDatabaseTable;

  private StatisticsController(
    Key secretKey, TeamDatabaseTable teamDatabaseTable,
    GroupDatabaseTable groupDatabaseTable, TicketDatabaseTable ticketDatabaseTable,
    QuestionInformationController teamEmailInformationController,
    TeamWorkDatabaseTable teamWorkDatabaseTable
  ) {
    super(secretKey, teamDatabaseTable, groupDatabaseTable);
    this.ticketDatabaseTable = ticketDatabaseTable;
    this.teamEmailInformationController = teamEmailInformationController;
    this.teamDatabaseTable = teamDatabaseTable;
    this.teamWorkDatabaseTable = teamWorkDatabaseTable;
  }

  private static final int TEAM_SIZE_GOAL = 20;

  @RequestMapping(path = "/statistics/", method = RequestMethod.GET)
  public CompletableFuture<Map<String, Object>> findStatistics(
    HttpServletRequest request
  ) {
    var futureResponse = new CompletableFuture<Map<String, Object>>();
    findMember(request).thenAccept(user -> findOpenTickets().thenAccept(openTickets ->
      findOpenEmails().thenAccept(openEmails -> findTeamSize().thenAccept(teamSize ->
        findPersonalProcessedRequests(user).thenAccept(processedRequests ->
          futureResponse.complete(assemblyStatistics(openTickets, openEmails,
            openTickets + openEmails, teamSize, TEAM_SIZE_GOAL, processedRequests)))))));
    return futureResponse;
  }

  private CompletableFuture<Integer> findOpenTickets() {
    var futureResponse = new CompletableFuture<Integer>();
    ticketDatabaseTable.findAllTickets().thenApply(tickets -> tickets.stream()
      .filter(ticket -> ticket.expirationTime() == -1).toList()).thenAccept(
        tickets -> futureResponse.complete(tickets.size()));
    return futureResponse;
  }

  private CompletableFuture<Integer> findOpenEmails() {
    var futureResponse = new CompletableFuture<Integer>();
    teamEmailInformationController.findOpenQuestions().thenAccept(
      emails -> futureResponse.complete(emails.size()));
    return futureResponse;
  }

  private CompletableFuture<Integer> findTeamSize() {
    var futureResponse = new CompletableFuture<Integer>();
    teamDatabaseTable.findAllTeamMembers().thenAccept(team ->
      futureResponse.complete(team.size()));
    return futureResponse;
  }

  private CompletableFuture<Map<Integer, Integer>> findPersonalProcessedRequests(
    TeamMember member
  ) {
    var futureResponse = new CompletableFuture<Map<Integer, Integer>>();
    teamWorkDatabaseTable.hasRequestReplies(member.id())
      .thenAccept(exists -> findPersonalProcessedRequests(member, exists)
        .thenAccept(futureResponse::complete));
    return futureResponse;
  }

  private CompletableFuture<Map<Integer, Integer>> findPersonalProcessedRequests(
    TeamMember member, boolean hasRequestReplies
  ) {
    if (!hasRequestReplies) {
      return CompletableFuture.completedFuture(Maps.newHashMap());
    }
    var futureResponse = new CompletableFuture<Map<Integer, Integer>>();
    teamWorkDatabaseTable.findRequestReplies(member.id()).thenAccept(replies ->
      futureResponse.complete(classifyDatesInMonthlyOccurrence(replies.stream()
        .map(this::formatTime).toList(), false)));
    return futureResponse;
  }

  private Map<Integer, Integer> classifyDatesInMonthlyOccurrence(
    List<String> dates, boolean totalValues
  ) {
    var currentYear = String.valueOf(Year.now().getValue());
    var calendar = Calendar.getInstance();
    calendar.setTime(new Date());
    int currentMonth = calendar.get(Calendar.MONTH) + 1;
    var monthlyNumber = Maps.<Integer, Integer>newHashMapWithExpectedSize(currentMonth);
    for (var i = 0; i < currentMonth; i++) {
      monthlyNumber.put(i, 0);
    }
    var previousDates = 0;
    for (var date : dates) {
      if (!date.contains(currentYear)) {
        previousDates++;
        continue;
      }
      var entryMonth = Integer.valueOf(date.split("\\.")[1]) - 1;
      monthlyNumber.put(entryMonth, monthlyNumber.get(entryMonth) + 1);
    }
    return totalValues ? calculateTotalOccurrenceValues(monthlyNumber, previousDates) :
      monthlyNumber;
  }

  private Map<Integer, Integer> calculateTotalOccurrenceValues(
    Map<Integer, Integer> monthlyNumber, int previousDates
  ) {
    for (var i = 1; i < monthlyNumber.size(); i++) {
      monthlyNumber.put(i, monthlyNumber.get(i) + monthlyNumber.get(i - 1));
    }
    for (var i = 1; i < monthlyNumber.size(); i++) {
      monthlyNumber.put(i, monthlyNumber.get(i) + previousDates);
    }
    return monthlyNumber;
  }

  private Map<String, Object> assemblyStatistics(
    int openTickets, int openEmails, int totalRequests, int teamSize,
    int teamSizeGoal, Map<Integer, Integer> personalProcessedRequests
  ) {
    var information = Maps.<String, Object>newHashMap();
    information.put("openTickets", openTickets);
    information.put("openEmails", openEmails);
    information.put("totalRequests", totalRequests);
    information.put("teamSize", teamSize);
    information.put("teamSizeGoal", teamSizeGoal);
    information.put("personalProcessedRequests", personalProcessedRequests);
    return information;
  }

  private String formatTime(long time) {
    var calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(calendar.getTime());
  }
}

