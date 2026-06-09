package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.application.user.dto.UpdateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UserResponse;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateUserService {

    private final UserRepository userRepository;

    public UpdateUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse execute(Long id, UpdateUserRequest request) {
        var user = userRepository.findById(id);
        user.setName(request.name());
        user.setMinistry(request.ministry());
        user.setNickname(request.nickname());
        user.setProfilePhoto(request.profilePhoto());
        user.setObservation(request.observation());
        user.setBirthdate(request.birthdate());
        user.setPhoneNumber(request.phoneNumber());
        user.setGenre(request.genre());
        user.setMaritalStatus(request.maritalStatus());
        user.setUpdatedDate(Instant.now());
        user.validate();
        var updated = userRepository.update(user);
        return new UserResponse(updated.getId(), updated.getName());
    }
}
