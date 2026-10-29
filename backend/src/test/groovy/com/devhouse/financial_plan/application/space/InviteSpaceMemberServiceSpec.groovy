package com.devhouse.financial_plan.application.space

import com.devhouse.financial_plan.application.space.dto.InviteRequest
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.Space
import com.devhouse.financial_plan.domain.SpaceInvite
import com.devhouse.financial_plan.domain.SpaceMember
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.enums.InviteStatus
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.RoleRepository
import com.devhouse.financial_plan.domain.repository.SpaceInviteRepository
import com.devhouse.financial_plan.domain.repository.SpaceMemberRepository
import com.devhouse.financial_plan.domain.repository.SpaceRepository
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.Session
import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import spock.lang.Specification

import java.time.Instant
import java.time.temporal.ChronoUnit

class InviteSpaceMemberServiceSpec extends Specification {

    SpaceRepository spaceRepository = Mock()
    RoleRepository roleRepository = Mock()
    SpaceInviteRepository spaceInviteRepository = Mock()
    SpaceMemberRepository spaceMemberRepository = Mock()
    JavaMailSender mailSender = Mock()
    InviteSpaceMemberService service = new InviteSpaceMemberService(spaceRepository, roleRepository,
            spaceInviteRepository, spaceMemberRepository, mailSender)

    MimeMessage sentMessage
    List<SpaceInvite> existingInvites = []
    List<SpaceMember> existingMembers = []

    def setup() {
        service.frontendUrl = "https://app.example.com"
        mailSender.createMimeMessage() >> new MimeMessage(Session.getInstance(new Properties()))
        mailSender.send(_ as MimeMessage) >> { MimeMessage message -> sentMessage = message }
        spaceInviteRepository.findBySpaceIdAndEmail(_, _) >> Optional.empty()
        spaceInviteRepository.findBySpaceId(1L) >> { existingInvites }
        spaceMemberRepository.findBySpaceId(1L) >> { existingMembers }
    }

    def "execute escapes the Space and role names in the invite e-mail body"() {
        given:
        givenSpaceAndRole('<a href="https://evil.example.com">Clique aqui</a>', '<b>Admin</b>')

        when:
        service.execute(1L, new InviteRequest("maria@example.com", 2L))

        then:
        // The real sender finalizes the message; parts only get their Content-Type headers then
        sentMessage.saveChanges()
        String html = htmlBody(sentMessage)
        html.contains('&lt;a href=&quot;https://evil.example.com&quot;&gt;Clique aqui&lt;/a&gt;')
        html.contains('&lt;b&gt;Admin&lt;/b&gt;')
        !html.contains('evil.example.com">')
        html.contains('href="https://app.example.com/"')
    }

    def "execute keeps the subject on a single line even when the Space name has line breaks"() {
        given:
        givenSpaceAndRole("Família\r\nBcc: alguem@example.com", "Admin")

        when:
        service.execute(1L, new InviteRequest("maria@example.com", 2L))

        then:
        sentMessage.subject == 'Você foi convidado para o espaço "Família  Bcc: alguem@example.com"'
        sentMessage.getHeader("Bcc") == null
    }

    def "execute saves a pending invite for the e-mail"() {
        given:
        givenSpaceAndRole("Família Silva", "Admin")

        when:
        service.execute(1L, new InviteRequest("maria@example.com", 2L))

        then:
        1 * spaceInviteRepository.save({ SpaceInvite invite -> invite.email == "maria@example.com" && invite.role.id == 2L })
    }

    def "execute rejects a role that does not exist without saving or sending anything"() {
        given:
        spaceRepository.findById(1L) >> new Space(1L, 0, "Família Silva", null, Instant.now(), null)
        roleRepository.findById(99L) >> null

        when:
        service.execute(1L, new InviteRequest("maria@example.com", 99L))

        then:
        thrown(DomainException)
        0 * spaceInviteRepository.save(_)
        sentMessage == null
    }

    def "execute rejects a role from another space"() {
        given:
        spaceRepository.findById(1L) >> new Space(1L, 0, "Família Silva", null, Instant.now(), null)
        Space otherSpace = new Space(5L, 0, "Outro", null, Instant.now(), null)
        roleRepository.findById(2L) >> new Role(2L, 0, otherSpace, "Admin", null, Instant.now(), null)

        when:
        service.execute(1L, new InviteRequest("maria@example.com", 2L))

        then:
        thrown(DomainException)
        0 * spaceInviteRepository.save(_)
    }

    def "execute refuses an invite when members and pending invites already fill the Space"() {
        given:
        Space space = givenSpaceAndRole("Família Silva", "Admin")
        existingMembers = (1..members).collect { buildMember(space, it) }
        existingInvites = (0..<pending).collect { buildInvite(space, it, InviteStatus.PENDING, 1) }

        when:
        service.execute(1L, new InviteRequest("maria@example.com", 2L))

        then:
        DomainException exception = thrown()
        exception.message == "space_member_limit_reached"
        0 * spaceInviteRepository.save(_)
        sentMessage == null

        where:
        members | pending
        1       | 4
        3       | 2
        5       | 0
    }

    def "execute sends the invite that takes the last seat, ignoring expired and answered invites"() {
        given:
        Space space = givenSpaceAndRole("Família Silva", "Admin")
        existingMembers = (1..2).collect { buildMember(space, it) }
        existingInvites = (1..2).collect { buildInvite(space, it, InviteStatus.PENDING, 1) } + [
                buildInvite(space, 5, InviteStatus.PENDING, -1),
                buildInvite(space, 6, InviteStatus.ACCEPTED, 1),
                buildInvite(space, 7, InviteStatus.CANCELLED, 1),
                buildInvite(space, 8, InviteStatus.DECLINED, 1)
        ]

        when:
        service.execute(1L, new InviteRequest("maria@example.com", 2L))

        then:
        1 * spaceInviteRepository.save({ SpaceInvite invite -> invite.email == "maria@example.com" })
        sentMessage != null
    }

    private static SpaceMember buildMember(Space space, int index) {
        Role role = new Role(2L, 0, space, "Admin", null, Instant.now(), null)
        User user = new User(index as Long, 0, "auth0|${index}", "User ${index}", null, null, null, null,
                "user${index}@example.com", null, true, null, null, Instant.now(), null, false)
        new SpaceMember(index as Long, 0, space, user, role, Instant.now())
    }

    private static SpaceInvite buildInvite(Space space, int index, InviteStatus status, int daysUntilExpiry) {
        Role role = new Role(2L, 0, space, "Admin", null, Instant.now(), null)
        new SpaceInvite(index as Long, 0, space, role, "convidado${index}@example.com", "token-${index}", status,
                Instant.now(), Instant.now().plus(daysUntilExpiry, ChronoUnit.DAYS))
    }

    private Space givenSpaceAndRole(String spaceName, String roleName) {
        Space space = new Space(1L, 0, spaceName, null, Instant.now(), null)
        spaceRepository.findById(1L) >> space
        roleRepository.findById(2L) >> new Role(2L, 0, space, roleName, null, Instant.now(), null)
        return space
    }

    private static String htmlBody(Part part) {
        if (part.isMimeType("text/html")) {
            return (String) part.content
        }
        if (part.content instanceof Multipart) {
            Multipart multipart = (Multipart) part.content
            for (int i = 0; i < multipart.count; i++) {
                String html = htmlBody(multipart.getBodyPart(i))
                if (html != null) {
                    return html
                }
            }
        }
        return null
    }
}
