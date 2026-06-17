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
//   strategies: none (deterministic pure value object; no clock/uuid/ordering/random/environment non-determinism present)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 (Phase B / ACL Phase-0 safety net) — accounting<->portfolio golden-master.

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.apache.fineract.accounting.journalentry.data.LoanTransactionDTO;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.junit.Test;

/**
 * Golden-master of {@code LoanTransactionDTO} — the accounting<->portfolio bridge value object carrying a loan
 * transaction into journal-entry creation. Pins the currently-observed pass-through of constructor arguments to
 * getters and the observed default of the mutable {@code isLoanToLoanTransfer} flag. Pure unit (no Spring, no DB).
 */
public class LoanTransactionDTOCharacterizationTest {

    // 2012-01-01T00:00:00Z — fixed epoch millis; date pinned as a long to stay timezone-stable.
    private static final long TXN_DATE_MILLIS = 1325376000000L;

    private LoanTransactionEnumData enumData() {
        return new LoanTransactionEnumData(1L, "loanTransactionType.repayment", "Repayment");
    }

    private LoanTransactionDTO newDto(final boolean reversed, final boolean isAccountTransfer) {
        return new LoanTransactionDTO(100L, 5L, "TXN-1", new Date(TXN_DATE_MILLIS), enumData(),
                new BigDecimal("1000.00"), new BigDecimal("800.00"), new BigDecimal("150.00"), new BigDecimal("30.00"),
                new BigDecimal("20.00"), new BigDecimal("0.00"), reversed, Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(),
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(), isAccountTransfer);
    }

    @Test
    public void getters_passThroughConstructorArguments_asObserved() {
        final LoanTransactionDTO dto = newDto(false, false);

        assertEquals(Long.valueOf(100L), dto.getOfficeId());
        assertEquals(Long.valueOf(5L), dto.getPaymentTypeId());
        assertEquals("TXN-1", dto.getTransactionId());
        assertEquals(TXN_DATE_MILLIS, dto.getTransactionDate().getTime());
        assertEquals(new BigDecimal("1000.00"), dto.getAmount());
        assertEquals(new BigDecimal("800.00"), dto.getPrincipal());
        assertEquals(new BigDecimal("150.00"), dto.getInterest());
        assertEquals(new BigDecimal("30.00"), dto.getFees());
        assertEquals(new BigDecimal("20.00"), dto.getPenalties());
        assertEquals(new BigDecimal("0.00"), dto.getOverPayment());
        assertEquals("Repayment", dto.getTransactionType().getValue());
        assertEquals("loanTransactionType.repayment", dto.getTransactionType().getCode());
        assertTrue(dto.getFeePayments().isEmpty());
        assertTrue(dto.getPenaltyPayments().isEmpty());
    }

    @Test
    public void reversedFlag_isObservedVerbatim() {
        assertFalse(newDto(false, false).isReversed());
        assertTrue(newDto(true, false).isReversed());
    }

    @Test
    public void accountTransferFlag_isObservedVerbatim() {
        assertFalse(newDto(false, false).isAccountTransfer());
        assertTrue(newDto(false, true).isAccountTransfer());
    }

    @Test
    public void loanToLoanTransfer_defaultsFalse_andReflectsSetter() {
        final LoanTransactionDTO dto = newDto(false, false);
        // OBSERVED: the primitive boolean field defaults to false when never set.
        assertFalse(dto.isLoanToLoanTransfer());
        dto.setIsLoanToLoanTransfer(true);
        assertTrue(dto.isLoanToLoanTransfer());
    }

    @Test
    public void transactionType_isReturnedByIdentity() {
        final LoanTransactionEnumData type = enumData();
        final LoanTransactionDTO dto = new LoanTransactionDTO(100L, 5L, "TXN-1", new Date(TXN_DATE_MILLIS), type,
                new BigDecimal("1000.00"), new BigDecimal("800.00"), new BigDecimal("150.00"), new BigDecimal("30.00"),
                new BigDecimal("20.00"), new BigDecimal("0.00"), false,
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(),
                Collections.<org.apache.fineract.accounting.journalentry.data.ChargePaymentDTO> emptyList(), false);
        assertSame(type, dto.getTransactionType());
    }
}
