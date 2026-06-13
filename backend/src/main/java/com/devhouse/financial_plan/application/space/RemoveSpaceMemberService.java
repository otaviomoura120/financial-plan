package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import org.springframework.stereotype.Service;

@Service
public class RemoveSpaceMemberService {

    private final SpaceMemberRepository spaceMemberRepository;

    public RemoveSpaceMemberService(SpaceMemberRepository spaceMemberRepository) {
        this.spaceMemberRepository = spaceMemberRepository;
    }

    public void execute(Long spaceId, Long userId) {
        SpaceMember member = spaceMemberRepository.findBySpaceIdAndUserId(spaceId, userId);
        if (member == null) {
            throw new DomainException("User is not a member of this space");
        }
        if (member.isOwner()) {
            throw new DomainException("Cannot remove the space owner");
        }
        spaceMemberRepository.delete(member.getId());
    }
}
