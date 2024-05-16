package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.service.AccountsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBody() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
      .andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  //Unit test cases to test scenarios for transfer functionality
  @Test
  void transferAmount() throws Exception {
    Account fromAccount = new Account("Id-1", new BigDecimal("100.00"));
    Account toAccount = new Account("Id-2", new BigDecimal("350.00"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"Id-1\",\"accountToId\":\"Id-2\",\"transferAmount\":20}"))
            .andExpect(status().isOk());

    assertThat(fromAccount.getBalance()).isEqualByComparingTo("80");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("370");
  }

  @Test
  void transferAmountToInvalidAccountId() throws Exception {
    Account fromAccount = new Account("Id-3", new BigDecimal("800.00"));
    Account toAccount = new Account("Id-4", new BigDecimal("670.00"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"Id-3789\",\"accountToId\":\"Id-4\",\"transferAmount\":160}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferAmountToSameAccountId() throws Exception {
    Account fromAccount = new Account("Id-5", new BigDecimal("800.00"));
    Account toAccount = new Account("Id-6", new BigDecimal("670.00"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountFromId\":\"Id-5\",\"accountToId\":\"Id-5\",\"transferAmount\":160}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferAmountToEmptyAccountId() throws Exception {
    Account fromAccount = new Account("Id-7", new BigDecimal("198.23"));
    Account toAccount = new Account("Id-8", new BigDecimal("345.76"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountFromId\":\"Id-7\",\"accountToId\":\"\",\"transferAmount\":49}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferNegativeAmount() throws Exception {
    Account fromAccount = new Account("Id-9", new BigDecimal("300.00"));
    Account toAccount = new Account("Id-10", new BigDecimal("7656.23"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountFromId\":\"Id-9\",\"accountToId\":\"Id-10\",\"transferAmount\":-50}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferZeroAmount() throws Exception {
    Account fromAccount = new Account("Id-11", new BigDecimal("300.00"));
    Account toAccount = new Account("Id-12", new BigDecimal("2590.34"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountFromId\":\"Id-11\",\"accountToId\":\"Id-12\",\"transferAmount\":0}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferAmountWithInsufficientBalance() throws Exception {
    Account fromAccount = new Account("Id-13", new BigDecimal("200.00"));
    Account toAccount = new Account("Id-14", new BigDecimal("2345.34"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountFromId\":\"Id-13\",\"accountToId\":\"Id-14\",\"transferAmount\":201.00}"))
            .andExpect(status().isBadRequest());
  }

  @Test
  void transferAmountSameAsBalance() throws Exception {
    Account fromAccount = new Account("Id-15", new BigDecimal("700.00"));
    Account toAccount = new Account("Id-16", new BigDecimal("100.34"));
    this.accountsService.createAccount(fromAccount);
    this.accountsService.createAccount(toAccount);

    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"accountFromId\":\"Id-15\",\"accountToId\":\"Id-16\",\"transferAmount\":700.00}"))
            .andExpect(status().isOk());
    assertThat(fromAccount.getBalance()).isEqualByComparingTo("0");
    assertThat(toAccount.getBalance()).isEqualByComparingTo("800.34");
  }

}
