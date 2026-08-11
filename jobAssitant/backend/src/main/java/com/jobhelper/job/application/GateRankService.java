package com.jobhelper.job.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.profile.infrastructure.WorkAuthorizationEntity;

@Service
public class GateRankService {

    private static final Pattern YEARS_EXPERIENCE = Pattern.compile(
            "\\b(\\d+)\\s*\\+?\\s*(?:years?|yrs?)(?:\\s+of)?\\s+(?:experience|exp)\\b",
            Pattern.CASE_INSENSITIVE);

    /** Strong software-role signals in title (or title+short desc). */
    private static final Pattern SWE_TITLE = Pattern.compile(
            "(?i)(\\bsoftware\\b|\\bdeveloper\\b|\\bdevops\\b|\\bsre\\b|\\bprogrammer\\b|"
                    + "\\bfull[\\s-]?stack\\b|\\bbackend\\b|\\bfront[\\s-]?end\\b|\\bswe\\b|\\bsde\\b|"
                    + "\\bjava\\b|\\bpython\\b|\\breact\\b|\\bnode\\.?js\\b|\\b\\.net\\b|"
                    + "site reliability|machine learning|\\bml engineer\\b|\\bai engineer\\b|"
                    + "data engineer|platform engineer|security engineer|cloud engineer|"
                    + "systems development|qa engineer|test engineer|software test|"
                    + "localisation qa|localization qa)");

    /** Bare “engineer” only counts with a software-adjacent qualifier. */
    private static final Pattern ENGINEER_SOFT = Pattern.compile(
            "(?i)\\b(software|platform|systems?|security|cloud|data|ml|ai|qa|test|devops|"
                    + "reliability|infrastructure|application|product)\\b[^\\n]{0,40}\\bengineer\\b|"
                    + "\\bengineer\\b[^\\n]{0,40}\\b(software|platform|backend|frontend|full[\\s-]?stack)\\b");

    private static final Pattern NON_SWE_TITLE = Pattern.compile(
            "(?i)(\\bsales\\b|\\bmarketing\\b|\\baccount(ing|ant| executive)?\\b|\\bhr\\b|"
                    + "human resources?|recruit(er|ing|ment)?|talent acquisition|"
                    + "\\bnurse\\b|\\bteacher\\b|quantity surveyor|cost manager|"
                    + "business development|customer support(?! engineer)|deskside support|"
                    + "end user support|investment analyst|fund accounting|"
                    + "equity derivatives|consulting support|\\blvia\\b|"
                    + "leadership programme|project manager(?!.*software)|"
                    + "supply chain|benefits and wellness|university recruiting|"
                    + "\\bcmo\\b|church|ministry residency|medtech|medical information)");

    private static final Pattern JUNIOR_MARKER = Pattern.compile(
            "(?i)(\\bintern\\b|\\binternship\\b|\\bgraduate\\b|\\bjunior\\b|entry[\\s-]?level|"
                    + "new grad|university recruit|early career|apprentice)");

    /** Word-boundary senior tokens in title (avoid "Leadership"/false substrings). */
    private static final Pattern SENIOR_TITLE = Pattern.compile(
            "(?i)\\b(senior|staff|principal|director|head)\\b|"
                    + "\\b(sr)\\.?\\b|"
                    + "\\blead\\b(?!\\s*(ership|er|ing)\\b)|"
                    + "\\bmanager\\b|"
                    + "\\barchitect\\b");

    private static final Pattern EXPERIENCED_TITLE = Pattern.compile(
            "(?i)(\\bexperienced\\b|mid[\\s-]?level|mid[\\s-]?senior|\\bmid\\b\\s*\\+|\\bexpert\\b)");

    /** Explicit mid/senior ladder in title (SWE II / Engineer III). */
    private static final Pattern LEVELLED_TITLE = Pattern.compile(
            "(?i)\\b(engineer|developer|sde|sre|swe)\\s*(ii|iii|iv|[2-6])\\b|"
                    + "\\b(ii|iii|iv)\\b[^\\n]{0,24}\\b(engineer|developer)\\b");

