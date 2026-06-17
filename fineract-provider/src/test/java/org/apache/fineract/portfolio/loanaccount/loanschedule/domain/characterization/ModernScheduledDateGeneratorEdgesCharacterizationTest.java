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
//   target: fineract-provider/src/main/java/org/apache/fineract/portfolio/loanaccount/loanschedule/domain/ModernScheduledDateGenerator.java
//   run_id: 000-modernization-roadmap-bba-loan-schedule-generator-step-7-legacy-retirement
//   strategies: none (pure date arithmetic over fixed inputs; no clock/uuid/ordering/random/environment non-determinism present)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 — edge-cases (strict superset of ModernScheduledDateGeneratorCharacterizationTest); inspection-fallback (no branch coverage on this Gradle 2.10 build). Carries the legacy DefaultScheduledDateGenerator edge golden-master onto the modern implementation at legacy-retirement.

import static org.junit.Assert.assertEquals;

import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.ModernScheduledDateGenerator;
import org.joda.time.LocalDate;
import org.junit.Test;

/**
 * Edge-case golden-master extending {@code ModernScheduledDateGeneratorCharacterizationTest} with the boundary and
 * error-path inputs the happy-path set did not pin: the divide-by-zero error path on a zero {@code repaidEvery}, the
 * unexercised {@code INVALID} enum branch, and the zero-interval / same-day boundaries. These assertions are identical
 * to the retired {@code DefaultScheduledDateGeneratorEdgesCharacterizationTest} — the equality is the behavioral-
 * equivalence evidence that the modern implementation preserves the legacy edge behavior. Pure unit (no
 * LoanApplicationTerms, no Spring, no DB). Categories: error-path, type-union-branch, boundary-input.
 */
public class ModernScheduledDateGeneratorEdgesCharacterizationTest {

    private final ModernScheduledDateGenerator generator = new ModernScheduledDateGenerator();

    // error-path: repaidEvery == 0 reaches `diff % repaidEvery` -> integer divide-by-zero.
    @Test(expected = ArithmeticException.class)
    public void isDateFallsInSchedule_throwsArithmeticException_whenRepaidEveryIsZero_months() {
        generator.isDateFallsInSchedule(PeriodFrequencyType.MONTHS, 0, new LocalDate(2012, 1, 1), new LocalDate(2012, 3, 1));
    }

    // error-path: same divide-by-zero on the DAYS branch.
    @Test(expected = ArithmeticException.class)
    public void isDateFallsInSchedule_throwsArithmeticException_whenRepaidEveryIsZero_days() {
        generator.isDateFallsInSchedule(PeriodFrequencyType.DAYS, 0, new LocalDate(2012, 1, 1), new LocalDate(2012, 1, 8));
    }

    // type-union-branch: INVALID frequency falls through the switch with no assignment -> returns startDate unchanged.
    @Test
    public void getRepaymentPeriodDate_returnsStartDateUnchanged_forInvalidFrequency() {
        final LocalDate start = new LocalDate(2012, 1, 1);
        assertEquals(start, generator.getRepaymentPeriodDate(PeriodFrequencyType.INVALID, 1, start));
    }

    // boundary-input: repaidEvery == 0 adds zero units -> returns startDate unchanged.
    @Test
    public void getRepaymentPeriodDate_returnsStartDate_whenRepaidEveryIsZero() {
        final LocalDate start = new LocalDate(2012, 1, 1);
        assertEquals(start, generator.getRepaymentPeriodDate(PeriodFrequencyType.MONTHS, 0, start));
    }

    // boundary-input: the schedule-start date itself (diff 0) is observed as on-schedule.
    @Test
    public void isDateFallsInSchedule_trueForTheStartDateItself() {
        final LocalDate start = new LocalDate(2012, 1, 1);
        assertEquals(Boolean.TRUE, generator.isDateFallsInSchedule(PeriodFrequencyType.MONTHS, 1, start, start));
    }

    // type-union-branch: INVALID frequency in isDateFallsInSchedule -> no case sets the flag -> false.
    @Test
    public void isDateFallsInSchedule_falseForInvalidFrequency() {
        assertEquals(Boolean.FALSE, generator.isDateFallsInSchedule(PeriodFrequencyType.INVALID, 1, new LocalDate(2012, 1, 1),
                new LocalDate(2012, 3, 1)));
    }
}
