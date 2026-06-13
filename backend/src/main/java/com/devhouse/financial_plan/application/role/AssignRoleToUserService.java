package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AssignRoleToUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public AssignRoleToUserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public void execute(Long userId, Long roleId) {
        User user = userRepository.findById(userId);
        Role role = roleRepository.findById(roleId);
        validateSameFamily(user, role);
        user.assignRole(role);
        userRepository.update(user);
    }

    private void validateSameFamily(User user, Role role) {
        if (user.getFamily() == null) {
            throw new DomainException("User must belong to a family before receiving a role");
        }
        if (!user.getFamily().getId().equals(role.getFamily().getId())) {
            throw new DomainException("Role must belong to the same family as the user");
        }
    }
}
