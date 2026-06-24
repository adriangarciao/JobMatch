package com.adriangarciao.jobmatch.model;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@Data
public class User {

    public User(){}

//    public User(String name, int age, int gradYear, String college, String major, List<String> desiredJobTitle, List<String> skills, List<String> experience, List<Application> applications, List<Resume> resumes, String email, Role role) {
//        this.name = name;
//        this.age = age;
//        this.gradYear = gradYear;
//        this.college = college;
//        this.major = major;
//        this.desiredJobTitle = desiredJobTitle;
//        this.skills = skills;
//        this.experience = experience;
//        this.applications = applications;
//        this.resumes = resumes;
//        this.email = email;
//        this.role = role;
//    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    @Min(value = 16, message = "Age must be at least 16")
    @Column(nullable = true)
    private Integer age;

    @Column(name = "grad_year", nullable = true)
    @Positive(message = "Graduation year must be a positive number")
    private Integer gradYear;

    @Column(nullable = true)
    private String college;

    @Column(nullable = true)
    private String major;


    @ElementCollection
    @CollectionTable(name = "user_desired_job_titles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "job_title")
    private List<String> desiredJobTitle = new ArrayList<>();;

    @ElementCollection
    @CollectionTable(name = "user_skills", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();;

    @ElementCollection
    @CollectionTable(name = "user_experience", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "experience")
    private List<String> experience = new ArrayList<>();;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Application> applications = new ArrayList<>();;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resume> resumes = new ArrayList<>();;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email is required")
    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "user_role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role = Role.USER;



    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Integer getGradYear() {
        return gradYear;
    }

    public void setGradYear(Integer gradYear) {
        this.gradYear = gradYear;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public List<String> getDesiredJobTitle() {
        return desiredJobTitle;
    }

    public void setDesiredJobTitle(List<String> desiredJobTitle) {
        this.desiredJobTitle = desiredJobTitle;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public List<String> getExperience() {
        return experience;
    }

    public void setExperience(List<String> experience) {
        this.experience = experience;
    }

    public List<Application> getApplications() {
        return applications;
    }

    public void setApplications(List<Application> applications) {
        this.applications = applications;
    }

    public List<Resume> getResumes() {
        return resumes;
    }

    public void setResumes(List<Resume> resumes) {
        this.resumes = resumes;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", gradYear=" + gradYear +
                ", college='" + college + '\'' +
                ", major='" + major + '\'' +
                ", desiredJobTitle=" + desiredJobTitle +
                ", skills=" + skills +
                ", experience=" + experience +
                ", applications=" + applications +
                ", resumes=" + resumes +
                '}';
    }
}
