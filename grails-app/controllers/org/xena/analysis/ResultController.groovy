package org.xena.analysis

import grails.converters.JSON
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

@ReadOnly
class ResultController {

  ResultService resultService
  final String BPA_ANALYSIS_SCRIPT = "src/main/rlang/bpa-analysis.R"

  static responseFormats = ['json', 'xml']
//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE",analyze: "POST", checkAnalysisEnvironment: "GET"]

  def index(Integer max) {
    println "number is ${max}"
    params.max = Math.min(max ?: 10, 100)
    respond resultService.list(params), model: [resultCount: resultService.count()]
  }

  def show(Long id) {
    respond resultService.get(id)
  }

  def test() {
    println "just testing"
    render new JSONArray() as JSON
  }

  Tpm generateTpmFromCohort(Object o) {
    null
  }

  Gmt generateGmtFile(Object o1, Object o2) {
  }

  File generateEmptyAnalysisFile(Gmt gmt, Object o) {
    null
  }

  def checkAnalysisEnvironment() throws Exception {

    def proc = "Rscript ${BPA_ANALYSIS_SCRIPT}".execute()
    String inputText = proc.in.text
    String errorText = proc.err.text
    log.debug("input text: ${inputText}")
    log.debug("error text: ${errorText}")

    assert inputText.contains("NULL")
    assert errorText.contains("Loading required package: Biobase")

    respond([status: 200])
    return 0

  }


  @Transactional
  def analyze() {
    def json = request.JSON
//    println "input json ${json as JSON}"
    String cohortName = json.cohort
    String method = json.method
    String gmtname = json.gmtname
    String gmtdata = json.gmtdata
    String tpmUrl = json.tpmurl
    println "analyzing '${cohortName}' with method '${method}' and gmt name '${gmtname}'"


    // handle and write gmt file
    String gmtDataHash = gmtdata.md5()
    Gmt gmt = Gmt.findByName(gmtname)
    if (gmt == null) {
      gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: gmtdata)
      gmt.save(failOnError: true, flush: true)
    }

    Cohort cohort = Cohort.findByName(cohortName)
    if (cohort == null) {
      cohort = new Cohort(name: cohortName).save(failOnError: true, flush: true)
    }
    String mangledCohortName = cohortName.replaceAll("[ |\\(|\\)]", "_")

    println "max memory: ${Runtime.getRuntime().maxMemory()}"
    println "total memory: ${Runtime.getRuntime().totalMemory()}"
    println "free memory: ${Runtime.getRuntime().freeMemory()}"

    // handl and write tpm file
    Tpm tpm = Tpm.findByCohort(cohort)
    File tpmFile = new File(mangledCohortName+ ".tpm.gz")
    println "tpm file ${tpmFile}"
    println "tpm file size ${tpmFile.size()}"
    if (tpm == null) {
      def out = new BufferedOutputStream(new FileOutputStream(tpmFile))
      out << tpmUrl.toURL().openStream()
      out.close()
      tpm = new Tpm(
        cohort: cohort,
        url: tpmUrl,
        data: tpmFile.absolutePath
      ).save(failOnError: true, flush: true)
      cohort.tpm = tpm
      cohort.save()
    }
    else{
      assert new File(tpm.data).exists()
      // nothign to do?
    }
//    else {
//      tpmFile.write(tpm.data)
//      tpmFile.deleteOnExit()
//    }


    // create output file
    Result outputResult = Result.findByMethodAndGmtAndCohort(method, gmt, cohort)
    if (outputResult != null) {
      render outputResult as JSON
      return
    }

    File gmtFile = File.createTempFile(gmtname, ".gmt")
    gmtFile.write(gmtdata)
//    gmtFile.deleteOnExit()

    println "input cohort name ${cohortName}"
    println "output cohort name ${mangledCohortName}"

    File outputFile = File.createTempFile("output-${mangledCohortName}${gmtDataHash}", ".tsv")
    outputFile.write("")
//    outputFile.deleteOnExit()

    this.checkAnalysisEnvironment()
    println("analysis environment exists ${method}")
    long lastOutputFileSize = 0
    int waitCount = 0
    if (method == 'BPA') {
      runBpaAnalysis(gmtFile, tpmFile, outputFile)
      println "gmt file ${gmtFile.absolutePath}"
      println "tpm file ${tpmFile.absolutePath}"
      println "output file ${outputFile.absolutePath}"

      StringBuilder stringBuffer = new StringBuilder()
      while ((outputFile.size() == 0 || outputFile.size() > lastOutputFileSize) && waitCount < 10) {
        println "waiting ${outputFile.size()}"
        sleep(2000)
        ++waitCount
      }
      BufferedReader bufferedReader = new BufferedReader(new FileReader(outputFile))
      String line
      while ((line = bufferedReader.readLine()) != null) {
        stringBuffer.append(line)
      }
      String outputData = stringBuffer.toString()
      println "output data ${outputData}"
      String jsonData = convertTsv(outputData)
      println "jsonData ${jsonData}"
      Result result = new Result(
        method: method,
        gmt: gmt,
        cohort: cohort,
        result: jsonData
      ).save(flush: true, failOnError: true)
      render result as JSON
    } else {
      throw new RuntimeException("Not sure how to handle method ${method}")
    }
  }

  String convertTsv(String tsvInput) {
    println "tsvInput"
    println tsvInput

    List<String> lines = tsvInput.split("\n")
    List<String> rawData = lines.subList(1, lines.size())
    List<String> data = rawData.findAll({ d ->
      d.trim().length() > 0
    })
    JSONArray jsonArray = new JSONArray()
    data.each { d ->
      List<String> entries = d.split('\t')
      def obj = new JSONObject()
      obj.geneset = entries[0]
      obj.data = entries.subList(1, entries.size()) as List<Float>
      jsonArray.add(obj)
    }

    println "input sample string ${lines[0]}"
    List<String> sampleList = lines[0].split('\t')
    println "input sample list ${sampleList}"
    JSONArray samplesJsonArray = new JSONArray()
    sampleList.each {
      samplesJsonArray.add(it)
    }

    return new JSONObject(
      [
        samples: samplesJsonArray
        , data : jsonArray
      ]
    )
  }

  private def runBpaAnalysis(File gmtFile, File tpmFile, File outputFile) {
    Process process = "Rscript ${BPA_ANALYSIS_SCRIPT} ${gmtFile.absolutePath} ${tpmFile.absolutePath} ${outputFile.absolutePath} BPA".execute()
    int exitValue = process.waitFor()
    println "exit value ${exitValue}"

//    println "stdout ${process.in.text}"
//    println "stderr ${process.err.text}"
    return exitValue
  }

  @Transactional
  def save(Result result) {
    if (result == null) {
      render status: NOT_FOUND
      return
    }
    if (result.hasErrors()) {
      transactionStatus.setRollbackOnly()
      respond result.errors
      return
    }

    try {
      resultService.save(result)
    } catch (ValidationException e) {
      respond result.errors
      return
    }

    respond result, [status: CREATED, view: "show"]
  }

  @Transactional
  def update(Result result) {
    if (result == null) {
      render status: NOT_FOUND
      return
    }
    if (result.hasErrors()) {
      transactionStatus.setRollbackOnly()
      respond result.errors
      return
    }

    try {
      resultService.save(result)
    } catch (ValidationException e) {
      respond result.errors
      return
    }

    respond result, [status: OK, view: "show"]
  }

  @Transactional
  def delete(Long id) {
    if (id == null || resultService.delete(id) == null) {
      render status: NOT_FOUND
      return
    }

    render status: NO_CONTENT
  }

}
