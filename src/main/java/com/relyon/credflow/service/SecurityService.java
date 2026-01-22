package com.relyon.credflow.service;

import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.repository.AccountRepository;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public boolean canAccessAccount(AuthenticatedUser user, Long accountId) {
        if (user == null || accountId == null) {
            return false;
        }
        return accountRepository.existsByIdAndUsersId(accountId, user.getUserId());
    }

    public boolean canModifyAccount(AuthenticatedUser user, Long accountId) {
        if (user == null || accountId == null) {
            return false;
        }
        return canAccessAccount(user, accountId) && canModify(user);
    }

    public boolean isOwnerOfAccount(AuthenticatedUser user, Long accountId) {
        if (user == null || accountId == null) {
            return false;
        }
        return canAccessAccount(user, accountId) && user.isOwner();
    }

    public boolean canAccessUser(AuthenticatedUser authenticatedUser, Long targetUserId) {
        if (authenticatedUser == null || targetUserId == null) {
            return false;
        }
        return userRepository.existsByIdAndAccountId(targetUserId, authenticatedUser.getAccountId());
    }

    public boolean canModify(AuthenticatedUser user) {
        if (user == null) {
            return false;
        }
        return !user.isReadOnly();
    }
}
