package com.mypolicy.matching.service;

import com.mypolicy.matching.client.CustomerClient;
import com.mypolicy.matching.client.PolicyClient; // Added missing import
import com.mypolicy.matching.dto.CustomerDTO;
import com.mypolicy.matching.dto.PolicyDTO; // Added missing import
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

// CRITICAL FIX: Changed 'jdk.internal' verify to Mockito verify
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test") // Ensures H2 is used
public class MatchingEngineH2Test {

    @Autowired
    private MatchingService matchingService;

    @MockBean
    private CustomerClient customerClient;

    @MockBean
    private PolicyClient policyClient; // FIX: Added this to resolve 'policyClient' symbol

    @Test
    public void testFuzzyMatchFailsForDifferentNames() {
        boolean result = matchingService.isSimilar("Rahul", "Subham");
        System.out.println("Negative Test - Is Match: " + result);
        assertFalse(result, "Logic should NOT match Rahul with Subham");
    }

    @Test
    public void testFullStitchingFlow() {
        // Setup Mock Customer
        CustomerDTO mockCustomer = new CustomerDTO();
        mockCustomer.setCustomerId("CUST-101");
        mockCustomer.setFirstName("Subham");
        mockCustomer.setLastName("Dutta");
        mockCustomer.setMobileNumber("9876543210");

        // Mock the search
        when(customerClient.searchByMobile("9876543210")).thenReturn(Optional.of(mockCustomer));

        // Input data with typo
        Map<String, Object> input = new HashMap<>();
        input.put("firstName", "Subam");
        input.put("lastName", "Dutta");
        input.put("mobileNumber", "9876543210");
        input.put("policyNumber", "POL-888");
        input.put("premiumAmount", new BigDecimal("5000"));

        // Execute Stitching
        matchingService.processAndMatchPolicy(input);

        // FIX: Explicitly cast to PolicyDTO in the lambda to avoid 'T' resolution errors
        verify(policyClient).createPolicy(argThat((PolicyDTO policy) ->
                "CUST-101".equals(policy.getCustomerId()) &&
                        "POL-888".equals(policy.getPolicyNumber())
        ));
    }

    @Test
    public void testFuzzyMatchWithMockData() {
        CustomerDTO mockCustomer = new CustomerDTO();
        mockCustomer.setCustomerId("CUST-LOCAL-01");
        mockCustomer.setFirstName("Subham");
        mockCustomer.setLastName("Dutta");

        when(customerClient.getCustomerById(anyString())).thenReturn(mockCustomer);

        String csvFirstName = "Subam";
        String dbFirstName = "Subham";

        boolean result = matchingService.isSimilar(csvFirstName, dbFirstName);

        System.out.println("Local H2 Test - Is Match: " + result);
        assertTrue(result, "Logic should bridge the gap between Subam and Subham");
    }
}