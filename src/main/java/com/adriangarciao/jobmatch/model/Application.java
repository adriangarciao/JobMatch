package com.adriangarciao.jobmatch.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "applications")
@Data
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String jobTitle;
    private String company;

    @Enumerated(EnumType.STRING)
    private ApplicationStatus status; // applied, interview, offer, rejected
    private LocalDate appliedDate;
    @Min(value = 0, message = "Compensation must be 0 or higher")
    private Integer compensation;


    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Application(){}

    public Application(String jobTitle, String company, ApplicationStatus status,
                       LocalDate appliedDate, int compensation, User user) {
        this.jobTitle = jobTitle;
        this.company = company;
        this.status = status;
        this.appliedDate = appliedDate;
        this.compensation = compensation;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }

    public Integer getCompensation() {
        return compensation;
    }

    public void setCompensation(Integer compensation) {
        this.compensation = compensation;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }


    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", jobTitle='" + jobTitle + '\'' +
                ", company='" + company + '\'' +
                ", status='" + status + '\'' +
                ", appliedDate=" + appliedDate +
                ", compensation=" + compensation +
                ", user=" + user +
                '}';
    }
}
