package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceMember;
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ListUserSpacesService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;

    public ListUserSpacesService(SpaceRepository spaceRepository, SpaceMemberRepository spaceMemberRepository) {
        this.spaceRepository = spaceRepository;
        this.spaceMemberRepository = spaceMemberRepository;
    }

    public List<SpaceResponse> execute(Long userId) {
        List<Space> spaces = spaceRepository.findByUserId(userId);
        List<SpaceMember> memberships = spaceMemberRepository.findByUserId(userId);
        Map<Long, String> roleBySpaceId = memberships.stream()
                .collect(Collectors.toMap(m -> m.getSpace().getId(), m -> m.getRole().getName()));
        return spaces.stream()
                .map(s -> new SpaceResponse(s.getId(), s.getName(), s.getDescription(),
                        s.getCreatedDate(), roleBySpaceId.get(s.getId())))
                .toList();
    }
}
