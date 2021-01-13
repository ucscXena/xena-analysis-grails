package org.xena.analysis


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

  void "generate global TPM file"() {
      given:
      String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
      File allTpmFile  = new File(AnalysisService.ALL_TPM_FILE_STRING)
      println "abs path: ${allTpmFile.absolutePath}"

      when:
      if(!allTpmFile.exists() || allTpmFile.size()==0 && allTpmFile.text.split("\n").size() < 5000 ){

        // cohort name, local file
        Map<String,File> fileMap = new TreeMap<>()

        allTpmFile.write("")
        def cohorts = new JSONObject(new URL(cohortUrl).text)
        cohorts.keySet().each{
          println "key ${it}"
          JSONObject cohortObject = cohorts.get(it)
//          println "cohort object ${cohortObject.toString()}"
          // TODO: get local name for EACH TPM file
          String localFileName = AnalysisService.generateTpmLocalUrl(it)
          println "local file name ${localFileName}"
          File localCompressedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm.gz")
          // TODO: if file exists then note, if not then download
          if(!localCompressedTpmFile.exists() || localCompressedTpmFile.size()==0){
            allTpmFile.write("")
            println "retrieving remote file"
            AnalysisService.retrieveTpmFile(localCompressedTpmFile,AnalysisService.generateTpmRemoteUrl(cohortObject))
          }
          else{
            println "file exists"
          }
          File unzippedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm")
          if(!unzippedTpmFile.exists() || unzippedTpmFile.size()==0) {
            unzippedTpmFile.delete()
            AnalysisService.decompressFile(localCompressedTpmFile,unzippedTpmFile)
            println "decompressing file ${unzippedTpmFile.absolutePath}"
          }
          else{
            println "decompressed file exists ${unzippedTpmFile.absolutePath}"
          }
          fileMap.put(it,unzippedTpmFile)
        }

        List<String> genes = AnalysisService.getGenesFromTpm(fileMap.iterator().next().getValue())
        println "# of genes to process ${genes.size()} . . . ${genes.subList(0,10).join("\t")}"
        // map<gene, map<sample,value>>
//        Map<String,Map<String,Double>> cohortData = [:]
        List<TpmData> cohortData  = []

        cohorts.keySet().each{
          TpmData tpmData = AnalysisService.getTpmDataFromFile(fileMap.get(it),genes)
          println "assembling TPM file ${it}"
          // TODO: construct the TPM file
          cohortData.add(tpmData)
        }

        // write out TPM file
        println "writing TPM data to file"
        AnalysisService.writeTpmAllFile(cohortData,allTpmFile,genes)
      }

      then:
      assert allTpmFile.exists() && allTpmFile.size()>0 && allTpmFile.text.split("\n").size() < 5000

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
