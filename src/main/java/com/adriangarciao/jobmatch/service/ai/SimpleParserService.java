package com.adriangarciao.jobmatch.service.ai;

import com.adriangarciao.jobmatch.dto.ParsedJobDTO;
import com.adriangarciao.jobmatch.dto.ParsedResumeDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SimpleParserService is a lightweight, deterministic placeholder parser.
 * It performs basic normalization and heuristic extraction of skills/experiences/title sections.
 */
@Service
public class SimpleParserService implements ParserService {

    private static final Pattern TITLE_LABEL = Pattern.compile("(?mi)^(?:title)\\s*[:\\-]\\s*(.+)$", Pattern.MULTILINE);
    private static final Pattern TECH_WORD = Pattern.compile("\\b([A-Za-z][A-Za-z0-9+#.\\-]{1,20})\\b");
    private static final Pattern BULLET_LINE = Pattern.compile("^\\s*[-*•]\\s+(.+)$", Pattern.MULTILINE);
    
    // Comprehensive stopword list to filter out non-skill words
    private static final Set<String> STOPWORDS = Set.of(
        // Articles, prepositions, conjunctions
        "the", "and", "or", "a", "an", "to", "of", "for", "in", "on", "with", "at", "by", "from", "as",
        "we", "you", "it", "this", "that", "these", "those", "our", "your", "their", "is", "are", "be",
        "will", "can", "may", "must", "should", "would", "could",
        // Domain/generic junk words
        "ny", "nyc", "us", "hq", "time", "note", "location", "new", "york", "internship", "intern",
        "department", "compensation", "overview", "about", "benefits", "company", "most", "top", "fall",
        "january", "february", "march", "april", "june", "july", "august", "september", "october",
        "november", "december", "blog", "role", "what", "do", "does", "did", "build", "building", "built",
        "currently", "nice", "haves", "magazine", "team", "work", "working", "opportunity", "opportunities",
        "position", "join", "career", "careers", "office", "offices", "salary", "pay", "paid", "bonus",
        "bonuses", "equity", "stock", "options", "remote", "hybrid", "onsite", "apply", "application",
        "applications", "resume", "resumes", "cover", "letter", "letters", "send", "submit", "email",
        "contact", "phone", "address", "description", "descriptions", "summary", "summaries",
        "experience", "proficiency", "knowledge", "skills", "skill", "ability", "abilities",
        // Common company names (to avoid treating them as skills)
        "google", "meta", "facebook", "stripe", "netflix", "amazon", "microsoft", "apple", "uber",
        "lyft", "airbnb", "twitter", "linkedin", "salesforce", "oracle", "ibm", "intel", "nvidia"
    );
    
    // Short tech terms that are valid despite being < 3 chars
    private static final Set<String> SHORT_TECH_WHITELIST = Set.of("c", "go", "r", "ai", "ml", "ui", "ux", "qa", "ci", "cd");
    
    // Location keywords to reject
    private static final Set<String> LOCATION_KEYWORDS = Set.of(
        "new york", "san francisco", "nyc", "sf", "remote", "hybrid", "onsite",
        "california", "texas", "washington", "boston", "seattle", "austin", "chicago"
    );

    @Override
    public ParsedResumeDTO parseResume(String resumeText) {
        String normalized = safeNormalize(resumeText);

        // redactedName: placeholder to simulate PII redaction
        String redactedName = "Candidate";

        List<String> skills = extractSkillsFromResume(normalized);
        List<String> experiences = extractExperiences(normalized);

        return new ParsedResumeDTO(redactedName, skills, experiences, normalized);
    }

    @Override
    public ParsedJobDTO parseJob(String jobPostingText) {
        String normalized = safeNormalize(jobPostingText);

        String title = extractTitle(normalized);
        List<String> required = extractJobSkillsStrict(normalized, false);
        List<String> niceToHave = extractJobSkillsStrict(normalized, true);
        String jobLocation = extractJobLocation(normalized);
        String compensationInfo = extractCompensationInfo(normalized);

        return new ParsedJobDTO(title, required, niceToHave, jobLocation, compensationInfo, normalized);
    }

