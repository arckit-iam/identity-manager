// SPDX-FileCopyrightText: 2026 Dédale Solutions
// SPDX-License-Identifier: EUPL-1.2

package secop.idm1.adapters.provide;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import secop.idm1.domain.port.EmployeeIdGenerator;

/** Matricules {@code I<DDDDDD>} numérotés par une séquence PostgreSQL (aucun doublon, même avec plusieurs instances). */
@Component
public class SequenceEmployeeIdGenerator implements EmployeeIdGenerator {

    private final JdbcTemplate jdbcTemplate;
    private final String prefix;

    @Autowired
    public SequenceEmployeeIdGenerator(JdbcTemplate jdbcTemplate,
            @Value("${arc.identity.employee-id-prefix:I}") String prefix) {
        this.jdbcTemplate = jdbcTemplate;
        this.prefix = prefix;
    }

    @Override
    public String next() {
        Long number = jdbcTemplate.queryForObject("SELECT nextval('employee_id_seq')", Long.class);
        return prefix + String.format("%06d", number);
    }
}