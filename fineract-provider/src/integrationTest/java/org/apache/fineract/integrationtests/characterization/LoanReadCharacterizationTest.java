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
//   target: fineract-provider/src/main/java/org/apache/fineract/portfolio/loanaccount/service/LoanReadPlatformServiceImpl.java
//   run_id: 000-modernization-roadmap-strangler-loan-read-service-step-1
//   strategies: none (deterministic fields only — fixed past dates, principal/interest/currency/status; no auto-ids or today-relative values asserted)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.fineract.integrationtests.common.ClientHelper;
import org.apache.fineract.integrationtests.common.Utils;
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
 * Characterization (golden-master) test for the loan READ path served by
 * {@code LoanReadPlatformServiceImpl}. Boots the real application, creates a
 * deterministic loan with FIXED PAST DATES, disburses it, then reads it back
 * via the REST API and pins the currently-observed read output.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanReadCharacterizationTest {

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;
    private LoanTransactionHelper loanTransactionHelper;

    @Before
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
        this.loanTransactionHelper = new LoanTransactionHelper(this.requestSpec, this.responseSpec);
    }

    @Test
    public void pinLoanReadAfterDisbursement() {
        // Client joining date must precede the loan submission/disbursement dates below.
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, "01 January 2011");
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);

        final Integer loanProductID = createLoanProduct();
        final Integer loanID = applyForLoanApplication(clientID, loanProductID, "12,000.00");

        // Pin pending status as read from the loan read service.
        final HashMap pendingStatus = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        assertEquals("Loan pending-approval flag (read service)", Boolean.TRUE, pendingStatus.get("pendingApproval"));

        this.loanTransactionHelper.approveLoan("20 September 2011", loanID);
        this.loanTransactionHelper.disburseLoan("20 September 2011", loanID);

        // Pin active status as read from the loan read service.
        final HashMap activeStatus = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        assertEquals("Loan active flag (read service)", Boolean.TRUE, activeStatus.get("active"));

        // READ the repayment schedule assembled by LoanReadPlatformServiceImpl and pin observed values.
        final ArrayList<HashMap> loanSchedule = this.loanTransactionHelper.getLoanRepaymentSchedule(this.requestSpec, this.responseSpec,
                loanID);

        // Schedule period count: 1 disbursal period (index 0) + 4 repayment periods.
        assertEquals("Schedule period count", 5, loanSchedule.size());

        // Period 1.
        assertEquals("Period 1 dueDate", new ArrayList<>(Arrays.asList(2011, 10, 20)), loanSchedule.get(1).get("dueDate"));
        assertEquals("Period 1 principalDue", new Float("2911.49"), loanSchedule.get(1).get("principalOriginalDue"));
        assertEquals("Period 1 interestDue", new Float("240.00"), loanSchedule.get(1).get("interestOriginalDue"));

        // Period 2.
        assertEquals("Period 2 dueDate", new ArrayList<>(Arrays.asList(2011, 11, 20)), loanSchedule.get(2).get("dueDate"));
        assertEquals("Period 2 principalDue", new Float("2969.72"), loanSchedule.get(2).get("principalDue"));
        assertEquals("Period 2 interestDue", new Float("181.77"), loanSchedule.get(2).get("interestOriginalDue"));

        // Period 3.
        assertEquals("Period 3 dueDate", new ArrayList<>(Arrays.asList(2011, 12, 20)), loanSchedule.get(3).get("dueDate"));
        assertEquals("Period 3 principalDue", new Float("3029.11"), loanSchedule.get(3).get("principalDue"));
        assertEquals("Period 3 interestDue", new Float("122.38"), loanSchedule.get(3).get("interestOriginalDue"));

        // Period 4 (final).
        assertEquals("Period 4 dueDate", new ArrayList<>(Arrays.asList(2012, 1, 20)), loanSchedule.get(4).get("dueDate"));
        assertEquals("Period 4 principalDue", new Float("3089.68"), loanSchedule.get(4).get("principalDue"));
        assertEquals("Period 4 interestDue", new Float("61.79"), loanSchedule.get(4).get("interestOriginalDue"));

        // READ the loan summary assembled by LoanReadPlatformServiceImpl and pin observed totals.
        final HashMap summary = this.loanTransactionHelper.getLoanSummary(this.requestSpec, this.responseSpec, loanID);
        assertEquals("Summary principalDisbursed", new Float("12000.00"), summary.get("principalDisbursed"));
        assertEquals("Summary totalExpectedRepayment", new Float("12605.94"), summary.get("totalExpectedRepayment"));

        // Pin the currency block as read from the loan read service.
        final HashMap currency = (HashMap) summary.get("currency");
        assertEquals("Summary currency code", "USD", currency.get("code"));
    }

    private Integer createLoanProduct() {
        final String loanProductJSON = new LoanProductTestBuilder() //
                .withPrincipal("12,000.00") //
                .withNumberOfRepayments("4") //
                .withRepaymentAfterEvery("1") //
                .withRepaymentTypeAsMonth() //
                .withinterestRatePerPeriod("1") //
                .withInterestRateFrequencyTypeAsMonths() //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .build(null);
        return this.loanTransactionHelper.getLoanProductId(loanProductJSON);
    }

    private Integer applyForLoanApplication(final Integer clientID, final Integer loanProductID, final String principal) {
        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal(principal) //
                .withLoanTermFrequency("4") //
                .withLoanTermFrequencyAsMonths() //
                .withNumberOfRepayments("4") //
                .withRepaymentEveryAfter("1") //
                .withRepaymentFrequencyTypeAsMonths() //
                .withInterestRatePerPeriod("2") //
                .withAmortizationTypeAsEqualInstallments() //
                .withInterestTypeAsDecliningBalance() //
                .withInterestCalculationPeriodTypeSameAsRepaymentPeriod() //
                .withExpectedDisbursementDate("20 September 2011") //
                .withSubmittedOnDate("20 September 2011") //
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }
}
