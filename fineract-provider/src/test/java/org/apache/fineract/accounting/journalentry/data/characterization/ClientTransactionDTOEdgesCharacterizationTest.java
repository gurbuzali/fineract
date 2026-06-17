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
//   strategies: none (deterministic value object; no clock/uuid/ordering/random/environment non-determinism present)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 — edge-cases (strict superset of ClientTransactionDTOCharacterizationTest); inspection-fallback (no branch coverage on this Gradle 2.10 build).

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;

import org.apache.fineract.accounting.journalentry.data.ClientTransactionDTO;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.junit.Test;

/**
 * Edge-case golden-master extending {@code ClientTransactionDTOCharacterizationTest}: the error path where
 * {@code isChargePayment()} dereferences a null transaction-type, and nullable-input pass-through on the getters.
 * Categories: error-path, nullable-input. Pure unit (no Spring, no DB).
 */
public class ClientTransactionDTOEdgesCharacterizationTest {

    private static final long TXN_DATE_MILLIS = 1325376000000L; // 2012-01-01T00:00:00Z

    // error-path: isChargePayment() calls this.transactionType.getId() -> NullPointerException on a null type.
    @Test(expected = NullPointerException.class)
    public void isChargePayment_throwsNullPointerException_whenTransactionTypeIsNull() {
        final ClientTransactionDTO dto = new ClientTransactionDTO(300L, 400L, 9L, 555L, new Date(TXN_DATE_MILLIS), null, "USD",
                new BigDecimal("75.00"), false, true,
                Collections.<org.apache.fineract.accounting.journalentry.data.ClientChargePaymentDTO> emptyList());
        dto.isChargePayment();
    }

    // nullable-input: null amount / currencyCode / charge-payments are passed through verbatim by the getters.
    @Test
    public void getters_returnNull_forNullableConstructorArguments() {
        final EnumOptionData type = new EnumOptionData(1L, "clientTransactionType.payCharge", "Pay Charge");
        final ClientTransactionDTO dto = new ClientTransactionDTO(300L, 400L, 9L, 555L, new Date(TXN_DATE_MILLIS), type, null,
                null, false, true, null);
        assertNull(dto.getCurrencyCode());
        assertNull(dto.getAmount());
        assertNull(dto.getChargePayments());
        // isChargePayment() still resolves because the (non-null) type id equals PAY_CHARGE (1).
        assertEquals(Boolean.TRUE, dto.isChargePayment());
    }
}
