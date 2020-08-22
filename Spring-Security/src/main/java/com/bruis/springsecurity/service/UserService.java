package com.bruis.springsecurity.service;

import com.bruis.springsecurity.dto.User;

import java.util.Set;

/**
 * @author LuoHaiYang
 */
public interface UserService {

    User findByUsername(String username);

    Set<String> findPermissions(String username);
}
