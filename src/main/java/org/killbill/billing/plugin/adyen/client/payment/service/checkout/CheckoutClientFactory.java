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

package org.killbill.billing.plugin.adyen.client.payment.service.checkout;

import org.killbill.billing.plugin.adyen.client.AdyenConfigProperties;
import org.killbill.billing.plugin.adyen.client.payment.service.AdyenCheckoutApiClient;

public class CheckoutClientFactory {
    private final AdyenConfigProperties adyenConfigProperties;

    public CheckoutClientFactory(final AdyenConfigProperties adyenConfigProperties) {
        this.adyenConfigProperties = adyenConfigProperties;
    }

    public AdyenCheckoutApiClient getCheckoutClient(final String countryCode) {
        final AdyenCheckoutApiClient adyenCheckoutApiClient = new AdyenCheckoutApiClient(adyenConfigProperties, countryCode);
        return adyenCheckoutApiClient;
    }
}
