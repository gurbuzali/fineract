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
//   target: fineract-provider/src/main/java/org/apache/fineract/portfolio/loanaccount/loanschedule/domain/AbstractLoanScheduleGenerator.java
//   run_id: 000-modernization-roadmap-bba-loan-schedule-generator-step-1
//   strategies: none (deterministic fields only — fixed past disbursement date, principal/interest/currency/period-count; no auto-ids or today-relative values asserted)
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
import org.apache.fineract.integrationtests.common.loans.LoanTransactionHelper;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

/**
 * Characterization (golden-master) test for the loan-schedule generation driven
 * by {@code AbstractLoanScheduleGenerator#generate()}. Boots the real
 * application and POSTs {@code command=calculateLoanSchedule}, which computes a
 * repayment schedule WITHOUT persisting a loan, then pins the currently-observed
 * computed schedule (FIXED PAST disbursement date keeps it deterministic).
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanScheduleCalculationCharacterizationTest {

    private static final String CALCULATE_SCHEDULE_URL = "/fineract-provider/api/v1/loans?command=calculateLoanSchedule&"
            + Utils.TENANT_IDENTIFIER;

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
    public void pinCalculatedLoanSchedule() {
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, "01 January 2012");
        final Integer loanProductID = createLoanProduct();

        final String loanApplicationJSON = new LoanApplicationTestBuilder() //
                .withPrincipal("12,000.00") //
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

        // Drive AbstractLoanScheduleGenerator.generate() via the non-persisting calculate endpoint.
        final HashMap response = Utils.performServerPost(this.requestSpec, this.responseSpec, CALCULATE_SCHEDULE_URL, loanApplicationJSON,
                "");
        final ArrayList<HashMap> periods = (ArrayList<HashMap>) response.get("periods");

        // Pin currency block of the computed schedule.
        final HashMap currency = (HashMap) response.get("currency");
        assertEquals("Computed schedule currency code", "USD", currency.get("code"));

        // Period count: 1 disbursal period (index 0) + 4 repayment periods.
        assertEquals("Computed schedule period count", 5, periods.size());

        // Period 1.
        assertEquals("Period 1 dueDate", new ArrayList<>(Arrays.asList(2011, 10, 20)), periods.get(1).get("dueDate"));
        assertEquals("Period 1 principalDue", new Float("2911.49"), periods.get(1).get("principalDue"));
        assertEquals("Period 1 interestDue", new Float("240.00"), periods.get(1).get("interestDue"));

        // Period 2.
        assertEquals("Period 2 dueDate", new ArrayList<>(Arrays.asList(2011, 11, 20)), periods.get(2).get("dueDate"));
        assertEquals("Period 2 principalDue", new Float("2969.72"), periods.get(2).get("principalDue"));
        assertEquals("Period 2 interestDue", new Float("181.77"), periods.get(2).get("interestDue"));

        // Period 3.
        assertEquals("Period 3 dueDate", new ArrayList<>(Arrays.asList(2011, 12, 20)), periods.get(3).get("dueDate"));
        assertEquals("Period 3 principalDue", new Float("3029.11"), periods.get(3).get("principalDue"));
        assertEquals("Period 3 interestDue", new Float("122.38"), periods.get(3).get("interestDue"));

        // Period 4 (final).
        assertEquals("Period 4 dueDate", new ArrayList<>(Arrays.asList(2012, 1, 20)), periods.get(4).get("dueDate"));
        assertEquals("Period 4 principalDue", new Float("3089.68"), periods.get(4).get("principalDue"));
        assertEquals("Period 4 interestDue", new Float("61.79"), periods.get(4).get("interestDue"));
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
}
