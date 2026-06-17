package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuResponse;
import com.devhouse.financial_plan.application.groupmenu.dto.UpdateGroupMenuRequest;
import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import org.springframework.stereotype.Service;

@Service
public class UpdateGroupMenuService {

    private final GroupMenuRepository groupMenuRepository;

    public UpdateGroupMenuService(GroupMenuRepository groupMenuRepository) {
        this.groupMenuRepository = groupMenuRepository;
    }

    public GroupMenuResponse execute(Long id, UpdateGroupMenuRequest request) {
        GroupMenu groupMenu = groupMenuRepository.findById(id);
        groupMenu.setVersion(request.version());
        groupMenu.update(request.name(), request.icon());
        groupMenu.validate();
        GroupMenu updated = groupMenuRepository.update(groupMenu);
        return toResponse(updated);
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
