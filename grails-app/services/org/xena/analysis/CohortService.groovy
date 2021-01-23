package org.xena.analysis

import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONObject

@Transactional
class CohortService {


  final static String COHORT_URL = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"

  Boolean validateCohorts() {

//      File allTpmFile  = new File(AnalysisService.ALL_TPM_FILE_STRING)
      def cohorts = new JSONObject(new URL(COHORT_URL).text)
//      Map<String,File> fileMap = new TreeMap<>()
      cohorts.keySet().each{
//        println "-----------------------"
//        println "processing cohort ${it}"
        Cohort cohort = Cohort.findByName(it)
//        println "cohort found ${cohort}"

//        File testFile = new File(localFileName)
        File testFile = null
        if(cohort && cohort.localTpmFile){
          testFile = new File(cohort.localTpmFile)
        }

        if(!cohort  || !testFile.exists() || testFile.size()==0){
          println "local file name ${cohort}: ${cohort?.localTpmFile}, ${testFile?.exists()}, ${testFile?.size()}"
          println "Cohort not downloaded so processing"
          JSONObject cohortObject = cohorts.get(it)
          if(cohort == null ){
            cohort = new Cohort(
              name: it
            )
          }
          String localFileName = AnalysisService.generateTpmName(it)
          // TODO: get local name for EACH TPM file
          File localCompressedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm.gz")
          String remoteUrl = AnalysisService.generateTpmRemoteUrl(cohortObject)
          // TODO: if file exists then note, if not then download
          if(!localCompressedTpmFile.exists() || localCompressedTpmFile.size()==0){
//          allTpmFile.write("")
            println "retrieving remote file ${remoteUrl} for ${it}"
            AnalysisService.retrieveTpmFile(localCompressedTpmFile,remoteUrl)
          }
          else{
            println "local compressed file exists $it"
          }
          cohort.remoteUrl = remoteUrl
          File unzippedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm")
          if(!unzippedTpmFile.exists() || unzippedTpmFile.size()==0) {
            unzippedTpmFile.delete()
            AnalysisService.decompressFile(localCompressedTpmFile,unzippedTpmFile)
            println "decompressing file ${unzippedTpmFile.absolutePath}"
          }
          else{
            println "local DE compressed file exists $it"
          }
          cohort.localTpmFile = unzippedTpmFile.absolutePath
          println "saving with local tpm file path ${cohort.localTpmFile} (not necessary $localFileName"
          cohort.save(failOnError: true,flush: true)
//          fileMap.put(it,unzippedTpmFile)
        }
//        else{
//          println "Cohort exists $it"
//        }
//        println "-----------------------"
      }

      println "Valid cohorts: ${Cohort.count}"


    }
}
