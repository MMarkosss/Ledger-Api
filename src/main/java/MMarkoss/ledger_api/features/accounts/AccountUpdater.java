package MMarkoss.ledger_api.features.accounts;

public class AccountUpdater {
    private final Account acc;
    private String holderName;

    private AccountUpdater(Account acc) {
        this.acc = acc;
    }

    public static AccountUpdater account(Account acc) {
        return new AccountUpdater(acc);
    }

    public AccountUpdater holderName(String holderName) {
        if (holderName == null || holderName.isBlank())
            throw new IllegalArgumentException("Nome não pode ser vazio");
        this.holderName = holderName;
        return this;
    }

    public Account build() {
        if (holderName == null)
            throw new IllegalStateException("Nome não informado");
        acc.setHolderName(holderName);
        return acc;
    }
}
