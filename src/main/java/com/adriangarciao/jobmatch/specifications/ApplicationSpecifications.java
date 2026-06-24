package com.adriangarciao.jobmatch.specifications;

import com.adriangarciao.jobmatch.model.Application;
import com.adriangarciao.jobmatch.model.ApplicationStatus;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.util.List;

public class ApplicationSpecifications {

    public static Specification<Application> hasUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Application> hasAnyStatus(List<ApplicationStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return null;
        return (root, query, cb) -> root.get("status").in(statuses);
    }

    public static Specification<Application> companyIn(List<String> companies) {
        if (companies == null || companies.isEmpty()) return null;
        return (root, query, cb) -> root.get("company").in(companies);
    }

    public static Specification<Application> companyContains(String company) {
        if (company == null || company.isBlank()) return null;
        return (root, query, cb) -> cb.like(cb.lower(root.get("company")),
                "%" + company.toLowerCase() + "%");
    }

    public static Specification<Application> compensationGreaterThanOrEqual(Integer minComp) {
        if (minComp == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("compensation"), minComp);
    }

    public static Specification<Application> compensationLessThanOrEqual(Integer maxComp) {
        if (maxComp == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("compensation"), maxComp);
    }

    public static Specification<Application> appliedDateAfter(LocalDate fromDate) {
        if (fromDate == null) return null;
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("appliedDate"), fromDate);
    }

    public static Specification<Application> appliedDateBefore(LocalDate toDate) {
        if (toDate == null) return null;
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("appliedDate"), toDate);
    }
}
