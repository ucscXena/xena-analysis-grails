package org.xena.analysis

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class CompareResultServiceSpec extends Specification {

    CompareResultService compareResultService
    @Autowired Datastore datastore

    private Long setupData() {
        // TODO: Populate valid domain instances and return a valid ID
        //new CompareResult(...).save(flush: true, failOnError: true)
        //new CompareResult(...).save(flush: true, failOnError: true)
        //CompareResult compareResult = new CompareResult(...).save(flush: true, failOnError: true)
        //new CompareResult(...).save(flush: true, failOnError: true)
        //new CompareResult(...).save(flush: true, failOnError: true)
        assert false, "TODO: Provide a setupData() implementation for this generated test suite"
        //compareResult.id
    }

    void cleanup() {
        assert false, "TODO: Provide a cleanup implementation if using MongoDB"
    }

    void "test get"() {
        setupData()

        expect:
        compareResultService.get(1) != null
    }

    void "test list"() {
        setupData()

        when:
        List<CompareResult> compareResultList = compareResultService.list(max: 2, offset: 2)

        then:
        compareResultList.size() == 2
        assert false, "TODO: Verify the correct instances are returned"
    }

    void "test count"() {
        setupData()

        expect:
        compareResultService.count() == 5
    }

    void "test delete"() {
        Long compareResultId = setupData()

        expect:
        compareResultService.count() == 5

        when:
        compareResultService.delete(compareResultId)
        datastore.currentSession.flush()

        then:
        compareResultService.count() == 4
    }

    void "test save"() {
        when:
        assert false, "TODO: Provide a valid instance to save"
        CompareResult compareResult = new CompareResult()
        compareResultService.save(compareResult)

        then:
        compareResult.id != null
    }
}
