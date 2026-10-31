package MMarkoss.ledger_api.shared.web;

import MMarkoss.ledger_api.features.accounts.AccountHasLedgerEntriesException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

@RestControllerAdvice
public class ApiExceptionHandler {

    // ProblemDetail: dto personalizado do spring para n fazer na mão


    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleNotFound(EntityNotFoundException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Conta não encontrada");
        return pd;
    }

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


    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleConflict(DataIntegrityViolationException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Documento já cadastrado");
        pd.setTitle("Conflito");
        return pd;
    }

    @ExceptionHandler(AccountHasLedgerEntriesException.class)
    public ProblemDetail handleAccountWithEntries(AccountHasLedgerEntriesException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conta com lançamentos");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleFallback(Exception ex) {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno");
    }
}
