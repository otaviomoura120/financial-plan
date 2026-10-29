package com.devhouse.financial_plan.domain

import com.devhouse.financial_plan.domain.exception.DomainException
import spock.lang.Specification

class ExternalIdentitySpec extends Specification {

    def "verifiedEmailOf returns the trimmed e-mail of a verified identity owned by the authenticated user"() {
        given:
        ExternalIdentity identity = new ExternalIdentity("auth0|1", " joao@example.com ", true)

        expect:
        identity.verifiedEmailOf("auth0|1") == "joao@example.com"
    }

    def "verifiedEmailOf rejects an identity that belongs to another user"() {
        given:
        ExternalIdentity identity = new ExternalIdentity(sub, "joao@example.com", true)

        when:
        identity.verifiedEmailOf("auth0|1")

        then:
        DomainException exception = thrown()
        exception.message == "identity_mismatch"

        where:
        sub << ["auth0|2", null]
    }

    def "verifiedEmailOf rejects an e-mail the provider has not verified or did not return"() {
        given:
        ExternalIdentity identity = new ExternalIdentity("auth0|1", email, verified)

        when:
        identity.verifiedEmailOf("auth0|1")

        then:
        DomainException exception = thrown()
        exception.message == "email_not_verified"

        where:
        email              | verified
        "joao@example.com" | false
        null               | true
        "  "               | true
    }
}
