package org.xena.analysis

import grails.converters.JSON
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
  final static String TPM_DIRECTORY = "data/tpm/"
  final static String ALL_TPM_FILE_STRING = "${TPM_DIRECTORY}/TCGA_ALL.tpm"
  // Count: 642665709
  // Total: 7.80789496160133E8
  final static Double TCGA_ALL_TPM_MEAN = 1.2149232255989761
  final static Double TCGA_ALL_TPM_VARIANCE = 2.3314200093496904
  final static Double TCGA_ALL_TPM_STD = Math.sqrt(TCGA_ALL_TPM_VARIANCE)

  @NotTransactional
  static List getValuesForIndex(JSONArray jsonArray, int i) {
    def inputA = jsonArray[0][i]
    def inputB = jsonArray[1][i]
    return inputA + inputB
  }

  @NotTransactional
  static Double getGlobalTpmZScore(Double rawScore) {
    return (rawScore - TCGA_ALL_TPM_MEAN) / TCGA_ALL_TPM_STD
  }

//  /Users/nathandunn/repositories/XENA/xena-analysis-grails/data/tpm/TCGA_Ovarian_Cancer__OV_.tpm.gz
  @NotTransactional
  static String getNewFileName(File originalFile, String sampleHash) {
    String TPM_SUFFIX = ".tpm.gz"
    String root = originalFile.getParent()
    String name = originalFile.getName()
    int suffixIndex = name.indexOf(TPM_SUFFIX)
    return root + "/" + name.substring(0, suffixIndex) + sampleHash + TPM_SUFFIX
  }


  static String generateLocalTpmName(String cohortName) {
    return cohortName.replaceAll("[ |\\(|\\)]", "_")
  }

  static String generateTpmRemoteUrl(JSONObject cohortObject) {
    return "${cohortObject['gene expression'].host}/download/${cohortObject['gene expression'].dataset}.gz"
  }

  static String generateTpmRawRemoteUrl(JSONObject cohortObject) {
    return "${cohortObject['gene expression raw'].host}/download/${cohortObject['gene expression raw'].dataset}.gz"
  }

  static List<String> getGenesFromTpm(File inputTpmFile) {
    String inputText = inputTpmFile.text
    def fullList = inputText.split("\n").findAll { it.split("\t").size() > 2 }.collect { it.split("\t")[0] }
    return fullList.subList(1, fullList.size())
  }

  static TpmData getTpmDataFromFile(File file, List<String> genes) {
    TpmData tpmData = new TpmData()
    List<String> samplesIndex = []
    int index = 0
    file.text.splitEachLine("\t") {
      if (index == 0) {
        samplesIndex = it.subList(1, it.size())
        tpmData.setSamples(samplesIndex)
      } else {
        String gene = it[0]
        tpmData.geneData.put(gene, it.subList(1, it.size()))
      }
      ++index
    }
    assert genes.size() == tpmData.geneData.size()
    return tpmData
  }

  static List<String> getAllSamples(List<File> tpmSerializedDataFileList) {
    List<String> samples = []
    tpmSerializedDataFileList.each { File tpmDataFile ->
      TpmData tpmData = getSerializedTpmDataFromFile(tpmDataFile)
      samples.addAll(tpmData.samples)
    }
    return samples
  }

  static TpmData getSerializedTpmDataFromFile(File file) {
    FileInputStream fis = new FileInputStream(file)
    ObjectInputStream ois = new ObjectInputStream(fis)
    TpmData tpmData = (TpmData) ois.readObject()
    ois.close()
    return tpmData
  }

  static void writeTpmAllFile(List<File> tpmSerializedDataFileList, File outputAllTpmFile, List<String> genes) {
    println "memory pre-samples"
    System.gc()
    OutputHandler.printMemory()
    List<String> samples = getAllSamples(tpmSerializedDataFileList)
    println "got samples"
    System.gc()
    OutputHandler.printMemory()
    println "samples list ${samples.size()} . . . .${samples.subList(0, 20).join("\t")}"
    outputAllTpmFile.write(samples.join("\t"))
    outputAllTpmFile.write("\n")
    println "wrote samples "
    System.gc()
    OutputHandler.printMemory()

//    genes.each { String gene ->
//      println "writing gene ${gene}"
//      tpmDataFileList.each {TpmData tpmData ->
//        def sampleData = tpmData.geneData.get(gene)
//        file.write(sampleData.join("\t"))
//      }
//      file.write("\n")
//    }
  }


  File getTpmFileForSamples(File originalFile, JSONArray samples) {
    if (samples == null || samples.size() == 0) return originalFile

    String samplesHash = samples.toString().md5()

    println "original file '${originalFile.name}' and '${originalFile.absolutePath}'"
    println "sample hash ${samplesHash}"

    String newName = getNewFileName(originalFile, samplesHash)
    println "new name ${newName}"

    File newFileCompressed = new File(newName)
    // does file exist with sample hash name?
    if (newFileCompressed.exists() && newFileCompressed.size() > 0) return newFileCompressed


    String newNameDecompressed = newName.substring(0, newName.length() - (".gz".length()))
    File newFileDecompressed = new File(newNameDecompressed)
    println "newNameDecomprssed: '${newNameDecompressed}'"

    // delete ot make sure it doesn't exist
    assert !newFileCompressed.exists() || newFileCompressed.delete()
    assert !newFileDecompressed.exists() || newFileDecompressed.delete()

    // unzip to new location
    GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(new FileInputStream(originalFile))
//    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gzipInputStream))

    FileOutputStream fileOutputStream = new FileOutputStream(newFileDecompressed)
    IOUtils.copy(gzipInputStream, fileOutputStream)
    gzipInputStream.close()
    fileOutputStream.close()

    // check that it exists
    assert newFileDecompressed.exists()
    assert newFileDecompressed.size() > 0

//    File tempFile = File.createTempFile("tpm-working",".tpm")
//    tempFile.deleteOnExit()

    // filter for samples
    String filteredTpmSampleString = filterTpmForSamples(newFileDecompressed, samples)
//    assert newFileDecompressed.delete()
    newFileDecompressed.write ""

    assert newFileDecompressed.exists()
    assert newFileDecompressed.size() == 0

    newFileDecompressed.write(filteredTpmSampleString)

    // rezip to new location with sampleHash file


    FileOutputStream compressedFileOutputStream = new FileOutputStream(newFileCompressed)
    CompressorOutputStream compressorOutputStream = new CompressorStreamFactory()
      .createCompressorOutputStream(CompressorStreamFactory.GZIP, compressedFileOutputStream)
    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(newFileDecompressed))
    IOUtils.copy(inputStream, compressorOutputStream)
    compressorOutputStream.close()
    inputStream.close()

    assert newFileCompressed.exists()
    assert newFileCompressed.size() > 0

    return newFileCompressed
  }

  static File decompressFile(File localCompressedTpmFile, File unzippedTpmFile) {
    GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(new FileInputStream(localCompressedTpmFile))
    FileOutputStream fileOutputStream = new FileOutputStream(unzippedTpmFile)
    IOUtils.copy(gzipInputStream, fileOutputStream)
    gzipInputStream.close()
    fileOutputStream.close()

    assert unzippedTpmFile.exists()
    assert unzippedTpmFile.size() > 0
    return unzippedTpmFile
  }

  def setJobState(Long id, RunState runState, String errorMessage = null) {
    TpmGmtAnalysisJob.executeUpdate("update TpmGmtAnalysisJob j set j.runState = :state , j.lastUpdated = :date, j.errorMessage = :error where j.id = :id", [id: id, state: runState, date: new Date(), error: errorMessage])
//    analysisJob.save(flush: true, failOnError: true)
  }


  def doBpaAnalysis2(TpmGmtAnalysisJob analysisJob) {

    println "doing BPA analysis with ${analysisJob}"

    Cohort cohort = analysisJob.cohort
    Gmt gmt = analysisJob.gmt

    TpmGmtResult result = TpmGmtResult.findByCohortAndGmtHashAndMethod(cohort, gmt.hash, gmt.method)
    println "result found ${result} for ${cohort.name} and $gmt.name "
    if (result) return result

    File tpmFile = new File(cohort.localTpmFile)


    File gmtFile = File.createTempFile("gmt-${gmt.name}${gmt.hash}", ".gmt")
    gmtFile.write(gmt.data)

    String mangledCohortName = cohort.name.replaceAll("[ |\\(|\\)]", "_")
    File outputFile = File.createTempFile("output-${mangledCohortName}${gmt.hash}", ".tsv")
    outputFile.write("")
    runBpaAnalysis(gmtFile, tpmFile, outputFile)

    long lastOutputFileSize = 0
    int waitCount = 0
    while ((outputFile.size() == 0 || outputFile.size() == lastOutputFileSize) && waitCount < 10) {
      println "waiting ${outputFile.size()}"
      sleep(2000)
      ++waitCount
    }


    File jsonFile = OutputHandler.convertTsvFromFile(outputFile)
    println "output returned $jsonFile"
    result = new TpmGmtResult(
      method: gmt.method,
      gmt: gmt,
      gmtHash: gmt.hash,
      cohort: cohort,
      result: jsonFile.text,
    ).save(failOnError: true)

//      setJobState(analysisJob,RunState.FINISHED)
//      }

    // if we have calculated all of them, then we take the mean and variance for EVERY TPM file in the cohort
//    int possibleCohortCount = new JSONObject(new URL(CohortService.COHORT_URL).text).keySet().size()
//    println "possible cohort count: ${possibleCohortCount}"
//    int resultCount = TpmGmtResult.countByGmt(gmt)
//    println "result count: ${resultCount}"
//    if (resultCount == possibleCohortCount) {
//      createGmtStats(gmt)
//    }
//    // count all
//    println "returnning "

    return result
  }

  def createGmtStats(Gmt gmt) {
    // 1. get sum and count
    println "same"
    TpmStatMap tpmStatMap = new TpmStatMap()
    // get id for each one, even if more queries, will reduce memory
    println "pre-query"
    List<Long> gmtIds = TpmGmtResult.executeQuery("select t.id from TpmGmtResult t join t.gmt g where g = :gmt", [gmt: gmt])
    println "gmt ids: ${gmtIds.size()}"
    println "list of gmt ids: ${gmtIds.join(",")}"
//    TpmGmtResult.findAllByGmt(gmt).each {
    for (Long gmtId in gmtIds) {
      println "doing query: $gmtId"
      TpmGmtResult tpmGmtResult = TpmGmtResult.get(gmtId)
      println "result: $tpmGmtResult"
      println "input ${tpmGmtResult.cohort.name}"
      def tpmResult = JSON.parse(tpmGmtResult.result) as JSONObject
      println "output ${tpmGmtResult.cohort.name}"
      tpmStatMap = TpmStatGenerator.getPathwayStatMap(tpmResult, tpmStatMap)
      println "post map ${tpmGmtResult.cohort.name}"
    }
    println "finished loop ${gmt.name}"
    gmt.stats = tpmStatMap.toString()
    println "status done $gmt.name"
    gmt.save(flush: true, failOnError: true)
    println "saved and flushed gmt"

  }

  def getVarianceForGmt(Gmt gmt, double mean, long count) {
    double variance = 0
    TpmGmtResult.findAllByGmt(gmt).each {
//      println "data: ${it.result}"
      def tpmResult = getVarianceForResult(it.result, mean, count)
      variance += tpmResult
//      println "data: ${it.result}"
    }
    return variance
  }

  @NotTransactional
  def getSumAndCountForResult(String resultData, JSONObject initObject) {
//    println "input resultData "
//    println resultData

    def object = JSON.parse(resultData)

    JSONArray dataArray = object.data as JSONArray
//    println "data size: ${dataArray.size()}"
//    println "parsed object"
//    println object.toString()
    long count = 0
    double sum = 0d
    for (int i = 0; i < dataArray.size(); i++) {
      def sampleData = dataArray.get(i).data
//      println "sample data: $sampleData"
      def doubleData = sampleData as List<Double>
//      println "double data: $doubleData"
      count += doubleData.size()
      doubleData.sum { sum += Double.parseDouble(it) }
    }

    return [count, sum]
  }

  @NotTransactional
  def getVarianceForResult(String resultData, double mean, long count) {
    def object = JSON.parse(resultData)
    JSONArray dataArray = object.data as JSONArray
    double variance = 0d
    for (int i = 0; i < dataArray.size(); i++) {
      def sampleData = dataArray.get(i).data
      def doubleData = sampleData as List<Double>
      doubleData.sum {
        variance += Math.pow(Double.parseDouble(it) - mean, 2.0)
      }
    }
    return variance / count
  }

  def getSumAndTotalForGmt(Gmt gmt) {
    double sum = 0
    long count = 0
    TpmGmtResult.findAllByGmt(gmt).each {
      def tpmResult = getSumAndCountForResult(it.result)
      sum += tpmResult[0]
      count += tpmResult[1]
    }
    return [sum, count]
  }

  /**
   * @deprecated
   *
   *
   * @param cohort
   * @param gmtFile
   * @param gmt
   * @param method
   * @param samples
   * @return
   */
  Result doBpaAnalysis(Cohort cohort, File gmtFile, Gmt gmt, String method, JSONArray samples) {

    Result result = Result.findByMethodAndCohortAndGmtHashAndSamples(method, cohort, gmt.hash, samples.toString())
    println "result found ${result} for ${cohort.name} and $gmt.name "
    if (result) return result

    File originalTpmFile = getOriginalTpmFile(cohort)
    File tpmFile = getTpmFileForSamples(originalTpmFile, samples)

    String mangledCohortName = cohort.name.replaceAll("[ |\\(|\\)]", "_")
    File outputFile = File.createTempFile("output-${mangledCohortName}${gmt.hash}", ".tsv")
    outputFile.write("")
    runBpaAnalysis(gmtFile, tpmFile, outputFile)

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
    String runScript = "Rscript ${BPA_ANALYSIS_SCRIPT} ${gmtFile.absolutePath} ${tpmFile.absolutePath} ${outputFile.absolutePath} BPA"
    println "running: $runScript"
    Process process = runScript.execute()
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

  static def retrieveTpmFile(File tpmFile, String tpmUrl) {
    def out = new BufferedOutputStream(new FileOutputStream(tpmFile))
    out << tpmUrl.toURL().openStream()
    out.close()
  }

  Cohort getOriginalTpmFile(Cohort cohort) {

//    Tpm tpm = Tpm.findByCohort(cohort)
    if (cohort.localTpmFile != null && new File(cohort.localTpmFile).exists()) {
      println "is good ${cohort.name}"
      return cohort
    }
    String mangledCohortName = cohort.name.replaceAll("[ |\\(|\\)]", "_")
    File tpmFile = new File(TPM_DIRECTORY + mangledCohortName + ".tpm.gz")
    println "tpm file ${tpmFile}"
    println "tpm file size ${tpmFile.size()}"
//    if (tpm == null) {
    println "tpm is null, so downloading"
    if (!tpmFile.exists() || tpmFile.size() == 0) {
      def out = new BufferedOutputStream(new FileOutputStream(tpmFile))
      out << tpmUrl.toURL().openStream()
      out.close()
    }
    cohort.localTpmFile = tpmFile.absolutePath
//      tpm = new Tpm(
//        cohort: cohort,
//        url: tpmUrl,
//        localFile: tpmFile.absolutePath
//      ).save(failOnError: true, flush: true)
//      cohort.tpm = tpm
    cohort.save(failOnError: true, flush: true)
//  else {
    assert new File(cohort.localTpmFile).exists()
    // nothign to do?
//    }
//    return tpmFile

  }


  @NotTransactional
  static JSONArray generateResult(String gmtData, Map meanMap) {
    JSONArray outputArray = new JSONArray()
    println "input calculating gene list"
    def geneList = gmtData.split("\n").findAll { it.split("\t").size() > 2 }.collect { it.split("\t") }
    println "output gene list ${geneList.size()}"
    println "mean map, geneset names ${meanMap.geneSetNames.join(",")}"
    println "mean map: ${meanMap}"
//    println "gene set names ${meanMap.geneSetNameMap}"


    for (List gene in geneList) {
//      def keyIndex = meanMap.geneSetNames.findIndexOf { it == gene[0] }
//      keyIndex = keyIndex >= 0 ? keyIndex : meanMap.geneSetNames.findIndexOf { it == "${gene[0]} (${gene[1]})" }
      def keyIndex = meanMap.geneSetNameMap[gene[0]]
      keyIndex = keyIndex >= 0 ? keyIndex : meanMap.geneSetNameMap["${gene[0]} (${gene[1]})"]
      if (keyIndex >= 0) {
        JSONObject jsonObject = new JSONObject()
        jsonObject.golabel = gene[0]
        jsonObject.goid = gene[1]

        jsonObject.gene = gene.subList(2, gene.size())
        jsonObject.firstGeneExpressionPathwayActivity = meanMap.zPathwayScores[0][keyIndex]
        jsonObject.secondGeneExpressionPathwayActivity = meanMap.zPathwayScores[1][keyIndex]
        jsonObject.firstGeneExpressionSampleActivity = meanMap.zSampleScores[0][keyIndex]
        jsonObject.secondGeneExpressionSampleActivity = meanMap.zSampleScores[1][keyIndex]


        outputArray.push(jsonObject)
      } else {
        println "key not found: [${gene[0]}] and [${gene[1]}]"
      }
    }
    outputArray.getJSONObject(outputArray.size() - 1).samples = meanMap.samples
    return outputArray
  }


  // input a regular data object and output of the shape: 2 cohorts, and each cohort has N genesets and each has S sample values
  // each value has to be parsed to double from string as well
  @NotTransactional
  static def extractValuesByCohort(JSONObject input, JSONObject statsObj, JSONArray samplesArray) {

    def values = new ArrayList()
    Map indicesMap = [:]
    Map relativeIndexMap = [:]
    List samples = samplesArray as List
    int indexCount = 0
    input.samples.eachWithIndex { def entry, int index ->
      int actualIndex = samples.indexOf(entry)
      if (actualIndex >= 0) {
        // this is the relative index
        relativeIndexMap.put(actualIndex, indexCount++)
        indicesMap.put(index, actualIndex)
      }
    }
    assert indicesMap.size() == samples.size()
    input.data.eachWithIndex { def entry, int i ->
      def genesetName = entry.geneset
      Double mean = statsObj[genesetName].mean
      Double std = statsObj[genesetName].stdev
      // filter for sample indices

      def filtered = []
      indicesMap.each { filtered.add(0) }
      indicesMap.each {
        filtered[it.value] = entry.data[it.key]
      }

      // do per gene-set z-value
      def reordered = new ArrayList(filtered.size())
      filtered.each { reordered.add(0) }
      def converted = filtered.collect {
        return (Double.parseDouble(it) - mean) / std
      }

      // sort
      converted.eachWithIndex { double nextEntry, int nextIndex ->
        int testIndex = relativeIndexMap.get(nextIndex)
        reordered.set(testIndex, nextEntry)
      }
      values[i] = converted
    }
    assert values[0].size() == samples.size()
    assert indicesMap.size() == samples.size()

    return values
  }

  // input a regular data object and output of the shape: 2 cohorts, and each cohort has N genesets and each has S sample values
  // each value has to be parsed to double from string as well
  @NotTransactional
  static List extractValues(JSONObject inputA, JSONObject inputB, JSONObject statsObj, JSONArray samplesArray) {
    return [extractValuesByCohort(inputA, statsObj, samplesArray[0]), extractValuesByCohort(inputB, statsObj, samplesArray[1])]
  }

  Map createMeanMapFromTpmGmt(Gmt gmt, TpmGmtResult resultA, TpmGmtResult resultB, JSONArray samplesArray) {
//    println "creating mean map"
    JSONObject dataA = new JSONObject(resultA.result)
    JSONObject dataB = new JSONObject(resultB.result)

    def geneSetNames = getGeneSetNames(dataA)
    JSONObject geneSetNameMap = new JSONObject()
    geneSetNames.eachWithIndex { Object entry, int i ->
      geneSetNameMap.put(entry, i)
    }

    JSONObject statsObject = JSON.parse(gmt.stats) as JSONObject

    JSONArray inputSamplesArray = new JSONArray()
    inputSamplesArray.add(samplesArray[0].size() == 0 ? dataA.getJSONArray("samples") : samplesArray[0])
    inputSamplesArray.add(samplesArray[1].size() == 0 ? dataB.getJSONArray("samples") : samplesArray[1])

    def zSampleScores = extractValues(dataA, dataB, statsObject, inputSamplesArray)

    // take the mean of each
    def zPathwayScores = getZPathwayScores(zSampleScores)

    JSONArray samples = new JSONArray()
    samples.add(dataA.getJSONArray("samples"))
    samples.add(dataB.getJSONArray("samples"))

    JSONObject jsonObject = new JSONObject()
    jsonObject.put("samples", samplesArray)
    jsonObject.put("zSampleScores", zSampleScores)
    jsonObject.put("zPathwayScores", zPathwayScores)
    jsonObject.put("geneSetNames", geneSetNames)
    jsonObject.put("geneSetNameMap", geneSetNameMap)

    return jsonObject
  }

  /**
   * @deprecated
   * @param resultA
   * @param resultB
   * @return
   */
  Map createMeanMap(Result resultA, Result resultB) {
    JSONObject dataA = new JSONObject(resultA.result)
    JSONObject dataB = new JSONObject(resultB.result)

    def samples = [dataA.samples, dataB.samples]

    def geneSetNames = getGeneSetNames(dataA)
    def values = extractValues(dataA, dataB)
    def dataStatisticsPerGeneSet = getDataStatisticsPerGeneSet(values)
    // calculates cohorts separately
    def zSampleScores = [getZSampleScores(values[0], dataStatisticsPerGeneSet), getZSampleScores(values[1], dataStatisticsPerGeneSet)]
    // uses mean separately
    def zPathwayScores = getZPathwayScores(zSampleScores)

    JSONObject jsonObject = new JSONObject()
    jsonObject.put("samples", samples)
    jsonObject.put("zSampleScores", zSampleScores)
    jsonObject.put("zPathwayScores", zPathwayScores)
    jsonObject.put("geneSetNames", geneSetNames)

    return jsonObject
  }
/**
 * Put mean sample scores in for each entry
 * @param sampleScores
 * @return
 */
  @NotTransactional
  static def getZPathwayScoresForCohort(List sampleScores) {
    def returnArray = []
    sampleScores.eachWithIndex { def entry, int index ->
      returnArray.add(entry.sum() / entry.size())
    }
    return returnArray
  }

// eslint-disable-next-line no-unused-vars
  @NotTransactional
  static def getZPathwayScores(sampleZScores) {
    return [getZPathwayScoresForCohort(sampleZScores[0]), getZPathwayScoresForCohort(sampleZScores[1])]
  }

  @NotTransactional
  static def getZSampleScores(values, dataStatisticsPerGeneSet) {
    def scoreValues = []
    values.eachWithIndex { def value, int index ->
      def statistics = dataStatisticsPerGeneSet[index] // [0] = mean, [1] = variance
      // TODO: collect for each
//      const array = values[index].data.map( v => (v - mean)/ variance )
      // TODO: do other collection
      def entryValue = []
      value.each {
        def convertedValue = (it - statistics.mean) / statistics.variance
//        println "input value ($it - $statistics.mean) / $statistics.variance = $convertedValue "
        entryValue.add(convertedValue)
      }
      scoreValues.add(entryValue)
    }
    return scoreValues
  }

  List getGeneSetNames(JSONObject inputData) {
    return inputData.data.collect { it.geneset }
  }

  @NotTransactional
  static List getDataStatisticsPerGeneSet(def inputData) {
    def outputData = []
    for (int i = 0; i < inputData[0].size(); i++) {
      def valuesForIndex = getValuesForIndex(inputData, i)
//      println "values for index $i"
//      println valuesForIndex
      def output = getDataStatisticsForGeneSet(valuesForIndex)
      def jsonObject = new JSONObject(mean: output[0], variance: output[1])
      outputData.add(jsonObject)
    }
    return outputData
  }

  @NotTransactional
  static def getDataStatisticsForGeneSet(List inputArray) {
    int count = inputArray.size()
    def total = 0
    for (double a in inputArray) {
      total += a
    }
    def mean = total / count
    double temp = 0
    for (double a in inputArray) {
      temp += (a - mean) * (a - mean)
    }
    def variance = temp / (inputArray.size() - 1)
    return [mean, variance]

  }

  @NotTransactional
  static String filterTpmForSamples(File originalTpmFile, JSONArray samplesArray) {
    if (samplesArray.size() == 0) return originalTpmFile

    List<String> sampleList = samplesArray as List<String>

    String tpmText = originalTpmFile.text

    List<String> tpmEntries = tpmText.split("\n") as List<String>

    String[] availableSamples = tpmEntries.get(0).split("\t")
    List<String> availableSamplesList = (availableSamples as List).subList(0, availableSamples.length)

    // find columns with matching samples
    List<String> intersectingLists = sampleList.intersect(availableSamplesList)
    List<Integer> matchingIndices = []
    intersectingLists.eachWithIndex { String entry, int index ->
      matchingIndices.add(availableSamplesList.indexOf(entry))
    }

    // for each line in tpmEntries, write out columns
    StringBuffer stringBuffer = new StringBuffer()
    for (String tpmEntry in tpmEntries) {
      String[] inputValues = tpmEntry.split("\t")
      List<String> outputArray = [inputValues[0]]
      matchingIndices.each {
        outputArray.add(inputValues[it])
      }
      stringBuffer.append(outputArray.join("\t")).append("\n")
    }

    return stringBuffer.toString()

  }


  /**
   * Extract TPM
   * @deprecated
   * @param cohort
   * @return
   */
  Collection<Double> getTpmData(Cohort cohort) {
    // TODO:
    return []
  }

  /**
   * TODO: calculate mean and variance
   * @deprecated
   */
  def calculateMeanAndVariance() {
    int size = 0
    List<Double> inputData = []

    // TODO: / populate inputData
    Cohort.all.eachParallel {
      inputData.addAll(getTpmData(it))
    }

    double total = inputData.sumParallel()
    double mean = total / inputData.size()

    double variance = 0
    inputData.eachParallel {
      variance += Math.pow(it - mean, 2.0)
    }
    variance = variance / inputData.size()
    return [mean, variance]
  }

}
