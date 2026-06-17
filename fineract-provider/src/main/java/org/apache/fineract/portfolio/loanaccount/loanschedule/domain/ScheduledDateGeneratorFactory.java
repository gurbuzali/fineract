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
 * {@code bba-loan-schedule-generator}). Before this seam existed, five caller classes each hard-coded
 * {@code new DefaultScheduledDateGenerator()} inline — a direct, baked-in dependency on a concrete
 * implementation with no swap point. Routing every caller through this factory centralises the choice of
 * {@code ScheduledDateGenerator} implementation in one place so the implementation can be flipped (and the
 * legacy one retired) without touching the callers' surrounding logic.
 *
 * <p>
 * The two-method shape is intentional and transitional: {@link #legacyDateGenerator()} returns the legacy
 * implementation and exists only until every caller has been flipped; {@code dateGenerator()} (added in the
 * impl-flip step) returns the new implementation and is the durable survivor once the legacy one is retired.
 */
// acceptance: AC-007 (Phase C / BBA abstraction-introduction) — the construction seam migrated callers route through.
public final class ScheduledDateGeneratorFactory {

    private ScheduledDateGeneratorFactory() {
        // utility — no instances
    }

    /**
     * Returns the legacy {@link DefaultScheduledDateGenerator}. Transitional: callers are migrated here as pure
     * indirection during abstraction-introduction, then flipped off it during impl-flip; removed at legacy-retirement.
     */
    public static ScheduledDateGenerator legacyDateGenerator() {
        return new DefaultScheduledDateGenerator();
    }
}
