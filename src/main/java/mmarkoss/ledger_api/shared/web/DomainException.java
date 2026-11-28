package mmarkoss.ledger_api.shared.web;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * Erro de domínio com o contrato HTTP já resolvido pela feature que o lançou.
 * O advice não faz dispatch por tipo: toda subclasse vira um ProblemDetail.
 */
public abstract class DomainException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final String code;
    private final boolean retryable;

    protected DomainException(HttpStatus status, String title, String message) {
        this(status, title, message, null, false);
    }

    protected DomainException(HttpStatus status,
                              String title,
                              String message,
                              String code,
                              boolean retryable) {
        super(message);
        this.status = Objects.requireNonNull(status, "status");
        this.title = Objects.requireNonNull(title, "title");
        this.code = code;
        this.retryable = retryable;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getCode() {
        return code;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
