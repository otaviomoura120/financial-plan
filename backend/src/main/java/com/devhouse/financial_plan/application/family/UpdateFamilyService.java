package com.devhouse.financial_plan.application.family;

import com.devhouse.financial_plan.application.family.dto.FamilyResponse;
import com.devhouse.financial_plan.application.family.dto.UpdateFamilyRequest;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UpdateFamilyService {

    private final FamilyRepository familyRepository;

    public UpdateFamilyService(FamilyRepository familyRepository) {
        this.familyRepository = familyRepository;
    }

    public FamilyResponse execute(Long id, UpdateFamilyRequest request) {
        var family = familyRepository.findById(id);
        family.rename(request.name());
        family.setUpdatedDate(Instant.now());
        var updated = familyRepository.update(family);
        return new FamilyResponse(updated.getId(), updated.getName(), updated.getOwnerId(), updated.getCreatedDate());
    }
}
