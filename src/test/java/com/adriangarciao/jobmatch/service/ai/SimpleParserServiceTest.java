package com.adriangarciao.jobmatch.service.ai;

import com.adriangarciao.jobmatch.dto.ParsedJobDTO;
import com.adriangarciao.jobmatch.dto.ParsedResumeDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SimpleParserServiceTest {

    private final SimpleParserService parser = new SimpleParserService();

    @Test
    void parseResume_emptyAndNullDontCrash() {
        ParsedResumeDTO r1 = parser.parseResume(null);
        assertNotNull(r1);
        assertEquals("Candidate", r1.redactedName());
        assertTrue(r1.skills().isEmpty());
        assertTrue(r1.experiences().isEmpty());

        ParsedResumeDTO r2 = parser.parseResume("");
        assertNotNull(r2);
        assertEquals("Candidate", r2.redactedName());
        assertTrue(r2.skills().isEmpty());
        assertTrue(r2.experiences().isEmpty());
    }

    @Test
    void parseResume_extractsSkillsAndExperiences() {
        String resume = "John Doe\n\nSkills:\nJava, Spring, SQL\n\nExperience:\nSoftware Engineer at X\n- Did stuff\n\nSoftware Engineer at Y\n- Did other stuff\n";
        ParsedResumeDTO dto = parser.parseResume(resume);
        assertEquals("Candidate", dto.redactedName());
        // Skills are normalized to lowercase
        assertTrue(dto.skills().contains("java"));
        assertTrue(dto.skills().contains("spring"));
        assertTrue(dto.skills().contains("sql"));
        assertTrue(dto.experiences().size() >= 2);
    }

    @Test
    void parseJob_extractsTitleAndSkills() {
        String job = "Title: Senior Java Engineer\n\nRequirements:\nJava\nSpring\nAWS\n\nNice to have:\nDocker\nKubernetes\n";
        ParsedJobDTO dto = parser.parseJob(job);
        assertEquals("Senior Java Engineer", dto.title());
        // Skills are normalized to lowercase
        assertTrue(dto.requiredSkills().stream().anyMatch(s -> s.equals("java")));
        assertTrue(dto.requiredSkills().stream().anyMatch(s -> s.equals("spring")));
        assertTrue(dto.niceToHaveSkills().stream().anyMatch(s -> s.equals("docker")));
    }
    
    @Test
    void parseJob_withRequirementsBullets_extractsOnlyValidSkills() {
        String job = """
            Software Engineer - NY
            
            About Us:
            We are a company in New York building cool stuff.
            
            Requirements:
            - Java and Spring Boot experience
            - Strong knowledge of SQL
            - Experience with AWS or GCP
            - 3+ years of experience
            - Note: Must be authorized to work in the US
            
            Nice to have:
            - Docker, Kubernetes
            - CI/CD pipelines
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        // Should extract valid skills
        assertTrue(dto.requiredSkills().contains("java"), "Should contain java");
        assertTrue(dto.requiredSkills().contains("spring boot"), "Should contain spring boot");
        assertTrue(dto.requiredSkills().contains("sql"), "Should contain sql");
        assertTrue(dto.requiredSkills().contains("aws") || dto.requiredSkills().contains("gcp"), "Should contain cloud provider");
        
        // Should NOT extract junk words
        assertFalse(dto.requiredSkills().contains("ny"), "Should not contain location 'ny'");
        assertFalse(dto.requiredSkills().contains("note"), "Should not contain 'note'");
        assertFalse(dto.requiredSkills().contains("us"), "Should not contain 'us'");
        assertFalse(dto.requiredSkills().contains("we"), "Should not contain 'we'");
        assertFalse(dto.requiredSkills().contains("company"), "Should not contain 'company'");
        assertFalse(dto.requiredSkills().contains("3"), "Should not contain number");
        assertFalse(dto.requiredSkills().contains("years"), "Should not contain 'years'");
        
        // Nice to have should work correctly
        assertTrue(dto.niceToHaveSkills().contains("docker"), "Should contain docker in nice-to-have");
        assertTrue(dto.niceToHaveSkills().contains("kubernetes"), "Should contain kubernetes in nice-to-have");
    }
    
    @Test
    void parseJob_filtersOutCompanyDescriptionsAndBenefits() {
        String job = """
            Backend Engineer
            
            About the Company:
            We're backed by Google, Meta, and Stripe. Our team includes alumni from Netflix and Amazon.
            
            Qualifications:
            - Python and Django
            - PostgreSQL
            
            Benefits:
            - Health insurance
            - 401k matching
            - Free lunch
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        // Should extract real skills
        assertTrue(dto.requiredSkills().contains("python"), "Should contain python");
        assertTrue(dto.requiredSkills().contains("django"), "Should contain django");
        assertTrue(dto.requiredSkills().contains("postgresql"), "Should contain postgresql");
        
        // Should NOT treat company names as skills
        assertFalse(dto.requiredSkills().contains("google"), "Should not contain company name 'google'");
        assertFalse(dto.requiredSkills().contains("meta"), "Should not contain company name 'meta'");
        assertFalse(dto.requiredSkills().contains("stripe"), "Should not contain company name 'stripe'");
        assertFalse(dto.requiredSkills().contains("netflix"), "Should not contain company name 'netflix'");
        assertFalse(dto.requiredSkills().contains("amazon"), "Should not contain company name 'amazon'");
        
        // Should NOT extract benefits
        assertFalse(dto.requiredSkills().contains("health"), "Should not contain 'health'");
        assertFalse(dto.requiredSkills().contains("insurance"), "Should not contain 'insurance'");
        assertFalse(dto.requiredSkills().contains("lunch"), "Should not contain 'lunch'");
    }
    
    @Test
    void parseJob_normalizesSkillsProperlyRemovingSuffixes() {
        String job = """
            Requirements:
            - Strong Java experience
            - Excellent communication skills
            - Proven Docker knowledge
            - Deep understanding of Kubernetes
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        // Should normalize away "experience", "skills", "knowledge", qualifiers
        assertTrue(dto.requiredSkills().contains("java"), "Should normalize 'Java experience' to 'java'");
        assertTrue(dto.requiredSkills().contains("docker"), "Should normalize 'Docker knowledge' to 'docker'");
        assertTrue(dto.requiredSkills().contains("kubernetes"), "Should normalize to 'kubernetes'");
        
        // "communication" might be extracted, but "skills" suffix should be removed
        // It's borderline whether "communication" is a skill or stopword - current implementation may include it
    }
    
    @Test
    void parseJob_handlesNoExplicitSectionsGracefully() {
        String job = """
            Full Stack Developer
            
            - React and Node.js
            - MongoDB
            - TypeScript
            
            Location: San Francisco
            Salary: Competitive
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        // Should fall back to extracting from bullet points
        assertTrue(dto.requiredSkills().contains("react"), "Should contain react");
        assertTrue(dto.requiredSkills().contains("node.js") || dto.requiredSkills().contains("nodejs"), "Should contain nodejs");
        assertTrue(dto.requiredSkills().contains("mongodb"), "Should contain mongodb");
        assertTrue(dto.requiredSkills().contains("typescript"), "Should contain typescript");
        
        // Should not extract location or salary info
        assertFalse(dto.requiredSkills().contains("san francisco"), "Should not contain location");
        assertFalse(dto.requiredSkills().contains("competitive"), "Should not contain 'competitive'");
    }
    
    @Test
    void parseJob_extractsLocationWithExplicitLabel() {
        String job = """
            Software Engineer
            
            Requirements:
            - Java, Python
            
            Location: New York, NY (hybrid)
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertEquals("New York, NY (hybrid)", dto.jobLocation());
    }
    
    @Test
    void parseJob_extractsLocationFromBasedIn() {
        String job = """
            Backend Developer
            
            Based in: San Francisco, CA
            
            Requirements:
            - Python, Django
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertEquals("San Francisco, CA", dto.jobLocation());
    }
    
    @Test
    void parseJob_extractsLocationRemoteOnly() {
        String job = """
            Full Stack Engineer
            
            Remote
            
            Requirements:
            - React, Node.js
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertNotNull(dto.jobLocation());
        assertTrue(dto.jobLocation().toLowerCase().contains("remote"));
    }
    
    @Test
    void parseJob_extractsLocationFromCityStatePattern() {
        String job = """
            Senior Engineer
            
            Requirements:
            - Java, Spring
            
            Austin, TX
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertEquals("Austin, TX", dto.jobLocation());
    }
    
    @Test
    void parseJob_returnsNullLocationWhenNotFound() {
        String job = """
            Developer
            
            Requirements:
            - Java, Python
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertNull(dto.jobLocation());
    }
    
    @Test
    void parseJob_extractsCompensationWithExplicitLabel() {
        String job = """
            Software Engineer
            
            Requirements:
            - Java, Python
            
            Compensation: $120k - $150k + equity
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertEquals("$120k - $150k + equity", dto.compensationInfo());
    }
    
    @Test
    void parseJob_extractsCompensationFromSalaryLabel() {
        String job = """
            Backend Developer
            
            Requirements:
            - Python, Django
            
            Salary: $100,000 - $130,000
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertEquals("$100,000 - $130,000", dto.compensationInfo());
    }
    
    @Test
    void parseJob_extractsCompensationFromLineWithDollarSignAndKeywords() {
        String job = """
            Full Stack Engineer
            
            Requirements:
            - React, Node.js
            
            Base: $140k, OTE: $180k
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertNotNull(dto.compensationInfo());
        assertTrue(dto.compensationInfo().contains("$140k"));
    }
    
    @Test
    void parseJob_extractsCompensationFromRangePattern() {
        String job = """
            Senior Engineer
            
            Requirements:
            - Java, Spring
            
            $110,000 - $145,000
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertEquals("$110,000 - $145,000", dto.compensationInfo());
    }
    
    @Test
    void parseJob_returnsNullCompensationWhenNotFound() {
        String job = """
            Developer
            
            Requirements:
            - Java, Python
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertNull(dto.compensationInfo());
    }
    
    @Test
    void parseJob_extractsBothLocationAndCompensation() {
        String job = """
            Senior Software Engineer
            
            Location: Seattle, WA (remote friendly)
            
            Requirements:
            - Java, Spring Boot
            - AWS, Kubernetes
            
            Compensation: $130k - $160k + equity
            """;
        
        ParsedJobDTO dto = parser.parseJob(job);
        
        assertEquals("Senior Software Engineer", dto.title());
        assertEquals("Seattle, WA (remote friendly)", dto.jobLocation());
        assertEquals("$130k - $160k + equity", dto.compensationInfo());
        assertTrue(dto.requiredSkills().contains("java"));
    }
}
