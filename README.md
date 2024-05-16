I would like below few points to consider before the project is turned production ready:
• In the class AccountsRepositoryInMemory, for the method getAccount(), we can add check to see if the accountID is present and give corresponding response back instead of giving 200 response.
• We can use @Qualifier with AccountsRepository in the constructor of AccountsService to avoid ambiguity if there are multiple implementation of AccountsRepository in future.
