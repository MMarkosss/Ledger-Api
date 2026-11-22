package mmarkoss.ledger_api.features.transfers;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import mmarkoss.ledger_api.features.transfers.dto.TransferReq;
import mmarkoss.ledger_api.features.transfers.dto.TransferRes;
import mmarkoss.ledger_api.shared.web.InvalidIdempotencyKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@RequestMapping("/transfers")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferRes> transfer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid TransferReq dto) {

        if (idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException();
        }

        TransferResult result = transferService.transfer(
                dto.sourceAccountId(),
                dto.destinationAccountId(),
                idempotencyKey,
                dto.amount());
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;

        return ResponseEntity.status(status)
                .body(new TransferRes(
                        result.transactionId(),
                        result.sourceBalance(),
                        result.destinationBalance()));
    }
}
