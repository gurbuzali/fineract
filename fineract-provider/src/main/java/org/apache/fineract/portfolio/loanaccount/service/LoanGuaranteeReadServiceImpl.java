/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.portfolio.loanaccount.service;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * New implementation of the loan guarantee-requirement read slice for the strangler-fig
 * transformation of {@link LoanReadPlatformServiceImpl}.
 *
 * <p>
 * The query is a verbatim copy of the legacy {@code LoanReadPlatformServiceImpl.isGuaranteeRequired}
 * read so the cutover is a behaviour-preserving move (pinned green by
 * {@code LoanGuaranteeRequiredCharacterizationTest}); the strangler verifies <em>no observable
 * change</em>, not correctness. The {@code loanId} is bound as a query parameter (no string
 * concatenation) per sql-injection-prevention. DI follows the legacy read service idiom:
 * a {@link JdbcTemplate} constructed over the injected {@link RoutingDataSource}.
 * </p>
 */
@Service
public class LoanGuaranteeReadServiceImpl implements LoanGuaranteeReadService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public LoanGuaranteeReadServiceImpl(final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean isGuaranteeRequired(final Long loanId) {
        final String sql = "select pl.hold_guarantee_funds from m_loan ml inner join m_product_loan pl on pl.id = ml.product_id where ml.id=?";
        return this.jdbcTemplate.queryForObject(sql, Boolean.class, loanId);
    }
}
