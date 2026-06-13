package com.devhouse.financial_plan.application.family;

import com.devhouse.financial_plan.domain.Family;
import com.devhouse.financial_plan.domain.User;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.FamilyRepository;
import com.devhouse.financial_plan.domain.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AddFamilyMemberService {

    private final FamilyRepository familyRepository;
    private final UserRepository userRepository;

    public AddFamilyMemberService(FamilyRepository familyRepository, UserRepository userRepository) {
        this.familyRepository = familyRepository;
        this.userRepository = userRepository;
    }

    public void execute(Long familyId, Long userId) {
        Family family = familyRepository.findById(familyId);
        User user = userRepository.findById(userId);
        if (user.getFamily() != null) {
            throw new DomainException("User already belongs to a family");
        }
        user.setFamily(family);
        userRepository.update(user);
    }
}
