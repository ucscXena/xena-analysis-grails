package org.xena.analysis

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class TpmServiceSpec extends Specification {

    TpmService tpmService
    @Autowired Datastore datastore

    private Long setupData() {
        // TODO: Populate valid domain instances and return a valid ID
        //new Tpm(...).save(flush: true, failOnError: true)
        //new Tpm(...).save(flush: true, failOnError: true)
        //Tpm tpm = new Tpm(...).save(flush: true, failOnError: true)
        //new Tpm(...).save(flush: true, failOnError: true)
        //new Tpm(...).save(flush: true, failOnError: true)
        assert false, "TODO: Provide a setupData() implementation for this generated test suite"
        //tpm.id
    }

    void cleanup() {
        assert false, "TODO: Provide a cleanup implementation if using MongoDB"
    }

    void "test get"() {
        setupData()

        expect:
        tpmService.get(1) != null
    }

    void "test list"() {
        setupData()

        when:
        List<Tpm> tpmList = tpmService.list(max: 2, offset: 2)

        then:
        tpmList.size() == 2
        assert false, "TODO: Verify the correct instances are returned"
    }

    void "test count"() {
        setupData()

        expect:
        tpmService.count() == 5
    }

    void "test delete"() {
        Long tpmId = setupData()

        expect:
        tpmService.count() == 5

        when:
        tpmService.delete(tpmId)
        datastore.currentSession.flush()

        then:
        tpmService.count() == 4
    }

    void "test save"() {
        when:
        assert false, "TODO: Provide a valid instance to save"
        Tpm tpm = new Tpm()
        tpmService.save(tpm)

        then:
        tpm.id != null
    }
}
