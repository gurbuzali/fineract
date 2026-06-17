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
package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

/**
 * Construction seam for {@link ScheduledDateGenerator}.
 *
 * <p>
 * Branch-by-abstraction seam (transformation run {@code 000-modernization-roadmap}, component
 * {@code bba-loan-schedule-generator}). Before this seam existed, five caller classes each hard-coded a
 * {@code new}-instantiation of the concrete schedule-date generator inline — a direct, baked-in dependency with no
 * swap point. Routing every caller through this factory centralised the choice of {@code ScheduledDateGenerator}
 * implementation in one place; the transformation used that single point to flip every caller onto
 * {@link ModernScheduledDateGenerator} and retire the legacy implementation. It remains the single, durable
 * construction point for {@link ScheduledDateGenerator}.
 */
// acceptance: AC-007 (Phase C / BBA) — the durable construction seam every caller routes through.
public final class ScheduledDateGeneratorFactory {

    private ScheduledDateGeneratorFactory() {
        // utility — no instances
    }

    /**
     * Returns a {@link ScheduledDateGenerator} — the {@link ModernScheduledDateGenerator} implementation. This is the
     * single construction point for the repayment-schedule date generator across all callers.
     */
    public static ScheduledDateGenerator dateGenerator() {
        return new ModernScheduledDateGenerator();
    }
}
