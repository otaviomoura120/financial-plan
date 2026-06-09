package com.devhouse.financial_plan.application.family;

import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteFamilyService {

    private final FamilyRepository familyRepository;

    public DeleteFamilyService(FamilyRepository familyRepository) {
        this.familyRepository = familyRepository;
    }

    public void execute(Long id) {
        familyRepository.delete(id);
    }
}
