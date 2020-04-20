/*
 * Copyright 2014-2020 Groupon, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.plugin.adyen.client.payment.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.jooq.tools.StringUtils;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.plugin.adyen.client.model.PaymentData;
import org.killbill.billing.plugin.adyen.client.model.PaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.PaymentServiceProviderResult;
import org.killbill.billing.plugin.adyen.client.model.PurchaseResult;
import org.killbill.billing.plugin.adyen.client.model.UserData;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo;
import org.killbill.billing.plugin.adyen.client.model.paymentinfo.KlarnaPaymentInfo.KlarnaPaymentInfoBuilder;
import org.killbill.billing.plugin.adyen.client.payment.builder.AdyenRequestFactory;
import org.killbill.billing.plugin.adyen.client.payment.service.checkout.CheckoutApiTestHelper;
import org.killbill.billing.plugin.adyen.client.payment.service.checkout.CheckoutClientFactory;
import org.testng.annotations.Test;

import com.adyen.model.ApiError;
import com.adyen.model.checkout.PaymentsDetailsRequest;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.service.exception.ApiException;

import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.REQUEST_NOT_SEND;
import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.RESPONSE_ABOUT_INVALID_REQUEST;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/*
Test class for Klarna Payment using Checkout API with mocks
 */
public class TestAdyenCheckoutKlarnaPayment {

    @Test(groups = "fast")
    public void testAuthoriseKlarnaPayment() throws Exception {
        final PaymentsRequest request = new PaymentsRequest();
        final PaymentsResponse authResponse = CheckoutApiTestHelper.getRedirectShopperResponse();
        final AdyenCallResult<PaymentsResponse> authResult = new SuccessfulAdyenCall<PaymentsResponse>(authResponse, 100);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final KlarnaPaymentInfo paymentInfo = new KlarnaPaymentInfoBuilder()
                .setCountryCode("DE")
                .setMerchantAccount(merchantAccount)
                .build();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR, UUID.randomUUID().toString(), paymentInfo);

        final AdyenCheckoutApiClient checkoutApiClient = mock(AdyenCheckoutApiClient.class);
        when(checkoutApiClient.createPayment(request)).thenReturn(authResult);

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.createKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final CheckoutClientFactory checkoutClientFactory = mock(CheckoutClientFactory.class);
        when(checkoutClientFactory.getCheckoutClient("DE")).thenReturn(checkoutApiClient);

