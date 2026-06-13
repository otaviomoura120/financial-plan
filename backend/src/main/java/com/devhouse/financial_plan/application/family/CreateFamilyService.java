package com.devhouse.financial_plan.application.family;

import com.devhouse.financial_plan.application.family.dto.CreateFamilyRequest;
import com.devhouse.financial_plan.application.family.dto.FamilyResponse;
import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateFamilyService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    public CreateFamilyService(FamilyRepository familyRepository, UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
    }

    public FamilyResponse execute(CreateFamilyRequest request) {
        User owner = userRepository.findById(request.ownerId());
        Family family = new Family(null, 0, request.name(), owner, Instant.now(), null);
        family.validate();
        Family saved = familyRepository.save(family);
        return new FamilyResponse(saved.getId(), saved.getName(), saved.getOwner().getId(), saved.getCreatedDate());
    }
}
