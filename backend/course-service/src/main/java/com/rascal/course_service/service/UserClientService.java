package com.rascal.course_service.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.rascal.course_service.dto.response.ApiSuccessResponse;
import com.rascal.course_service.dto.response.UserLookupResponse;

@Service
public class UserClientService {

    @Value("${services.internal_signature}")
    private String internalSignature;
    private WebClient usersWebClient;

    public UserClientService(WebClient usersWebClient) {
        this.usersWebClient = usersWebClient;
    }


    public List<UserLookupResponse> lookupUsersByIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return List.of();

        String ids = userIds.stream()
            .distinct()
            .map(String::valueOf)
            .collect(Collectors.joining(","));

        return this.usersWebClient.get()
            .uri(url -> url
                .path("/lookup")
                .queryParam("ids", ids)
                .build()
            )
            .header("X-User-Id", "0")
            .header("X-User-Roles", "SERVICE")
            .header("X-User-Authorities", "user.lookup")
            .header("X-Internal-Signature", internalSignature)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<ApiSuccessResponse<List<UserLookupResponse>>>() { })
            .map(ApiSuccessResponse::data)
        .block();
    }
    
}
