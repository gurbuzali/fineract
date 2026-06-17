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
package org.apache.fineract.accounting.journalentry.data.characterization;

// Characterization test — pinned-behavior snapshot of OBSERVED output.
//   target: fineract-provider/src/main/java/org/apache/fineract/accounting/journalentry/data/LoanTransactionDTO.java
//   run_id: 000-modernization-roadmap-acl-accounting-portfolio-step-1
//   strategies: none (deterministic value object; no clock/uuid/ordering/random/environment non-determinism present)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 — edge-cases (strict superset of LoanTransactionDTOCharacterizationTest); inspection-fallback (no branch coverage on this Gradle 2.10 build).

import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.apache.fineract.accounting.journalentry.data.LoanTransactionDTO;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.junit.Test;

/**
 * Edge-case golden-master extending {@code LoanTransactionDTOCharacterizationTest} with nullable-input pass-through:
 * the breakdown {@code BigDecimal} fields (principal/interest/fees/penalties/overPayment) and the transaction-type
 * are observed to be returned verbatim as null when the bridge supplies null. Category: nullable-input. Pure unit.
 */
public class LoanTransactionDTOEdgesCharacterizationTest {

    private static final long TXN_DATE_MILLIS = 1325376000000L; // 2012-01-01T00:00:00Z

    // nullable-input: null breakdown amounts and a null transaction-type are passed through verbatim by the getters
    // (the DTO performs no defaulting). Pinned because downstream accounting reads these directly.
    @Test
    public void getters_returnNull_forNullBreakdownAndType() {
        final LoanTransactionDTO dto = new LoanTransactionDTO(100L, 5L, "TXN-EDGE", new Date(TXN_DATE_MILLIS),
                (LoanTransactionEnumData) null, null, null, null, null, null, null, false,
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(),
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(), false);
        assertNull(dto.getAmount());
        assertNull(dto.getPrincipal());
        assertNull(dto.getInterest());
        assertNull(dto.getFees());
        assertNull(dto.getPenalties());
        assertNull(dto.getOverPayment());
        assertNull(dto.getTransactionType());
    }
}
