package mmarkoss.ledger_api.shared.web;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex, HttpServletRequest request) {
        log.warn("traceId={} {} {} -> {} {}: {}",
                traceId(),
                request.getMethod(),
                request.getRequestURI(),
                ex.getStatus().value(),
                ex.getCode(),
                ex.getMessage());

        ProblemDetail pd = problem(ex.getStatus(), ex.getTitle(), ex.getMessage());
        if (ex.getCode() != null) {
            pd.setProperty("code", ex.getCode());
        }
        if (ex.isRetryable()) {
            pd.setProperty("retryable", true);
        }
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = problem(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                "Dados inválidos");
        Map<String, String> errors = ex.getFieldErrors().stream()
                .collect(toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Corpo da requisição inválido");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Header " + ex.getHeaderName() + " é obrigatório");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Requisição inválida",
                "Parâmetro inválido: " + ex.getName());
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleNoResource(Exception ex, HttpServletRequest request) {
        log.warn("traceId={} {} {} -> 404",
                traceId(),
                request.getMethod(),
                request.getRequestURI());

        return problem(
                HttpStatus.NOT_FOUND,
                "Recurso não encontrado",
                "Recurso não encontrado");
    }

    @ExceptionHandler({
            PessimisticLockingFailureException.class,
            PessimisticLockException.class,
            LockTimeoutException.class,
            OptimisticLockingFailureException.class
    })
    public ProblemDetail handleConcurrencyConflict(RuntimeException ex, HttpServletRequest request) {
        log.warn("traceId={} {} {} -> 409 CONCURRENCY_CONFLICT",
                traceId(),
                request.getMethod(),
                request.getRequestURI());

        ProblemDetail pd = problem(
                HttpStatus.CONFLICT,
                "Conflito de concorrência",
                "Conflito de concorrência; tente novamente");
        pd.setProperty("code", "CONCURRENCY_CONFLICT");
        pd.setProperty("retryable", true);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleFallback(Exception ex, HttpServletRequest request) {
        log.error("traceId={} {} {}",
                traceId(),
                request.getMethod(),
                request.getRequestURI(),
                ex);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Erro interno");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);

        String traceId = traceId();
        if (traceId != null) {
            pd.setProperty("traceId", traceId);
        }
        return pd;
    }

    private String traceId() {
        return MDC.get(TraceIdFilter.MDC_KEY);
    }
}
