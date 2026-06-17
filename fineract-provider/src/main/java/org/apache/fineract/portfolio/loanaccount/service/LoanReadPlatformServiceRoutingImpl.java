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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;

import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.portfolio.calendar.data.CalendarData;
import org.apache.fineract.portfolio.floatingrates.data.InterestRatePeriodData;
import org.apache.fineract.portfolio.loanaccount.data.DisbursementData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.data.LoanApprovalData;
import org.apache.fineract.portfolio.loanaccount.data.LoanScheduleAccrualData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.data.PaidInAdvanceData;
import org.apache.fineract.portfolio.loanaccount.data.RepaymentScheduleRelatedLoanData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Strangler-fig routing facade in front of the legacy {@link LoanReadPlatformServiceImpl}.
 *
 * <p>
 * This is the durable seam for the loan READ surface. It implements
 * {@link LoanReadPlatformService} and is marked {@link Primary} so every existing
 * consumer that injects the interface resolves to this facade rather than the legacy
 * implementation directly &mdash; without any consumer change. At seam introduction
 * (transformation step {@code seam-introduction}) it delegates <em>100%</em> of read
 * operations to the legacy implementation, so there is no observable behaviour change:
 * the loan-read characterization safety net stays green. Individual read slices are then
 * migrated to a new implementation behind this facade one at a time, each flipped only
 * after a green characterization gate.
 * </p>
 *
 * <p>
 * Constructor injection follows the DI idiom of the wrapped {@link LoanReadPlatformServiceImpl}.
 * The legacy bean is injected by its concrete type so the {@code @Primary} facade does not
 * resolve to itself.
 * </p>
 */
@Service
@Primary
public class LoanReadPlatformServiceRoutingImpl implements LoanReadPlatformService {

    private final LoanReadPlatformServiceImpl legacy;

    @Autowired
    public LoanReadPlatformServiceRoutingImpl(final LoanReadPlatformServiceImpl legacy) {
        this.legacy = legacy;
    }

    @Override
    public LoanAccountData retrieveOne(final Long loanId) {
        return this.legacy.retrieveOne(loanId);
    }

    @Override
    public LoanScheduleData retrieveRepaymentSchedule(final Long loanId,
            final RepaymentScheduleRelatedLoanData repaymentScheduleRelatedData, final Collection<DisbursementData> disbursementData,
            final boolean isInterestRecalculationEnabled, final BigDecimal totalPaidFeeCharges) {
        return this.legacy.retrieveRepaymentSchedule(loanId, repaymentScheduleRelatedData, disbursementData,
                isInterestRecalculationEnabled, totalPaidFeeCharges);
    }

    @Override
    public Collection<LoanTransactionData> retrieveLoanTransactions(final Long loanId) {
        return this.legacy.retrieveLoanTransactions(loanId);
    }

    @Override
    public LoanAccountData retrieveTemplateWithClientAndProductDetails(final Long clientId, final Long productId) {
        return this.legacy.retrieveTemplateWithClientAndProductDetails(clientId, productId);
    }

    @Override
    public LoanAccountData retrieveTemplateWithGroupAndProductDetails(final Long groupId, final Long productId) {
        return this.legacy.retrieveTemplateWithGroupAndProductDetails(groupId, productId);
    }

    @Override
    public LoanTransactionData retrieveLoanTransactionTemplate(final Long loanId) {
        return this.legacy.retrieveLoanTransactionTemplate(loanId);
    }

    @Override
    public LoanTransactionData retrieveWaiveInterestDetails(final Long loanId) {
        return this.legacy.retrieveWaiveInterestDetails(loanId);
    }

    @Override
    public LoanTransactionData retrieveLoanTransaction(final Long loanId, final Long transactionId) {
        return this.legacy.retrieveLoanTransaction(loanId, transactionId);
    }

    @Override
    public LoanTransactionData retrieveNewClosureDetails() {
        return this.legacy.retrieveNewClosureDetails();
    }

    @Override
    public LoanTransactionData retrieveDisbursalTemplate(final Long loanId, final boolean paymentDetailsRequired) {
        return this.legacy.retrieveDisbursalTemplate(loanId, paymentDetailsRequired);
    }

    @Override
    public LoanApprovalData retrieveApprovalTemplate(final Long loanId) {
        return this.legacy.retrieveApprovalTemplate(loanId);
    }

    @Override
    public LoanAccountData retrieveTemplateWithCompleteGroupAndProductDetails(final Long groupId, final Long productId) {
        return this.legacy.retrieveTemplateWithCompleteGroupAndProductDetails(groupId, productId);
    }

    @Override
    public LoanAccountData retrieveLoanProductDetailsTemplate(final Long productId, final Long clientId, final Long groupId) {
        return this.legacy.retrieveLoanProductDetailsTemplate(productId, clientId, groupId);
    }

    @Override
    public LoanAccountData retrieveClientDetailsTemplate(final Long clientId) {
        return this.legacy.retrieveClientDetailsTemplate(clientId);
    }

    @Override
    public LoanAccountData retrieveGroupDetailsTemplate(final Long groupId) {
        return this.legacy.retrieveGroupDetailsTemplate(groupId);
    }

