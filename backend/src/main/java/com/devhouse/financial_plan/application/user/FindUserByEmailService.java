package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.application.user.dto.UserSearchResponse;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class FindUserByEmailService {

    private final UserRepository userRepository;

    public FindUserByEmailService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<UserSearchResponse> execute(String email) {
        return userRepository.findByEmail(email)
                .map(u -> new UserSearchResponse(u.getId(), u.getName(), u.getEmail()));
    }
}
