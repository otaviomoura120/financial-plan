package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteSpaceService {

    private final SpaceRepository spaceRepository;

    public DeleteSpaceService(SpaceRepository spaceRepository) {
        this.spaceRepository = spaceRepository;
    }

    public void execute(Long id) {
        spaceRepository.delete(id);
    }
}
