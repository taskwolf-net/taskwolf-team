package com.dulno.team.access;

import com.dulno.core.error.ErrorRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TeamExceptionStore {
  private final ErrorRepository errorRepository;

  @ExceptionHandler(Exception.class)
  public String processError(Exception exception, Model model) {
    errorRepository.processError(exception);
    return "error";
  }
}