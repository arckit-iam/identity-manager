// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class SequenceEmployeeIdGeneratorTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    @Test
    void numbersEmployeeIdsOnSixDigitsAfterThePrefix() {
        when(jdbc.queryForObject("SELECT nextval('employee_id_seq')", Long.class)).thenReturn(1L, 123456L);
        SequenceEmployeeIdGenerator generator = new SequenceEmployeeIdGenerator(jdbc, "I");

        assertEquals("I000001", generator.next());
        assertEquals("I123456", generator.next());
    }

    @Test
    void usesTheConfiguredPrefix() {
        when(jdbc.queryForObject("SELECT nextval('employee_id_seq')", Long.class)).thenReturn(7L);

        assertEquals("X000007", new SequenceEmployeeIdGenerator(jdbc, "X").next());
    }
}