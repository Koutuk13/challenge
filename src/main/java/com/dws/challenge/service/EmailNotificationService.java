package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component("EmailNotification")
@Slf4j
public class EmailNotificationService implements NotificationService {

  @Override
  public void notifyAboutTransfer(Account account, String transferDescription) {
    //THIS METHOD SHOULD NOT BE CHANGED - ASSUME YOUR COLLEAGUE WILL IMPLEMENT IT
    log
      .info("Sending notification to owner of {}: {}", account.getAccountId(), transferDescription);
  }

}
