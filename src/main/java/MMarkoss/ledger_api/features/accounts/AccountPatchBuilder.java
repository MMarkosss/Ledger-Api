package MMarkoss.ledger_api.features.accounts;




public class AccountPatchBuilder {
    private final Account acc;

    private AccountPatchBuilder(Account acc) {
        this.acc = acc;
    }

    public static AccountPatchBuilder account(Account acc) {
        return new AccountPatchBuilder(acc);
    }

    public AccountPatchBuilder holderName(String holderName) {
        if (holderName != null) acc.setHolderName(holderName);
        return this;
    }

    public Account build() {
        return acc;
    }
}

