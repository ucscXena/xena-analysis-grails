package org.xena.analysis


import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import org.apache.commons.compress.compressors.CompressorOutputStream
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.utils.IOUtils
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

@Transactional
class AnalysisService {

  final String BPA_ANALYSIS_SCRIPT = "src/main/rlang/bpa-analysis.R"
  final String TPM_DIRECTORY = "data/tpm/"

  @NotTransactional
  static List getValuesForIndex(JSONArray jsonArray, int i) {
    def inputA = jsonArray[0][i]
    def inputB = jsonArray[1][i]
    return inputA + inputB
  }

//  /Users/nathandunn/repositories/XENA/xena-analysis-grails/data/tpm/TCGA_Ovarian_Cancer__OV_.tpm.gz
  @NotTransactional
  static String getNewFileName(File originalFile,String sampleHash){
    String TPM_SUFFIX = ".tpm.gz"
    String root = originalFile.getParent()
    String name  = originalFile.getName()
    int suffixIndex = name.indexOf(TPM_SUFFIX)
    return root + "/" + name.substring(0,suffixIndex) + sampleHash + TPM_SUFFIX
  }

  File getTpmFileForSamples(File originalFile, JSONArray samples) {
    if(samples==null || samples.size()==0) return originalFile

    String samplesHash = samples.toString().md5()

    println "original file '${originalFile.name}' and '${originalFile.absolutePath}'"
    println "sample hash ${samplesHash}"

    String newName = getNewFileName(originalFile,samplesHash)
    println "new name ${newName}"

    File newFileCompressed = new File(newName)
    // does file exist with sample hash name?
    if(newFileCompressed.exists() && newFileCompressed.size()>0)  return newFileCompressed



    String newNameDecompressed = newName.substring(0,newName.length()-(".gz".length()))
    File newFileDecompressed = new File(newNameDecompressed)
    println "newNameDecomprssed: '${newNameDecompressed}'"

    // delete ot make sure it doesn't exist
    assert !newFileCompressed.exists() || newFileCompressed.delete()
    assert !newFileDecompressed.exists() || newFileDecompressed.delete()

    // unzip to new location
    GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(new FileInputStream(originalFile))
//    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gzipInputStream))

    FileOutputStream fileOutputStream = new FileOutputStream(newFileDecompressed)
    IOUtils.copy(gzipInputStream,fileOutputStream)
    gzipInputStream.close()
    fileOutputStream.close()

    // check that it exists
    assert newFileDecompressed.exists()
    assert newFileDecompressed.size()>0

//    File tempFile = File.createTempFile("tpm-working",".tpm")
//    tempFile.deleteOnExit()

    // filter for samples
    String filteredTpmSampleString = filterTpmForSamples( newFileDecompressed,samples)
//    assert newFileDecompressed.delete()
    newFileDecompressed.write ""

    assert newFileDecompressed.exists()
    assert newFileDecompressed.size()==0

    newFileDecompressed.write(filteredTpmSampleString)

    // rezip to new location with sampleHash file


    FileOutputStream compressedFileOutputStream = new FileOutputStream(newFileCompressed)
    CompressorOutputStream compressorOutputStream = new CompressorStreamFactory()
      .createCompressorOutputStream(CompressorStreamFactory.GZIP, compressedFileOutputStream)
    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(newFileDecompressed))
    IOUtils.copy(inputStream,compressorOutputStream)
    compressorOutputStream.close()
    inputStream.close()

    assert newFileCompressed.exists()
    assert newFileCompressed.size() > 0

    return newFileCompressed
  }


  Result doBpaAnalysis(Cohort cohort,File gmtFile,Gmt gmt,String method,String tpmUrl,JSONArray samples){

    Result result = Result.findByMethodAndCohortAndGmtHashAndSamples(method,cohort,gmt.hash,samples.toString())
    println "result found ${result} for ${cohort.name} and $gmt.name "
    if(result) return result

    File originalTpmFile = getOriginalTpmFile(cohort,tpmUrl)
    File tpmFile = getTpmFileForSamples(originalTpmFile,samples)

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
      result: jsonFile.text,
      samples: samples.toString()
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

  File getOriginalTpmFile(Cohort cohort, String tpmUrl){

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
        localFile: tpmFile.absolutePath
      ).save(failOnError: true, flush: true)
      cohort.tpm = tpm
      cohort.save()
    } else {
      assert new File(tpm.localFile).exists()
      // nothign to do?
    }
    return tpmFile

  }


  @NotTransactional
  static JSONArray generateResult(String gmtData,Map meanMap) {
    JSONArray outputArray = new JSONArray()
    def geneList = gmtData.split("\n").findAll{it.split("\t").size()>2 }.collect{ it.split("\t")}

//    println "mean map"
//    println new JSONObject(meanMap) as JSON
//    println "gene set names"
//    println meanMap.geneSetNames
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
//    println "output mean map"
//    println new JSONObject(meanMap) as JSON


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
  @NotTransactional
  static def extractValuesByCohort(JSONObject input){

    def values = []
    input.data.eachWithIndex { def entry, int i ->
      def converted = entry.localFile.collect { Float.parseFloat(it)}
      values.add(converted )
    }
    return values
  }

  // input a regular data object and output of the shape: 2 cohorts, and each cohort has N genesets and each has S sample values
  // each value has to be parsed to double from string as well
  @NotTransactional
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
  @NotTransactional
  static def getZPathwayScoresForCohort(List sampleScores){
    def returnArray = []
    sampleScores.each {
      returnArray.add(it.sum() / it.size())
    }
    return returnArray
  }

// eslint-disable-next-line no-unused-vars
  @NotTransactional
  static def getZPathwayScores(sampleZScores){
    return [getZPathwayScoresForCohort(sampleZScores[0]),getZPathwayScoresForCohort(sampleZScores[1])]
  }

  @NotTransactional
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

  @NotTransactional
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

  @NotTransactional
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

  @NotTransactional
  static String filterTpmForSamples(File originalTpmFile, JSONArray samplesArray) {
    if(samplesArray.size()==0) return originalTpmFile

    List<String> sampleList = samplesArray as List<String>

    String tpmText = originalTpmFile.text

    List<String> tpmEntries = tpmText.split("\n") as List<String>

    String[] availableSamples = tpmEntries.get(0).split("\t")
    List<String> availableSamplesList = (availableSamples as List).subList(0,availableSamples.length)

    // find columns with matching samples
    List<String> intersectingLists = sampleList.intersect(availableSamplesList)
    List<Integer> matchingIndices = []
    intersectingLists.eachWithIndex { String entry, int index ->
      matchingIndices.add(availableSamplesList.indexOf(entry))
    }

    // for each line in tpmEntries, write out columns
    StringBuffer stringBuffer = new StringBuffer()
    for(String tpmEntry in tpmEntries){
      String[] inputValues = tpmEntry.split("\t")
      List<String> outputArray = [inputValues[0]]
      matchingIndices.each {
        outputArray.add(inputValues[it])
      }
      stringBuffer.append(outputArray.join("\t")).append("\n")
    }

    return stringBuffer.toString()

  }
}
