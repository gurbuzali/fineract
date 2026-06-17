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
//   target: fineract-provider/src/main/java/org/apache/fineract/accounting/journalentry/data/ClientTransactionDTO.java
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

import org.apache.fineract.accounting.journalentry.data.ClientTransactionDTO;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.junit.Test;

/**
 * Golden-master of {@code ClientTransactionDTO} — the client-charge accounting<->portfolio bridge value object. Pins
 * the observed getter pass-through and the observed {@code isChargePayment()} derivation, which keys off the
 * transaction-type id matching {@code ClientTransactionType.PAY_CHARGE} (id 1). Pure unit (no Spring, no DB).
 */
public class ClientTransactionDTOCharacterizationTest {

    private static final long TXN_DATE_MILLIS = 1325376000000L; // 2012-01-01T00:00:00Z

    private ClientTransactionDTO newDto(final EnumOptionData transactionType, final boolean reversed,
            final boolean accountingEnabled) {
        return new ClientTransactionDTO(300L, 400L, 9L, 555L, new Date(TXN_DATE_MILLIS), transactionType, "USD",
                new BigDecimal("75.00"), reversed, accountingEnabled,
                Collections.<org.apache.fineract.accounting.journalentry.data.ClientChargePaymentDTO> emptyList());
    }

    @Test
    public void getters_passThroughConstructorArguments_asObserved() {
        final EnumOptionData type = new EnumOptionData(1L, "clientTransactionType.payCharge", "Pay Charge");
        final ClientTransactionDTO dto = newDto(type, false, true);

        assertEquals(Long.valueOf(300L), dto.getClientId());
        assertEquals(Long.valueOf(400L), dto.getOfficeId());
        assertEquals(Long.valueOf(9L), dto.getPaymentTypeId());
        assertEquals(Long.valueOf(555L), dto.getTransactionId());
        assertEquals(TXN_DATE_MILLIS, dto.getTransactionDate().getTime());
        assertEquals("USD", dto.getCurrencyCode());
        assertEquals(new BigDecimal("75.00"), dto.getAmount());
        assertEquals(Boolean.TRUE, dto.getAccountingEnabled());
        assertEquals(Long.valueOf(1L), dto.getTransactionType().getId());
        assertTrue(dto.getChargePayments().isEmpty());
    }

    @Test
    public void reversedFlag_isObservedVerbatim() {
        final EnumOptionData type = new EnumOptionData(1L, "clientTransactionType.payCharge", "Pay Charge");
        assertFalse(newDto(type, false, true).isReversed());
        assertTrue(newDto(type, true, true).isReversed());
    }

    @Test
    public void isChargePayment_observedTrueForPayChargeTypeIdOneFalseOtherwise() {
        // OBSERVED: isChargePayment() is true only when the transaction-type id equals PAY_CHARGE's value (1).
        final EnumOptionData payCharge = new EnumOptionData(1L, "clientTransactionType.payCharge", "Pay Charge");
        final EnumOptionData withdrawal = new EnumOptionData(2L, "clientTransactionType.withdrawal", "Withdrawal");
        assertTrue(newDto(payCharge, false, true).isChargePayment());
        assertFalse(newDto(withdrawal, false, true).isChargePayment());
    }
}
