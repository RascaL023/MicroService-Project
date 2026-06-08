package com.rascal.course_service.service;

import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import id.rascal.response_kit.exception.BadRequestException;

@Service
public class CurrentUserService {

    public Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) 
            throw new AccessDeniedException("Invalid authorization data");

        try { return Long.parseLong(auth.getName()); } 
        catch (NumberFormatException e) { throw new BadRequestException("Invalid User ID"); }
    }

    public boolean hasAnyAuthority(String ...authorities) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;

        return auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(Set.of(authorities)::contains);
    }
    
}
