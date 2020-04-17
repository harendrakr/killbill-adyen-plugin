package org.killbill.billing.plugin.adyen.client.payment.service;

import com.adyen.Client;
import com.adyen.enums.Environment;
import com.adyen.model.checkout.PaymentsDetailsRequest;
import com.adyen.model.checkout.PaymentsRequest;
import com.adyen.model.checkout.PaymentsResponse;
import com.adyen.service.Checkout;
import com.adyen.service.exception.ApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.jooq.tools.StringUtils;
import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;

import static org.killbill.billing.plugin.adyen.client.payment.service.AdyenCallErrorStatus.*;


public class AdyenCheckoutApiClient {
    final Checkout checkoutApi;
    private static final Logger logger = LoggerFactory.getLogger(AdyenCheckoutApiClient.class);
    private static final ObjectMapper mapper = new ObjectMapper();


    public AdyenCheckoutApiClient(final AdyenConfigProperties adyenConfigProperties, final String countryCode) {
        // initialize the REST client here
        Environment environment = Environment.TEST; //default Adyen environmnet
        String envProperty = adyenConfigProperties.getEnvironment();
        if(!StringUtils.isEmpty(envProperty) && envProperty.equals("LIVE")) {
            environment = Environment.LIVE;
        }

        //default API key KEY_NOT_FOUND, log it
        final String apiKey = adyenConfigProperties.getApiKey(countryCode);
        final Client client = new Client(apiKey, environment);
        checkoutApi = new Checkout(client);
    }

    public AdyenCallResult<PaymentsResponse> createPayment(PaymentsRequest request) {
        final AdyenCallResult<PaymentsResponse> result;
        result = callApi(request, new ApiRequest<PaymentsRequest, PaymentsResponse>() {
            @Override
            public PaymentsResponse call() throws ApiException, IOException {
                final PaymentsResponse response = checkoutApi.payments(request);
                logResponse(response);
                return response;
            }
        });

        return result;
    }

    private void logResponse(final PaymentsResponse response) {
        final StringBuilder responseBuilder = new StringBuilder();
        final String result;
        if(response != null) {
            result = "SUCCESS";
            responseBuilder.append("ResultCode:").append(response.getResultCode()).append("\n")
                           .append("PspReference:").append(response.getPspReference()).append("\n")
                           .append("MerchantReference").append(response.getMerchantReference()).append("\n")
                           .append("RefusalReason:").append(response.getRefusalReason()).append("\n")
                           .append("RefusalCode:").append(response.getResultCode()).append("\n");
            if (response.getDetails() != null) {
                responseBuilder.append("Details:").append(response.getDetails().toString()).append("\n");
            }
            if (response.getAction() != null) {
                responseBuilder.append("Action.type:").append(response.getAction().getType()).append("\n")
                               .append("Action.method:").append(response.getAction().getMethod()).append("\n")
                               .append("Action.paymentType:").append(response.getAction().getPaymentMethodType());
            }
        } else {
            result = "FAILED";
            responseBuilder.append("No response received");
        }
        final String responseLog = responseBuilder.toString();
        logger.info("Checkout API {}:\n{}", result, responseLog);
    }

    public AdyenCallResult<PaymentsResponse> paymentDetails(PaymentsDetailsRequest request) {
        final AdyenCallResult<PaymentsResponse> result;
        result = callApi(request, new ApiRequest<PaymentsDetailsRequest, PaymentsResponse>() {
            @Override
            public PaymentsResponse call() throws ApiException, IOException {
                final PaymentsResponse response = checkoutApi.paymentsDetails(request);
                logResponse(response);
                return response;
            }
        });

        return result;
    }

    private <REQ, RES> AdyenCallResult<RES> callApi(REQ request, ApiRequest<REQ, RES> apiRequest) {
        final String logRequest = jsonObject(request);
        logger.info("Checkout API request: \n\n" + logRequest);

        final long startTime = System.currentTimeMillis();
        try {
            final RES result = apiRequest.call();
            final long duration = System.currentTimeMillis() - startTime;
            logger.info("Checkout call duration: "+ duration);
            return new SuccessfulAdyenCall<RES>(result, duration);
        } catch (ApiException ex) {
            final long duration = System.currentTimeMillis() - startTime;
            logger.error("Checkout API exception: \n{}\n", ex.toString());
            return handleException(ex, duration);
        } catch (IOException ex) {
            final long duration = System.currentTimeMillis() - startTime;
            logger.error("Checkout API exception: \n{}\n", ex.toString());
            return handleException(ex, duration);
        }
    }

    private <T> String jsonObject(T logObject) {
        String resultStr;
        try {
            resultStr = mapper.writeValueAsString(logObject);
        } catch (IOException e) {
            logger.info("Unable to convert log object to JSON");
            resultStr = logObject.toString();
        }

        return resultStr;
    }

    private <T> UnSuccessfulAdyenCall<T> handleException(final Exception ex, final long duration) {
        final Throwable rootCause = Throwables.getRootCause(ex);
        logger.info("Checkout API duration="+ duration +" response=exception");
        logger.error("Error sending request: {}", rootCause.getMessage());
        if(ex instanceof ApiException) {
            return new FailedCheckoutApiCall<T>(RESPONSE_ABOUT_INVALID_REQUEST, rootCause, ex);
        } else {
            return new FailedCheckoutApiCall<T>(REQUEST_NOT_SEND, rootCause, ex);
        }
    }

    private interface ApiRequest<REQ, RES> {
        RES call() throws ApiException, IOException;
    }
}
