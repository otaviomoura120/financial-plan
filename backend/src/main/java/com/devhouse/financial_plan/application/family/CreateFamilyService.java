package com.devhouse.financial_plan.application.family;

import com.devhouse.financial_plan.application.family.dto.CreateFamilyRequest;
import com.devhouse.financial_plan.application.family.dto.FamilyResponse;
import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class CreateFamilyService {

    private final FamilyRepository familyRepository;

    public CreateFamilyService(FamilyRepository familyRepository) {
        this.familyRepository = familyRepository;
    }

    public FamilyResponse execute(CreateFamilyRequest request) {
        var family = new Family(null, 0, request.name(), request.ownerId(), Instant.now(), null);
        family.validate();
        Family saved = familyRepository.save(family);
        return new FamilyResponse(saved.getId(), saved.getName(), saved.getOwnerId(), saved.getCreatedDate());
    }
}
