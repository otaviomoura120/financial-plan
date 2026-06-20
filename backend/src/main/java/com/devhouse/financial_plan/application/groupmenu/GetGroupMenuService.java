package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuChildrenResponse;
import com.devhouse.financial_plan.application.groupmenu.dto.GroupMenuWithChildrenResponse;
import com.devhouse.financial_plan.domain.GroupMenu;
import com.devhouse.financial_plan.domain.GroupMenuChildren;
import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GetGroupMenuService {

    private final GroupMenuRepository groupMenuRepository;

    public GetGroupMenuService(GroupMenuRepository groupMenuRepository) {
        this.groupMenuRepository = groupMenuRepository;
    }

    public List<GroupMenuWithChildrenResponse> execute() {
        List<GroupMenu> menus = groupMenuRepository.findAllWithChildren();
        return menus.stream().map(this::toResponse).toList();
    }

    private GroupMenuWithChildrenResponse toResponse(GroupMenu groupMenu) {
        List<GroupMenuChildrenResponse> children = groupMenu.getChildren().stream()
                .map(this::toChildResponse)
                .toList();

        return new GroupMenuWithChildrenResponse(
                groupMenu.getId(),
                groupMenu.getVersion(),
                groupMenu.getName(),
                groupMenu.getIcon(),
                children,
                groupMenu.getCreatedAt(),
                groupMenu.getUpdatedAt()
        );
    }

    private GroupMenuChildrenResponse toChildResponse(GroupMenuChildren child) {
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
