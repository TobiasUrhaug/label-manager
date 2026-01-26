package org.omt.labelmanager.finance.cost.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.omt.labelmanager.finance.shared.Money;
import org.omt.labelmanager.finance.cost.CostOwner;
import org.omt.labelmanager.finance.cost.CostType;
import org.omt.labelmanager.finance.cost.RegisterCostUseCase;
import org.omt.labelmanager.finance.cost.VatAmount;
import org.omt.labelmanager.test.TestSecurityConfig;
import org.omt.labelmanager.identity.user.AppUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CostController.class)
@Import(TestSecurityConfig.class)
class CostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterCostUseCase registerCostUseCase;

    private final AppUserDetails testUser =
            new AppUserDetails(1L, "test@example.com", "password", "Test User");

    @Test
    void registerCostForRelease_callsUseCaseAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels/1/releases/42/costs")
                        .with(user(testUser))
                        .with(csrf())
                        .param("netAmount", "100.00")
                        .param("vatAmount", "25.00")
                        .param("vatRate", "0.25")
                        .param("grossAmount", "125.00")
                        .param("costType", "MASTERING")
                        .param("incurredOn", "2024-06-15")
                        .param("description", "Mastering for album")
                        .param("documentReference", "INV-2024-001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/1/releases/42"));

        verify(registerCostUseCase).registerCost(
                eq(Money.of(new BigDecimal("100.00"))),
                eq(new VatAmount(Money.of(new BigDecimal("25.00")), new BigDecimal("0.25"))),
                eq(Money.of(new BigDecimal("125.00"))),
                eq(CostType.MASTERING),
                eq(LocalDate.of(2024, 6, 15)),
                eq("Mastering for album"),
                eq(CostOwner.release(42L)),
                eq("INV-2024-001")
        );
    }

    @Test
    void registerCostForLabel_callsUseCaseAndRedirects() throws Exception {
        mockMvc
                .perform(post("/labels/10/costs")
                        .with(user(testUser))
                        .with(csrf())
                        .param("netAmount", "50.00")
                        .param("vatAmount", "12.50")
                        .param("vatRate", "0.25")
                        .param("grossAmount", "62.50")
                        .param("costType", "HOSTING")
                        .param("incurredOn", "2024-07-01")
                        .param("description", "Website hosting"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/labels/10"));

        verify(registerCostUseCase).registerCost(
                eq(Money.of(new BigDecimal("50.00"))),
                eq(new VatAmount(Money.of(new BigDecimal("12.50")), new BigDecimal("0.25"))),
                eq(Money.of(new BigDecimal("62.50"))),
                eq(CostType.HOSTING),
                eq(LocalDate.of(2024, 7, 1)),
                eq("Website hosting"),
                eq(CostOwner.label(10L)),
                eq(null)
        );
    }
}
