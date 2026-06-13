package com.devhouse.financial_plan.application.family;

import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class RemoveFamilyMemberService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    public RemoveFamilyMemberService(FamilyRepository familyRepository, UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
    }

    public void execute(Long familyId, Long userId) {
        Family family = familyRepository.findById(familyId);
        User user = userRepository.findById(userId);
        if (user.getFamily() == null || !family.getId().equals(user.getFamily().getId())) {
            throw new DomainException("User does not belong to this family");
        }
        if (family.getOwner().getId().equals(userId)) {
            throw new DomainException("Cannot remove the family owner");
        }
        user.setFamily(null);
        userRepository.update(user);
    }
}
