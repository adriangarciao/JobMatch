package com.adriangarciao.jobmatch.security;

import java.security.Principal;

public record AppPrincipal(Long id, String email, String role) implements Principal {
    @Override public String getName() { return email; } // Spring expects a name
}
