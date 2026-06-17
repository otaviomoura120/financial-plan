package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.application.groupmenu.dto.CreateGroupMenuRequest;
import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuResponse;
import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class CreateGroupMenuService {

    private final GroupMenuRepository groupMenuRepository;

    public CreateGroupMenuService(GroupMenuRepository groupMenuRepository) {
        this.groupMenuRepository = groupMenuRepository;
    }

    public GroupMenuResponse execute(CreateGroupMenuRequest request) {
        GroupMenu groupMenu = new GroupMenu(null, null, request.name(), request.icon(), List.of(), Instant.now(), null);
        groupMenu.validate();
        GroupMenu saved = groupMenuRepository.save(groupMenu);
        return toResponse(saved);
    }

    private GroupMenuResponse toResponse(GroupMenu groupMenu) {
        return new GroupMenuResponse(
                groupMenu.getId(),
                groupMenu.getVersion(),
                groupMenu.getName(),
                groupMenu.getIcon(),
                groupMenu.getCreatedAt(),
                groupMenu.getUpdatedAt()
        );
    }
}
