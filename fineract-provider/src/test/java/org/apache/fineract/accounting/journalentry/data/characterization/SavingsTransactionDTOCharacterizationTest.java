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
//   strategies: none (deterministic pure value object; no clock/uuid/ordering/random/environment non-determinism present)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 (Phase B / ACL Phase-0 safety net) — accounting<->portfolio golden-master.

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.apache.fineract.accounting.journalentry.data.SavingsTransactionDTO;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionEnumData;
import org.junit.Test;

/**
 * Golden-master of {@code SavingsTransactionDTO} — the savings-side accounting<->portfolio bridge value object. Pins
 * the observed getter pass-through and, importantly, the observed {@code isOverdraftTransaction()} derivation across
 * positive / zero / null overdraft amounts. Pure unit (no Spring, no DB).
 */
public class SavingsTransactionDTOCharacterizationTest {

    private static final long TXN_DATE_MILLIS = 1325376000000L; // 2012-01-01T00:00:00Z

    private SavingsTransactionDTO newDto(final BigDecimal amount, final boolean reversed, final BigDecimal overdraftAmount,
            final boolean isAccountTransfer) {
        return new SavingsTransactionDTO(200L, 7L, "STXN-1", new Date(TXN_DATE_MILLIS),
                new SavingsAccountTransactionEnumData(1L, "savingsAccountTransactionType.deposit", "Deposit"), amount, reversed,
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(),
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(), overdraftAmount,
                isAccountTransfer, Collections.<org.apache.fineract.accounting.journalentry.data.TaxPaymentDTO> emptyList());
    }

    @Test
    public void getters_passThroughConstructorArguments_asObserved() {
        final SavingsTransactionDTO dto = newDto(new BigDecimal("500.00"), false, new BigDecimal("0.00"), false);

        assertEquals(Long.valueOf(200L), dto.getOfficeId());
        assertEquals(Long.valueOf(7L), dto.getPaymentTypeId());
        assertEquals("STXN-1", dto.getTransactionId());
        assertEquals(TXN_DATE_MILLIS, dto.getTransactionDate().getTime());
        assertEquals(new BigDecimal("500.00"), dto.getAmount());
        assertEquals(new BigDecimal("0.00"), dto.getOverdraftAmount());
        assertEquals("Deposit", dto.getTransactionType().getValue());
        assertEquals(Long.valueOf(1L), dto.getTransactionType().getId());
        assertTrue(dto.getFeePayments().isEmpty());
        assertTrue(dto.getPenaltyPayments().isEmpty());
        assertTrue(dto.getTaxPayments().isEmpty());
    }

    @Test
    public void reversedAndAccountTransferFlags_areObservedVerbatim() {
        assertFalse(newDto(new BigDecimal("500.00"), false, new BigDecimal("0.00"), false).isReversed());
        assertTrue(newDto(new BigDecimal("500.00"), true, new BigDecimal("0.00"), false).isReversed());
        assertFalse(newDto(new BigDecimal("500.00"), false, new BigDecimal("0.00"), false).isAccountTransfer());
        assertTrue(newDto(new BigDecimal("500.00"), false, new BigDecimal("0.00"), true).isAccountTransfer());
    }

    @Test
    public void isOverdraftTransaction_observedDerivationAcrossPositiveZeroAndNull() {
        // OBSERVED: true only when overdraftAmount is non-null AND its doubleValue() is strictly > 0.
        assertTrue(newDto(new BigDecimal("500.00"), false, new BigDecimal("25.00"), false).isOverdraftTransaction());
        assertFalse(newDto(new BigDecimal("500.00"), false, new BigDecimal("0.00"), false).isOverdraftTransaction());
        assertFalse(newDto(new BigDecimal("500.00"), false, null, false).isOverdraftTransaction());
    }
}
