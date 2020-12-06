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
        new Result(method: "BPA1",geneset: "geneset1",cohort: "cohort1",result: "result1").save(flush: true, failOnError: true)
      new Result(method: "BPA2",geneset: "geneset2",cohort: "cohort2",result: "result2").save(flush: true, failOnError: true)
      Result result = new Result(method: "BPA3",geneset: "geneset3",cohort: "cohort3",result: "result3").save(flush: true, failOnError: true)
      new Result(method: "BPA4",geneset: "geneset4",cohort: "cohort4",result: "result4").save(flush: true, failOnError: true)
      new Result(method: "BPA5",geneset: "geneset5",cohort: "cohort5",result: "result5").save(flush: true, failOnError: true)
      result.id
    }

    void cleanup() {
//        assert false, "TODO: Provide a cleanup implementation if using MongoDB"
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
//        assert false, "TODO: Verify the correct instances are returned"
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
        Result result = new Result(method: "BPA6",geneset: "geneset6",cohort: "cohort6",result: "result6").save(flush: true, failOnError: true)
        resultService.save(result)

        then:
        result.id != null
    }
}
