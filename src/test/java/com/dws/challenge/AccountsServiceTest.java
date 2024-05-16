package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.concurrent.*;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransactionDetail;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidAccountIDException;
import com.dws.challenge.exception.SameAccountIdException;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }
  }

  //Unit test cases to test scenarios for transfer functionality
  @Test
  void transferAmount(){
    Account fromAccount = new Account("Id-51", new BigDecimal("700.00"));
    Account toAccount = new Account("Id-52", new BigDecimal("100.34"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    TransactionDetail transactionDetail = new TransactionDetail("Id-51","Id-52",new BigDecimal("435"));
    this.accountsService.transferAmount(transactionDetail);

    assertThat(fromAccount.getBalance()).isEqualByComparingTo("265");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("535.34");
  }

  @Test
  void transferAmount_invalidAccountId(){
    Account fromAccount = new Account("Id-53", new BigDecimal("32450.00"));
    Account toAccount = new Account("Id-54", new BigDecimal("23434.34"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    TransactionDetail transactionDetail = new TransactionDetail("Id-3345","Id-54",new BigDecimal("2345"));
    try {
      this.accountsService.transferAmount(transactionDetail);
      fail("AccountFromId or AccountToId doest not exist.");
    } catch (InvalidAccountIDException ex) {
      assertThat(ex.getMessage()).isEqualTo("accountFromId Id-3345 or accountToId Id-54 does not exist!");
    }
  }

  @Test
  void transferAmount_sameAccountId(){
    Account fromAccount = new Account("Id-56", new BigDecimal("3435.00"));
    Account toAccount = new Account("Id-57", new BigDecimal("2454.34"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    TransactionDetail transactionDetail = new TransactionDetail("Id-56","Id-56",new BigDecimal("1233"));
    try {
      this.accountsService.transferAmount(transactionDetail);
      fail("Both the accounts are same.");
    } catch (SameAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("accountFromId Id-56 and accountToId Id-56 are same!");
    }
  }

  @Test
  void transferAmount_insufficientBalance(){
    Account fromAccount = new Account("Id-58", new BigDecimal("754.00"));
    Account toAccount = new Account("Id-59", new BigDecimal("3467.34"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    TransactionDetail transactionDetail = new TransactionDetail("Id-58","Id-59",new BigDecimal("1233"));
    try {
      this.accountsService.transferAmount(transactionDetail);
      fail("Insufficient balance.");
    } catch (InsufficientBalanceException ex) {
      assertThat(ex.getMessage()).isEqualTo("Insufficient balance in accountFromId Id-58");
    }
  }

  @Test
  void transferAmount_concurrency() throws InterruptedException {
    Account fromAccount = new Account("Id-61", new BigDecimal("1000.00"));
    Account toAccount = new Account("Id-62", new BigDecimal("400.00"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);
    int numberOfThreads = 5;
    ExecutorService es = Executors.newFixedThreadPool(numberOfThreads);
    Runnable task = ()->{
      TransactionDetail transactionDetail = new TransactionDetail("Id-61","Id-62",new BigDecimal("100"));
      this.accountsService.transferAmount(transactionDetail);
    };
    for(int i=1;i<=numberOfThreads;i++){
      es.submit(task);
    }
    es.shutdown();
    es.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

    assertThat(fromAccount.getBalance()).isEqualByComparingTo("500");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("900");
  }

}
