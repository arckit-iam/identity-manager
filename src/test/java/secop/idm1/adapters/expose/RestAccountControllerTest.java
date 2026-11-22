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

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import secop.idm1.domain.exception.AccountConflictException;
import secop.idm1.domain.exception.InvalidAccountException;
import secop.idm1.domain.exception.NotFoundException;
import secop.idm1.domain.model.Account;
import secop.idm1.domain.model.AccountKind;
import secop.idm1.domain.model.AccountSource;
import secop.idm1.domain.model.AccountType;
import secop.idm1.domain.model.MetadataField;
import secop.idm1.domain.model.MetadataType;
import secop.idm1.domain.service.AccountManager;

@WebMvcTest(RestAccountController.class)
@ContextConfiguration(classes = {RestAccountController.class, DtoMapper.class, SecurityConfig.class,
        TestJwtConfig.class})
@Import(BusinessExceptionHandler.class)
class RestAccountControllerTest {

    private static final String BODY = "{\"identifier\":\"e1234\",\"metadata\":{\"department\":\"IT\"}}";

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AccountManager manager;

    private Account account() {
        Account account = new Account();
        account.setIdentifier("e1234");
        account.setType("employee");
        account.setIdentity("e1234");
        account.setSource(AccountSource.API);
        account.setMetadata(Map.of("department", "IT"));
        return account;
    }

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
        mvc.perform(get("/account-types")).andExpect(status().isUnauthorized());
        mvc.perform(get("/accounts").header("Authorization", bearer("garbage"))).andExpect(status().isUnauthorized());
        mvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void aTokenWithoutRoleIsForbidden() throws Exception {
        mvc.perform(get("/accounts").header("Authorization", bearer("nobody"))).andExpect(status().isForbidden());
    }

    @Test
    void readerReadsButCannotWrite() throws Exception {
        when(manager.getAccounts(null, null, false)).thenReturn(List.of(account()));

        mvc.perform(get("/accounts").header("Authorization", bearer("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].identifier").value("e1234"))
                .andExpect(jsonPath("$[0].source").value("API"))
                .andExpect(jsonPath("$[0].metadata.department").value("IT"));
        mvc.perform(post("/accounts").header("Authorization", bearer("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/accounts/x").header("Authorization", bearer("reader")))
                .andExpect(status().isForbidden());
        mvc.perform(put("/accounts/x/metadata").header("Authorization", bearer("reader"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsFilteredAccounts() throws Exception {
        when(manager.getAccounts("e1234", "employee", true)).thenReturn(List.of(account()));

        mvc.perform(get("/accounts?identity=e1234&type=employee&uncorrelated=true")
                        .header("Authorization", bearer("reader")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void exposesAccountTypes() throws Exception {
        when(manager.getAccountTypes()).thenReturn(List.of(new AccountType("employee", AccountKind.USER,
                Pattern.compile("(?<identity>[a-z]+[0-9]+)"),
                List.of(new MetadataField("department", MetadataType.STRING, true)))));

        mvc.perform(get("/account-types").header("Authorization", bearer("reader")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("employee"))
                .andExpect(jsonPath("$[0].kind").value("USER"))
                .andExpect(jsonPath("$[0].pattern").value("(?<identity>[a-z]+[0-9]+)"))
                .andExpect(jsonPath("$[0].metadata[0].required").value(true));
    }

    @Test
    void getsOneAccountOr404() throws Exception {
        when(manager.getAccount("e1234")).thenReturn(account());
        when(manager.getAccount("absent")).thenThrow(new NotFoundException("absent"));

        mvc.perform(get("/accounts/e1234").header("Authorization", bearer("reader")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.identity").value("e1234"));
        mvc.perform(get("/accounts/absent").header("Authorization", bearer("reader")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publisherCreatesAnAccount() throws Exception {
        when(manager.createAccount(eq("e1234"), any(), any(), any())).thenReturn(account());

        mvc.perform(post("/accounts").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("employee"));
    }

    @Test
    void businessErrorsBecomeHttpStatuses() throws Exception {
        when(manager.createAccount(eq("bad"), any(), any(), any())).thenThrow(new InvalidAccountException("bad"));
        when(manager.createAccount(eq("dup"), any(), any(), any())).thenThrow(new AccountConflictException("dup"));

        mvc.perform(post("/accounts").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"bad\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/accounts").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"identifier\":\"dup\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void publisherUpdatesMetadataAndIdentityThenCorrelatesAllThenDeletes() throws Exception {
        when(manager.updateMetadata(eq("a"), any())).thenReturn(account());
        when(manager.correlate("a", "e1234")).thenReturn(account());
        when(manager.correlateAll()).thenReturn(3);

        mvc.perform(put("/accounts/a/metadata").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"department\":\"HR\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/accounts/a/identity").header("Authorization", bearer("publisher"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"identity\":\"e1234\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/accounts/correlate").header("Authorization", bearer("publisher")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.correlated").value(3));
        mvc.perform(delete("/accounts/a").header("Authorization", bearer("publisher")))
                .andExpect(status().isNoContent());

        verify(manager).deleteAccount("a");
    }

    @Test
    void unknownPathsAreDenied() throws Exception {
        mvc.perform(get("/products").header("Authorization", bearer("publisher")))
                .andExpect(status().isForbidden());
    }
}
