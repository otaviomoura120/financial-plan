package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuResponse;
import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetGroupMenuService {

    private final GroupMenuRepository groupMenuRepository;

    public GetGroupMenuService(GroupMenuRepository groupMenuRepository) {
        this.groupMenuRepository = groupMenuRepository;
    }

    public List<GroupMenuResponse> execute() {
        List<GroupMenu> menus = groupMenuRepository.findAllWithChildren();
        return menus.stream().map(this::toResponse).toList();
    }

    private GroupMenuResponse toResponse(GroupMenu groupMenu) {
        return new GroupMenuResponse(
                groupMenu.getId(),
                groupMenu.getName(),
                groupMenu.getIcon(),
                groupMenu.getCreatedAt(),
                groupMenu.getUpdatedAt()
        );
    }
}
