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
package org.apache.fineract.portfolio.loanaccount.service;

/**
 * Focused read service for the loan guarantee-requirement slice, extracted from the
 * legacy {@link LoanReadPlatformServiceImpl} as part of the strangler-fig transformation
 * of the loan READ surface.
 *
 * <p>
 * This is the <em>new implementation</em> the strangler routing facade
 * ({@link LoanReadPlatformServiceRoutingImpl}) flips the {@code isGuaranteeRequired} slice
 * onto, behind a green characterization gate. The legacy {@code LoanReadPlatformServiceImpl}
 * implementation of this slice is retired once the slice is fully cut over.
 * </p>
 */
public interface LoanGuaranteeReadService {

    /**
     * Whether the loan's product requires guarantee funds to be held.
     *
     * @param loanId the loan id
     * @return {@code true} when the loan's product has {@code hold_guarantee_funds} set
     */
    boolean isGuaranteeRequired(Long loanId);
}
