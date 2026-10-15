package com.stubdeck.support;

import com.stubdeck.model.RequestPattern;
import com.stubdeck.model.WireMockMapping;

public class TestUtils {

    public static WireMockMapping mapping(String id) {
        return mapping(id, "GET", "/api/test");
    }

    public static WireMockMapping mapping(String id, String method, String url) {
        RequestPattern req = new RequestPattern();
        req.setMethod(method);
        req.setUrl(url);
        WireMockMapping m = new WireMockMapping();
        m.setId(id);
        m.setFileName(id != null ? id + ".json" : null);
        m.setRequest(req);
        return m;
    }

    public static WireMockMapping mapping(String method, String urlPattern) {
        RequestPattern req = new RequestPattern();
        req.setMethod(method);
        req.setUrlPattern(urlPattern);
        WireMockMapping m = new WireMockMapping();
        m.setRequest(req);
        return m;
    }
}
