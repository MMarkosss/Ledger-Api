package mmarkoss.ledger_api.features.deposits;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import mmarkoss.ledger_api.features.deposits.dto.DepositReq;
import mmarkoss.ledger_api.features.deposits.dto.DepositRes;
import mmarkoss.ledger_api.shared.web.InvalidIdempotencyKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/accounts/{accountId}/deposits")
public class DepositController {

    private final DepositService depositService;

    @PostMapping
    public ResponseEntity<DepositRes> deposit(
            @PathVariable UUID accountId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid DepositReq dto) {

        if (idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException();
        }

        DepositResult result = depositService.deposit(accountId, idempotencyKey, dto.amount());
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status)
                .body(new DepositRes(result.transactionId(), result.balance()));
    }
}
