package mmarkoss.ledger_api.shared.web;

import mmarkoss.ledger_api.features.accounts.Account;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new ApiExceptionHandler())
            .addFilters(new TraceIdFilter())
            .build();

    @Test
    @DisplayName("Conflito otimista fora dos retries vira 409 retryable (D23)")
    void deveMapearConflitoOtimistaPara409Retryable() throws Exception {
        mockMvc.perform(get("/test/optimistic-conflict")
                        .header(TraceIdFilter.HEADER, "trace-d23"))
                .andExpect(status().isConflict())
                .andExpect(header().string(TraceIdFilter.HEADER, "trace-d23"))
                .andExpect(jsonPath("$.title").value("Conflito de concorrência"))
                .andExpect(jsonPath("$.code").value("CONCURRENCY_CONFLICT"))
                .andExpect(jsonPath("$.retryable").value(true))
                .andExpect(jsonPath("$.traceId").value("trace-d23"));
    }

    @Test
    @DisplayName("Fallback retorna mensagem estática com traceId")
    void deveRetornarFallbackSemVazarMensagem() throws Exception {
        mockMvc.perform(get("/test/boom")
                        .header(TraceIdFilter.HEADER, "trace-500"))
                .andExpect(status().isInternalServerError())
                .andExpect(header().string(TraceIdFilter.HEADER, "trace-500"))
                .andExpect(jsonPath("$.title").value("Erro interno"))
                .andExpect(jsonPath("$.detail").value("Erro interno"))
                .andExpect(jsonPath("$.traceId").value("trace-500"))
                .andExpect(jsonPath("$.message").doesNotExist());
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/optimistic-conflict")
        void optimisticConflict() {
            throw new ObjectOptimisticLockingFailureException(Account.class, UUID.randomUUID());
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new RuntimeException("segredo");
        }
    }
}
