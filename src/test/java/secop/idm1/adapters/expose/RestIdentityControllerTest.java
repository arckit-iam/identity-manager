// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.expose;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static secop.idm1.adapters.expose.TestJwtConfig.bearer;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import secop.idm1.domain.exception.IdentityConflictException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Account;
import secop.idm1.domain.model.AccountSource;
import secop.idm1.domain.model.Identity;
import secop.idm1.domain.service.AccountManager;
import secop.idm1.domain.service.IdentityManager;

@WebMvcTest(RestIdentityController.class)
@ContextConfiguration(classes = {RestIdentityController.class, DtoMapper.class, SecurityConfig.class,
        TestJwtConfig.class})
@Import(BusinessExceptionHandler.class)
class RestIdentityControllerTest {

    private static final String BODY =
            "{\"employeeId\":\"e1234\",\"firstName\":\"Alice\",\"lastName\":\"Martin\",\"email\":\"a@example.org\"}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private IdentityManager identities;

    @MockBean
    private AccountManager accounts;

    private static Identity identity() {
        Identity identity = new Identity();
        identity.setEmployeeId("e1234");
        identity.setFirstName("Alice");
        identity.setLastName("Martin");
        identity.setEmail("a@example.org");
        return identity;
    }

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(get("/identities")).andExpect(status().isUnauthorized());
        mvc.perform(post("/identities").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readerReadsButCannotWrite() throws Exception {
        when(identities.getIdentities()).thenReturn(List.of(identity()));
        when(identities.getIdentity("e1234")).thenReturn(identity());

        mvc.perform(get("/identities").header("Authorization", bearer("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value("e1234"))
                .andExpect(jsonPath("$[0].email").value("a@example.org"));
        mvc.perform(get("/identities/e1234").header("Authorization", bearer("reader")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.lastName").value("Martin"));
        mvc.perform(post("/identities").header("Authorization", bearer("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(put("/identities/e1234").header("Authorization", bearer("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/identities/e1234").header("Authorization", bearer("reader")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsTheAccountsOfAnIdentity() throws Exception {
        Account account = new Account();
        account.setIdentifier("e1234-adm");
        account.setType("employee");
        account.setIdentity("e1234");
        account.setSource(AccountSource.DISCOVERED);
        when(identities.getIdentity("e1234")).thenReturn(identity());
        when(accounts.getAccounts("e1234", null, false)).thenReturn(List.of(account));

        mvc.perform(get("/identities/e1234/accounts").header("Authorization", bearer("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identifier").value("e1234-adm"));
    }

    @Test
    void unknownIdentityIs404() throws Exception {
        when(identities.getIdentity("absent")).thenThrow(new NotFoundException("absent"));

        mvc.perform(get("/identities/absent").header("Authorization", bearer("reader")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/identities/absent/accounts").header("Authorization", bearer("reader")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publisherCreatesUpdatesAndDeletes() throws Exception {
        when(identities.createIdentity(any())).thenReturn(identity());
        when(identities.updateIdentity(org.mockito.ArgumentMatchers.eq("e1234"), any())).thenReturn(identity());

        mvc.perform(post("/identities").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/identities/e1234"));
        mvc.perform(put("/identities/e1234").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
        mvc.perform(delete("/identities/e1234").header("Authorization", bearer("publisher")))
                .andExpect(status().isNoContent());

        verify(identities).deleteIdentity("e1234");
    }

    @Test
    void duplicateIdentityIs409() throws Exception {
        when(identities.createIdentity(any())).thenThrow(new IdentityConflictException("dup"));

        mvc.perform(post("/identities").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isConflict());
    }
}
