package com.devhouse.financial_plan.application.role;

import com.devhouse.financial_plan.domain.repository.RoleRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteRoleService {

    private final RoleRepository roleRepository;

    public DeleteRoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    public void execute(Long id) {
        roleRepository.findById(id);
        roleRepository.delete(id);
    }
}
