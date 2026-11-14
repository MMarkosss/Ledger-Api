package mmarkoss.ledger_api.features.ledger;

public enum EntryDirection {
    CREDIT("C"),
    DEBIT("D");

    private final String code;

    EntryDirection(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    public static EntryDirection fromCode(String code) {
        for (EntryDirection direction : values()) {
            if (direction.code.equals(code)) {
                return direction;
            }
        }
        throw new IllegalArgumentException("Direção de lançamento inválida: " + code);
    }
}
