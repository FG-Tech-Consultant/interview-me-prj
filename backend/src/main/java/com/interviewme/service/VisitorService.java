package com.interviewme.service;

import com.interviewme.billing.exception.InsufficientBalanceException;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.billing.config.BillingProperties;
import com.interviewme.dto.visitor.*;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import com.interviewme.common.exception.PublicProfileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final VisitorSessionRepository visitorSessionRepository;
    private final VisitorChatLogRepository visitorChatLogRepository;
    private final ContactRevealRepository contactRevealRepository;
    private final ProfileRepository profileRepository;
    private final CoinWalletService coinWalletService;
    private final BillingProperties billingProperties;

    @Transactional
    public VisitorIdentifyResponse identify(String slug, VisitorIdentifyRequest request, String ipAddress, String userAgent) {
        Profile profile = profileRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new PublicProfileNotFoundException(slug));

        Visitor visitor = new Visitor();
        visitor.setTenantId(profile.getTenantId());
        visitor.setProfileId(profile.getId());
        visitor.setName(request.name());
        visitor.setCompany(request.company());
        visitor.setJobRole(request.jobRole());
        visitor.setLinkedinUrl(request.linkedinUrl());
        visitor.setContactEmail(request.contactEmail());
        visitor.setContactWhatsapp(request.contactWhatsapp());
        visitor.setIpAddress(ipAddress);
        visitor.setUserAgent(userAgent);
        visitor.setVisitorToken(UUID.randomUUID().toString());

        visitor = visitorRepository.save(visitor);
        log.info("Visitor identified: id={} profile={} name={} company={}", visitor.getId(), profile.getId(), visitor.getName(), visitor.getCompany());

        return new VisitorIdentifyResponse(visitor.getId(), visitor.getVisitorToken());
    }

    @Transactional
    public VisitorSession getOrCreateSession(String visitorToken, String ipAddress, String userAgent) {
        Visitor visitor = visitorRepository.findByVisitorToken(visitorToken)
                .orElseThrow(() -> new IllegalArgumentException("Invalid visitor token"));

        VisitorSession session = new VisitorSession();
        session.setVisitorId(visitor.getId());
        session.setTenantId(visitor.getTenantId());
        session.setProfileId(visitor.getProfileId());
        session.setSessionToken(UUID.randomUUID().toString());
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        return visitorSessionRepository.save(session);
    }

    @Transactional
    public void logChatMessage(String visitorToken, String role, String content, Integer tokensUsed) {
        Visitor visitor = visitorRepository.findByVisitorToken(visitorToken).orElse(null);
        if (visitor == null) return;

        List<VisitorSession> sessions = visitorSessionRepository.findByVisitorIdOrderByStartedAtDesc(visitor.getId());
        VisitorSession session;
        if (sessions.isEmpty()) {
            session = new VisitorSession();
            session.setVisitorId(visitor.getId());
            session.setTenantId(visitor.getTenantId());
            session.setProfileId(visitor.getProfileId());
            session.setSessionToken(UUID.randomUUID().toString());
            session = visitorSessionRepository.save(session);
        } else {
            session = sessions.get(0);
        }

        VisitorChatLog chatLog = new VisitorChatLog();
        chatLog.setVisitorSessionId(session.getId());
        chatLog.setTenantId(visitor.getTenantId());
        chatLog.setRole(role);
        chatLog.setContent(content);
        chatLog.setTokensUsed(tokensUsed);
        visitorChatLogRepository.save(chatLog);

        session.setMessageCount(session.getMessageCount() + 1);
        visitorSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public VisitorStatsResponse getVisitorStats(Long tenantId, Long profileId) {
        Profile profile = profileRepository.findById(profileId).orElse(null);
        long profileViews = profile != null ? profile.getViewCount() : 0;
        long totalVisitors = visitorRepository.countByProfileId(profileId);
        long chatVisitors = visitorRepository.countChatVisitorsByProfileId(profileId);
        return new VisitorStatsResponse(profileViews, totalVisitors, chatVisitors);
    }

    @Transactional(readOnly = true)
    public Page<VisitorResponse> getVisitorsByProfile(Long profileId, Long tenantId, Pageable pageable) {
        Page<Visitor> visitors = visitorRepository.findByProfileId(profileId, pageable);
        return visitors.map(v -> toVisitorResponse(v, tenantId));
    }

    @Transactional(readOnly = true)
    public Page<VisitorResponse> getAllVisitors(Long tenantId, Pageable pageable) {
        Page<Visitor> visitors;
        if (tenantId == null) {
            visitors = visitorRepository.findAllOrderByCreatedAtDesc(pageable);
        } else {
            visitors = visitorRepository.findByTenantId(tenantId, pageable);
        }
        return visitors.map(v -> toVisitorResponse(v, v.getTenantId()));
    }

    @Transactional(readOnly = true)
    public List<VisitorSessionResponse> getVisitorSessions(Long visitorId) {
        List<VisitorSession> sessions = visitorSessionRepository.findByVisitorIdOrderByStartedAtDesc(visitorId);
        return sessions.stream().map(s -> {
            Visitor visitor = visitorRepository.findById(s.getVisitorId()).orElse(null);
            return new VisitorSessionResponse(
                    s.getId(), s.getVisitorId(),
                    visitor != null ? visitor.getName() : "Unknown",
                    visitor != null ? visitor.getCompany() : "Unknown",
                    s.getStartedAt(), s.getEndedAt(), s.getMessageCount());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<VisitorChatLogResponse> getSessionMessages(Long sessionId) {
        return visitorChatLogRepository.findByVisitorSessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(l -> new VisitorChatLogResponse(l.getId(), l.getRole(), l.getContent(), l.getTokensUsed(), l.getCreatedAt()))
                .toList();
    }

    @Transactional
    public VisitorResponse revealContact(Long tenantId, Long visitorId) {
        var existing = contactRevealRepository.findByTenantIdAndVisitorIdAndExpiresAtAfter(tenantId, visitorId, Instant.now());
        if (existing.isPresent()) {
            Visitor visitor = visitorRepository.findById(visitorId).orElseThrow();
            return toVisitorResponse(visitor, tenantId);
        }

        int cost = billingProperties.getCosts().getOrDefault("CONTACT_REVEAL", 5);
        try {
            coinWalletService.spend(tenantId, cost, RefType.CONTACT_REVEAL, visitorId.toString(), "Reveal visitor contact");
        } catch (InsufficientBalanceException e) {
            throw e;
        }

        ContactReveal reveal = new ContactReveal();
        reveal.setTenantId(tenantId);
        reveal.setVisitorId(visitorId);
        reveal.setRevealedAt(Instant.now());
        reveal.setExpiresAt(Instant.now().plus(12, ChronoUnit.HOURS));
        contactRevealRepository.save(reveal);

        Visitor visitor = visitorRepository.findById(visitorId).orElseThrow();
        return toVisitorResponse(visitor, tenantId);
    }

    private VisitorResponse toVisitorResponse(Visitor v, Long tenantId) {
        boolean isRevealed = contactRevealRepository
                .findByTenantIdAndVisitorIdAndExpiresAtAfter(tenantId, v.getId(), Instant.now())
                .isPresent();

        List<VisitorSession> sessions = visitorSessionRepository.findByVisitorIdOrderByStartedAtDesc(v.getId());
        int sessionCount = sessions.size();
        int totalMessages = sessions.stream().mapToInt(VisitorSession::getMessageCount).sum();

        return new VisitorResponse(
                v.getId(), v.getName(), v.getCompany(), v.getJobRole(),
                isRevealed ? v.getLinkedinUrl() : null,
                isRevealed ? v.getContactEmail() : null,
                isRevealed ? v.getContactWhatsapp() : null,
                v.getCreatedAt(), sessionCount, totalMessages, isRevealed);
    }
}