    /**
     * Extracts job location from the posting text.
     * Looks for lines containing location keywords or patterns.
     * 
     * Examples:
     * - "Location: New York, NY (hybrid)" → "New York, NY (hybrid)"
     * - "Remote" → "Remote"
     * - "San Francisco, CA" → "San Francisco, CA"
     * 
     * @return The location string if found, null otherwise
     */
    private String extractJobLocation(String text) {
        if (text == null || text.isBlank()) return null;
        
        String[] lines = text.split("\n");
        
        // Strategy 1: Look for explicit "Location:" label
        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            
            // Match "Location:", "Location -", "Based in:", etc.
            if (lower.matches("^(location|based in|office|where)\\s*[:\\-].*")) {
                // Extract the part after the label
                String extracted = trimmed.replaceFirst("(?i)^(location|based in|office|where)\\s*[:\\-]\\s*", "").trim();
                if (!extracted.isEmpty()) {
                    return extracted;
                }
            }
        }
        
        // Strategy 2: Look for lines that contain location indicators
        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            
            // Skip very long lines (likely paragraphs, not location info)
            if (trimmed.length() > 100) continue;
            
            // Look for Remote/Hybrid/Onsite indicators
            if (lower.matches(".*(remote|hybrid|on-site|onsite).*") && trimmed.length() < 50) {
                return trimmed;
            }
            
