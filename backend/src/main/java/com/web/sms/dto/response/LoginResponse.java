package com.web.sms.dto.response;

import com.web.sms.enums.Role;

public class LoginResponse {
    private String token;
    private UserInfo user;

    public LoginResponse(String token, UserInfo user) {
        this.token = token;
        this.user = user;
    }
    
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserInfo getUser() { return user; }
    public void setUser(UserInfo user) { this.user = user; }

    public static class UserInfo {
        private Long id;
        private String loginId;
        private String name;
        private Role role;
        private String email;

        public UserInfo(Long id, String loginId, String name, Role role, String email) {
            this.id = id;
            this.loginId = loginId;
            this.name = name;
            this.role = role;
            this.email = email;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getLoginId() { return loginId; }
        public void setLoginId(String loginId) { this.loginId = loginId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Role getRole() { return role; }
        public void setRole(Role role) { this.role = role; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
