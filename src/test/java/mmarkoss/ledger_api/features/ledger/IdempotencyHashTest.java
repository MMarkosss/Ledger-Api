package mmarkoss.ledger_api.features.ledger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyHashTest {

    @Test
    @DisplayName("Canonicaliza valor removendo zeros à direita e notação científica")
    void deveCanonicalizarValor() {
        assertThat(IdempotencyHash.canonicalAmount(new BigDecimal("100.00"))).isEqualTo("100");
        assertThat(IdempotencyHash.canonicalAmount(new BigDecimal("100.0"))).isEqualTo("100");
        assertThat(IdempotencyHash.canonicalAmount(new BigDecimal("10.50"))).isEqualTo("10.5");
        assertThat(IdempotencyHash.canonicalAmount(new BigDecimal("0.00"))).isEqualTo("0");
        assertThat(IdempotencyHash.canonicalAmount(new BigDecimal("1E+3"))).isEqualTo("1000");
    }

    @Test
    @DisplayName("Mesmo valor normalizado gera o mesmo hash")
    void deveGerarMesmoHashParaValorNormalizado() {
        String first = IdempotencyHash.sha256(
                "acc|" + IdempotencyHash.canonicalAmount(new BigDecimal("100.00")));
        String replay = IdempotencyHash.sha256(
                "acc|" + IdempotencyHash.canonicalAmount(new BigDecimal("100.0")));

        assertThat(replay).isEqualTo(first);
    }
}
