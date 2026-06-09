package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteUserService {

    private final UserRepository userRepository;

    public DeleteUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void execute(Long id) {
        var user = userRepository.findById(id);
        user.deactivate();
        userRepository.update(user);
    }
}
