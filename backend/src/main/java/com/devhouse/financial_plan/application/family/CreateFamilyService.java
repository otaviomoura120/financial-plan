package com.devhouse.financial_plan.application.family;

import com.devhouse.financial_plan.application.family.dto.CreateFamilyRequest;
import com.devhouse.financial_plan.application.family.dto.FamilyResponse;
import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateFamilyService {

    private final FamilyRepository familyRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    public CreateFamilyService(FamilyRepository familyRepository, RoleRepository roleRepository, UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
    }

    public FamilyResponse execute(CreateFamilyRequest request) {
        User creator = userRepository.findById(request.creatorId());
        Family family = new Family(null, 0, request.name(), Instant.now(), null);
        family.validate();
        Family saved = familyRepository.save(family);
        assignCreatorToFamily(creator, saved);
        return new FamilyResponse(saved.getId(), saved.getName(), saved.getCreatedDate());
    }

    private void assignCreatorToFamily(User creator, Family family) {
        Role ownerRole = new Role(null, 0, family, Role.OWNER_ROLE_NAME, "Family owner", Instant.now(), null);
        Role savedRole = roleRepository.save(ownerRole);
        creator.setFamily(family);
        creator.setRole(savedRole);
        userRepository.update(creator);
    }
}
