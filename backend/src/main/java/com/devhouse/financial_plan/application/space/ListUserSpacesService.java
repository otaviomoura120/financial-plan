package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListUserSpacesService {

    private final SpaceRepository spaceRepository;

    public ListUserSpacesService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public List<SpaceResponse> execute(Long userId) {
        List<Space> spaces = spaceRepository.findByUserId(userId);
        return spaces.stream()
                .map(s -> new SpaceResponse(s.getId(), s.getName(), s.getDescription(), s.getCreatedDate()))
                .toList();
    }
}
