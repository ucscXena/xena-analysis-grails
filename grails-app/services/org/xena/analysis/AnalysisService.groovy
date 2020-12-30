package org.xena.analysis

import grails.converters.JSON
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

@Transactional
class AnalysisService {

  final String BPA_ANALYSIS_SCRIPT = "src/main/rlang/bpa-analysis.R"
  final String TPM_DIRECTORY = "data/tpm/"

  static List getValuesForIndex(JSONArray jsonArray, int i) {
    def inputA = jsonArray[0][i]
    def inputB = jsonArray[1][i]
    return inputA + inputB
  }

  Result doBpaAnalysis(Cohort cohort,File gmtFile,Gmt gmt,String method,String tpmUrl){

    Result result = Result.findByMethodAndCohortAndGmtHash(method,cohort,gmt.hash)
    if(result) return result

    File tpmFile = getTpmFile(cohort,tpmUrl)

    String mangledCohortName = cohort.name.replaceAll("[ |\\(|\\)]", "_")
    File outputFile = File.createTempFile("output-${mangledCohortName}${gmt.hash}", ".tsv")
    outputFile.write("")
    runBpaAnalysis(gmtFile,tpmFile,outputFile)

    long lastOutputFileSize = 0
    int waitCount = 0
    while ((outputFile.size() == 0 || outputFile.size() == lastOutputFileSize) && waitCount < 10) {
      println "waiting ${outputFile.size()}"
      sleep(2000)
      ++waitCount
    }


    File jsonFile = OutputHandler.convertTsvFromFile(outputFile)
    result = new Result(
      method: method,
      gmt: gmt,
      gmtHash: gmt.hash,
      cohort: cohort,
      result: jsonFile.text
    ).save(flush: true, failOnError: true)
    return result
  }

  @NotTransactional
  def runBpaAnalysis(File gmtFile, File tpmFile, File outputFile) {
    Process process = "Rscript ${BPA_ANALYSIS_SCRIPT} ${gmtFile.absolutePath} ${tpmFile.absolutePath} ${outputFile.absolutePath} BPA".execute()
    int exitValue = process.waitFor()
    println "exit value ${exitValue}"

//    println "stdout ${process.in.text}"
//    println "stderr ${process.err.text}"
    return exitValue
  }

  @NotTransactional
  def checkAnalysisEnvironment() throws Exception {

    def proc = "Rscript ${BPA_ANALYSIS_SCRIPT}".execute()
    String inputText = proc.in.text
    String errorText = proc.err.text
    log.debug("input text: ${inputText}")
    log.debug("error text: ${errorText}")

    assert inputText.contains("NULL")
    assert errorText.contains("Loading required package: Biobase")
    return 0

  }

  File getTpmFile(Cohort cohort,String tpmUrl){

    Tpm tpm = Tpm.findByCohort(cohort)
    String mangledCohortName = cohort.name.replaceAll("[ |\\(|\\)]", "_")
    File tpmFile = new File(TPM_DIRECTORY + mangledCohortName + ".tpm.gz")
    println "tpm file ${tpmFile}"
    println "tpm file size ${tpmFile.size()}"
    if (tpm == null) {
      println "tpm is null, so downloading"
      if (!tpmFile.exists() || tpmFile.size() == 0) {
        def out = new BufferedOutputStream(new FileOutputStream(tpmFile))
        out << tpmUrl.toURL().openStream()
        out.close()
      }
      tpm = new Tpm(
        cohort: cohort,
        url: tpmUrl,
        data: tpmFile.absolutePath
      ).save(failOnError: true, flush: true)
      cohort.tpm = tpm
      cohort.save()
    } else {
      assert new File(tpm.data).exists()
      // nothign to do?
    }
    return tpmFile

  }


  @NotTransactional
  static JSONArray generateResult(String gmtData,Map meanMap) {
    JSONArray outputArray = new JSONArray()
    def geneList = gmtData.split("\n").findAll{it.split("\t").size()>2 }.collect{ it.split("\t")}
    for(List gene in geneList){
        def keyIndex = meanMap.geneSetNames.findIndexOf { it==gene[0]}
        keyIndex = keyIndex >=0 ? keyIndex : meanMap.geneSetNames.findIndexOf { it=="${gene[0]} (${gene[1]})" }
        JSONObject jsonObject = new JSONObject()
        jsonObject.golabel = gene[0]
        jsonObject.goid = gene[1]

        jsonObject.gene = gene.subList(2,gene.size())
        jsonObject.firstSamples= meanMap.samples[0]
        jsonObject.secondSamples= meanMap.samples[1]
        jsonObject.firstGeneExpressionPathwayActivity= meanMap.zPathwayScores[0][keyIndex]
        jsonObject.secondGeneExpressionPathwayActivity= meanMap.zPathwayScores[1][keyIndex]
        jsonObject.firstGeneExpressionSampleActivity= meanMap.zSampleScores[0][keyIndex]
        jsonObject.secondGeneExpressionSampleActivity= meanMap.zSampleScores[1][keyIndex]

        outputArray.push(jsonObject)
    }
    return outputArray
  }

