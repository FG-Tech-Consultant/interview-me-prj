package com.interviewme.service;

import com.interviewme.aichat.service.LlmRouterService;
import com.interviewme.dto.settings.AiSettingsResponse;
import com.interviewme.dto.settings.AiSettingsResponse.AvailableProvider;
import com.interviewme.model.Tenant;
import com.interviewme.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSettingsService {

    private final TenantRepository tenantRepository;
    private final LlmRouterService llmRouterService;

    @Transactional(readOnly = true)
    public Optional<String> getAiProvider(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(Tenant::getSettings)
                .filter(Objects::nonNull)
                .map(s -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ai = (Map<String, Object>) s.get("ai");
                    return ai;
                })
                .filter(Objects::nonNull)
                .map(ai -> (String) ai.get("provider"));
    }

    @Transactional(readOnly = true)
    public Optional<String> getAiChatModel(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(Tenant::getSettings)
                .filter(Objects::nonNull)
                .map(s -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ai = (Map<String, Object>) s.get("ai");
                    return ai;
                })
                .filter(Objects::nonNull)
                .map(ai -> (String) ai.get("chatModel"));
    }

    @Transactional
    public void updateAiSettings(Long tenantId, String provider, String chatModel) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        Map<String, Object> settings = tenant.getSettings();
        if (settings == null) {
            settings = new HashMap<>();
        }

        if (provider == null && chatModel == null) {
            // Reset AI settings to global defaults
            settings.remove("ai");
            log.info("Reset AI settings to global defaults for tenantId={}", tenantId);
        } else {
            Map<String, Object> aiSettings = new HashMap<>();
            if (provider != null) {
                aiSettings.put("provider", provider);
            }
            if (chatModel != null) {
                aiSettings.put("chatModel", chatModel);
            }
            settings.put("ai", aiSettings);
            log.info("Updated AI settings for tenantId={} provider={} chatModel={}", tenantId, provider, chatModel);
        }

        tenant.setSettings(settings);
        tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public AiSettingsResponse getAiSettings(Long tenantId) {
        String currentProvider = getAiProvider(tenantId).orElse(null);
        String currentModel = getAiChatModel(tenantId).orElse(null);

        List<AvailableProvider> availableProviders = llmRouterService.getAvailableProviders().stream()
                .sorted()
                .map(name -> new AvailableProvider(name, llmRouterService.getDefaultModel(name)))
                .collect(Collectors.toList());

        log.debug("AI settings for tenantId={}: provider={} chatModel={} availableProviders={}",
                tenantId, currentProvider, currentModel, availableProviders.size());

        return new AiSettingsResponse(currentProvider, currentModel, availableProviders);
    }
}
