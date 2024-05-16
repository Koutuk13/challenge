package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransactionDetail;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.exception.InvalidAccountIDException;
import com.dws.challenge.exception.SameAccountIdException;
import com.dws.challenge.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    public NotificationService notificationService;

    @Autowired
    public AccountsRepositoryInMemory(@Qualifier("EmailNotification") NotificationService notificationService){
        this.notificationService = notificationService;
    }

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }

    public void transferAmount(TransactionDetail transactionDetail) throws InvalidAccountIDException, SameAccountIdException, InsufficientBalanceException{
        String accountFromId = transactionDetail.getAccountFromId();
        String accountToId = transactionDetail.getAccountToId();
        BigDecimal transferAmount = transactionDetail.getTransferAmount();
        synchronized (this) {
            if (!(accounts.containsKey(accountFromId) && accounts.containsKey(accountToId))) {
                throw new InvalidAccountIDException("accountFromId " + accountFromId +
                        " or accountToId " + accountToId + " does not exist!");
            }
            if (accountFromId.equals(accountToId)) {
                throw new SameAccountIdException("accountFromId " + accountFromId +
                        " and accountToId " + accountToId + " are same!");
            }
            Account fromAccount = getAccount(accountFromId);
            Account toAccount = getAccount(accountToId);
            if (fromAccount.getBalance().compareTo(transferAmount) < 0) {
                throw new InsufficientBalanceException("Insufficient balance in accountFromId " + accountFromId);
            }
            fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmount));
            toAccount.setBalance(toAccount.getBalance().add(transferAmount));

            //send notification to both the accounts with the transaction details message
            notificationService.notifyAboutTransfer(fromAccount, "Amount " + transferAmount + " transferred to " + accountToId);
            notificationService.notifyAboutTransfer(toAccount, "Amount " + transferAmount + " transferred from " + accountFromId);
        }
    }

}
