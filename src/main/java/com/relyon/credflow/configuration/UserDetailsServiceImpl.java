package com.relyon.credflow.configuration;

import com.relyon.credflow.model.user.AuthenticatedUser;
import com.relyon.credflow.model.user.User;
import com.relyon.credflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new AuthenticatedUser(
                user.getId(),
                user.getAccount().getId(),
                user.getEmail(),
                user.getName(),
                user.getPassword(),
                user.getRole()
        );
    }
}