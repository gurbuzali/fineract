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
package org.apache.fineract.portfolio.loanaccount.loanschedule.domain.characterization;

// Characterization test — pinned-behavior snapshot of OBSERVED output.
//   target: fineract-provider/src/main/java/org/apache/fineract/portfolio/loanaccount/loanschedule/domain/DefaultScheduledDateGenerator.java
//   run_id: 000-modernization-roadmap-bba-loan-schedule-generator-step-1
//   strategies: none (pure date arithmetic over fixed LocalDate inputs; no clock/uuid/ordering/random/environment non-determinism present)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 (Phase C / BBA Phase-0 safety net) — loan-schedule golden-master.

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.joda.time.LocalDate;
import org.junit.Test;

/**
 * Golden-master of the pure, collaborator-free date arithmetic in {@code DefaultScheduledDateGenerator} —
 * {@code getRepaymentPeriodDate(...)} and {@code isDateFallsInSchedule(...)}. These are the deterministic spine of
 * repayment-schedule date generation. Includes Joda-Time month-end / leap-day clamping behaviors, pinned as observed.
 * Pure unit (no LoanApplicationTerms, no Spring, no DB).
 */
public class DefaultScheduledDateGeneratorCharacterizationTest {

    private final DefaultScheduledDateGenerator generator = new DefaultScheduledDateGenerator();

    @Test
    public void getRepaymentPeriodDate_addsTheFrequencyUnitTimesRepaidEvery() {
        assertEquals(new LocalDate(2012, 1, 11),
                generator.getRepaymentPeriodDate(PeriodFrequencyType.DAYS, 10, new LocalDate(2012, 1, 1)));
        assertEquals(new LocalDate(2012, 1, 15),
                generator.getRepaymentPeriodDate(PeriodFrequencyType.WEEKS, 2, new LocalDate(2012, 1, 1)));
        assertEquals(new LocalDate(2012, 2, 1),
                generator.getRepaymentPeriodDate(PeriodFrequencyType.MONTHS, 1, new LocalDate(2012, 1, 1)));
    }

    @Test
    public void getRepaymentPeriodDate_clampsMonthEndAndLeapDay_asObserved() {
        // OBSERVED: Joda-Time clamps an overflowing day-of-month to the last valid day of the target month.
        assertEquals(new LocalDate(2012, 4, 30),
                generator.getRepaymentPeriodDate(PeriodFrequencyType.MONTHS, 3, new LocalDate(2012, 1, 31)));
        // OBSERVED: a Feb-29 (leap) start one year later clamps to Feb-28 (non-leap).
        assertEquals(new LocalDate(2013, 2, 28),
                generator.getRepaymentPeriodDate(PeriodFrequencyType.YEARS, 1, new LocalDate(2012, 2, 29)));
    }

    @Test
    public void isDateFallsInSchedule_trueOnAnIntervalBoundary() {
        assertTrue(generator.isDateFallsInSchedule(PeriodFrequencyType.MONTHS, 1, new LocalDate(2012, 1, 1),
                new LocalDate(2012, 3, 1)));
        assertTrue(generator.isDateFallsInSchedule(PeriodFrequencyType.MONTHS, 2, new LocalDate(2012, 1, 1),
                new LocalDate(2012, 3, 1)));
        assertTrue(generator.isDateFallsInSchedule(PeriodFrequencyType.DAYS, 7, new LocalDate(2012, 1, 1),
                new LocalDate(2012, 1, 15)));
    }

    @Test
    public void isDateFallsInSchedule_falseOffAnIntervalBoundary() {
        assertFalse(generator.isDateFallsInSchedule(PeriodFrequencyType.MONTHS, 2, new LocalDate(2012, 1, 1),
                new LocalDate(2012, 2, 1)));
        assertFalse(generator.isDateFallsInSchedule(PeriodFrequencyType.DAYS, 7, new LocalDate(2012, 1, 1),
                new LocalDate(2012, 1, 10)));
    }
}
