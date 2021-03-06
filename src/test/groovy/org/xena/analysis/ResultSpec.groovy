package org.xena.analysis

import grails.testing.gorm.DomainUnitTest
import org.grails.web.json.JSONObject
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

  void "TSV converter string"(){
    given:
    String tsvInput = new File("src/test/data/input.tsv").text
    JSONObject output = OutputHandler.convertTsv(tsvInput)

    expect:
    output.getJSONArray("samples").size()==548
    output.getJSONArray("data").size()==9
    output.getJSONArray("data").getJSONObject(0).geneset == "Notch signaling (GO:0007219)"
    output.getJSONArray("data").getJSONObject(0).data.size()==548

  }

  void "TSV converter from file "(){
    given:
    def tsvInput = new File("src/test/data/input.tsv")
    File outputFile = OutputHandler.convertTsvFromFile(tsvInput)
    def output = new JSONObject(outputFile.text)
    println "output as JSON"
    println output

    expect:
    output.getJSONArray("samples").size()==548
    output.getJSONArray("data").size()==9
    output.getJSONArray("data").getJSONObject(0).geneset == "Notch signaling (GO:0007219)"
    output.getJSONArray("data").getJSONObject(0).data.size()==548

  }
}
