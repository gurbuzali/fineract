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
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;

/**
 * Anti-corruption layer (ACL) port owned by the accounting (journal-entry) bounded context.
 *
 * <p>When the journal-entry posting code creates a {@code JournalEntry} it must anchor that entry to the
 * originating portfolio transaction. Historically {@code AccountingProcessorHelper} reached straight into the
 * portfolio persistence layer ({@code LoanTransactionRepository}, {@code SavingsAccountTransactionRepository},
 * {@code ClientTransactionRepositoryWrapper}) to look those transactions up &mdash; a direct
 * accounting&rarr;portfolio data-access back-edge that anchors the dominant 79-module strongly-connected
 * component (roadmap DEBT-005 / DEBT-018).</p>
 *
 * <p>This interface is accounting's <em>own</em> contract for that lookup. The single
 * {@link LegacyPortfolioTransactionAdapter} implementation is the only class in the accounting module permitted
 * to inject portfolio's transaction <em>repositories</em>; every accounting consumer depends on this port
 * instead, so the reach is isolated behind one durable seam.</p>
 *
 * <p><strong>Scope &mdash; honest limitation of this increment:</strong> the return types are still the portfolio
 * domain entities, because the accounting {@code JournalEntry} entity holds them through JPA {@code @ManyToOne}
 * relations (the {@code loan_transaction_id} / {@code savings_transaction_id} / {@code client_transaction_id}
 * foreign keys). This increment therefore isolates the <em>data-access</em> coupling (the repository reach) and
 * establishes the port as the durable boundary; replacing the entity return types with accounting-owned DTOs
 * would require a separate persistence/schema workstream and is intentionally out of scope. ACL is durable
 * isolation, not retirement &mdash; the portfolio subsystem keeps running behind this port.</p>
 */
public interface PortfolioTransactionPort {

    /**
     * Returns the portfolio loan transaction with the given id, or {@code null} when none exists &mdash; preserving
     * the observed behaviour of the legacy {@code LoanTransactionRepository.findOne(Long)} reach it replaces.
     */
    LoanTransaction findLoanTransaction(Long loanTransactionId);

    /**
     * Returns the portfolio savings-account transaction with the given id, or {@code null} when none exists
     * &mdash; preserving the observed behaviour of the legacy
     * {@code SavingsAccountTransactionRepository.findOne(Long)} reach it replaces.
     */
    SavingsAccountTransaction findSavingsAccountTransaction(Long savingsAccountTransactionId);

    /**
     * Returns the portfolio client transaction for the given client and transaction id, raising the same
     * not-found exception as the legacy {@code ClientTransactionRepositoryWrapper.findOneWithNotFoundDetection}
     * reach it replaces.
     */
    ClientTransaction findClientTransaction(Long clientId, Long clientTransactionId);
}