    private static final Set<String> SENIOR_ENRICH = Set.of(
            "senior", "lead", "staff", "principal", "c_level");
    private static final Set<String> JUNIOR_ENRICH = Set.of("intern", "junior");
    private static final Set<String> MID_ENRICH = Set.of("middle");

    public void evaluate(CanonicalJobEntity job, ProfileEntity profile) {
        List<Map<String, Object>> dims = new ArrayList<>();
        dims.add(locationGate(job));
        dims.add(workAuthGate(job, profile));
        dims.add(roleGate(job));
        Map<String, Object> seniority = seniorityGate(job);
        dims.add(seniority);
        dims.add(languageGate(job, profile));

        String gate = aggregateGate(dims);
        job.setGateStatus(gate);
        job.setGateDimensionsJson(toJson(dims));

        if ("FAILED".equals(gate)) {
            job.setHiddenByDefault(true);
            job.setRankTier("UNRANKED");
            job.setRankFactorsJson("[]");
            return;
        }
        job.setHiddenByDefault(false);
        boolean juniorFit = "JUNIOR_FIT".equals(String.valueOf(seniority.get("capTier")));
        List<Map<String, Object>> factors = rankFactors(job, profile, juniorFit);
        job.setRankFactorsJson(toJson(factors));
        job.setRankTier(aggregateRank(factors, juniorFit));
    }

    private Map<String, Object> locationGate(CanonicalJobEntity job) {
        String loc = safe(job.getLocation()).toLowerCase(Locale.ROOT);
        String status;
        if (loc.contains("ireland") || loc.contains("dublin") || loc.contains("cork") || loc.contains("galway")
                || loc.contains("remote") || loc.contains("limerick") || loc.contains("waterford")) {
            status = "PASS";
        } else if (loc.isBlank() || loc.contains("eu")) {
            status = "NEEDS_CONFIRMATION";
        } else {
            status = "FAIL";
        }
        return Map.of("dimension", "LOCATION", "status", status, "explanation", "location=" + job.getLocation());
    }

    private Map<String, Object> workAuthGate(CanonicalJobEntity job, ProfileEntity profile) {
        if (job.getExpectedStartDate() == null) {
            return Map.of("dimension", "WORK_AUTH", "status", "NEEDS_CONFIRMATION",
                    "explanation", "expectedStartDate missing");
        }
        boolean covered = profile != null
                && profile.getIdentityStatus() != null
                && !profile.getIdentityStatus().isBlank()
                && (profile.getIdentityValidUntil() == null
                        || !job.getExpectedStartDate().isAfter(profile.getIdentityValidUntil()));
        return Map.of("dimension", "WORK_AUTH", "status", covered ? "PASS" : "FAIL",
                "explanation", covered ? "identityStatus covers start" : "no covering identityStatus");
    }

    private boolean covers(WorkAuthorizationEntity wa, CanonicalJobEntity job) {
        if (job.getExpectedStartDate() == null) {
            return false;
        }
        if (wa.getValidFrom() != null && job.getExpectedStartDate().isBefore(wa.getValidFrom())) {
            return false;
        }
        return wa.getValidUntil() == null || !job.getExpectedStartDate().isAfter(wa.getValidUntil());
    }

    /**
     * ROLE hard filter: keep software-development shaped titles; drop marketing/accounting/etc.
     * Title is authoritative — JD may mention “software” incidentally (e.g. Technical Writer).
     */
    private Map<String, Object> roleGate(CanonicalJobEntity job) {
        String title = safe(job.getTitle());

        if (NON_SWE_TITLE.matcher(title).find() && !hasSoftwareSignal(title)) {
            return Map.of("dimension", "ROLE", "status", "FAIL",
                    "explanation", "non-software title excluded: " + trimExpl(title));
        }
        if (hasSoftwareSignal(title)) {
            return Map.of("dimension", "ROLE", "status", "PASS",
                    "explanation", "software-role signal in title");
        }
        return Map.of("dimension", "ROLE", "status", "FAIL",
                "explanation", "no software-role signal: " + trimExpl(title));
    }

