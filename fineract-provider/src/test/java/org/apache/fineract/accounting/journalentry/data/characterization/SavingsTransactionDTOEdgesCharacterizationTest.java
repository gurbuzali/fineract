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
//   target: fineract-provider/src/main/java/org/apache/fineract/accounting/journalentry/data/SavingsTransactionDTO.java
//   run_id: 000-modernization-roadmap-acl-accounting-portfolio-step-1
//   strategies: none (deterministic value object; no clock/uuid/ordering/random/environment non-determinism present)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 — edge-cases (strict superset of SavingsTransactionDTOCharacterizationTest); inspection-fallback (no branch coverage on this Gradle 2.10 build).

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.apache.fineract.accounting.journalentry.data.SavingsTransactionDTO;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionEnumData;
import org.junit.Test;

/**
 * Edge-case golden-master extending {@code SavingsTransactionDTOCharacterizationTest}: the negative side of the
 * {@code isOverdraftTransaction()} {@code doubleValue() > 0} predicate (happy pinned positive/zero/null), plus
 * nullable-input pass-through. Categories: boundary-literal, nullable-input. Pure unit (no Spring, no DB).
 */
public class SavingsTransactionDTOEdgesCharacterizationTest {

    private static final long TXN_DATE_MILLIS = 1325376000000L; // 2012-01-01T00:00:00Z

    private SavingsTransactionDTO newDto(final BigDecimal amount, final BigDecimal overdraftAmount) {
        return new SavingsTransactionDTO(200L, 7L, "STXN-EDGE", new Date(TXN_DATE_MILLIS),
                new SavingsAccountTransactionEnumData(1L, "savingsAccountTransactionType.deposit", "Deposit"), amount, false,
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(),
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(), overdraftAmount,
                false, Collections.<org.apache.fineract.accounting.journalentry.data.TaxPaymentDTO> emptyList());
    }

    // boundary-literal: a NEGATIVE overdraft amount is observed as NOT an overdraft transaction (predicate is > 0).
    @Test
    public void isOverdraftTransaction_falseForNegativeOverdraftAmount() {
        assertFalse(newDto(new BigDecimal("500.00"), new BigDecimal("-25.00")).isOverdraftTransaction());
    }

    // nullable-input: a null amount is passed through verbatim by the getter.
    @Test
    public void getAmount_returnsNull_forNullAmount() {
        assertNull(newDto(null, new BigDecimal("0.00")).getAmount());
    }

    // boundary-literal: a very small positive overdraft (above zero) is observed as an overdraft transaction.
    @Test
    public void isOverdraftTransaction_trueForSmallPositiveOverdraftAmount() {
        assertEquals(true, newDto(new BigDecimal("500.00"), new BigDecimal("0.01")).isOverdraftTransaction());
    }
}
