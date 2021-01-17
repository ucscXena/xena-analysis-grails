package org.xena.analysis

import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONObject

@Transactional
class CohortService {

    Boolean validateCohorts() {

      String cohortUrl = "https://raw.githubusercontent.com/ucscXena/XenaGoWidget/develop/src/data/defaultDatasetForGeneset.json"
//      File allTpmFile  = new File(AnalysisService.ALL_TPM_FILE_STRING)
      def cohorts = new JSONObject(new URL(cohortUrl).text)
//      Map<String,File> fileMap = new TreeMap<>()
      cohorts.keySet().each{
        Cohort cohort = Cohort.findOrSaveByName(it)
        String localFileName = AnalysisService.generateTpmLocalUrl(it)
        File testFile = new File(localFileName)
        if(!cohort || cohort.localTpmFile != localFileName || !testFile.exists() || !testFile.size()>0){
          JSONObject cohortObject = cohorts.get(it)
//          println "cohort object ${cohortObject.toString()}"
          // TODO: get local name for EACH TPM file
          File localCompressedTpmFile = new File("${AnalysisService.TPM_DIRECTORY}/${localFileName}.tpm.gz")
          // TODO: if file exists then note, if not then download
          if(!localCompressedTpmFile.exists() || localCompressedTpmFile.size()==0){
//          allTpmFile.write("")
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
//          fileMap.put(it,unzippedTpmFile)
        }

      }


    }
}