    private boolean hasSoftwareSignal(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return SWE_TITLE.matcher(text).find() || ENGINEER_SOFT.matcher(text).find();
    }

    /**
     * SENIORITY: hard-hide clear senior; soft-cap years-in-JD; prefer enrichment + title junior markers.
     * Cap tiers: JUNIOR_FIT | MID_OK | SENIOR_HIDE (SENIOR_HIDE ⇒ FAIL).
     */
    private Map<String, Object> seniorityGate(CanonicalJobEntity job) {
        String enrich = safe(job.getSeniority()).trim().toLowerCase(Locale.ROOT);
        String title = safe(job.getTitle());
        String titleLc = title.toLowerCase(Locale.ROOT);
        String desc = safe(job.getDescription()).toLowerCase(Locale.ROOT);
        String descHead = desc.substring(0, Math.min(desc.length(), 1200));

        boolean juniorTitle = JUNIOR_MARKER.matcher(titleLc).find();
        boolean juniorEnrich = JUNIOR_ENRICH.contains(enrich);
        boolean junior = juniorTitle || juniorEnrich;

        if (SENIOR_ENRICH.contains(enrich) && !junior) {
            return Map.of(
                    "dimension", "SENIORITY",
                    "status", "FAIL",
                    "capTier", "SENIOR_HIDE",
                    "explanation", "enrichment seniority=" + enrich);
        }
        if (SENIOR_TITLE.matcher(title).find() && !junior) {
            return Map.of(
                    "dimension", "SENIORITY",
                    "status", "FAIL",
                    "capTier", "SENIOR_HIDE",
                    "explanation", "senior token in title: " + trimExpl(title));
        }
        if (EXPERIENCED_TITLE.matcher(titleLc).find() && !junior) {
            return Map.of(
                    "dimension", "SENIORITY",
                    "status", "FAIL",
                    "capTier", "SENIOR_HIDE",
                    "explanation", "experienced/mid token in title");
        }
        if (LEVELLED_TITLE.matcher(title).find() && !junior) {
            return Map.of(
                    "dimension", "SENIORITY",
                    "status", "FAIL",
                    "capTier", "SENIOR_HIDE",
                    "explanation", "levelled title (II/III/…) not junior");
        }

        if (junior) {
            return Map.of(
                    "dimension", "SENIORITY",
                    "status", "PASS",
                    "capTier", "JUNIOR_FIT",
                    "explanation", juniorEnrich
                            ? "enrichment seniority=" + enrich
                            : "junior/graduate/intern marker in title");
        }

        int maxYears = maxYearsMentioned(descHead);
        if (maxYears >= 5) {
            return Map.of(
                    "dimension", "SENIORITY",
                    "status", "NEEDS_CONFIRMATION",
                    "capTier", "MID_OK",
                    "explanation", maxYears + "+ years in JD (soft — not hard-hidden)");
        }
        if (MID_ENRICH.contains(enrich)) {
            return Map.of(
                    "dimension", "SENIORITY",
                    "status", "PASS",
                    "capTier", "MID_OK",
                    "explanation", "enrichment seniority=middle");
        }
        return Map.of(
                "dimension", "SENIORITY",
                "status", "PASS",
                "capTier", "MID_OK",
                "explanation", "no senior signal — mid/generic OK");
    }

    private int maxYearsMentioned(String text) {
        int max = 0;
        Matcher years = YEARS_EXPERIENCE.matcher(text);
        while (years.find()) {
            max = Math.max(max, Integer.parseInt(years.group(1)));
        }
        return max;
    }

    private Map<String, Object> languageGate(CanonicalJobEntity job, ProfileEntity profile) {
        String desc = safe(job.getDescription()).toLowerCase(Locale.ROOT);
        if (!desc.contains("mandatory language") && !desc.contains("must speak")) {
            return Map.of("dimension", "LANGUAGE", "status", "PASS", "explanation", "English/unspecified");
        }
        boolean hasEn = profile != null && profile.getLanguageProficiencies().stream()
                .anyMatch(l -> "en".equalsIgnoreCase(l.getLanguageCode()) || "english".equalsIgnoreCase(l.getLanguageCode()));
        return Map.of("dimension", "LANGUAGE", "status", hasEn ? "PASS" : "NEEDS_CONFIRMATION",
                "explanation", "mandatory language clause present");
    }

