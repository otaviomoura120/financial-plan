package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.application.groupmenu.dto.CreateGroupMenuChildrenRequest;
import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuChildrenResponse;
import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.GroupMenuChildren;
import com.devhouse.financial_plan.domain.repository.GroupMenuChildrenRepository;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AddGroupMenuChildrenService {

    private final GroupMenuRepository groupMenuRepository;
    private final GroupMenuChildrenRepository groupMenuChildrenRepository;

    public AddGroupMenuChildrenService(GroupMenuRepository groupMenuRepository, GroupMenuChildrenRepository groupMenuChildrenRepository) {
        this.groupMenuRepository = groupMenuRepository;
        this.groupMenuChildrenRepository = groupMenuChildrenRepository;
    }

    public GroupMenuChildrenResponse execute(CreateGroupMenuChildrenRequest request) {
        GroupMenu groupMenu = groupMenuRepository.findById(request.groupMenuId());
        GroupMenuChildren child = new GroupMenuChildren(null, null, request.name(), request.endpoint(), request.icon(), groupMenu, Instant.now(), null);
        child.validate();
        GroupMenuChildren saved = groupMenuChildrenRepository.save(child);
        return toResponse(saved);
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
