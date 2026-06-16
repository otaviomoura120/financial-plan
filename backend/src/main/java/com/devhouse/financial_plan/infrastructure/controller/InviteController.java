package com.devhouse.financial_plan.infrastructure.controller;

import com.devhouse.financial_plan.application.invite.AcceptInviteService;
import com.devhouse.financial_plan.application.invite.DeclineInviteService;
import com.devhouse.financial_plan.application.invite.ListMyInvitesService;
import com.devhouse.financial_plan.application.invite.dto.AcceptInviteResponse;
import com.devhouse.financial_plan.application.invite.dto.MyInviteResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/invites")
public class InviteController {

    private final ListMyInvitesService listMyInvitesService;
    private final AcceptInviteService acceptInviteService;
    private final DeclineInviteService declineInviteService;

    public InviteController(ListMyInvitesService listMyInvitesService,
                            AcceptInviteService acceptInviteService,
                            DeclineInviteService declineInviteService) {
        this.listMyInvitesService = listMyInvitesService;
        this.acceptInviteService = acceptInviteService;
        this.declineInviteService = declineInviteService;
    }

    @GetMapping
    public List<MyInviteResponse> listMine(Authentication authentication) {
        return listMyInvitesService.execute(authentication.getName());
    }

    @PostMapping("/{token}/accept")
    public AcceptInviteResponse accept(@PathVariable String token, Authentication authentication) {
        return acceptInviteService.execute(token, authentication.getName());
    }

    @PostMapping("/{token}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(@PathVariable String token, Authentication authentication) {
        declineInviteService.execute(token, authentication.getName());
    }
}
