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

    void "geenrate TpmUrl"(){
      given:
     JSONObject jsonObject = new JSONObject("{\"viewInPathway\":true,\"genome background\":{\"mutation\":{\"feature_event_K\":\"event_K\",\"feature_total_pop_N\":\"total_pop_N\",\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"mutation_sampleEvent\"},\"copy number\":{\"feature_event_K\":\"event_K\",\"feature_total_pop_N\":\"total_pop_N\",\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"cnv_sampleEvent\"}},\"gene expression pathway activity\":{\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"pathway_act/TCGA_LUAD_BPA_Z.tsv\"},\"copy number for pathway view\":{\"host\":\"https://tcga.xenahubs.net\",\"amplificationThreshold\":2,\"dataset\":\"TCGA.LUAD.sampleMap/Gistic2_CopyNumber_Gistic2_all_thresholded.by_genes\",\"deletionThreshold\":-2},\"simple somatic mutation\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"mc3/LUAD_mc3.txt\"},\"copy number\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"TCGA.LUAD.sampleMap/SNP6_nocnv_genomicSegment\"},\"gene expression\":{\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"expr_tpm/TCGA-LUAD_tpm_tab.tsv\"},\"PARADIGM pathway activity\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"PanCan33_ssGSEA_1387GeneSets_NonZero_sample_level_Z/LUAD_PanCan33_ssGSEA_1387GeneSets_NonZero_sample_level_Z.txt\"},\"Regulon activity\":{\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"regulon_activity_matrix_tcga_luad.tsv\"},\"PARADIGM\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"merge_merged_reals/LUAD_merge_merged_reals.txt\"}}\n")

      when:
      String remoteUrl = AnalysisService.generateTpmRemoteUrl(jsonObject)
      println remoteUrl

      then:
      assert remoteUrl == "https://xenago.xenahubs.net/download/expr_tpm/TCGA-LUAD_tpm_tab.tsv.gz"

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
        cohorts.keySet().each{
          println "key ${it}"
          JSONObject cohortObject = cohorts.get(it)
          println "cohort object ${cohortObject.toString()}"
          // TODO: get local name for EACH TPM file
          // TODO: if file exists then note, if not then download
        }

        List<String> samples = []
        List<String> genes = []
        // map<gene, map<sample,value>>
        Map<String,Map<String,Double>> cohortData = [:]
        JSONObject firstCohort = cohorts.get(cohorts.keySet().first())
        // TODO: get all the gene names

        cohorts.keySet().each{
          JSONObject cohortObject = cohorts.get(it)
          // TODO: construct the TPM file
        }
      }

      then:
      assert file.exists() && file.size()>0 && file.text.split("\n").size() < 5000

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
