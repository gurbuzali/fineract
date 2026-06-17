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
package org.apache.fineract.integrationtests.characterization;

// Characterization test — pinned-behavior snapshot of OBSERVED output.
//   target: fineract-provider/src/main/java/org/apache/fineract/accounting/journalentry/service/JournalEntryWritePlatformServiceJpaRepositoryImpl.java
//   run_id: 000-modernization-roadmap-acl-accounting-portfolio-step-1
//   strategies: none (deterministic fields only — fixed past disbursement date, GL debit/credit amounts and entry types; no auto-ids or today-relative values asserted)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006

import java.util.HashMap;

import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
import org.apache.fineract.integrationtests.common.accounting.Account;
import org.apache.fineract.integrationtests.common.accounting.AccountHelper;
import org.apache.fineract.integrationtests.common.accounting.JournalEntry;
import org.apache.fineract.integrationtests.common.accounting.JournalEntryHelper;
import org.apache.fineract.integrationtests.common.loans.LoanApplicationTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanProductTestBuilder;
import org.apache.fineract.integrationtests.common.loans.LoanStatusChecker;
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

/**
 * Characterization (golden-master) test for the accounting&harr;portfolio journal
 * path written by {@code JournalEntryWritePlatformServiceJpaRepositoryImpl}. A
 * loan disbursal on a cash-based, accounting-enabled product triggers
 * {@code createJournalEntriesForLoan}; this test boots the real application,
 * disburses on a FIXED PAST DATE, and pins the currently-observed GL postings.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class JournalEntryAccountingCharacterizationTest {

    private static final String CASH_BASED = "2";

    private static final String DISBURSAL_DATE = "20 September 2011";
    private static final String SUBMITTED_ON_DATE = "20 September 2011";
    private static final String DATE_OF_JOINING = "01 January 2011";
    private final Float LP_PRINCIPAL = 10000.0f;

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private LoanTransactionHelper loanTransactionHelper;
    private AccountHelper accountHelper;
    private JournalEntryHelper journalEntryHelper;

    @Before
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
        this.accountHelper = new AccountHelper(this.requestSpec, this.responseSpec);
        this.journalEntryHelper = new JournalEntryHelper(this.requestSpec, this.responseSpec);
    }

    @Test
    public void pinDisbursalJournalEntriesCashBased() {
        final Account assetAccount = this.accountHelper.createAssetAccount();
        final Account incomeAccount = this.accountHelper.createIncomeAccount();
        final Account expenseAccount = this.accountHelper.createExpenseAccount();
        final Account liabilityAccount = this.accountHelper.createLiabilityAccount();

        final Integer loanProductID = createLoanProductWithCashBasedAccounting(assetAccount, incomeAccount, expenseAccount,
                liabilityAccount);

        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, DATE_OF_JOINING);
        final Integer loanID = applyForLoanApplication(clientID, loanProductID);

        this.loanTransactionHelper.approveLoan(DISBURSAL_DATE, loanID);

        // Disbursal triggers JournalEntryWritePlatformService.createJournalEntriesForLoan (cash-based).
        final HashMap disbursedStatus = this.loanTransactionHelper.disburseLoan(DISBURSAL_DATE, loanID);
        LoanStatusChecker.verifyLoanIsActive(disbursedStatus);

        // Pin the OBSERVED disbursal GL postings on the asset account. Under cash-based mapping the
        // single asset account is both LOAN_PORTFOLIO and FUND_SOURCE, so disbursal posts a DEBIT
        // (loan portfolio) and a CREDIT (fund source) of the full disbursal amount.
        final JournalEntry[] assetAccountDisbursalEntries = {
                new JournalEntry(this.LP_PRINCIPAL, JournalEntry.TransactionType.DEBIT),
                new JournalEntry(this.LP_PRINCIPAL, JournalEntry.TransactionType.CREDIT) };
        this.journalEntryHelper.checkJournalEntryForAssetAccount(assetAccount, DISBURSAL_DATE, assetAccountDisbursalEntries);
    }

    private Integer createLoanProductWithCashBasedAccounting(final Account... accounts) {
        final String loanProductJSON = new LoanProductTestBuilder() //
                .withPrincipal(this.LP_PRINCIPAL.toString()) //
                .withRepaymentTypeAsMonth() //
                .withRepaymentAfterEvery("2") //
                .withNumberOfRepayments("5") //
                .withinterestRatePerPeriod("1") //
                .withInterestRateFrequencyTypeAsMonths() //
                .withAmortizationTypeAsEqualPrincipalPayment() //
                .withInterestTypeAsFlat() //
                .withAccountingRuleAsCashBased(accounts) //
                .build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID) {
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(this.LP_PRINCIPAL.toString()) //
                .withLoanTermFrequency("10") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("5") //
                .withRepaymentEveryAfter("2") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("1") //
                .withInterestTypeAsFlatBalance() //
                .withAmortizationTypeAsEqualPrincipalPayments() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate(DISBURSAL_DATE) //
                .withSubmittedOnDate(SUBMITTED_ON_DATE) //
                .withLoanType("individual") //
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }
}