            // Look for city, state patterns (e.g., "New York, NY" or "San Francisco, CA")
            if (trimmed.matches(".*, [A-Z]{2}.*")) {
                return trimmed;
            }
        }
        
        // Strategy 3: Look for standalone short lines with location keywords
        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            
            // Must be relatively short to be a location line
            if (trimmed.length() > 60) continue;
            
            // Check for major city names
            if (lower.matches(".*(new york|san francisco|los angeles|chicago|boston|seattle|austin|denver|portland|miami|atlanta|dallas|houston).*")) {
                return trimmed;
            }
        }
        
        return null;
    }

    /**
     * Extracts compensation/salary information from the posting text.
     * Looks for lines containing salary, compensation, or pay indicators.
     * 
     * Examples:
     * - "Compensation: $120k - $150k + equity" → "$120k - $150k + equity"
     * - "Salary Range: $100,000 - $130,000" → "$100,000 - $130,000"
     * - "Base: $140k, OTE: $180k" → "Base: $140k, OTE: $180k"
     * 
     * @return The compensation string if found, null otherwise
     */
    private String extractCompensationInfo(String text) {
        if (text == null || text.isBlank()) return null;
        
        String[] lines = text.split("\n");
        
        // Strategy 1: Look for lines with explicit compensation keywords
        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            
            // Skip very long lines (likely paragraphs)
            if (trimmed.length() > 150) continue;
            
            // Check for compensation-related keywords
            if (lower.contains("compensation") || lower.contains("salary") || 
                lower.contains("pay range") || lower.contains("base salary") ||
                lower.contains("total compensation") || lower.contains("annual salary")) {
                
                // If line starts with label, strip it
                if (lower.matches("^(compensation|salary|pay|base|total compensation|annual salary)\\s*[:\\-].*")) {
                    String extracted = trimmed.replaceFirst("(?i)^(compensation|salary|pay|base|total compensation|annual salary)\\s*[:\\-]\\s*", "").trim();
                    if (!extracted.isEmpty() && extracted.length() <= 120) {
                        return extracted;
                    }
                }
                // Otherwise return the whole line if it's reasonable length
                if (trimmed.length() <= 120) {
                    return trimmed;
                }
            }
        }
        
        // Strategy 2: Look for lines with dollar signs and relevant keywords
        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase(Locale.ROOT);
            
            // Skip very long lines
            if (trimmed.length() > 150) continue;
            
            // Look for $ combined with comp-related terms or "k" (for $100k notation)
            if (trimmed.contains("$")) {
                if (lower.contains("salary") || lower.contains("compensation") || 
                    lower.contains("pay") || lower.contains("base") || 
                    lower.contains("range") || lower.contains("ote") ||
                    lower.contains("equity") || lower.contains("bonus") ||
                    lower.contains("hourly") || lower.contains("usd") ||
                    trimmed.matches(".*\\$[0-9,]+k.*")) {
                    
                    // Limit to reasonable length
                    if (trimmed.length() <= 120) {
                        return trimmed;
                    }
                }
            }
        }
        
        // Strategy 3: Look for salary range patterns with $ signs
        for (String line : lines) {
            String trimmed = line.trim();
            
            // Skip very long lines
            if (trimmed.length() > 150) continue;
            
            // Match patterns like "$100k - $150k", "$100,000 - $150,000"
            if (trimmed.matches(".*\\$[0-9]{2,3}[kK]?[,0-9]*\\s*[-–]\\s*\\$[0-9]{2,3}[kK]?[,0-9]*.*") ||
                trimmed.matches(".*\\$[0-9,]+\\s*[-–]\\s*\\$[0-9,]+.*")) {
                
                if (trimmed.length() <= 120) {
                    return trimmed;
                }
            }
        }
        
        // Strategy 4: Fallback - any line with $ and digits
        for (String line : lines) {
            String trimmed = line.trim();
            
            if (trimmed.length() > 150) continue;
            
            // Look for $ followed by digits
            if (trimmed.matches(".*\\$\\s*[0-9,]+.*") && trimmed.length() <= 120) {
                return trimmed;
            }
        }
        
        return null;
    }

    /**
     * Extracts skills from job posting with strict filtering.
     * 
     * @param text The job posting text
     * @param niceToHave If true, extract from "nice to have"/"preferred" sections. 
     *                   If false, extract from requirements/qualifications sections.
     * @return List of normalized, filtered skills
     */
    private List<String> extractJobSkillsStrict(String text, boolean niceToHave) {
        Set<String> skillSet = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return new ArrayList<>(skillSet);
        
        String lower = text.toLowerCase(Locale.ROOT);
        String[] lines = text.split("\n");
        
        // Define section keywords based on what we're looking for
        String[] sectionKeywords;
        if (niceToHave) {
            sectionKeywords = new String[]{
                "nice to have", "nice-to-have", "bonus", "preferred", "plus", "additionally"
            };
        } else {
            sectionKeywords = new String[]{
                "requirements", "required", "qualifications", "what you'll do", "what you will do",
                "you will", "responsibilities", "what you'll bring", "must have", "should have"
            };
        }
        
        // Find relevant sections
        List<String> relevantLines = new ArrayList<>();
        boolean inRelevantSection = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String lineLower = line.toLowerCase(Locale.ROOT);
            
            // Check if this line starts a relevant section
            boolean startsRelevantSection = false;
            for (String keyword : sectionKeywords) {
                if (lineLower.contains(keyword)) {
                    startsRelevantSection = true;
                    inRelevantSection = true;
                    break;
                }
            }
            
            // Check if this line starts an irrelevant section (company info, benefits, etc.)
            if (lineLower.matches(".*\\b(about us|about the company|company overview|benefits|compensation|location|office|our team|why join|why work|equal opportunity|diversity).*")) {
                inRelevantSection = false;
            }
            
            // If we hit a new major section header (all caps or ends with colon), might be leaving relevant section
            if (line.matches("^[A-Z][A-Z\\s]{3,}:?$") && !startsRelevantSection) {
                // Only exit if it's clearly a different section
                for (String keyword : sectionKeywords) {
                    if (!lineLower.contains(keyword)) {
                        inRelevantSection = false;
                        break;
                    }
                }
            }
            
            // Collect lines from relevant sections
            if (inRelevantSection || startsRelevantSection) {
                relevantLines.add(line);
            }
        }
        
        // If no relevant sections found, fall back to bullet lines only (conservative)
        if (relevantLines.isEmpty()) {
            for (String line : lines) {
                if (line.trim().matches("^[-*•]\\s+.+")) {
                    relevantLines.add(line);
                }
            }
        }
        
        // Extract skills from relevant lines
        for (String line : relevantLines) {
            // Focus on bullet points
            if (line.trim().matches("^[-*•]\\s+.+")) {
                String content = line.replaceFirst("^\\s*[-*•]\\s+", "").trim();
                extractSkillsFromLine(content, skillSet);
            } else if (!line.trim().isEmpty()) {
                // Also process non-bullet lines in relevant sections, but more carefully
                extractSkillsFromLine(line, skillSet);
            }
        }
        
        return new ArrayList<>(skillSet);
    }
    
    /**
     * Extracts candidate skills from a single line by splitting on delimiters
     * and applying normalization + filtering.
     */
    private void extractSkillsFromLine(String line, Set<String> skillSet) {
        // Skip section headers (lines ending with colon)
        if (line.trim().matches("^[^:]+:$")) {
            return;
        }
        
        // Skip lines that start with "note:", "important:", etc. - these are usually disclaimers
        if (line.trim().toLowerCase().matches("^(note|important|please|disclaimer):.*")) {
            return;
        }
        
        // Pre-process to extract core terms from common phrases
        String processedLine = line;
        
        // "knowledge of X" → "X", "understanding of X" → "X"
        processedLine = processedLine.replaceAll("(?i)(knowledge|understanding)\\s+of\\s+", "");
        
        // "experience with X" → "X", "proficiency in X" → "X"
        processedLine = processedLine.replaceAll("(?i)(experience|proficiency)\\s+(with|in)\\s+", "");
        
        // "X+ years of Y" → "Y", "X years of Y" → "Y"
        processedLine = processedLine.replaceAll("(?i)\\d+\\+?\\s*years?\\s+of\\s+", "");
        
        // "strong X" → "X", "excellent X" → "X" (but keep the qualifiers for later removal in normalization)
        // This is already handled in normalizeSkill(), so we continue
        
        // Split on common delimiters: comma, semicolon, "and", "or"
        String[] candidatePhrases = processedLine.split("\\s+(?:and|or)\\s+|[,;]+");
        
        for (String phrase : candidatePhrases) {
            phrase = phrase.trim();
            if (phrase.isEmpty()) continue;
            
            // Normalize the skill
            String normalized = normalizeSkill(phrase);
            
            // Filter out junk
            if (isLikelySkill(normalized)) {
                skillSet.add(normalized);
            }
        }
    }

    private String safeNormalize(String text) {
        if (text == null) return "";
        // trim, normalize CRLF to LF, collapse multiple blank lines to a single blank line,
        String t = text.replace("\r\n", "\n").replace('\r', '\n').trim();
        t = t.replaceAll("\n{3,}", "\n\n");
        // collapse excessive whitespace within lines
        t = t.replaceAll("[ \t]{2,}", " ");
        return t;
    }

    /**
     * Normalizes a skill string for comparison:
     * - Converts to lowercase
     * - Strips punctuation (except + # . -)
     * - Removes trailing words like "experience", "skills", "knowledge", "proficiency", "background"
     * - Removes qualifier words like "strong", "solid", "excellent", "good"
     * - Collapses multiple spaces
     */
    private String normalizeSkill(String skill) {
        if (skill == null || skill.isBlank()) return "";
        
        String s = skill.toLowerCase(Locale.ROOT);
        
        // Strip common punctuation but keep + # . - for tech terms (C++, C#, .NET, etc.)
        s = s.replaceAll("[,;()\\[\\]{}/\\\\\"'`]", " ");
        
        // Remove leading qualifier words
        s = s.replaceAll("^\\s*(strong|solid|excellent|good|great|deep|proven|demonstrated)\\s+", "");
        
        // Remove trailing suffix words
        s = s.replaceAll("\\s+(experience|skills?|knowledge|proficiency|background|ability|abilities|developer|engineer|programmer|architect|specialist|admin|administrator)\\b\\s*$", "");
        
        // Remove "with" prefix (e.g., "experience with Java" -> "java")
        s = s.replaceAll("^\\s*with\\s+", "");
        
        // Collapse multiple spaces
        s = s.replaceAll("\\s+", " ").trim();
        
        return s;
    }
    
    /**
     * Determines if a normalized string is likely to be an actual skill.
     * Filters out stopwords, location names, company names, and other junk.
     */
    private boolean isLikelySkill(String skill) {
        if (skill == null || skill.isBlank()) return false;
        
        String s = skill.toLowerCase(Locale.ROOT).trim();
        
        // Reject very short strings unless whitelisted
        if (s.length() < 3 && !SHORT_TECH_WHITELIST.contains(s)) {
            return false;
        }
        
        // Reject stopwords
        if (STOPWORDS.contains(s)) {
            return false;
        }
        
        // Reject location keywords
        for (String loc : LOCATION_KEYWORDS) {
            if (s.contains(loc)) {
                return false;
            }
        }
        
        // Reject pure numbers
        if (s.matches("^\\d+$")) {
            return false;
        }
        
        // Reject strings that are just punctuation or single letters repeated
        if (s.matches("^[^a-z0-9]+$") || s.matches("^(.)\\1+$")) {
            return false;
        }
        
        // Reject overly generic phrases
        if (s.matches(".*(years?|months?|weeks?|days?)\\s+(of\\s+)?(experience|work|working).*")) {
            return false;
        }
        
        return true;
    }

    private List<String> extractSkillsFromResume(String text) {
        Set<String> skillSet = new LinkedHashSet<>();
        if (text.isEmpty()) return new ArrayList<>(skillSet);

        // Strategy 1: Look for Skills/Technical Skills section
        String lower = text.toLowerCase(Locale.ROOT);
        int skillsIdx = findSectionStart(lower, "skills", "technical skills", "technologies");
        
        if (skillsIdx >= 0) {
            String section = extractSectionContent(text, skillsIdx);
            for (String token : splitSkillTokens(section)) {
                String normalized = normalizeSkill(token);
                if (isLikelySkill(normalized)) {
                    skillSet.add(normalized);
                }
            }
        }

        // Strategy 2: Scan for tech-like repeated tokens throughout the resume
        List<String> candidateTokens = extractTechTokens(text);
        for (String token : candidateTokens) {
            String normalized = normalizeSkill(token);
            if (isLikelySkill(normalized)) {
                skillSet.add(normalized);
            }
        }

        return new ArrayList<>(skillSet);
    }

    /**
     * Extracts tech-like tokens from text (capitalized words, known patterns).
     * Returns raw tokens before normalization.
     */
    private List<String> extractTechTokens(String text) {
        List<String> tokens = new ArrayList<>();
        
        // Pattern 1: All-caps words (SQL, AWS, API, etc.)
        Pattern upperPattern = Pattern.compile("\\b([A-Z]{2,})\\b");
        Matcher upperMatcher = upperPattern.matcher(text);
        while (upperMatcher.find()) {
            tokens.add(upperMatcher.group(1));
        }
        
        // Pattern 2: Common tech terms with mixed case
        Matcher techMatcher = TECH_WORD.matcher(text);
        while (techMatcher.find()) {
            String word = techMatcher.group(1);
            // Keep if it contains special tech chars or is commonly capitalized
            if (word.matches(".*[+#.].*") || word.matches("[A-Z][a-z]+")) {
                tokens.add(word);
            }
        }
        
        return tokens;
    }

    private int findSectionStart(String lowerText, String... headers) {
        for (String header : headers) {
            // Look for "\nHeader" or start of text
            int idx = lowerText.indexOf("\n" + header);
            if (idx >= 0) return idx + 1 + header.length();
            if (lowerText.startsWith(header)) return header.length();
        }
        return -1;
    }

    private String extractSectionContent(String text, int startIdx) {
        if (startIdx < 0 || startIdx >= text.length()) return "";
        
        String after = text.substring(startIdx);
        // Limit to next section (double newline) or 500 chars
        String[] parts = after.split("\n\n", 2);
        String section = parts[0];
        
        if (section.length() > 500) {
            section = section.substring(0, 500);
        }
        
        return section;
    }

    private String[] splitSkillTokens(String section) {
        // Split on commas, semicolons, pipes, newlines
        return section.split("[,;|\\n]+");
    }

    private List<String> extractExperiences(String text) {
        List<String> exps = new ArrayList<>();
        if (text.isEmpty()) return exps;

        // Split by one or more blank lines (two or more newlines)
        String[] chunks = text.split("\n\s*\n+");
        for (String c : chunks) {
            String s = c.trim();
            if (!s.isBlank() && s.length() > 20) {
                exps.add(s);
            }
        }
        return exps;
    }

    private String extractTitle(String text) {
        if (text == null || text.isBlank()) return "";
        String[] lines = text.split("\n");
        // look for explicit "Title:" label
        for (String line : lines) {
            String l = line.trim();
            if (l.toLowerCase(Locale.ROOT).startsWith("title:")) {
                return l.substring(l.indexOf(":") + 1).trim();
            }
        }
        // fallback: first non-empty line
        for (String line : lines) {
            String l = line.trim();
            if (!l.isEmpty()) return l;
        }
        return "";
    }
}
