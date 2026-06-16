package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.application.user.dto.UserMeResponse;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class FindUserByAuth0SubService {

    private final UserRepository userRepository;

    public FindUserByAuth0SubService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserMeResponse execute(String auth0Sub) {
        User user = userRepository.findByAuth0Sub(auth0Sub);
        if (user == null) {
            return null;
        }
        return new UserMeResponse(user.getId(), user.getName(), user.getEmail(),
                user.getNickname(), user.getPhoneNumber(), user.getBirthdate(),
                user.getGenre(), user.getMaritalStatus());
    }
}
