package com.devhouse.financial_plan.application.groupmenu;

import com.devhouse.financial_plan.domain.repository.GroupMenuChildrenRepository;
import org.springframework.stereotype.Service;

@Service
public class RemoveGroupMenuChildrenService {

    private final GroupMenuChildrenRepository groupMenuChildrenRepository;

    public RemoveGroupMenuChildrenService(GroupMenuChildrenRepository groupMenuChildrenRepository) {
        this.groupMenuChildrenRepository = groupMenuChildrenRepository;
    }

    public void execute(Long id) {
        groupMenuChildrenRepository.findById(id);
        groupMenuChildrenRepository.delete(id);
    }
}
