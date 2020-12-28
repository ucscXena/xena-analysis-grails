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


  void generateResult(String s1, Cohort cohort, String s2) {

  }

  /**
   * Emulates XenaGoWidget:AnalysisService.js:calculateCustomGeneSetActivity so we aren't transferring large files back and forth
   * @param result1
   * @param result2
   * @return
   */
  CompareResult calculateCustomGeneSetActivity(Gmt gmt,Result resultA, Result resultB,String method,String samples) {
    println "custom gene set activity ${gmt}, ${resultA}, ${resultB}"
    Map meanMap = createMeanMap(resultA,resultB)
    println "mean map output ${meanMap}"
    String gmtData = gmt.data
    println "gmt data: ${gmtData}"

    String outputResult = null
    // TODO: implement

//    gmtData.split("\n").findAll{ it.split('\\').length>2}

//    return gmtData.split('\n')
//      .filter( l => l.split('\t').length>2)
//      .map( line => {
//        const entries = line.split('\t')
//
//        // we need to handle the space encoding
//        // this fails test due to an outdated library I think
//        let keyIndex = meanMap.geneSetNames.indexOf(entries[0])
//        keyIndex = keyIndex >=0 ? keyIndex : meanMap.geneSetNames.indexOf(`${entries[0]} (${entries[1]})` )
//        // console.log('key index',keyIndex,'entries',entries[0],'entries 1',entries[1],entries[0] + ' ' + entries[1])
//        return {
//          golabel: entries[0],
//          goid: entries[1],
//          gene: entries.slice(2),
//          firstSamples: meanMap.samples[0], // TODO: probably a better way to handle this
//          secondSamples: meanMap.samples[1],
//          firstGeneExpressionPathwayActivity: meanMap.zPathwayScores[0][keyIndex],
//          secondGeneExpressionPathwayActivity: meanMap.zPathwayScores[1][keyIndex],
//          firstGeneExpressionSampleActivity: meanMap.zSampleScores[0][keyIndex],
//          secondGeneExpressionSampleActivity: meanMap.zSampleScores[1][keyIndex],
//        }
//      } )

    CompareResult compareResult = new CompareResult(
      method: method,
      gmt: gmt,
      samples: samples,
      cohortA: resultA.cohort,
      cohortB: resultA.cohort,
      result: outputResult
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

    println "mean map ${resultA} ${resultB}"
    JSONObject dataA = new JSONObject(resultA.result)
    JSONObject dataB = new JSONObject(resultB.result)

    def samples = [  dataA.samples,dataB.samples ]

    def geneSetNames = getGeneSetNames(dataA)
    println "input dataA"
    println dataA as JSON
    println "input dataB"
    println dataB as JSON
    def values = extractValues(dataA,dataB)
    println "output values"
    println values
    def dataStatisticsPerGeneSet = getDataStatisticsPerGeneSet(values)
    // calculates cohorts separately
    def zSampleScores = [getZSampleScores(values[0],dataStatisticsPerGeneSet),getZSampleScores(values[1],dataStatisticsPerGeneSet)]
    println 'sample zScores'
    println zSampleScores
    // uses mean separately
    def zPathwayScores = getZPathwayScores(zSampleScores)
    println "z pathway scores"
    println zPathwayScores

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
    println "input sample scores ${sampleScores}"
//    def returnArray = []
//    for(List<Double> s in sampleScores){
//      println "input s: ${s}"
//      returnArray.push( s.sum()/s.size()  )
//    }
//    println "return array ${returnArray}"
//    return returnArray
    return (sampleScores as List ).sum() / sampleScores.size()
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
        entryValue.push( (it - statistics.mean) / statistics.variance )
      }
      scoreValues.push(entryValue)
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
      def output = getDataStatisticsForGeneSet(valuesForIndex)
      def jsonObject = new JSONObject(mean: output[0],variance: output[1])
      outputData.push(jsonObject)
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
