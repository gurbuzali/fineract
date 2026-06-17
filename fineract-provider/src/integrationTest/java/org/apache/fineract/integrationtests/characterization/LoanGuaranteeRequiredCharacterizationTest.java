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
//   target: fineract-provider/src/main/java/org/apache/fineract/portfolio/loanaccount/service/LoanReadPlatformServiceImpl.java (isGuaranteeRequired slice)
//   run_id: 000-modernization-roadmap-strangler-loan-read-service-slice-isguaranteerequired
//   strategies: none (deterministic — default loan product hold_guarantee_funds=false; observed absence of accountLinkingOptions)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 / AC-007 — strangler slice characterization (isGuaranteeRequired via GET guarantors/accounts/template)

import static org.junit.Assert.assertNull;

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
 * Characterization (golden-master) test for the {@code isGuaranteeRequired} READ slice of
 * {@code LoanReadPlatformServiceImpl}, the slice the strangler-fig transformation migrates to
 * {@code LoanGuaranteeReadServiceImpl} behind the {@code LoanReadPlatformServiceRoutingImpl} facade.
 *
 * <p>
 * The slice has exactly one consumer &mdash; {@code GuarantorsApiResource.accountsTemplate}
 * (<code>GET /loans/{loanId}/guarantors/accounts/template</code>), which calls
 * {@code isGuaranteeRequired(loanId)} and only populates {@code accountLinkingOptions} when it
 * returns {@code true}. A loan on the default product (<code>hold_guarantee_funds = 0</code>) yields
 * {@code false}, so the OBSERVED template OMITS {@code accountLinkingOptions}. This pins that
 * observed behaviour: it stays green when the slice is route-flipped to the new implementation
 * (identical query) and would fail if the new path threw (HTTP 500) or regressed the boolean
 * (the attribute would appear).
 * </p>
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class LoanGuaranteeRequiredCharacterizationTest {

    private static final String DATE_OF_JOINING = "01 January 2011";
    private static final String LOAN_DATE = "20 September 2011";

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
     * Edge category: nullable-input / boundary-literal. Read the guarantor account-linking template
     * for a loan on the default product (guarantee NOT required). OBSERVED at generation time: the
     * read path's {@code isGuaranteeRequired} returns false, so {@code accountLinkingOptions} is
     * absent (null) from the serialized template rather than an empty array.
     */
    @Test
    public void pinGuaranteeNotRequiredForDefaultProductHasNoAccountLinkingOptions() {
        final Integer clientID = ClientHelper.createClient(this.requestSpec, this.responseSpec, DATE_OF_JOINING);
        ClientHelper.verifyClientCreatedOnServer(this.requestSpec, this.responseSpec, clientID);

        final Integer loanProductID = createLoanProduct();
        final Integer loanID = applyForLoanApplication(clientID, loanProductID, "12,000.00");

        // READ the guarantor account-linking template; the default product has hold_guarantee_funds=0, so
        // the read service's isGuaranteeRequired(loanId) is false and accountLinkingOptions is omitted.
        final String url = "/fineract-provider/api/v1/loans/" + loanID + "/guarantors/accounts/template?clientId=" + clientID + "&"
                + Utils.TENANT_IDENTIFIER;
        final Object accountLinkingOptions = Utils.performServerGet(this.requestSpec, this.responseSpec, url, "accountLinkingOptions");
        assertNull("accountLinkingOptions absent when guarantee not required (read service isGuaranteeRequired=false)",
                accountLinkingOptions);
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
