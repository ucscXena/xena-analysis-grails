package org.xena.analysis

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class ResultServiceSpec extends Specification {

    ResultService resultService
    @Autowired Datastore datastore

    private Long setupData() {
        // TODO: Populate valid domain instances and return a valid ID
        //new Result(...).save(flush: true, failOnError: true)
        //new Result(...).save(flush: true, failOnError: true)
        //Result result = new Result(...).save(flush: true, failOnError: true)
        //new Result(...).save(flush: true, failOnError: true)
        //new Result(...).save(flush: true, failOnError: true)
        assert false, "TODO: Provide a setupData() implementation for this generated test suite"
        //result.id
    }

    void cleanup() {
        assert false, "TODO: Provide a cleanup implementation if using MongoDB"
    }

    void "test get"() {
        setupData()

        expect:
        resultService.get(1) != null
    }

    void "test list"() {
        setupData()

        when:
        List<Result> resultList = resultService.list(max: 2, offset: 2)

        then:
        resultList.size() == 2
        assert false, "TODO: Verify the correct instances are returned"
    }

    void "test count"() {
        setupData()

        expect:
        resultService.count() == 5
    }

    void "test delete"() {
        Long resultId = setupData()

        expect:
        resultService.count() == 5

        when:
        resultService.delete(resultId)
        datastore.currentSession.flush()

        then:
        resultService.count() == 4
    }

    void "test save"() {
        when:
        assert false, "TODO: Provide a valid instance to save"
        Result result = new Result()
        resultService.save(result)

        then:
        result.id != null
    }
}
