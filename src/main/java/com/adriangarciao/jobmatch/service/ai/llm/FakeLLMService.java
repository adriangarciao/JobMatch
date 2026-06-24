package com.adriangarciao.jobmatch.service.ai.llm;

import com.adriangarciao.jobmatch.dto.FeedbackDTO;
import com.adriangarciao.jobmatch.dto.ParsedJobDTO;
import com.adriangarciao.jobmatch.dto.ParsedResumeDTO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class FakeLLMService implements LLMService {

    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "or", "a", "an", "to", "of", "for", "in", "on", "with",
            "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "should", "could", "may", "might",
            "at", "by", "from", "as", "that", "this", "it", "we", "you", "they"
    );

    // Core technical skills/concepts that should appear in feedback
    // Note: "go" is intentionally excluded as it's too commonly used as a verb in job postings
    // "golang" can still be detected via the isCoreSkill special handling
    private static final Set<String> CORE_SKILL_KEYWORDS = Set.of(
            "java", "python", "c++", "c#", "javascript", "typescript", "golang", "rust", "ruby", "php", "swift", "kotlin",
            "react", "angular", "vue", "node", "express", "spring", "django", "flask", "rails",
            "docker", "kubernetes", "k8s", "aws", "gcp", "azure", "cloud",
            "sql", "postgres", "mysql", "mongodb", "redis", "cassandra", "elasticsearch",
            "rest", "graphql", "api", "microservices", "grpc",
            "kafka", "rabbitmq", "memcached",
            "linux", "unix", "git", "ci/cd", "jenkins", "github", "gitlab",
            "backend", "frontend", "full stack", "fullstack",
            "data structures", "algorithms", "distributed systems",
            "terraform", "ansible", "nginx", "apache",
            "hadoop", "spark", "airflow",
            "pytorch", "tensorflow", "scikit", "pandas", "numpy",
            "elixir", "erlang", "scala", "clojure"
    );

    private static final double SKILL_WEIGHT = 0.7;
    private static final double TEXT_OVERLAP_WEIGHT = 0.3;
    private static final int TEXT_OVERLAP_CAP = 60;
    private static final int MAX_STRENGTH_SKILLS = 10;
    private static final int MAX_WEAKNESS_SKILLS = 10;

    @Override
    public FeedbackDTO generateFeedback(ParsedResumeDTO resume, ParsedJobDTO job, boolean includeCoverLetter) {
        // Extract core skills from job and resume
        Set<String> jobCoreSkills = extractJobCoreSkills(job);
        Set<String> resumeCoreSkills = toCoreSkills(resume == null ? List.of() : resume.skills());

        // Compute matched and missing skills
        Set<String> matchedCoreSkills = new HashSet<>(resumeCoreSkills);
        matchedCoreSkills.retainAll(jobCoreSkills);
        
        Set<String> missingCoreSkills = new HashSet<>(jobCoreSkills);
        missingCoreSkills.removeAll(resumeCoreSkills);

        // Calculate match score (keep existing formula)
        int jobSkillCount = Math.max(jobCoreSkills.size(), 1);
        int matchedCount = matchedCoreSkills.size();
        double skillScore = (matchedCount / (double) jobSkillCount) * 100.0;

        String resumeText = resume == null ? "" : resume.rawText();
        String jobText = job == null ? "" : job.rawText();
        double textOverlapScore = calculateTextOverlapScore(resumeText, jobText);

        int matchScore = (int) Math.round(SKILL_WEIGHT * skillScore + TEXT_OVERLAP_WEIGHT * textOverlapScore);
        matchScore = Math.max(0, Math.min(100, matchScore));

        // Build clean, focused feedback
        List<String> strengths = buildStrengths(matchedCoreSkills);
        List<String> weaknesses = buildWeaknesses(missingCoreSkills, matchScore);
        List<String> suggestions = buildSuggestions(missingCoreSkills, matchedCoreSkills, skillScore, 
                                                     textOverlapScore, includeCoverLetter);

        String band = getScoreBand(matchScore);
        String title = (job == null || job.title() == null || job.title().isBlank()) ? "this role" : job.title();
        String summary = String.format("For the %s position, your resume shows a %s fit (%d%% match). See suggestions to improve alignment.", 
                                       title, band, matchScore);
        
        String jobLocation = (job != null) ? job.jobLocation() : null;
        String compensationInfo = (job != null) ? job.compensationInfo() : null;

        return new FeedbackDTO(matchScore, strengths, weaknesses, suggestions, summary, jobLocation, compensationInfo);
    }

    /**
     * Checks if a skill string contains a real technical skill/concept.
     * Special handling: plain "go" is rejected (too common as a verb),
     * but "golang" is accepted as a valid skill.
     */
    private boolean isCoreSkill(String skill) {
        if (skill == null || skill.isBlank()) return false;
        String normalized = skill.toLowerCase(Locale.ROOT).trim();
        
        // Explicitly reject plain "go" as it's a common verb, not the language
        if (normalized.equals("go")) {
            return false;
        }
        
        // Allow "golang" or phrases containing "golang"
        if (normalized.contains("golang")) {
            return true;
        }
        
        // Check if any core keyword appears in this skill
        return CORE_SKILL_KEYWORDS.stream().anyMatch(normalized::contains);
    }

    /**
     * Converts a collection of skill phrases to a set of core technical skills.
     * Extracts and identifies core skill keywords from phrases.
     */
    private Set<String> toCoreSkills(Collection<String> phrasesOrSkills) {
        Set<String> coreSkills = new HashSet<>();
        
        for (String phrase : phrasesOrSkills) {
            if (phrase == null || phrase.isBlank()) continue;
            
            String normalized = normalizeSkillName(phrase);
            
            // Find which core skill keywords are present in this phrase
            for (String keyword : CORE_SKILL_KEYWORDS) {
                if (normalized.contains(keyword)) {
                    coreSkills.add(keyword);
                }
            }
        }
        
        return coreSkills;
    }

    /**
     * Extracts core skills from both required and nice-to-have job skills.
     */
    private Set<String> extractJobCoreSkills(ParsedJobDTO job) {
        if (job == null) return Set.of();
        
        Set<String> coreSkills = new HashSet<>();
        coreSkills.addAll(toCoreSkills(job.requiredSkills()));
        coreSkills.addAll(toCoreSkills(job.niceToHaveSkills()));
        return coreSkills;
    }

    /**
     * Normalizes a skill name for matching and display.
     * Examples: "AWS experience" → "aws", "Spring-Boot" → "spring boot"
     */
    private String normalizeSkillName(String skill) {
        if (skill == null) return "";
        
        String normalized = skill.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.\\s-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        
        // Standardize common variations
        normalized = normalized.replaceAll("\\bk8s\\b", "kubernetes");
        normalized = normalized.replaceAll("\\bspring-?boot\\b", "spring boot");
        normalized = normalized.replaceAll("\\bfull-?stack\\b", "full stack");
        normalized = normalized.replaceAll("\\bci-?cd\\b", "ci/cd");
        normalized = normalized.replaceAll("\\bpostgresql\\b", "postgres");
        
        return normalized;
    }

    private double calculateTextOverlapScore(String resumeText, String jobText) {
        if (resumeText == null || resumeText.isBlank() || jobText == null || jobText.isBlank()) {
            return 0.0;
        }

        Set<String> resumeTokens = tokenizeAndFilter(resumeText);
        Set<String> jobTokens = tokenizeAndFilter(jobText);

        if (jobTokens.isEmpty()) return 0.0;

        long overlapCount = resumeTokens.stream()
                .filter(jobTokens::contains)
                .count();

        double score = (overlapCount / (double) jobTokens.size()) * 100.0;
        return Math.min(score, TEXT_OVERLAP_CAP);
    }

    private Set<String> tokenizeAndFilter(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(token -> token.replaceAll("[^a-z0-9+#.-]", ""))
                .filter(token -> !token.isBlank())
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOPWORDS.contains(token))
                .collect(Collectors.toSet());
    }

    /**
     * Capitalizes the first letter of a skill name for display.
     */
    private String capitalizeSkill(String skill) {
        if (skill == null || skill.isEmpty()) {
            return skill;
        }
        return skill.substring(0, 1).toUpperCase(Locale.ROOT) + skill.substring(1);
    }

    /**
     * Builds a concise list of matched core skills as strengths.
     */
    private List<String> buildStrengths(Set<String> matchedCoreSkills) {
        List<String> strengths = new ArrayList<>();
        
        if (matchedCoreSkills.isEmpty()) {
            strengths.add("Your resume does not yet highlight the key technologies mentioned in this job.");
        } else {
            // Add each matched skill as a separate strength, capped at MAX_STRENGTH_SKILLS
            matchedCoreSkills.stream()
                    .sorted()
                    .limit(MAX_STRENGTH_SKILLS)
                    .map(this::capitalizeSkill)
                    .forEach(strengths::add);
        }
        
        return strengths;
    }

    /**
     * Builds a concise list of missing core skills as weaknesses.
     */
    private List<String> buildWeaknesses(Set<String> missingCoreSkills, int matchScore) {
        List<String> weaknesses = new ArrayList<>();
        
        if (missingCoreSkills.isEmpty()) {
            // No core skills missing, but score isn't perfect
            if (matchScore < 100) {
                weaknesses.add("Consider aligning your skills section more closely with the job description.");
            }
        } else {
            // Add each missing skill as a separate weakness, capped at MAX_WEAKNESS_SKILLS
            missingCoreSkills.stream()
                    .sorted()
                    .limit(MAX_WEAKNESS_SKILLS)
                    .map(this::capitalizeSkill)
                    .forEach(weaknesses::add);
        }
        
        return weaknesses;
    }

    /**
     * Builds 2-3 high-level suggestions instead of per-skill spam.
     */
    private List<String> buildSuggestions(Set<String> missingCoreSkills, Set<String> matchedCoreSkills,
                                          double skillScore, double textOverlapScore, boolean includeCoverLetter) {
        List<String> suggestions = new ArrayList<>();
        
        // Suggestion 1: If missing skills, suggest highlighting them if candidate knows them
        if (!missingCoreSkills.isEmpty()) {
            List<String> exampleMissing = missingCoreSkills.stream()
                    .sorted()
                    .limit(3)
                    .collect(Collectors.toList());
            
            String examples = String.join(", ", exampleMissing);
            suggestions.add(String.format(
                    "If you already know some of the missing skills (e.g. %s), explicitly list them in your skills or experience sections.",
                    examples
            ));
        }
        
        // Suggestion 2: If missing skills, suggest building projects
        if (!missingCoreSkills.isEmpty()) {
            List<String> exampleMissing = missingCoreSkills.stream()
                    .sorted()
                    .limit(2)
                    .collect(Collectors.toList());
            
            String examples = String.join(" or ", exampleMissing);
            suggestions.add(String.format(
                    "Consider building a small project that uses technologies like %s and adding it to your resume.",
                    examples
            ));
        }
        
        // Suggestion 3: If matched skills, suggest making them more visible
        if (!matchedCoreSkills.isEmpty()) {
            List<String> exampleMatched = matchedCoreSkills.stream()
                    .sorted()
                    .limit(2)
                    .collect(Collectors.toList());
            
            String examples = String.join(", ", exampleMatched);
            suggestions.add(String.format(
                    "Make sure the matched skills (e.g. %s) are easy to spot—group them in a dedicated skills section.",
                    examples
            ));
        }
        
        // Suggestion 4: If text overlap is high but skill score is low, suggest terminology alignment
        if (textOverlapScore > 30 && skillScore < 50) {
            suggestions.add(
                    "Your resume has relevant content, but consider aligning your skill terminology more closely with the job posting's specific keywords."
            );
        }
        
        // Cover letter suggestion (if requested)
        if (includeCoverLetter) {
            String hint;
            if (!matchedCoreSkills.isEmpty()) {
                hint = matchedCoreSkills.stream()
                        .sorted()
                        .limit(3)
                        .collect(Collectors.joining(", "));
            } else if (!missingCoreSkills.isEmpty()) {
                hint = missingCoreSkills.stream().findFirst().orElse("relevant skills");
            } else {
                hint = "relevant skills";
            }
            suggestions.add(String.format("You may want to generate a tailored cover letter emphasizing %s.", hint));
        }
        
        return suggestions;
    }

    private String getScoreBand(int matchScore) {
        if (matchScore >= 70) return "strong";
        if (matchScore >= 40) return "moderate";
        return "weak";
    }

    private String normalizeForMatching(String skill) {
        if (skill == null) return "";
        return skill.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#.\\s-]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}