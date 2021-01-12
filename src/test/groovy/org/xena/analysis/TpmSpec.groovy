package org.xena.analysis

import grails.testing.gorm.DomainUnitTest
import org.grails.web.json.JSONObject
import spock.lang.Specification

class TpmSpec extends Specification implements DomainUnitTest<Tpm> {

  final String allTpmFile = "${AnalysisService.TPM_DIRECTORY}/TCGA_ALL.tpm"

  def setup() {
    }

    def cleanup() {
    }

    void "generate global TPM file"() {
      given:
      String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
      File file  = new File(allTpmFile)
      println "abs path: ${file.absolutePath}"

      when:
      if(!file.exists() || file.size()==0 && file.text.split("\n").size() < 5000 ){
        file.write("")
        def cohorts = new JSONObject(new URL(cohortUrl).text)
        cohorts.keySet().eachParallel {
          JSONObject cohortObject = cohorts.get(it)
          // TODO: get local name for EACH TPM file
          // TODO: if file exists then note, if not then download
        }

        List<String> samples = []
        List<String> genes = []
        // map<gene, map<sample,value>>
        Map<String,Map<String,Double>> cohortData = [:]
        JSONObject firstCohort = cohorts.get(cohorts.keySet().first())
        // TODO: get all the gene names

        cohorts.keySet().eachParallel {
          JSONObject cohortObject = cohorts.get(it)
          // TODO: construct the TPM file
        }
      }


    }
//
//  void "get TPM file mean and variance"() {
////        expect:"fix me"
////            true == false
//  }
//
//  void "output Z-scores"() {
////        expect:"fix me"
////            true == false
//  }
}
