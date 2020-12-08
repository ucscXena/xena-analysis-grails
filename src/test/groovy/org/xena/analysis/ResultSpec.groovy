package org.xena.analysis

import grails.testing.gorm.DomainUnitTest
import spock.lang.Specification

class ResultSpec extends Specification implements DomainUnitTest<Result> {

    def setup() {
    }

    def cleanup() {
    }

    void "get mangled name "() {
      given:
      String cohortName  = "TCGA Ova(rai)ngx"

      expect:
      "TCGA_Ova_rai_ngx"==cohortName.replaceAll("[ |\\(|\\)]","_")
    }

  void "TSV converter"(){
    given:
    String tsvInput = ""

    expect:
    true==true
  }
}
