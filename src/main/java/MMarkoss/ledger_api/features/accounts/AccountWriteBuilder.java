package MMarkoss.ledger_api.features.accounts;


public class AccountWriteBuilder {
    private final Account acc;

    private AccountWriteBuilder(Account acc) {
        this.acc = acc;
    }

    public static AccountWriteBuilder create() {
        return new AccountWriteBuilder(Account.builder().build());
    }

    public static AccountWriteBuilder account(Account acc) {
        return new AccountWriteBuilder(acc);
    }

    public AccountWriteBuilder holderName(String holderName) {
        acc.setHolderName(holderName);
        return this;
    }

    public AccountWriteBuilder document(String document) {
        acc.setDocument(document);
        return this;
    }

    public Account build() {
        return acc;
    }
}
