package org.xena.analysis

import grails.converters.JSON
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import groovy.json.JsonSlurper
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

@ReadOnly
class ResultController {

  ResultService resultService
  AnalysisService analysisService


  static responseFormats = ['json', 'xml']
//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE",analyze: "POST", checkAnalysisEnvironment: "GET"]

  def index(Integer max) {
    println "number is ${max}"
    params.max = Math.min(max ?: 10, 100)
    respond resultService.list(params), model: [resultCount: resultService.count()]
  }

  def show(Long id) {
    respond resultMarshaller(resultService.get(id))
  }

  def findResult(String method, String geneSetName,String cohort) {
    println "finding result with ${method},${geneSetName}, ${cohort}"
    Gmt gmt = Gmt.findByName(geneSetName)
    println "gmt name ${gmt}"
    Cohort cohortResult = Cohort.findByName(cohort)
    println "cohort name ${cohort}"
    Result result = Result.findByGmtAndCohortAndMethod(gmt,cohortResult,method)
    println "result ${result}"
    Result.all.each {
      println it.gmt.name +" " + it.gmt.id + " " + it.cohort.name + " " + it.method
    }
    respond resultMarshaller(result)
  }

  def test() {
    println "just testing"
    render new JSONArray() as JSON
  }

  JSONObject resultMarshaller(Result result) {
    JSONObject jsonObject = new JSONObject()
    jsonObject.cohort = result.cohort.name
    jsonObject.gmt = result.gmt.name
    jsonObject.gmtId = result.gmt.id
    def dataObject = new JsonSlurper().parseText(result.result) as JSONObject
    jsonObject.genesets = dataObject.data as List<Float>
    jsonObject.samples = dataObject.samples
    return jsonObject
  }

  @Transactional
  def analyze() {
    def json = request.JSON
    String cohortName = json.cohort
    String method = json.method
    String gmtname = json.gmtname
    String tpmUrl = json.tpmurl
    println "analyzing '${cohortName}' with method '${method}' and gmt name '${gmtname}'"

    Gmt gmt = Gmt.findByName(gmtname)
    if (gmt == null) {
      throw new RuntimeException("Gmt file not found for ${gmtname}")
    }

    Cohort cohort = Cohort.findByName(cohortName)
    if (cohort == null) {
      cohort = new Cohort(name: cohortName).save(failOnError: true, flush: true)
    }
    String mangledCohortName = cohortName.replaceAll("[ |\\(|\\)]", "_")

    println "max memory: ${Runtime.getRuntime().maxMemory()}"
    println "total memory: ${Runtime.getRuntime().totalMemory()}"
    println "free memory: ${Runtime.getRuntime().freeMemory()}"

    File tpmFile = analysisService.getTpmFile(cohort,tpmUrl)


    // create output file
//    def outputResults = Result.withCriteria {
//      eq("method",method)
//      eq("cohort",cohort)
//      eq("gmtHash",gmt.hash)
//    }
//    println "output result ${outputResult} . . ${outputResult.size()}"
//    Result outputResult = outputResults.size()>0 ?outputResults[0] : null
    Result outputResult = Result.findByMethodAndCohortAndGmtHash(method,cohort,gmt.hash)
//    if(resultsResults.si)

    if (outputResult != null) {
      render resultMarshaller(outputResult) as JSON
      return
    }

    File gmtFile = File.createTempFile(gmtname, ".gmt")
    gmtFile.write(gmt.data)
    gmtFile.deleteOnExit()

    println "input cohort name ${cohortName}"
    println "output cohort name ${mangledCohortName}"

    File outputFile = File.createTempFile("output-${mangledCohortName}${gmt.hash}", ".tsv")
    outputFile.write("")
    println "output file"
    println outputFile.absolutePath
//    outputFile.deleteOnExit()

    analysisService.checkAnalysisEnvironment()
    println("analysis environment exists ${method}")
    long lastOutputFileSize = 0
    int waitCount = 0
    if (method=='BPA Gene Expression') {
      runBpaAnalysis(gmtFile, tpmFile, outputFile)
      println "gmt file ${gmtFile.absolutePath}"
      println "tpm file ${tpmFile.absolutePath}"
      println "output file ${outputFile.absolutePath}"

      println "A with GC"
      OutputHandler.printMemory()
      System.gc()
      while ((outputFile.size() == 0 || outputFile.size() == lastOutputFileSize) && waitCount < 10) {
        println "waiting ${outputFile.size()}"
        sleep(2000)
        ++waitCount
      }
      println "B"
      OutputHandler.printMemory()
      File jsonFile = OutputHandler.convertTsvFromFile(outputFile)
//      jsonFile.deleteOnExit()
      println "C"
      OutputHandler.printMemory()
      Result result = new Result(
        method: method,
        gmt: gmt,
        gmtHash: gmt.hash,
        cohort: cohort,
        result: jsonFile.text
      ).save(flush: true, failOnError: true)
      println "D"
      OutputHandler.printMemory()
      render resultMarshaller(result) as JSON
      println "E"
      OutputHandler.printMemory()
    } else {
      throw new RuntimeException("Not sure how to handle method ${method}")
    }
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
