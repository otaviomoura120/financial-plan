package com.devhouse.financial_plan.application.family

import com.devhouse.financial_plan.application.family.dto.CreateFamilyRequest
import com.devhouse.financial_plan.application.family.dto.FamilyResponse
import com.devhouse.financial_plan.domain.Family
import com.devhouse.financial_plan.domain.Role
import com.devhouse.financial_plan.domain.User
import com.devhouse.financial_plan.domain.exception.DomainException
import com.devhouse.financial_plan.domain.repository.FamilyRepository
import com.devhouse.financial_plan.domain.repository.RoleRepository
import com.devhouse.financial_plan.domain.repository.UserRepository
import spock.lang.Specification

import java.time.Instant

class CreateFamilyServiceSpec extends Specification {

    FamilyRepository familyRepository = Mock()
    RoleRepository roleRepository = Mock()
    UserRepository userRepository = Mock()
    CreateFamilyService service = new CreateFamilyService(familyRepository, roleRepository, userRepository)

    def "execute creates family, assigns OWNER role and links creator"() {
        given:
        CreateFamilyRequest request = new CreateFamilyRequest("Smith Family", 1L)
        User creator = new User(1L, 0, null, "auth0|abc", null, "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
        Family savedFamily = new Family(10L, 0, "Smith Family", Instant.now(), null)
        Role savedRole = new Role(20L, 0, savedFamily, Role.OWNER_ROLE_NAME, "Family owner", Instant.now(), null)

        userRepository.findById(1L) >> creator
        familyRepository.save(_) >> savedFamily

        when:
        FamilyResponse response = service.execute(request)

        then:
        1 * roleRepository.save({ Role r -> r.getName() == Role.OWNER_ROLE_NAME && r.getFamily().getId() == 10L }) >> savedRole
        1 * userRepository.update({ User u -> u.getFamily().getId() == 10L && u.getRole().getId() == 20L }) >> creator
        response.id() == 10L
        response.name() == "Smith Family"
    }

    def "execute throws DomainException when family name is blank"() {
        given:
        CreateFamilyRequest request = new CreateFamilyRequest("", 1L)
        User creator = new User(1L, 0, null, "auth0|abc", null, "John", null, null, null,
                null, "john@test.com", null, true, null, null, Instant.now(), null)
        userRepository.findById(1L) >> creator

        when:
        service.execute(request)

        then:
        thrown(DomainException)
        0 * familyRepository.save(_)
        0 * roleRepository.save(_)
        0 * userRepository.update(_)
    }
}
