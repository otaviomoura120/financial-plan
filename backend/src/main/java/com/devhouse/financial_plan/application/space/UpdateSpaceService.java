package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.SpaceResponse;
import com.devhouse.financial_plan.application.space.dto.UpdateSpaceRequest;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateSpaceService {

    private final SpaceRepository spaceRepository;

    public UpdateSpaceService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public SpaceResponse execute(Long id, UpdateSpaceRequest request) {
        Space space = spaceRepository.findById(id);
        space.rename(request.name());
        Space updated = spaceRepository.update(space);
        return new SpaceResponse(updated.getId(), updated.getName(), updated.getDescription(), updated.getCreatedDate(), null);
    }
}
