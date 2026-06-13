package com.devhouse.financial_plan.application.family

import com.devhouse.financial_plan.domain.Family
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.FamilyRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class RemoveFamilyMemberServiceSpec extends Specification {

    FamilyRepository familyRepository = Mock()
    UserRepository userRepository = Mock()
    RemoveFamilyMemberService service = new RemoveFamilyMemberService(familyRepository, userRepository)

    def "execute removes member who does not have OWNER role"() {
        given:
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        Role memberRole = new Role(2L, 0, family, "MEMBER", "Regular member", Instant.now(), null)
        User user = new User(5L, 0, family, "auth0|abc", memberRole, "Jane", null, null, null,
                null, "jane@test.com", null, true, null, null, Instant.now(), null)

        familyRepository.findById(1L) >> family
        userRepository.findById(5L) >> user

        when:
        service.execute(1L, 5L)

        then:
        1 * userRepository.update({ User u -> u.getFamily() == null })
    }

    def "execute throws DomainException when trying to remove the OWNER"() {
        given:
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        Role ownerRole = new Role(1L, 0, family, Role.OWNER_ROLE_NAME, "Family owner", Instant.now(), null)
        User owner = new User(2L, 0, family, "auth0|abc", ownerRole, "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)

        familyRepository.findById(1L) >> family
        userRepository.findById(2L) >> owner

        when:
        service.execute(1L, 2L)

        then:
        thrown(DomainException)
        0 * userRepository.update(_)
    }

    def "execute throws DomainException when user does not belong to the family"() {
        given:
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        Family otherFamily = new Family(99L, 0, "Other Family", Instant.now(), null)
        User user = new User(5L, 0, otherFamily, "auth0|abc", null, "Jane", null, null, null,
                null, "jane@test.com", null, true, null, null, Instant.now(), null)

        familyRepository.findById(1L) >> family
        userRepository.findById(5L) >> user

        when:
        service.execute(1L, 5L)

        then:
        thrown(DomainException)
        0 * userRepository.update(_)
    }

    def "execute throws DomainException when user has no family"() {
        given:
        Family family = new Family(1L, 0, "Smith Family", Instant.now(), null)
        User user = new User(5L, 0, null, "auth0|abc", null, "Jane", null, null, null,
                null, "jane@test.com", null, true, null, null, Instant.now(), null)

        familyRepository.findById(1L) >> family
        userRepository.findById(5L) >> user

        when:
        service.execute(1L, 5L)

        then:
        thrown(DomainException)
        0 * userRepository.update(_)
    }
}
