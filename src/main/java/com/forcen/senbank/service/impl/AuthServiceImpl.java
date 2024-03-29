package com.forcen.senbank.service.impl;

import com.forcen.senbank.domain.Role;
import com.forcen.senbank.domain.User;
import com.forcen.senbank.repository.RoleRepository;
import com.forcen.senbank.repository.UserRepository;
import com.forcen.senbank.security.jwt.JwtTokenProvider;
import com.forcen.senbank.service.AuthService;
import com.forcen.senbank.web.rest.errors.EmailAlreadyUsedException;
import com.forcen.senbank.web.rest.errors.UsernameAlreadyUsedException;
import com.forcen.senbank.web.rest.vm.LoginVm;
import com.forcen.senbank.web.rest.vm.RegisterUserVm;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {

    private AuthenticationManager authenticationManager;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private final RoleRepository roleRepository;


    public AuthServiceImpl(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, RoleRepository roleRepository) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.roleRepository = roleRepository;
    }

    @Override
    public String login(LoginVm loginVm) {

        // Authenticate User with Username and Password from LoginVM (View Model)
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                loginVm.getUsernameOrEmail(), loginVm.getPassword()));

        // Set Authentication to Security Context Holder (Spring Security)
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT Token from Authentication Object (User Details)
        return jwtTokenProvider.generateToken(authentication);
    }

    @Override
    public User register(RegisterUserVm registerUserVm) {

        // Check if Username or Email is already taken by another User in Database and throw UsernameAlreadyUsedException or EmailAlreadyUsedException
        if (userRepository.existsByUsername(registerUserVm.getUsername())) {
            throw new UsernameAlreadyUsedException();
        }

        if (userRepository.existsByEmail(registerUserVm.getEmail())) {
            throw new EmailAlreadyUsedException();
        }

        // Create new User's account and encode password with PasswordEncoder Bean from SecurityConfig Class (Spring Security)
        User user = new User();
        user.setUsername(registerUserVm.getUsername());
        user.setEmail(registerUserVm.getEmail());
        user.setPassword(passwordEncoder.encode(registerUserVm.getPassword()));
        user.setEnabled(true);

        // Set Roles for new User from RoleRepository (Database)
        Set<Role> roles = roleRepository.findByName(registerUserVm.getRole())
                .map(Set::of)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRoles(roles);

        // Save new User to Database and return User Object (Model)
        return userRepository.save(user);
    }

}
