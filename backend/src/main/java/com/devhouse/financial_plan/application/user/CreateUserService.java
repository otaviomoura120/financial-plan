package com.devhouse.financial_plan.application.user;

import com.devhouse.financial_plan.application.user.dto.CreateUserRequest;
import com.devhouse.financial_plan.application.user.dto.UserResponse;
import com.devhouse.financial_plan.domain.ExternalIdentity;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.gateway.IdentityProvider;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateUserService {

    private final UserRepository userRepository;
    private final IdentityProvider identityProvider;

    public CreateUserService(UserRepository userRepository, IdentityProvider identityProvider) {
        this.userRepository = userRepository;
        this.identityProvider = identityProvider;
    }

    /**
     * The e-mail comes from the identity provider, never from the request: invites are matched by e-mail,
     * so a typed address would let anyone read and accept invites sent to someone else.
     */
    public UserResponse execute(String auth0Sub, String accessToken, CreateUserRequest request) {
        rejectIfAlreadyRegistered(auth0Sub);
        ExternalIdentity identity = identityProvider.fetchIdentity(accessToken);
        String email = identity.verifiedEmailOf(auth0Sub);
        rejectIfEmailTaken(email);

        User user = new User(null, 0, auth0Sub, request.name(), request.nickname(),
                request.profilePhoto(), request.observation(), request.birthdate(), email,
                request.phoneNumber(), true, request.genre(), request.maritalStatus(), Instant.now(), null, false);
        user.validate();
        User saved = userRepository.save(user);
        return new UserResponse(saved.getId(), saved.getVersion(), saved.getName());
    }

    private void rejectIfAlreadyRegistered(String auth0Sub) {
        if (userRepository.findByAuth0Sub(auth0Sub) != null) {
            throw new DomainException("user_already_registered");
        }
    }

    private void rejectIfEmailTaken(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DomainException("email_already_registered");
        }
    }
}
