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

  void "generate global TPM file"() {
      given:
      String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
      File allTpmFile  = new File(AnalysisService.ALL_TPM_FILE_STRING)
      println "abs path: ${allTpmFile.absolutePath}"
      def cohorts = new JSONObject(new URL(cohortUrl).text)
      println "keys size: ${cohorts.size()}"

    when:
      if(!allTpmFile.exists() || allTpmFile.size()==0 ){

        // cohort name, local file
        Map<String,File> fileMap = new TreeMap<>()

        allTpmFile.write("")
        cohorts.keySet().each{
          JSONObject cohortObject = cohorts.get(it)
//          println "cohort object ${cohortObject.toString()}"
          // TODO: get local name for EACH TPM file
          String localFileName = AnalysisService.generateTpmName(it)
          File localCompressedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm.gz")
          // TODO: if file exists then note, if not then download
          if(!localCompressedTpmFile.exists() || localCompressedTpmFile.size()==0){
            allTpmFile.write("")
            String remoteurl = AnalysisService.generateTpmRemoteUrl(cohortObject)
            println "retrieving remote file ${remoteurl} for ${it}"
            AnalysisService.retrieveTpmFile(localCompressedTpmFile,remoteurl)
          }
          File unzippedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm")
          if(!unzippedTpmFile.exists() || unzippedTpmFile.size()==0) {
            unzippedTpmFile.delete()
            AnalysisService.decompressFile(localCompressedTpmFile,unzippedTpmFile)
            println "decompressing file ${unzippedTpmFile.absolutePath}"
          }
          fileMap.put(it,unzippedTpmFile)
        }

        List<String> genes = AnalysisService.getGenesFromTpm(fileMap.iterator().next().getValue())
        println "# of genes to process ${genes.size()} . . . ${genes.subList(0,10).join("\t")}"

//        // lets process the samples first
//        List<String> allSamples = []
//        fileMap.values().each {File tpmFile ->
//        }
//        allTpmFile.write(" \t"+allSamples.join("\t"))
//
//
//
//
//
//        // process gene by gene
//        genes.each {
//          // process each file
//        }
//
//

        // map<gene, map<sample,value>>
//        Map<String,Map<String,Double>> cohortData = [:]
//        List<File> cohortDataFiles  = []
//
//        cohorts.keySet().eachWithIndex{ def it , int index ->
//          println "memory 1"
//          System.gc()
//          OutputHandler.printMemory()
//
//          String tpmLocalFile = "${AnalysisService.TPM_DIRECTORY}
//          File tpmDataFile = new File()
//            TpmData tpmData = AnalysisService.getTpmDataFromFile(fileMap.get(it),genes)
//          FileOutputStream fileOutputStream = new FileOutputStream(tpmDataFile)
//          ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)
//          objectOutputStream.writeObject(tpmData)
//          objectOutputStream.close()
//          cohortDataFiles.add(tpmDataFile)
//
//          println "memory 2"
//          System.gc()
//          OutputHandler.printMemory()
//          println "assembling TPM file ${it}"
//          // TODO: construct the TPM file
////          cohortData.add(tpmData)
//          println "memory 3"
//          System.gc()
//          OutputHandler.printMemory()
//        }
//
//        // write out TPM file
//        println "writing TPM data to file"
//        AnalysisService.writeTpmAllFile(cohortDataFiles,allTpmFile,genes)
      }

      then:
      assert allTpmFile.exists() && allTpmFile.size()>0 && allTpmFile.text.split("\n").size() < 5000

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
