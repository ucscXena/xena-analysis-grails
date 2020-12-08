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
    println "doing analyze"
    def json = request.JSON
//    println "input json ${json as JSON}"
    String cohortName = json.cohort
    String method = json.method
    String gmtname = json.gmtname
    String gmtdata = json.gmtdata
    String tpmUrl = json.tpmurl


    // handle and write gmt file
    String gmtDataHash = gmtdata.md5()
    Gmt gmt = Gmt.findByName(gmtname)
    if (gmt == null) {
      gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: gmtdata)
      gmt.save(failOnError: true, flush: true)
    }

    Cohort cohort = Cohort.findByName(cohortName)
    if(cohort ==null ){
      cohort = new Cohort(name: cohortName).save(failOnError: true, flush: true)
    }

    // handl and write tpm file
    Tpm tpm = Tpm.findByCohort(cohort)
    File tpmFile = File.createTempFile(cohortName, "tpm")
    if (tpm == null) {
      def out = new BufferedOutputStream(new FileOutputStream(tpmFile))
      out << tpmUrl.toURL().openStream()
      out.close()
      String tpmData = tpmFile.text
      tpm = new Tpm(
        cohort: cohort,
        url: tpmUrl,
        data: tpmData
      ).save(failOnError: true, flush: true)
      cohort.tpm = tpm
      cohort.save()
    }
    else{
      tpmFile.write(tpm.data)
      tpmFile.deleteOnExit()
    }


    // create output file
    Result outputResult = Result.findByMethodAndGmtAndCohort(method, gmt, cohort)
    if (outputResult != null) {
      render outputResult as JSON
      return
    }

    File gmtFile = File.createTempFile(gmtname, "gmt")
    gmtFile.write(gmtdata)
    gmtFile.deleteOnExit()

    println "input cohort name ${cohortName}"
    String mangledCohortName = cohortName.replaceAll("[ |\\(|\\)]","_")
    println "output cohort name ${mangledCohortName}"

    File outputFile = File.createTempFile("output-${mangledCohortName}${gmtDataHash}", "tsv")
    outputFile.write("")


    this.checkAnalysisEnvironment()
    println("analysis environmeent fine ${method}")
    if (method == 'BPA') {
      runBpaAnalysis(gmtFile, tpmFile, outputFile)
      String outputData = outputFile.text
      String jsonData = convertTsv(outputData)
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
    return new JSONObject(
      [
        samples: (lines[0] as List<String>).subList(1, 1).split('\t')
        , data : jsonArray
      ]
    )
  }

  void runBpaAnalysis(File gmtFile, File tpmFile, File outputFile) {
    Process process = "Rscript ${BPA_ANALYSIS_SCRIPT} ${gmtFile.absolutePath} ${tpmFile.absolutePath} ${outputFile.absolutePath} BPA".execute()
    log.debug "stdout ${process.in.text}"
    log.debug "stderr ${process.err.text}"
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
