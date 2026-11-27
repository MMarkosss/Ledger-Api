package mmarkoss.ledger_api.shared.web;

import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PessimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import mmarkoss.ledger_api.features.accounts.AccountNotFoundException;
import mmarkoss.ledger_api.features.accounts.DuplicateDocumentException;
import mmarkoss.ledger_api.features.accounts.InsufficientBalanceException;
import mmarkoss.ledger_api.features.accounts.InvalidAmountException;
import mmarkoss.ledger_api.features.accounts.InvalidHolderNameException;
import mmarkoss.ledger_api.features.ledger.AccountHasLedgerEntriesException;
import mmarkoss.ledger_api.features.ledger.IdempotencyConflictException;
import mmarkoss.ledger_api.features.statements.InvalidStatementQueryException;
import mmarkoss.ledger_api.features.transfers.SameAccountTransferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    // ProblemDetail: dto personalizado do spring para n fazer na mão


    // Exceptions do validation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados inválidos");
        pd.setTitle("Dados inválidos");
        Map<String,String> errors = ex.getFieldErrors().stream()
                .collect(toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b)->a));
        pd.setProperty("errors", errors);
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadable(HttpMessageNotReadableException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Corpo da requisição inválido");
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Header " + ex.getHeaderName() + " é obrigatório");
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ProblemDetail handleInvalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(InvalidStatementQueryException.class)
    public ProblemDetail handleInvalidStatementQuery(InvalidStatementQueryException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Consulta inválida");
        return pd;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Parâmetro inválido: " + ex.getName());
        pd.setTitle("Requisição inválida");
        return pd;
    }

    @ExceptionHandler(InvalidAmountException.class)
    public ProblemDetail handleInvalidAmount(InvalidAmountException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        pd.setTitle("Valor inválido");
        return pd;
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ProblemDetail handleInsufficientBalance(InsufficientBalanceException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        pd.setTitle("Saldo insuficiente");
        return pd;
    }

    @ExceptionHandler(InvalidHolderNameException.class)
    public ProblemDetail handleInvalidHolderName(InvalidHolderNameException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        pd.setTitle("Nome inválido");
        return pd;
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ProblemDetail handleNotFound(AccountNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Conta não encontrada");
        return pd;
    }

    @ExceptionHandler(SameAccountTransferException.class)
    public ProblemDetail handleSameAccountTransfer(SameAccountTransferException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        pd.setTitle("Transferência inválida");
        return pd;
    }

    @ExceptionHandler(DuplicateDocumentException.class)
    public ProblemDetail handleDuplicateDocument(DuplicateDocumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflito");
        return pd;
    }

    @ExceptionHandler(AccountHasLedgerEntriesException.class)
    public ProblemDetail handleAccountWithEntries(AccountHasLedgerEntriesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conta com lançamentos");
        return pd;
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ProblemDetail handleIdempotencyConflict(IdempotencyConflictException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflito de idempotência");
        pd.setProperty("code", "IDEMPOTENCY_CONFLICT");
        return pd;
    }

    @ExceptionHandler({
            PessimisticLockingFailureException.class,
            PessimisticLockException.class,
            LockTimeoutException.class
    })
    public ProblemDetail handleConcurrencyConflict(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT, "Conflito de concorrência; tente novamente");
        pd.setTitle("Conflito de concorrência");
        pd.setProperty("code", "CONCURRENCY_CONFLICT");
        pd.setProperty("retryable", true);
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleFallback(Exception ex, HttpServletRequest request) {
        log.error("{} {}", request.getMethod(), request.getRequestURI(), ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
    }
}
