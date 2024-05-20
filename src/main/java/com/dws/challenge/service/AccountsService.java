package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransactionDetail;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidAccountIDException;
import com.dws.challenge.exception.SameAccountIdException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  private final Map<String, Lock> accountLockMap = new ConcurrentHashMap<>();

  @Autowired
  @Qualifier("EmailNotification")
  public NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository) {
    this.accountsRepository = accountsRepository;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void transferAmount(TransactionDetail transactionDetail){
    String accountFromId = transactionDetail.getAccountFromId();
    String accountToId = transactionDetail.getAccountToId();
    BigDecimal transferAmount = transactionDetail.getTransferAmount();

    Lock fromAccountLock = accountLockMap.computeIfAbsent(accountFromId, k -> new ReentrantLock());
    Lock toAccountLock = accountLockMap.computeIfAbsent(accountToId, k -> new ReentrantLock());

    //Acquire the locks in the consistent order based on the comparison of the AccountId
    Lock firstLock, secondLock;
    if (accountFromId.compareTo(accountToId) < 0) {
      firstLock = fromAccountLock;
      secondLock = toAccountLock;
    } else {
      firstLock = toAccountLock;
      secondLock = fromAccountLock;
    }

    firstLock.lock();
    try {
      secondLock.lock();
      try {
        executeTransfer(accountFromId,accountToId,transferAmount);
      } finally {
        secondLock.unlock();
      }
    } finally {
      firstLock.unlock();
    }
  }

  private void executeTransfer(String accountFromId, String accountToId, BigDecimal transferAmount) throws InvalidAccountIDException, SameAccountIdException, InsufficientBalanceException{
    if (!(this.accountsRepository.isAccountIdPresent(accountFromId) && this.accountsRepository.isAccountIdPresent(accountToId))) {
      throw new InvalidAccountIDException("accountFromId " + accountFromId +
              " or accountToId " + accountToId + " does not exist!");
    }
    if (accountFromId.equals(accountToId)) {
      throw new SameAccountIdException("accountFromId " + accountFromId +
              " and accountToId " + accountToId + " are same!");
    }
    Account fromAccount = this.accountsRepository.getAccount(accountFromId);
    Account toAccount = this.accountsRepository.getAccount(accountToId);
    if (fromAccount.getBalance().compareTo(transferAmount) < 0) {
      throw new InsufficientBalanceException("Insufficient balance in accountFromId " + accountFromId);
    }
    fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount));
    toAccount.setBalance(toAccount.getBalance().add(transferAmount));

    //send notification to both the accounts with the transaction details message
    this.notificationService.notifyAboutTransfer(fromAccount, "Amount " + transferAmount + " transferred to " + accountToId);
    this.notificationService.notifyAboutTransfer(toAccount, "Amount " + transferAmount + " transferred from " + accountFromId);
  }

}
