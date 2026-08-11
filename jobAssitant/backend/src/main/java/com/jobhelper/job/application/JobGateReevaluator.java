package com.jobhelper.job.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jobhelper.job.infrastructure.CanonicalJobEntity;
import com.jobhelper.job.infrastructure.CanonicalJobRepository;
import com.jobhelper.profile.infrastructure.ProfileEntity;
import com.jobhelper.profile.infrastructure.ProfileRepository;

/**
 * Re-applies Gate/Rank (ROLE + Cap-aware SENIORITY) to existing jobs after deploy.
 */
@Component
public class JobGateReevaluator {

    private static final Logger log = LoggerFactory.getLogger(JobGateReevaluator.class);

    private final CanonicalJobRepository jobRepository;
    private final ProfileRepository profileRepository;
    private final GateRankService gateRankService;

    public JobGateReevaluator(
            CanonicalJobRepository jobRepository,
            ProfileRepository profileRepository,
            GateRankService gateRankService) {
        this.jobRepository = jobRepository;
        this.profileRepository = profileRepository;
        this.gateRankService = gateRankService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void reevaluateAll() {
        ProfileEntity profile = profileRepository.findSingleton().orElse(null);
        int n = 0;
        int hidden = 0;
        for (CanonicalJobEntity job : jobRepository.findAll()) {
            gateRankService.evaluate(job, profile);
            jobRepository.save(job);
            n++;
            if (job.isHiddenByDefault()) {
                hidden++;
            }
        }
        log.info("Re-evaluated {} jobs after Gate ROLE/SENIORITY update; hiddenByDefault={}", n, hidden);
    }
}