        final AdyenPaymentServiceProviderPort adyenPort = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, checkoutClientFactory);

        PurchaseResult result = adyenPort.authoriseKlarnaPayment(false, merchantAccount, paymentData, userData);
        assertEquals(result.getResultCode(), "RedirectShopper");
        assertTrue(result.getAdditionalData().size() > 0);

        Map<String, String> additionalData = result.getAdditionalData();
        assertEquals(additionalData.get("paymentData"), CheckoutApiTestHelper.PAYMENT_DATA);
        assertEquals(additionalData.get("paymentMethod"), "klarna");
        assertEquals(additionalData.get("formUrl"), CheckoutApiTestHelper.URL);
        assertEquals(additionalData.get("formMethod"), "GET");
        assertFalse(StringUtils.isEmpty(additionalData.get("resultKeys")));
        assertEquals(additionalData.get("merchantAccountCode"), "TestAccount");
        assertEquals(result.getPaymentTransactionExternalKey(), paymentData.getPaymentTransactionExternalKey());
    }

    @Test(groups = "fast")
    public void testCompleteAuthoriseKlarna() throws Exception {
        final PaymentsDetailsRequest request = new PaymentsDetailsRequest();
        final PaymentsResponse authResponse = CheckoutApiTestHelper.getAuthorisedResponse();
        final AdyenCallResult<PaymentsResponse> authResult = new SuccessfulAdyenCall<PaymentsResponse>(authResponse, 100);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final KlarnaPaymentInfo paymentInfo = new KlarnaPaymentInfoBuilder()
                .setCountryCode("DE")
                .setMerchantAccount(merchantAccount)
                .build();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR, UUID.randomUUID().toString(), paymentInfo);

        final AdyenCheckoutApiClient checkoutApiClient = mock(AdyenCheckoutApiClient.class);
        when(checkoutApiClient.paymentDetails(request)).thenReturn(authResult);

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.completeKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final CheckoutClientFactory checkoutClientFactory = mock(CheckoutClientFactory.class);
        when(checkoutClientFactory.getCheckoutClient("DE")).thenReturn(checkoutApiClient);

        final AdyenPaymentServiceProviderPort adyenPort = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, checkoutClientFactory);

        final PurchaseResult result = adyenPort.authoriseKlarnaPayment(true, merchantAccount, paymentData, userData);
        assertEquals(result.getResultCode(), "Authorised");
        assertEquals(result.getPspReference(), CheckoutApiTestHelper.PSP_REFERENCE);
        Map<String, String> additionalData = result.getAdditionalData();
        assertEquals(additionalData.get("merchantAccountCode"), "TestAccount");
        assertEquals(result.getPaymentTransactionExternalKey(), paymentData.getPaymentTransactionExternalKey());
    }

    @Test(groups = "fast")
    public void testAuthoriseKlarnaApiError() throws Exception {
        final PaymentsRequest request = new PaymentsRequest();
        final ApiException exception = new ApiException("API exception", 411);
        final ApiError error = new ApiError();
        error.setMessage("Invalid payload");
        error.setPspReference("ABCDE6789FG");
        exception.setError(error);
        final AdyenCallResult callResult = new FailedCheckoutApiCall(
                RESPONSE_ABOUT_INVALID_REQUEST, exception, exception);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final KlarnaPaymentInfo paymentInfo = new KlarnaPaymentInfoBuilder()
                .setCountryCode("DE")
                .setMerchantAccount(merchantAccount)
                .build();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR, UUID.randomUUID().toString(), paymentInfo);

        final AdyenCheckoutApiClient checkoutApiClient = mock(AdyenCheckoutApiClient.class);
        when(checkoutApiClient.createPayment(request)).thenReturn(callResult);

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.createKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final CheckoutClientFactory checkoutClientFactory = mock(CheckoutClientFactory.class);
        when(checkoutClientFactory.getCheckoutClient("DE")).thenReturn(checkoutApiClient);

        final AdyenPaymentServiceProviderPort adyenPort = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, checkoutClientFactory);
        final PurchaseResult result = adyenPort.authoriseKlarnaPayment(false, merchantAccount, paymentData, userData);
        assertTrue(result.getResult().isPresent());
        assertEquals(result.getResult().get(), PaymentServiceProviderResult.ERROR);
        assertNull(result.getResultCode());
        assertEquals(result.getPspReference(), "ABCDE6789FG");
        assertEquals(result.getReason(), "Invalid payload");
    }

    @Test(groups = "fast")
    public void testAuthoriseKlarnaIOError() throws Exception {
        final PaymentsRequest request = new PaymentsRequest();
        final IOException exception = new IOException("Network error");
        final AdyenCallResult callResult = new FailedCheckoutApiCall(
                REQUEST_NOT_SEND, exception, exception);

        final String merchantAccount = "TestAccount";
        final UserData userData = new UserData();
        final KlarnaPaymentInfo paymentInfo = new KlarnaPaymentInfoBuilder()
                .setCountryCode("DE")
                .setMerchantAccount(merchantAccount)
                .build();
        final PaymentData paymentData = new PaymentData<PaymentInfo>(
                BigDecimal.TEN, Currency.EUR, UUID.randomUUID().toString(), paymentInfo);

        final AdyenCheckoutApiClient checkoutApiClient = mock(AdyenCheckoutApiClient.class);
        when(checkoutApiClient.createPayment(request)).thenReturn(callResult);

        final AdyenRequestFactory adyenRequestFactory = mock(AdyenRequestFactory.class);
        when(adyenRequestFactory.createKlarnaPayment(merchantAccount, paymentData, userData)).thenReturn(request);

        final CheckoutClientFactory checkoutClientFactory = mock(CheckoutClientFactory.class);
        when(checkoutClientFactory.getCheckoutClient("DE")).thenReturn(checkoutApiClient);

        final AdyenPaymentServiceProviderPort adyenPort = new AdyenPaymentServiceProviderPort(
                adyenRequestFactory, null, checkoutClientFactory);
        final PurchaseResult result = adyenPort.authoriseKlarnaPayment(false, merchantAccount, paymentData, userData);
        assertTrue(result.getResult().isPresent());
        assertEquals(result.getResult().get(), PaymentServiceProviderResult.ERROR);
        assertNull(result.getResultCode());
        assertEquals(result.getReason(), "Network error");
    }
}