    @Override
    public LoanAccountData retrieveGroupAndMembersDetailsTemplate(final Long groupId) {
        return this.legacy.retrieveGroupAndMembersDetailsTemplate(groupId);
    }

    @Override
    public Collection<CalendarData> retrieveCalendars(final Long groupId) {
        return this.legacy.retrieveCalendars(groupId);
    }

    @Override
    public Page<LoanAccountData> retrieveAll(final SearchParameters searchParameters) {
        return this.legacy.retrieveAll(searchParameters);
    }

    @Override
    public Collection<StaffData> retrieveAllowedLoanOfficers(final Long selectedOfficeId, final boolean staffInSelectedOfficeOnly) {
        return this.legacy.retrieveAllowedLoanOfficers(selectedOfficeId, staffInSelectedOfficeOnly);
    }

    @Override
    public Collection<OverdueLoanScheduleData> retrieveAllLoansWithOverdueInstallments(final Long penaltyWaitPeriod,
            final Boolean backdatePenalties) {
        return this.legacy.retrieveAllLoansWithOverdueInstallments(penaltyWaitPeriod, backdatePenalties);
    }

    @Override
    public Integer retriveLoanCounter(final Long groupId, final Integer loanType, final Long productId) {
        return this.legacy.retriveLoanCounter(groupId, loanType, productId);
    }

    @Override
    public Integer retriveLoanCounter(final Long clientId, final Long productId) {
        return this.legacy.retriveLoanCounter(clientId, productId);
    }

    @Override
    public Collection<DisbursementData> retrieveLoanDisbursementDetails(final Long loanId) {
        return this.legacy.retrieveLoanDisbursementDetails(loanId);
    }

    @Override
    public DisbursementData retrieveLoanDisbursementDetail(final Long loanId, final Long disbursementId) {
        return this.legacy.retrieveLoanDisbursementDetail(loanId, disbursementId);
    }

    @Override
    public Collection<LoanTermVariationsData> retrieveLoanTermVariations(final Long loanId, final Integer termType) {
        return this.legacy.retrieveLoanTermVariations(loanId, termType);
    }

    @Override
    public Collection<LoanScheduleAccrualData> retriveScheduleAccrualData() {
        return this.legacy.retriveScheduleAccrualData();
    }

    @Override
    public LoanTransactionData retrieveRecoveryPaymentTemplate(final Long loanId) {
        return this.legacy.retrieveRecoveryPaymentTemplate(loanId);
    }

    @Override
    public LoanTransactionData retrieveLoanWriteoffTemplate(final Long loanId) {
        return this.legacy.retrieveLoanWriteoffTemplate(loanId);
    }

    @Override
    public Collection<LoanScheduleAccrualData> retrivePeriodicAccrualData(final LocalDate tillDate) {
        return this.legacy.retrivePeriodicAccrualData(tillDate);
    }

    @Override
    public Collection<Long> fetchLoansForInterestRecalculation() {
        return this.legacy.fetchLoansForInterestRecalculation();
    }

    @Override
    public LoanTransactionData retrieveLoanPrePaymentTemplate(final Long loanId, final LocalDate onDate) {
        return this.legacy.retrieveLoanPrePaymentTemplate(loanId, onDate);
    }

    @Override
    public Collection<LoanTransactionData> retrieveWaiverLoanTransactions(final Long loanId) {
        return this.legacy.retrieveWaiverLoanTransactions(loanId);
    }

    @Override
    public Collection<LoanSchedulePeriodData> fetchWaiverInterestRepaymentData(final Long loanId) {
        return this.legacy.fetchWaiverInterestRepaymentData(loanId);
    }

    @Override
    public Date retrieveMinimumDateOfRepaymentTransaction(final Long loanId) {
        return this.legacy.retrieveMinimumDateOfRepaymentTransaction(loanId);
    }

    @Override
    public PaidInAdvanceData retrieveTotalPaidInAdvance(final Long loanId) {
        return this.legacy.retrieveTotalPaidInAdvance(loanId);
    }

    @Override
    public LoanTransactionData retrieveRefundByCashTemplate(final Long loanId) {
        return this.legacy.retrieveRefundByCashTemplate(loanId);
    }

    @Override
    public Collection<InterestRatePeriodData> retrieveLoanInterestRatePeriodData(final LoanAccountData loan) {
        return this.legacy.retrieveLoanInterestRatePeriodData(loan);
    }

    @Override
    public Collection<Long> retrieveLoanIdsWithPendingIncomePostingTransactions() {
        return this.legacy.retrieveLoanIdsWithPendingIncomePostingTransactions();
    }

    @Override
    public LoanTransactionData retrieveLoanForeclosureTemplate(final Long loanId, final LocalDate transactionDate) {
        return this.legacy.retrieveLoanForeclosureTemplate(loanId, transactionDate);
    }

    @Override
    public LoanAccountData retrieveLoanByLoanAccount(final String loanAccountNumber) {
        return this.legacy.retrieveLoanByLoanAccount(loanAccountNumber);
    }

    @Override
    public Long retrieveLoanIdByAccountNumber(final String loanAccountNumber) {
        return this.legacy.retrieveLoanIdByAccountNumber(loanAccountNumber);
    }
}
