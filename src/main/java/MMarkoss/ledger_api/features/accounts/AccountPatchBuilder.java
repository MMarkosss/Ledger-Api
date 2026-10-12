package MMarkoss.ledger_api.features.accounts;

import java.math.BigDecimal;
import java.util.Objects;


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

    public AccountPatchBuilder document(String document) {
        if (document != null) acc.setDocument(document);
        return this;
    }

    public AccountPatchBuilder balance(BigDecimal balance) {
        if (balance != null) acc.setBalance(balance);
        return this;
    }

    public Account build() {
        return acc;
    }
}

