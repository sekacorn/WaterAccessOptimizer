package com.water.auth.repository;

import com.water.auth.model.SsoConfig;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * SSO Configuration Repository
 *
 * Manages persistence of SSO configurations
 */
@Repository
public class SsoConfigRepository {

    // In-memory storage for demo (use JPA/JDBC in production)
    private final Map<UUID, SsoConfig> store = new HashMap<>();
    private final Map<String, UUID> domainIndex = new HashMap<>();

    public void save(SsoConfig config) {
        store.put(config.getEnterpriseId(), config);

        // Index by allowed domains
        if (config.getAllowedDomains() != null) {
            for (String domain : config.getAllowedDomains()) {
                domainIndex.put(domain, config.getEnterpriseId());
            }
        }
    }

    public void update(SsoConfig config) {
        save(config);
    }

    public Optional<SsoConfig> findByEnterpriseId(UUID enterpriseId) {
        return Optional.ofNullable(store.get(enterpriseId));
    }

    public Optional<SsoConfig> findByDomain(String domain) {
        UUID enterpriseId = domainIndex.get(domain);
        if (enterpriseId != null) {
            return Optional.ofNullable(store.get(enterpriseId));
        }
        return Optional.empty();
    }

    public void deleteByEnterpriseId(UUID enterpriseId) {
        SsoConfig config = store.remove(enterpriseId);
        if (config != null && config.getAllowedDomains() != null) {
            for (String domain : config.getAllowedDomains()) {
                domainIndex.remove(domain);
            }
        }
    }
}
