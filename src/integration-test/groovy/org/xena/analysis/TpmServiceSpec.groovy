package org.xena.analysis

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.grails.datastore.mapping.core.Datastore
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

@Integration
@Rollback
class TpmServiceSpec extends Specification {

  TpmService tpmService
  @Autowired
  Datastore datastore

  private Long setupData() {
    // TODO: Populate valid domain instances and return a valid ID
    new Tpm(cohort: "cohort1", data: "data1", url: "url1").save(flush: true, failOnError: true)
    new Tpm(cohort: "cohort2", data: "data2", url: "url2").save(flush: true, failOnError: true)
    Tpm tpm = new Tpm(cohort: "cohort3", data: "data3", url: "url3").save(flush: true, failOnError: true)
    new Tpm(cohort: "cohort4", data: "data4", url: "url4").save(flush: true, failOnError: true)
    new Tpm(cohort: "cohort5", data: "data5", url: "url5").save(flush: true, failOnError: true)
    tpm.id
  }

  void cleanup() {
//    assert false, "TODO: Provide a cleanup implementation if using MongoDB"
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
//    assert false, "TODO: Verify the correct instances are returned"
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
    Tpm tpm = new Tpm(cohort: "cohort6", data: "data6", url: "url6").save(flush: true, failOnError: true)
    tpmService.save(tpm)

    then:
    tpm.id != null
  }
}
