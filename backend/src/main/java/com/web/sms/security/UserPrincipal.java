package com.web.sms.security;

import com.web.sms.entity.Admin;
import com.web.sms.entity.Incharge;
import com.web.sms.enums.Role;
import com.web.sms.entity.Student;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {
    private Long id;
    private String loginId;
    private String password;
    private Role role;
    private String name;
    private String status;

    public UserPrincipal(Long id, String loginId, String password, Role role, String name, String status) {
        this.id = id;
        this.loginId = loginId;
        this.password = password;
        this.role = role;
        this.name = name;
        this.status = status;
    }

    public static UserPrincipal fromStudent(Student s) {
        return new UserPrincipal(s.getId(), s.getRollNumber(), s.getPassword(), Role.STUDENT, s.getName(), s.getStatus());
    }

    public static UserPrincipal fromIncharge(Incharge i) {
        return new UserPrincipal(i.getId(), i.getTeacherId(), i.getPassword(), Role.INCHARGE, i.getName(), i.getStatus());
    }

    public static UserPrincipal fromAdmin(Admin a) {
        return new UserPrincipal(a.getId(), a.getAdminId(), a.getPassword(), Role.ADMIN, a.getName(), a.getStatus());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == null || "ACTIVE".equalsIgnoreCase(status);
    }
    
    public Long getId() { return id; }
    public String getLoginId() { return loginId; }
    public Role getRole() { return role; }
    public String getName() { return name; }
    public String getStatus() { return status; }
}
