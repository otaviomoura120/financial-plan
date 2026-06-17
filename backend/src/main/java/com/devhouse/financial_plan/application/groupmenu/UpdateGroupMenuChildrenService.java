package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuChildrenResponse;
import com.devhouse.financial_plan.application.groupmenu.dto.UpdateGroupMenuChildrenRequest;
import com.devhouse.financial_plan.domain.GroupMenuChildren;
import com.devhouse.financial_plan.domain.repository.GroupMenuChildrenRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateGroupMenuChildrenService {

    private final GroupMenuChildrenRepository groupMenuChildrenRepository;

    public UpdateGroupMenuChildrenService(GroupMenuChildrenRepository groupMenuChildrenRepository) {
        this.groupMenuChildrenRepository = groupMenuChildrenRepository;
    }

    public GroupMenuChildrenResponse execute(Long id, UpdateGroupMenuChildrenRequest request) {
        GroupMenuChildren child = groupMenuChildrenRepository.findById(id);
        child.setVersion(request.version());
        child.update(request.name(), request.endpoint(), request.icon());
        child.validate();
        GroupMenuChildren updated = groupMenuChildrenRepository.update(child);
        return toResponse(updated);
    }

    private GroupMenuChildrenResponse toResponse(GroupMenuChildren child) {
        return new GroupMenuChildrenResponse(
                child.getId(),
                child.getVersion(),
                child.getGroupMenu().getId(),
                child.getName(),
                child.getEndpoint(),
                child.getIcon(),
                child.getCreatedAt(),
                child.getUpdatedAt()
        );
    }
}
