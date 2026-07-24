package com.teaverse.compensation.service;

import com.teaverse.compensation.dto.request.MoMoCreatePaymentRequest;
import com.teaverse.compensation.dto.response.MoMoCreatePaymentResponse;
import com.teaverse.compensation.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MoMoPaymentClient implements MoMoGatewayClient {
    private final RestClient restClient;
    private final String createUrl;

    public MoMoPaymentClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.momo.create-url}") String createUrl
    ) {
        this.restClient = restClientBuilder.build();
        this.createUrl = createUrl;
    }

    @Override
    public MoMoCreatePaymentResponse createPayment(MoMoCreatePaymentRequest request) {
        try {
            MoMoCreatePaymentResponse response = restClient.post()
                    .uri(createUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(MoMoCreatePaymentResponse.class);
            if (response == null) {
                throw new BadRequestException("MoMo returned an empty create-payment response");
            }
            return response;
        } catch (RestClientResponseException ex) {
            throw new BadRequestException("MoMo create-payment request failed with HTTP " + ex.getStatusCode().value());
        } catch (RestClientException ex) {
            throw new BadRequestException("Unable to connect to the MoMo payment gateway");
        }
    }
}
