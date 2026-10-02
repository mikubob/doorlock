package com.hnkjzyxy.ab.security.user;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.SpringSecurityCoreVersion;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import java.util.Collection;

/**
 * 认证用户信息，保存用户ID、凭据、权限及账户状态
 *
 * @author Absolute-cold
 * @version 1.0
 * @time 2022/7/12 14:15
 */

public class AccountUser implements UserDetails {
    /**
     * 序列化版本标识
     */
    private static final long serialVersionUID = SpringSecurityCoreVersion.SERIAL_VERSION_UID;
    /**
     * 日志记录器
     */
    private static final Log logger = LogFactory.getLog(User.class);
    /**
     * 用户名
     */
    private final String username;
    /**
     * 用户权限集合
     */
    private final Collection<? extends GrantedAuthority> authorities;
    /**
     * 账户是否未过期
     */
    private final boolean accountNonExpired;
    /**
     * 账户是否未锁定
     */
    private final boolean accountNonLocked;
    /**
     * 认证凭据是否未过期
     */
    private final boolean credentialsNonExpired;
    /**
     * 账户是否启用
     */
    private final boolean enabled;
    /**
     * 用户唯一编号
     */
    private Integer userId;
    /**
     * 认证密码
     */
    private String password;


    /**
     * 初始化认证用户
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param password 密码
     * @param authorities 用户权限集合
     */
    public AccountUser(Integer userId, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        this(userId, username, password, true, true, true, true, authorities);
    }

    /**
     * 初始化认证用户
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param password 密码
     * @param enabled 是否启用
     * @param accountNonExpired 账户是否未过期
     * @param credentialsNonExpired 凭据是否未过期
     * @param accountNonLocked 账户是否未锁定
     * @param authorities 用户权限集合
     */
    public AccountUser(Integer userId, String username, String password, boolean enabled, boolean accountNonExpired,
                       boolean credentialsNonExpired, boolean accountNonLocked,
                       Collection<? extends GrantedAuthority> authorities) {
        Assert.isTrue(username != null && !"".equals(username) && password != null,
                "Cannot pass null or empty values to constructor");
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.credentialsNonExpired = credentialsNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    /**
     * 获取认证用户ID
     *
     * @return 查询得到的数值
     */
    public Integer getUserId() {
        return userId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPassword() {
        return this.password;
    }

    /**
     * 设置认证用户密码
     *
     * @param password 密码
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "AccountUser{" +
                "userId=" + userId +
                ", password='" + password + '\'' +
                ", username='" + username + '\'' +
                ", authorities=" + authorities +
                ", accountNonExpired=" + accountNonExpired +
                ", accountNonLocked=" + accountNonLocked +
                ", credentialsNonExpired=" + credentialsNonExpired +
                ", enabled=" + enabled +
                '}';
    }
}
