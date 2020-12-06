package org.xena.analysis

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class GmtServiceSpec extends Specification {

  GmtService gmtService
  @Autowired
  Datastore datastore

  private Long setupData() {
    // TODO: Populate valid domain instances and return a valid ID
    new Gmt(name: "asdf1", hash: "zfzf1", data:"asdfasf1").save(flush: true, failOnError: true)
    new Gmt(name: "asdf2", hash: "zfzf2", data:"asdfasf2").save(flush: true, failOnError: true)
    Gmt gmt = new Gmt(name: "asdf3", hash: "zfzf3", data:"asdfasf3").save(flush: true, failOnError: true)
    new Gmt(name: "asdf4", hash: "zfzf4", data:"asdfasf4").save(flush: true, failOnError: true)
    new Gmt(name: "asdf5", hash: "zfzf5", data:"asdfasf5").save(flush: true, failOnError: true)
    gmt.id
  }

  void cleanup() {
//    assert false, "TODO: Provide a cleanup implementation if using MongoDB"
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
//    assert false, "TODO: Verify the correct instances are returned"
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
    Gmt gmt = new Gmt(name: "asdf6", hash: "zfzf6", data:"asdfasf6")
    gmtService.save(gmt)

    then:
    gmt.id != null
  }
}
