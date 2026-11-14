package mmarkoss.ledger_api.features.ledger;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query(value = """
            INSERT INTO transaction
                (id, type, amount, idempotency_key, idempotency_payload_hash, status)
            VALUES
                (:id, :type, :amount, :idempotencyKey, :idempotencyPayloadHash, :status)
            ON CONFLICT (idempotency_key) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("type") String type,
                       @Param("amount") BigDecimal amount,
                       @Param("idempotencyKey") String idempotencyKey,
                       @Param("idempotencyPayloadHash") String idempotencyPayloadHash,
                       @Param("status") String status);
}
