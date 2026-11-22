// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static secop.idm1.adapters.expose.TestJwtConfig.bearer;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Contract;
import secop.idm1.domain.model.ContractType;
import secop.idm1.domain.model.Identity;
import secop.idm1.domain.model.MetadataField;
import secop.idm1.domain.model.MetadataType;
import secop.idm1.domain.service.ContractManager;
import secop.idm1.domain.service.IdentityManager;

@WebMvcTest(RestContractController.class)
@ContextConfiguration(classes = {RestContractController.class, DtoMapper.class, SecurityConfig.class,
        TestJwtConfig.class})
@Import(BusinessExceptionHandler.class)
class RestContractControllerTest {

    private static final String BODY =
            "{\"type\":\"contractor\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-12-31\","
                    + "\"metadata\":{\"company\":\"Acme\"}}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ContractManager contracts;

    @MockBean
    private IdentityManager identities;

    private static Contract contract() {
        Contract contract = new Contract();
        contract.setId(5L);
        contract.setEmployeeId("I000001");
        contract.setType("contractor");
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setMetadata(Map.of("company", "Acme"));
        return contract;
    }

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(get("/contract-types")).andExpect(status().isUnauthorized());
        mvc.perform(get("/contracts")).andExpect(status().isUnauthorized());
    }

    @Test
    void readerListsTypesAndContracts() throws Exception {
        when(contracts.getContractTypes()).thenReturn(List.of(new ContractType("contractor",
                List.of(new MetadataField("company", MetadataType.STRING, true)))));
        when(contracts.getContracts(null, "contractor")).thenReturn(List.of(contract()));
        when(contracts.getContracts("I000001", null)).thenReturn(List.of(contract()));
        when(identities.getIdentity("I000001")).thenReturn(new Identity());

        mvc.perform(get("/contract-types").header("Authorization", bearer("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("contractor"))
                .andExpect(jsonPath("$[0].metadata[0].required").value(true));
        mvc.perform(get("/contracts?type=contractor").header("Authorization", bearer("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identity").value("I000001"));
        mvc.perform(get("/identities/I000001/contracts").header("Authorization", bearer("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].metadata.company").value("Acme"));
    }

    @Test
    void readerCannotWrite() throws Exception {
        mvc.perform(post("/identities/I000001/contracts").header("Authorization", bearer("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/identities/I000001/contracts/5").header("Authorization", bearer("reader")))
                .andExpect(status().isForbidden());
    }

    @Test
    void publisherCreatesUpdatesAndDeletes() throws Exception {
        when(contracts.createContract(eq("I000001"), eq("contractor"), any(), any(), any())).thenReturn(contract());
        when(contracts.updateContract(eq("I000001"), eq(5L), eq("contractor"), any(), any(), any()))
                .thenReturn(contract());

        mvc.perform(post("/identities/I000001/contracts").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
        mvc.perform(put("/identities/I000001/contracts/5").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        mvc.perform(delete("/identities/I000001/contracts/5").header("Authorization", bearer("publisher")))
                .andExpect(status().isNoContent());

        verify(contracts).deleteContract("I000001", 5L);
    }

    @Test
    void businessErrorsAreMappedToHttpStatuses() throws Exception {
        when(contracts.createContract(any(), any(), any(), any(), any()))
                .thenThrow(new InvalidAccountException("bad"));
        when(identities.getIdentity("absent")).thenThrow(new NotFoundException("absent"));

        mvc.perform(post("/identities/I000001/contracts").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/identities/absent/contracts").header("Authorization", bearer("publisher")))
                .andExpect(status().isNotFound());
    }
}