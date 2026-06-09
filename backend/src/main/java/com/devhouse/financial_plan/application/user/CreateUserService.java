package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.application.user.dto.CreateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UserResponse;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateUserService {

    private final UserRepository userRepository;

    public CreateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse execute(CreateUserRequest request) {
        var user = new User(null, 0, null, request.name(), request.ministry(), request.nickname(), request.profilePhoto(), request.observation(), request.birthdate(), request.email(), request.phoneNumber(), true, request.genre(), request.maritalStatus(), Instant.now(), null);
        user.validate();
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getName());
    }
}
