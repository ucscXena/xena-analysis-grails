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
  final String TPM_DIRECTORY = "data/tpm/"

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
    return 0

  }

  JSONObject resultMarshaller(Result result){
    JSONObject jsonObject= new JSONObject()
    jsonObject.cohort = result.cohort.name
    jsonObject.gmt = result.gmt.name
    jsonObject.data = result.result
    return jsonObject
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
    File tpmFile = new File(TPM_DIRECTORY+mangledCohortName+ ".tpm.gz")
    println "tpm file ${tpmFile}"
    println "tpm file size ${tpmFile.size()}"
    if (tpm == null) {
      println "tpm is null, so downloading"
      if(!tpmFile.exists() || tpmFile.size()==0){
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
      render resultMarshaller(outputResult) as JSON
      return
    }

    File gmtFile = File.createTempFile(gmtname, ".gmt")
    gmtFile.write(gmtdata)
//    gmtFile.deleteOnExit()

    println "input cohort name ${cohortName}"
    println "output cohort name ${mangledCohortName}"

    File outputFile = File.createTempFile("output-${mangledCohortName}${gmtDataHash}", ".tsv")
    outputFile.write("")
    println "output file"
    println outputFile.absolutePath
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
      while ((outputFile.size() == 0 || outputFile.size() == lastOutputFileSize) && waitCount < 10) {
        println "waiting ${outputFile.size()}"
        sleep(2000)
        ++waitCount
      }
      BufferedReader bufferedReader = new BufferedReader(new FileReader(outputFile))
      String line
      while ((line = bufferedReader.readLine()) != null) {
        stringBuffer.append(line).append("\n")
      }
      String outputData = stringBuffer.toString()
//      String outputData = outputFile.text
      println "output data ${outputData}"
      JSONObject jsonData = convertTsv(outputData)
//      println "jsonData"
//      println jsonData as JSON
      Result result = new Result(
        method: method,
        gmt: gmt,
        cohort: cohort,
        result: jsonData.toString()
      ).save(flush: true, failOnError: true)
      render resultMarshaller(result) as JSON
    } else {
      throw new RuntimeException("Not sure how to handle method ${method}")
    }
  }

  static JSONObject convertTsv(String tsvInput) {

    List<String> lines = tsvInput.split("\\n")
    println "# of lines ${lines.size()}"
    List<String> rawData = lines.subList(1, lines.size())
    println "# of raw data ${rawData.size()}"
    List<String> data = rawData.findAll({ d ->
      d.trim().length() > 0
    })
    println "trimmed data ${data.size()}"
    JSONArray jsonArray = new JSONArray()
    data.eachWithIndex { d,i ->
      List<String> entries = d.split("\\t")
      def obj = new JSONObject()
      obj.geneset = entries[0]
      obj.data = entries.subList(1, entries.size()) as List<Float>
      if(i < 4){
        println "d: ${d}"
        println "entries: ${entries.size()}"
        println "geneset: ${entries[0]}"
        println "data: ${obj.data}"
      }
      jsonArray.add(obj)
    }

    List<String> sampleList = lines[0].split('\t')
    JSONArray samplesJsonArray = new JSONArray()
    sampleList.subList(1,sampleList.size()).each {
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
