package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.application.user.dto.UserResponse;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateUserStatusService {

    private final UserRepository userRepository;

    public UpdateUserStatusService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse execute(Long id, boolean active) {
        User user = userRepository.findById(id);
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        User updated = userRepository.update(user);
        return new UserResponse(updated.getId(), updated.getVersion(), updated.getName());
    }
}
