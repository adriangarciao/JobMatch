package com.adriangarciao.jobmatch.repository;

import com.adriangarciao.jobmatch.model.Application;
import com.adriangarciao.jobmatch.model.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {
    Page<Application> findByUserId(Long userId, Pageable pageable);
    Page<Application> findByUserIdAndStatus(Long userId, ApplicationStatus status, Pageable pageable);
    Page<Application> findByUserIdAndCompanyContainingIgnoreCase(Long userId, String company, Pageable pageable);

    @Query("select a.user.id from Application a where a.id = :id")
    Long findOwnerIdByApplicationId(@Param("id") Long id);

    Page<Application> findAll(Specification<Application> spec, Pageable pageable);
}
