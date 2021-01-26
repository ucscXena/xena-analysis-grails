package org.xena.analysis

import grails.converters.JSON
import grails.testing.gorm.DomainUnitTest
import org.grails.web.json.JSONObject
import spock.lang.Specification

class TpmSpec extends Specification implements DomainUnitTest<Tpm> {


  def setup() {
    }

    def cleanup() {
    }

    void "generate TpmUrl"(){
      given:
     JSONObject jsonObject = new JSONObject("{\"viewInPathway\":true,\"genome background\":{\"mutation\":{\"feature_event_K\":\"event_K\",\"feature_total_pop_N\":\"total_pop_N\",\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"mutation_sampleEvent\"},\"copy number\":{\"feature_event_K\":\"event_K\",\"feature_total_pop_N\":\"total_pop_N\",\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"cnv_sampleEvent\"}},\"gene expression pathway activity\":{\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"pathway_act/TCGA_LUAD_BPA_Z.tsv\"},\"copy number for pathway view\":{\"host\":\"https://tcga.xenahubs.net\",\"amplificationThreshold\":2,\"dataset\":\"TCGA.LUAD.sampleMap/Gistic2_CopyNumber_Gistic2_all_thresholded.by_genes\",\"deletionThreshold\":-2},\"simple somatic mutation\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"mc3/LUAD_mc3.txt\"},\"copy number\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"TCGA.LUAD.sampleMap/SNP6_nocnv_genomicSegment\"},\"gene expression\":{\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"expr_tpm/TCGA-LUAD_tpm_tab.tsv\"},\"PARADIGM pathway activity\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"PanCan33_ssGSEA_1387GeneSets_NonZero_sample_level_Z/LUAD_PanCan33_ssGSEA_1387GeneSets_NonZero_sample_level_Z.txt\"},\"Regulon activity\":{\"host\":\"https://xenago.xenahubs.net\",\"dataset\":\"regulon_activity_matrix_tcga_luad.tsv\"},\"PARADIGM\":{\"host\":\"https://tcga.xenahubs.net\",\"dataset\":\"merge_merged_reals/LUAD_merge_merged_reals.txt\"}}\n")

      when:
      String remoteUrl = AnalysisService.generateTpmRemoteUrl(jsonObject)
      println remoteUrl

      then:
      assert remoteUrl == "https://xenago.xenahubs.net/download/expr_tpm/TCGA-LUAD_tpm_tab.tsv.gz"
    }

  void "get genes from tpm"(){
    given:
    File file = new File("src/test/data/sample_tpm_file.tpm")

    when:
    List<String> geneList = AnalysisService.getGenesFromTpm(file)

    then:
    assert geneList[0]=='5_8S_rRNA'
    assert geneList[10]=='A2ML1-AS2'
    assert geneList.size()==11

  }


  void "validate TpmStat"(){

    given: "a tpmstat of values"
    TpmStat tpmStat = new TpmStat()

    when:
    tpmStat.addStat(-3.0)
    tpmStat.addStat(5.0)
    tpmStat.addStat(-7.0)
    tpmStat.addStat(9.0)
    JSONObject tpmStatObject = JSON.parse(tpmStat.toString()) as JSONObject
    TpmStat tpmStatCopy = new TpmStat(tpmStatObject)

    then:
    tpmStat.numDataValues()==4
    tpmStat.mean()==1
    tpmStat.standardDeviation()==7.302967433402215
    tpmStatCopy.numDataValues()==4
    tpmStatCopy.mean()==1
    tpmStatCopy.standardDeviation()==7.302967433402215
    println "-3 -> ${tpmStat.getZValue(-3)}"
    println "5 -> ${tpmStat.getZValue(5)}"
    println "-7 -> ${tpmStat.getZValue(-7)}"
    println "9 -> ${tpmStat.getZValue(9)}"
    Math.abs(tpmStat.getZValue(-3) - (-3 - 1 ) / 7.302967433402215) < 0.00001
    Math.abs(tpmStat.getZValue(5) - (5 - 1 ) / 7.302967433402215) < 0.00001
    Math.abs(tpmStat.getZValue(-7) - (-7 - 1 ) / 7.302967433402215) < 0.00001
    Math.abs(tpmStat.getZValue(9) - (9 - 1 ) / 7.302967433402215) < 0.00001



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
