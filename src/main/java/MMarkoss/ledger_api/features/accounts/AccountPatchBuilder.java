package MMarkoss.ledger_api.features.accounts;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

public class AccountPatchBuilder {
    private final Account acc;
    private final Set<String> updatedFields = new LinkedHashSet<>();

    private AccountPatchBuilder(Account acc) {
        this.acc = acc;
    }

    public static AccountPatchBuilder account(Account acc) {
        return new AccountPatchBuilder(acc);
    }

    public AccountPatchBuilder holderName(String holderName) {
        if (holderName != null && !Objects.equals(acc.getHolderName(), holderName)) {
            acc.setHolderName(holderName);
            updatedFields.add("holderName");
        }
        return this;
    }

    public AccountPatchBuilder document(String document) {
        if (document != null && !Objects.equals(acc.getDocument(), document)) {
            acc.setDocument(document);
            updatedFields.add("document");
        }
        return this;
    }

    public AccountPatchBuilder balance(BigDecimal balance) {
        if (balance != null && !Objects.equals(acc.getBalance(), balance)) {
            acc.setBalance(balance);
            updatedFields.add("balance");
        }
        return this;
    }

    public Set<String> getUpdatedFields() {
        return Collections.unmodifiableSet(updatedFields);
    }

    public Account build() {
        return acc;
    }
}

