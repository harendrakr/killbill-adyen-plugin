package org.killbill.billing.plugin.adyen.client.payment.service.checkout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.killbill.billing.plugin.adyen.api.AdyenPaymentPluginApi;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.testng.annotations.Test;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class TestCheckoutPaymentsResult {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test(groups = "fast")
    public void testToPurchaseResult() throws Exception {
        final String paymentData = "AbcdefghijklmnopqrstuvwxyZ#1234567890";
        final Map<String,String> authCompleteKeys= ImmutableMap.<String,String>builder()
                .put("key1", "text")
                .put("key2", "blob")
                .build();
        final Map<String,String> formParameters= ImmutableMap.<String,String>builder()
                .put("ref1", "ABCDEF12345")
                .put("ref2", "xyz@89880087")
                .build();

        final CheckoutPaymentsResult paymentsResult = new CheckoutPaymentsResult();
        paymentsResult.setFormMethod("POST");
        paymentsResult.setFormUrl("http://company.com/callback");
        paymentsResult.setResultCode("RedirectShopper");
        paymentsResult.setPaymentMethod("klarna_payment");
        paymentsResult.setPspReference("1234567890");
        paymentsResult.setPaymentData(paymentData);
        paymentsResult.setAuthResultKeys(authCompleteKeys);
        paymentsResult.setFormParameter(formParameters);
        paymentsResult.setMerchantAccount("MERCHANT_ACCOUNT");

        final PurchaseResult result = paymentsResult.toPurchaseResult();
        assertEquals(result.getResultCode().toString(), "RedirectShopper");
        assertEquals(result.getPspReference(), "1234567890");

        //rest of the fields go in additional data
        final Map<String, String> additionalData = result.getAdditionalData();
        assertEquals(additionalData.get("formMethod"), "POST");
        assertEquals(additionalData.get("formUrl"), "http://company.com/callback");
        assertEquals(additionalData.get("paymentMethod"), "klarna_payment");
        assertEquals(additionalData.get("paymentData"), paymentData);
        assertEquals(additionalData.get(AdyenPaymentPluginApi.PROPERTY_MERCHANT_ACCOUNT_CODE), "MERCHANT_ACCOUNT");

        //result keys
        final Map authKeyNames = mapper.readValue(additionalData.get("resultKeys"), Map.class);
        assertEquals(authKeyNames.get("key1"), "text");
        assertEquals(authKeyNames.get("key2"), "blob");

        //result keys
        final Map formParams = mapper.readValue(additionalData.get("formParameter"), Map.class);
        assertEquals(formParams.get("ref1"), "ABCDEF12345");
        assertEquals(formParams.get("ref2"), "xyz@89880087");
    }

    @Test(groups = "fast")
    public void testEmptyToPurchaseResult() throws Exception {
        final CheckoutPaymentsResult paymentsResult = new CheckoutPaymentsResult();
        final PurchaseResult result = paymentsResult.toPurchaseResult();
        assertTrue(result.getResult().isPresent());
        assertEquals(result.getResult().get(), PaymentServiceProviderResult.ERROR);
        assertNull(result.getResultCode());
        assertNull(result.getReason());
        assertNull(result.getPspReference());
        assertNull(result.getFormUrl());
        assertTrue(result.getFormParameter().isEmpty());
        assertTrue(result.getAdditionalData().isEmpty());
    }
}
