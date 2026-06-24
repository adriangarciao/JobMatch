package com.adriangarciao.jobmatch.model;

import org.springframework.stereotype.Component;
import com.adriangarciao.jobmatch.security.AppPrincipal;

@Component("authz")
public class Authz {
    public boolean isAdmin(AppPrincipal p) {
        return p != null && "ADMIN".equalsIgnoreCase(p.role());
    }
    public boolean isSelfOrAdmin(Long pathUserId, AppPrincipal p) {
        return p != null && (isAdmin(p) || (p.id() != null && p.id().equals(pathUserId)));
    }
}