    private String aggregateGate(List<Map<String, Object>> dims) {
        boolean needs = false;
        boolean unknown = false;
        for (Map<String, Object> d : dims) {
            String s = String.valueOf(d.get("status"));
            if ("FAIL".equals(s)) {
                return "FAILED";
            }
            if ("NEEDS_CONFIRMATION".equals(s)) {
                needs = true;
            }
            if ("UNKNOWN".equals(s)) {
                unknown = true;
            }
        }
        if (needs) {
            return "NEEDS_CONFIRMATION";
        }
        if (unknown) {
            return "UNKNOWN";
        }
        return "PASSED";
    }

    private List<Map<String, Object>> rankFactors(CanonicalJobEntity job, ProfileEntity profile, boolean juniorFit) {
        List<Map<String, Object>> factors = new ArrayList<>();
        factors.add(Map.of("factor", "TARGET_ROLE", "vote", goalVote(job, profile)));
        factors.add(Map.of("factor", "SKILL", "vote", skillVote(job, profile)));
        factors.add(Map.of("factor", "FRESHNESS", "vote", "POSITIVE"));
        factors.add(Map.of("factor", "LOCATION_FIT", "vote",
                safe(job.getLocation()).toLowerCase(Locale.ROOT).contains("dublin") ? "POSITIVE" : "NEUTRAL"));
        factors.add(Map.of("factor", "JUNIOR_FIT", "vote", juniorFit ? "POSITIVE" : "NEUTRAL"));
        return factors;
    }

    private String goalVote(CanonicalJobEntity job, ProfileEntity profile) {
        if (profile == null || profile.getJobSeekingGoal() == null || profile.getJobSeekingGoal().isBlank()) {
            return hasSoftwareSignal(safe(job.getTitle())) ? "POSITIVE" : "NEUTRAL";
        }
        String title = safe(job.getTitle()).toLowerCase(Locale.ROOT);
        for (String token : profile.getJobSeekingGoal().toLowerCase(Locale.ROOT).split("\\s+")) {
            if (token.length() > 3 && title.contains(token)) {
                return "POSITIVE";
            }
        }
        return "NEUTRAL";
    }

    private String skillVote(CanonicalJobEntity job, ProfileEntity profile) {
        if (profile == null || profile.getSkills().isEmpty()) {
            return "NEUTRAL";
        }
        String blob = (safe(job.getTitle()) + " " + safe(job.getDescription())).toLowerCase(Locale.ROOT);
        for (var skill : profile.getSkills()) {
            String name = safe(skill.getSkillName()).toLowerCase(Locale.ROOT);
            if (name.length() > 1 && blob.contains(name)) {
                return "POSITIVE";
            }
        }
        return "NEUTRAL";
    }

    private String aggregateRank(List<Map<String, Object>> factors, boolean juniorFit) {
        int pos = 0;
        int neg = 0;
        for (Map<String, Object> f : factors) {
            String v = String.valueOf(f.get("vote"));
            if ("POSITIVE".equals(v)) {
                pos++;
            }
            if ("NEGATIVE".equals(v)) {
                neg++;
            }
        }
        if (juniorFit && neg == 0) {
            return "HIGH";
        }
        if (pos >= 3 && neg == 0) {
            return "HIGH";
        }
        if (neg >= 2) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String trimExpl(String s) {
        String t = safe(s);
        return t.length() <= 80 ? t : t.substring(0, 80);
    }

    private String toJson(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{');
            boolean first = true;
            for (var e : list.get(i).entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(e.getKey()).append("\":\"")
                        .append(String.valueOf(e.getValue()).replace("\"", "'")).append('"');
            }
            sb.append('}');
        }
        sb.append(']');
        return sb.toString();
    }
}
