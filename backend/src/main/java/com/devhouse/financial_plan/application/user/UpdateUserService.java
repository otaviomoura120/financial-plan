package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.application.user.dto.UpdateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UserResponse;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserService {

    private final UserRepository userRepository;

    public UpdateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse execute(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id);
        user.setVersion(request.version());
        user.update(request.name(), request.nickname(), request.profilePhoto(),
                request.observation(), request.birthdate(), request.phoneNumber(),
                request.genre(), request.maritalStatus());
        user.validate();
        User updated = userRepository.update(user);
        return new UserResponse(updated.getId(), updated.getVersion(), updated.getName());
    }
}
