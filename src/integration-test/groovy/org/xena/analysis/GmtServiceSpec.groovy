package org.xena.analysis

import grails.testing.mixin.integration.Integration
import grails.gorm.transactions.Rollback
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class GmtServiceSpec extends Specification {

    GmtService gmtService
    @Autowired Datastore datastore

    private Long setupData() {
        // TODO: Populate valid domain instances and return a valid ID
        //new Gmt(...).save(flush: true, failOnError: true)
        //new Gmt(...).save(flush: true, failOnError: true)
        //Gmt gmt = new Gmt(...).save(flush: true, failOnError: true)
        //new Gmt(...).save(flush: true, failOnError: true)
        //new Gmt(...).save(flush: true, failOnError: true)
        assert false, "TODO: Provide a setupData() implementation for this generated test suite"
        //gmt.id
    }

    void cleanup() {
        assert false, "TODO: Provide a cleanup implementation if using MongoDB"
    }

    void "test get"() {
        setupData()

        expect:
        gmtService.get(1) != null
    }

    void "test list"() {
        setupData()

        when:
        List<Gmt> gmtList = gmtService.list(max: 2, offset: 2)

        then:
        gmtList.size() == 2
        assert false, "TODO: Verify the correct instances are returned"
    }

    void "test count"() {
        setupData()

        expect:
        gmtService.count() == 5
    }

    void "test delete"() {
        Long gmtId = setupData()

        expect:
        gmtService.count() == 5

        when:
        gmtService.delete(gmtId)
        datastore.currentSession.flush()

        then:
        gmtService.count() == 4
    }

    void "test save"() {
        when:
        assert false, "TODO: Provide a valid instance to save"
        Gmt gmt = new Gmt()
        gmtService.save(gmt)

        then:
        gmt.id != null
    }
}
