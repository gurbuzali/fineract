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
//   strategies: none (deterministic — fixed past dates, observed HTTP status / pinned schedule fields)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 — edge-cases (strict superset); inspection-fallback (no branch coverage)

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
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
 * Edge-case characterization (golden-master) tests for the loan READ path served
 * by {@code LoanReadPlatformServiceImpl}. Strict superset of the happy-path
 * {@code LoanReadCharacterizationTest}: pins the OBSERVED behaviour on two edges
 * the happy test never exercises — an error-path read of a non-existent loan id,
 * and a boundary-literal read of an APPROVED-but-not-yet-disbursed loan whose
 * transactions collection is observed empty.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanReadEdgesCharacterizationTest {

    private static final String DATE_OF_JOINING = "01 January 2011";
    private static final String LOAN_DATE = "20 September 2011";

    // A loan id that does not exist; the read path raises LoanNotFoundException.
    private static final Integer NON_EXISTENT_LOAN_ID = 99999999;

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

    /**
     * Edge category: error-path. GET a loan id that does not exist and pin the
     * OBSERVED HTTP status (404) plus the globalisation error code emitted by the
     * not-found mapper. The status code was observed at generation time, not
     * assumed; the matching ResponseSpecification asserts exactly that code.
     */
    @Test
    public void pinLoanReadNonExistentIdIsNotFound() {
        final ResponseSpecification notFoundSpec = new ResponseSpecBuilder().expectStatusCode(404).build();

        final String url = "/fineract-provider/api/v1/loans/" + NON_EXISTENT_LOAN_ID + "?associations=all&" + Utils.TENANT_IDENTIFIER;

        // Pin the OBSERVED 404 body. Top-level userMessageGlobalisationCode is the generic
        // resource-not-found code; the per-resource loan code is nested under errors[0].
        final String globalCode = Utils.performServerGet(this.requestSpec, notFoundSpec, url, "userMessageGlobalisationCode");
        assertEquals("Not-found global code (read service error path)", "error.msg.resource.not.found", globalCode);

        final ArrayList<HashMap> errors = Utils.performServerGet(this.requestSpec, notFoundSpec, url, "errors");
        assertEquals("Loan not-found resource code (read service error path)", "error.msg.loan.id.invalid",
                errors.get(0).get("userMessageGlobalisationCode"));
    }

    /**
     * Edge category: nullable-input. Read a loan that has been APPROVED but NOT
     * yet disbursed. OBSERVED at generation time: the loan read serialization
     * OMITS the transactions field entirely in this pre-disbursal state (the
     * attribute resolves to null, not an empty array). This pins a read state the
     * happy path — which always disburses first — never observes.
     */
    @Test
    public void pinApprovedLoanReadHasNullTransactions() {
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, DATE_OF_JOINING);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);

        final Integer loanProductID = createLoanProduct();
        final Integer loanID = applyForLoanApplication(clientID, loanProductID, "12,000.00");

        this.loanTransactionHelper.approveLoan(LOAN_DATE, loanID);

        // Pin approved-not-disbursed status as read from the loan read service.
        final HashMap approvedStatus = LoanStatusChecker.getStatusOfLoan(this.requestSpec, this.responseSpec, loanID);
        assertEquals("Loan approved flag (read service)", Boolean.TRUE, approvedStatus.get("waitingForDisbursal"));

        // READ the loan back. OBSERVED: no transactions collection is serialized pre-disbursal, so the
        // transactions attribute is absent (null) rather than an empty array.
        final ArrayList<HashMap> transactions = (ArrayList<HashMap>) this.loanTransactionHelper.getLoanDetail(this.requestSpec,
                this.responseSpec, loanID, "transactions");
        assertNull("Transactions attribute absent before disbursal (read service)", transactions);
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
                .withExpectedDisbursementDate(LOAN_DATE) //
                .withSubmittedOnDate(LOAN_DATE) //
                .build(clientID.toString(), loanProductID.toString(), null);
        return this.loanTransactionHelper.getLoanId(loanApplicationJSON);
    }
}
