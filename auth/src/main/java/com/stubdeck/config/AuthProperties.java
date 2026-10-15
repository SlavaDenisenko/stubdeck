package com.stubdeck.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "wiremock.auth")
@Data
@Component
public class AuthProperties {

    private boolean enabled = false;
    private AuthProviderType providerType = AuthProviderType.NONE;
    private Ldap ldap = new Ldap();
    private Memory memory = new Memory();

    @Data
    public static class Ldap {
        private String url = "ldap://localhost:389";
        private String bindDn = "";
        private String password = "";
        private String baseDn = "dc=company,dc=com";
        private String userSearchBase = "ou=Users";
        private String userSearchFilter = "(sAMAccountName={0})";
        private String groupSearchBase = "ou=Groups";
        private String groupSearchFilter = "(member={0})";
        private String departmentAttribute = "department";
        private Map<String, String> roleMappings = new HashMap<>();
    }

    @Data
    public static class Memory {
        private List<UserConfig> users = new ArrayList<>();
    }

    @Data
    public static class UserConfig {
        private String username;
        private String password;
        private List<String> roles = new ArrayList<>();
    }
}
