package com.devhouse.financial_plan.application.space;

import com.devhouse.financial_plan.application.space.dto.InviteRequest;
import com.devhouse.financial_plan.domain.Role;
import com.devhouse.financial_plan.domain.Space;
import com.devhouse.financial_plan.domain.SpaceInvite;
import com.devhouse.financial_plan.domain.enums.InviteStatus;
import com.devhouse.financial_plan.domain.exception.DomainException;
import com.devhouse.financial_plan.domain.repository.RoleRepository;
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository;
import com.devhouse.financial_plan.domain.repository.SpaceRepository;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class InviteSpaceMemberService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));

    private final SpaceRepository spaceRepository;
    private final RoleRepository roleRepository;
    private final SpaceInviteRepository spaceInviteRepository;
    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public InviteSpaceMemberService(SpaceRepository spaceRepository,
                                    RoleRepository roleRepository,
                                    SpaceInviteRepository spaceInviteRepository,
                                    JavaMailSender mailSender) {
        this.spaceRepository = spaceRepository;
        this.roleRepository = roleRepository;
        this.spaceInviteRepository = spaceInviteRepository;
        this.mailSender = mailSender;
    }

    public void execute(Long spaceId, InviteRequest request) {
        Space space = spaceRepository.findById(spaceId);
        Role role = roleRepository.findById(request.roleId());
        if (!role.getSpace().getId().equals(spaceId)) {
            throw new DomainException("Role must belong to this space");
        }
        spaceInviteRepository.findBySpaceIdAndEmail(spaceId, request.email())
                .filter(i -> i.getStatus() == InviteStatus.PENDING)
                .ifPresent(i -> { throw new DomainException("A pending invite already exists for this email"); });

        String token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiresAt = now.plus(7, ChronoUnit.DAYS);

        SpaceInvite invite = new SpaceInvite(null, space, role, request.email(),
                token, InviteStatus.PENDING, now, expiresAt);
        invite.validate();
        spaceInviteRepository.save(invite);

        sendInviteEmail(request.email(), space.getName(), role.getName(), token, expiresAt);
    }

    private void sendInviteEmail(String to, String spaceName, String roleName, String token, Instant expiresAt) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Você foi convidado para o espaço \"" + spaceName + "\"");

            String loginUrl = frontendUrl + "/";
            String expiryDate = DATE_FORMATTER.format(expiresAt);

            String html = """
                    <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
                      <h2 style="color: #1a1a1a;">Você foi convidado!</h2>
                      <p style="color: #444; font-size: 16px;">
                        Você recebeu um convite para participar do espaço
                        <strong>%s</strong> com a role <strong>%s</strong>.
                      </p>
                      <p style="color: #666; font-size: 14px;">
                        Este convite é válido até <strong>%s</strong>.
                      </p>
                      <div style="margin: 32px 0;">
                        <a href="%s"
                           style="background-color: #5865f2; color: #fff; padding: 14px 28px;
                                  text-decoration: none; border-radius: 6px; font-size: 16px; font-weight: bold;">
                          Entrar na Plataforma
                        </a>
                      </div>
                      <p style="color: #888; font-size: 13px;">
                        Faça login (ou crie uma conta, caso ainda não tenha uma) para visualizar e responder a este convite.
                      </p>
                      <hr style="border: none; border-top: 1px solid #eee; margin: 24px 0;" />
                      <p style="color: #aaa; font-size: 12px;">
                        Se você não esperava este convite, pode ignorar este e-mail com segurança.
                      </p>
                    </div>
                    """.formatted(spaceName, roleName, expiryDate, loginUrl);

            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new DomainException("Failed to send invite email: " + e.getMessage());
        }
    }
}
