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
//   target: fineract-provider/src/main/java/org/apache/fineract/accounting/journalentry/service/JournalEntryReadPlatformServiceImpl.java
//   run_id: 000-modernization-roadmap-acl-accounting-portfolio-step-1
//   strategies: none (deterministic — fixed past dates, observed HTTP status / pinned schedule fields)
//   pin-behavior: This test pins observed values, not intended values. A green test means "no observable change since generation"; it does NOT mean "the code is correct".
// acceptance: AC-006 — edge-cases (strict superset); inspection-fallback (no branch coverage)

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.fineract.integrationtests.common.Utils;
import org.junit.Before;
import org.junit.Test;

import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.builder.ResponseSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import com.jayway.restassured.specification.ResponseSpecification;

/**
 * Edge-case characterization (golden-master) test for the journal-entry READ path
 * served by {@code JournalEntryReadPlatformServiceImpl#retrieveGLJournalEntryById}.
 * Strict superset of the happy-path {@code JournalEntryAccountingCharacterizationTest}
 * (which pins disbursal GL postings): this pins the OBSERVED error-path behaviour
 * when a single journal entry is read by a non-existent id — a state the happy
 * path never exercises.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class JournalEntryAccountingEdgesCharacterizationTest {

    // A journal-entry id that does not exist; retrieveGLJournalEntryById raises
    // JournalEntriesNotFoundException on the empty JDBC result.
    private static final Long NON_EXISTENT_JOURNAL_ENTRY_ID = 99999999L;

    private ResponseSpecification responseSpec;
    private RequestSpecification requestSpec;

    @Before
    public void setup() {
        Utils.initializeRESTAssured();
        this.requestSpec = new RequestSpecBuilder().setContentType(ContentType.JSON).build();
        this.requestSpec.header("Authorization", "Basic " + Utils.loginIntoServerAndGetBase64EncodedAuthenticationKey());
        this.responseSpec = new ResponseSpecBuilder().expectStatusCode(200).build();
    }

    /**
     * Edge category: error-path. GET a single journal entry by a non-existent id
     * and pin the OBSERVED HTTP status (404) plus the globalisation error code
     * emitted by the not-found mapper. The status code was observed at generation
     * time, not assumed; the matching ResponseSpecification asserts exactly it.
     */
    @Test
    public void pinJournalEntryReadNonExistentIdIsNotFound() {
        final ResponseSpecification notFoundSpec = new ResponseSpecBuilder().expectStatusCode(404).build();

        final String url = "/fineract-provider/api/v1/journalentries/" + NON_EXISTENT_JOURNAL_ENTRY_ID + "?" + Utils.TENANT_IDENTIFIER;

        // Pin the OBSERVED 404 body. Top-level userMessageGlobalisationCode is the generic
        // resource-not-found code; the per-resource journal-entry code is nested under errors[0].
        final String globalCode = Utils.performServerGet(this.requestSpec, notFoundSpec, url, "userMessageGlobalisationCode");
        assertEquals("Not-found global code (journal-entry read error path)", "error.msg.resource.not.found", globalCode);

        final ArrayList<HashMap> errors = Utils.performServerGet(this.requestSpec, notFoundSpec, url, "errors");
        assertEquals("Journal-entry not-found resource code (read error path)", "error.msg.journalEntries.id.invalid",
                errors.get(0).get("userMessageGlobalisationCode"));
    }
}
