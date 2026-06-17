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
package org.apache.fineract.accounting.journalentry.service.acl;

import org.apache.fineract.portfolio.client.domain.ClientTransaction;
import org.apache.fineract.portfolio.client.domain.ClientTransactionRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Legacy-backed implementation of {@link PortfolioTransactionPort} &mdash; the anti-corruption layer adapter.
 *
 * <p>This is the single isolation point for the accounting&rarr;portfolio transaction-lookup back-edge: it is
 * intended to be the <em>only</em> class in the accounting module that injects portfolio transaction
 * <em>repositories</em>. It translates the accounting-facing {@link PortfolioTransactionPort} contract into calls
 * against the legacy portfolio persistence layer, which keeps running unchanged behind the port (ACL is durable
 * isolation, not retirement).</p>
 *
 * <p>Delegation here is verbatim &mdash; the same repository methods, arguments, and return values the journal-entry
 * code used inline before the migration &mdash; so the characterization baseline observes no behavioural change.</p>
 */
@Component
public class LegacyPortfolioTransactionAdapter implements PortfolioTransactionPort {

    private final LoanTransactionRepository loanTransactionRepository;
    private final SavingsAccountTransactionRepository savingsAccountTransactionRepository;
    private final ClientTransactionRepositoryWrapper clientTransactionRepository;

    @Autowired
    public LegacyPortfolioTransactionAdapter(final LoanTransactionRepository loanTransactionRepository,
            final SavingsAccountTransactionRepository savingsAccountTransactionRepository,
            final ClientTransactionRepositoryWrapper clientTransactionRepository) {
        this.loanTransactionRepository = loanTransactionRepository;
        this.savingsAccountTransactionRepository = savingsAccountTransactionRepository;
        this.clientTransactionRepository = clientTransactionRepository;
    }

    @Override
    public LoanTransaction findLoanTransaction(final Long loanTransactionId) {
        return this.loanTransactionRepository.findOne(loanTransactionId);
    }

    @Override
    public SavingsAccountTransaction findSavingsAccountTransaction(final Long savingsAccountTransactionId) {
        return this.savingsAccountTransactionRepository.findOne(savingsAccountTransactionId);
    }

    @Override
    public ClientTransaction findClientTransaction(final Long clientId, final Long clientTransactionId) {
        return this.clientTransactionRepository.findOneWithNotFoundDetection(clientId, clientTransactionId);
    }
}
