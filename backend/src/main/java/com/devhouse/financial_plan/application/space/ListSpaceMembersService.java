package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.SpaceMemberResponse;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListSpaceMembersService {

    private final SpaceMemberRepository spaceMemberRepository;

    public ListSpaceMembersService(SpaceMemberRepository spaceMemberRepository) {
        this.spaceMemberRepository = spaceMemberRepository;
    }

    public List<SpaceMemberResponse> execute(Long spaceId) {
        return spaceMemberRepository.findBySpaceId(spaceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private SpaceMemberResponse toResponse(SpaceMember m) {
        return new SpaceMemberResponse(
                m.getId(),
                m.getVersion(),
                m.getUser().getId(),
                m.getUser().getName(),
                m.getUser().getEmail(),
                m.getRole().getId(),
                m.getRole().getName(),
                m.getJoinedAt()
        );
    }
}
