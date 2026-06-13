package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.domain.repository.GroupMenuRepository;
import org.springframework.stereotype.Service;

@Service
public class DeleteGroupMenuService {

    private final GroupMenuRepository groupMenuRepository;

    public DeleteGroupMenuService(GroupMenuRepository groupMenuRepository) {
        this.groupMenuRepository = groupMenuRepository;
    }

    public void execute(Long id) {
        groupMenuRepository.findById(id);
        groupMenuRepository.delete(id);
    }
}
