package com.adriangarciao.jobmatch.service;

import com.adriangarciao.jobmatch.dto.ApplicationCreateDTO;
import com.adriangarciao.jobmatch.dto.ApplicationDTO;
import com.adriangarciao.jobmatch.dto.PagedResponse;
import com.adriangarciao.jobmatch.exception.ApplicationNotFoundException;
import com.adriangarciao.jobmatch.exception.ForbiddenOperationException;
import com.adriangarciao.jobmatch.exception.UserNotFoundException;
import com.adriangarciao.jobmatch.mapper.ApplicationMapper;
import com.adriangarciao.jobmatch.model.Application;
import com.adriangarciao.jobmatch.model.ApplicationStatus;
import com.adriangarciao.jobmatch.model.User;
import com.adriangarciao.jobmatch.repository.ApplicationRepository;
import com.adriangarciao.jobmatch.repository.ResumeRepository;
import com.adriangarciao.jobmatch.repository.UserRepository;
import com.adriangarciao.jobmatch.specifications.ApplicationSpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ApplicationService.class);

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationMapper applicationMapper;

    public ApplicationService(ApplicationRepository applicationRepository,
                              UserRepository userRepository,
                              ApplicationMapper applicationMapper) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.applicationMapper = applicationMapper;
        log.debug("ApplicationService initialized");
    }

    /* ===================== Helpers ===================== */

    private void assertOwnership(Application app, Long userId) {
        if (app.getUser() == null || !app.getUser().getId().equals(userId)) {
            throw new ForbiddenOperationException("You do not own this application.");
        }
    }

    private Application loadOwned(Long appId, Long userId) {
        Application app = applicationRepository.findById(appId)
                .orElseThrow(() -> new ApplicationNotFoundException(appId));
        assertOwnership(app, userId);
        return app;
    }

    /* ===================== Create / Read (Owned) ===================== */

    // Controller: createMyApplication -> createForUser(userId, dto)
    public ApplicationDTO createForUser(Long userId, ApplicationCreateDTO dto) {
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Application app = applicationMapper.fromCreateDto(dto);
        app.setUser(owner);

        if (app.getCompensation() == null) app.setCompensation(0);
        Application saved = applicationRepository.save(app);
        return applicationMapper.toDto(saved);
    }

    // Controller: getMyApplication -> getOwned(id, userId)
    public ApplicationDTO getOwned(Long id, Long userId) {
        Application app = loadOwned(id, userId);
        return applicationMapper.toDto(app);
    }

    /* ===================== List / Search (Owned) ===================== */

    // Controller: getMyApplicationsPaged -> getApplicationsByUser
    public Page<ApplicationDTO> getApplicationsByUser(Long userId, Pageable pageable) {
        return applicationRepository.findByUserId(userId, pageable)
                .map(applicationMapper::toDto);
    }

    // Controller: searchMyApplications -> searchApplications
    public PagedResponse<ApplicationDTO> searchApplications(
            Long userId, Pageable pageable,
            List<ApplicationStatus> statuses,
            List<String> companies,
            String company,
            Integer minComp, Integer maxComp,
            LocalDate fromDate, LocalDate toDate) {

        // Build individual specs (allowing some to be null)
        Specification<Application> sUser   = ApplicationSpecifications.hasUserId(userId);
        Specification<Application> s1      = ApplicationSpecifications.hasAnyStatus(statuses);
        Specification<Application> s2      = ApplicationSpecifications.companyIn(companies);
        Specification<Application> s3      = ApplicationSpecifications.companyContains(company);
        Specification<Application> sMin    = ApplicationSpecifications.compensationGreaterThanOrEqual(minComp);
        Specification<Application> sMax    = ApplicationSpecifications.compensationLessThanOrEqual(maxComp);
        Specification<Application> sAfter  = ApplicationSpecifications.appliedDateAfter(fromDate);
        Specification<Application> sBefore = ApplicationSpecifications.appliedDateBefore(toDate);

        // Combine only the non-null ones
        Specification<Application> spec = Specification.allOf(
                java.util.stream.Stream.of(sUser, s1, s2, s3, sMin, sMax, sAfter, sBefore)
                        .filter(java.util.Objects::nonNull)
                        .toList()
        );

        Page<Application> page = applicationRepository.findAll(spec, pageable);

        return new PagedResponse<>(
                page.getContent().stream().map(applicationMapper::toDto).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                pageable.getSort().toString(),
                pageable.getSort().isEmpty() ? "unsorted"
                        : (pageable.getSort().iterator().next().isAscending() ? "ASC" : "DESC")
        );
    }

    /* ===================== Update / Patch / Delete (Owned) ===================== */

    // Controller: updateMyApplication -> updateOwned
    public ApplicationDTO updateOwned(Long id, Long userId, ApplicationDTO dto) {
        Application app = loadOwned(id, userId);
        applicationMapper.updateApplicationFromDto(dto, app);
        Application saved = applicationRepository.save(app);
        return applicationMapper.toDto(saved);
    }

    // Controller: patchMyApplication -> patchOwned (very light ad-hoc patch)
    public ApplicationDTO patchOwned(Long id, Long userId, Map<String, Object> patch) {
        Application app = loadOwned(id, userId);

        // apply only known fields (expand as needed)
        if (patch.containsKey("company")) {
            app.setCompany((String) patch.get("company"));
        }
        if (patch.containsKey("status")) {
            Object s = patch.get("status");
            if (s != null) app.setStatus(ApplicationStatus.valueOf(s.toString()));
        }
        if (patch.containsKey("compensation")) {
            Object c = patch.get("compensation");
            if (c != null) app.setCompensation(Integer.valueOf(c.toString()));
        }
        if (patch.containsKey("appliedDate")) {
            Object d = patch.get("appliedDate");
            if (d != null) app.setAppliedDate(LocalDate.parse(d.toString()));
        }

        Application saved = applicationRepository.save(app);
        return applicationMapper.toDto(saved);
    }

    // Controller: deleteMyApplication -> deleteOwned
    public void deleteOwned(Long id, Long userId) {
        Application app = loadOwned(id, userId);
        applicationRepository.delete(app);
    }


//    @Deprecated
//    public ApplicationDTO createApplication(ApplicationCreateDTO dto) {
//        User user = userRepository.findById(dto.userId())
//                .orElseThrow(() -> new UserNotFoundException(dto.userId()));
//        Application application = applicationMapper.fromCreateDto(dto);
//        application.setUser(user);
//        if (application.getCompensation() == null) application.setCompensation(0);
//        return applicationMapper.toDto(applicationRepository.save(application));
//    }
//
//    @Deprecated
//    public ApplicationDTO updateApplication(Long id, ApplicationDTO dto) {
//        var app = applicationRepository.findById(id)
//                .orElseThrow(() -> new ApplicationNotFoundException(id));
//        applicationMapper.updateApplicationFromDto(dto, app);
//        return applicationMapper.toDto(applicationRepository.save(app));
//    }
//
//    @Deprecated
//    public ApplicationDTO patchApplication(Long id, ApplicationDTO dto) {
//        Application application = applicationRepository.findById(id)
//                .orElseThrow(() -> new ApplicationNotFoundException(id));
//        applicationMapper.updateApplicationFromDto(dto, application);
//        return applicationMapper.toDto(applicationRepository.save(application));
//    }
//
//    @Deprecated
//    public void deleteApplicationById(Long id) {
//        applicationRepository.deleteById(id);
//    }
}