  /**
   * Emulates XenaGoWidget:AnalysisService.js:calculateCustomGeneSetActivity so we aren't transferring large files back and forth
   * @param result1
   * @param result2
   * @return
   */
  CompareResult calculateCustomGeneSetActivity(Gmt gmt,Result resultA, Result resultB,String method,String samples) {

    Map meanMap = createMeanMap(resultA,resultB)
    println "output mean map"
    println new JSONObject(meanMap) as JSON


    String gmtData = gmt.data
    // TODO: implement
    JSONArray inputArray = AnalysisService.generateResult(gmtData,meanMap)

    CompareResult compareResult = new CompareResult(
      method: method,
      gmt: gmt,
      samples: samples,
      cohortA: resultA.cohort,
      cohortB: resultB.cohort,
      result:inputArray.toString()
    ).save(flush: true, failOnError: true)

    return compareResult

  }

  // input a regular data object and output of the shape: 2 cohorts, and each cohort has N genesets and each has S sample values
  // each value has to be parsed to double from string as well
  static def extractValuesByCohort(JSONObject input){

    def values = []
    input.data.eachWithIndex { def entry, int i ->
      def converted = entry.data.collect { Float.parseFloat(it)}
      values.add(converted )
    }
    return values
  }

  // input a regular data object and output of the shape: 2 cohorts, and each cohort has N genesets and each has S sample values
  // each value has to be parsed to double from string as well
  static JSONArray extractValues(JSONObject inputA,JSONObject inputB){
    return [extractValuesByCohort(inputA),extractValuesByCohort(inputB)]
  }

  Map createMeanMap(Result resultA,Result resultB) {
    JSONObject dataA = new JSONObject(resultA.result)
    JSONObject dataB = new JSONObject(resultB.result)

    def samples = [  dataA.samples,dataB.samples ]

    def geneSetNames = getGeneSetNames(dataA)
    def values = extractValues(dataA,dataB)
    def dataStatisticsPerGeneSet = getDataStatisticsPerGeneSet(values)
    // calculates cohorts separately
    def zSampleScores = [getZSampleScores(values[0],dataStatisticsPerGeneSet),getZSampleScores(values[1],dataStatisticsPerGeneSet)]
    // uses mean separately
    def zPathwayScores = getZPathwayScores(zSampleScores)

    JSONObject jsonObject = new JSONObject()
    jsonObject.put("samples",samples)
    jsonObject.put("zSampleScores",zSampleScores)
    jsonObject.put("zPathwayScores",zPathwayScores)
    jsonObject.put("geneSetNames",geneSetNames)

    return jsonObject
  }
/**
 * Put mean sample scores in for each entry
 * @param sampleScores
 * @return
 */
  static def getZPathwayScoresForCohort(List sampleScores){
    def returnArray = []
    sampleScores.each {
      returnArray.push(it.sum() / it.size())
    }
    return returnArray
  }

// eslint-disable-next-line no-unused-vars
  static def getZPathwayScores(sampleZScores){
    return [getZPathwayScoresForCohort(sampleZScores[0]),getZPathwayScoresForCohort(sampleZScores[1])]
  }

  static def getZSampleScores(values,dataStatisticsPerGeneSet){
    def scoreValues = []
    values.eachWithIndex { def value , int index ->
      def statistics  = dataStatisticsPerGeneSet[index] // [0] = mean, [1] = variance
      // TODO: collect for each
//      const array = values[index].data.map( v => (v - mean)/ variance )
      // TODO: do other collection
      def entryValue = []
      value.each{
        def convertedValue = (it - statistics.mean) / statistics.variance
//        println "input value ($it - $statistics.mean) / $statistics.variance = $convertedValue "
        entryValue.add( convertedValue )
      }
      scoreValues.add(entryValue)
    }
    return scoreValues
  }

  List getGeneSetNames(JSONObject inputData) {
    return inputData.data.collect{ it.geneset}
  }

  static List getDataStatisticsPerGeneSet(def inputData) {
    def outputData = []
    for(int i = 0 ; i < inputData[0].size() ; i++ ){
      def valuesForIndex = getValuesForIndex(inputData,i)
//      println "values for index $i"
//      println valuesForIndex
      def output = getDataStatisticsForGeneSet(valuesForIndex)
      def jsonObject = new JSONObject(mean: output[0],variance: output[1])
      outputData.add(jsonObject)
    }
    return outputData
  }

  static def getDataStatisticsForGeneSet(List inputArray){
    int count = inputArray.size()
    def total = 0
    for(double a in inputArray){
      total += a
    }
    def mean = total / count
    double temp = 0
    for(double a in inputArray){
      temp += (a - mean) * (a -mean)
    }
    def variance = temp / (inputArray.size() - 1)
    return [mean,variance]

  }
}
