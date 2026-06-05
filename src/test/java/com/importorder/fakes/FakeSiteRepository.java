package com.importorder.fakes;

import com.importorder.model.SiteInfo;
import com.importorder.repository.SiteRepository;

import java.util.HashMap;
import java.util.Map;

public class FakeSiteRepository extends SiteRepository {

    private Map<String, SiteInfo> database = new HashMap<>();

    @Override
    public SiteInfo findByCode(String siteCode) {
        return database.get(siteCode);
    }

    public void save(SiteInfo site) {
        database.put(site.getSiteCode(), site);
    }
}
