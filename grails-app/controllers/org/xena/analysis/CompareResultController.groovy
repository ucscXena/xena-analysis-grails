package org.xena.analysis

import grails.converters.JSON
import grails.validation.ValidationException
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK

import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional

@ReadOnly
class CompareResultController {

    CompareResultService compareResultService
    AnalysisService analysisService

    static responseFormats = ['json', 'xml']
    static allowedMethods = [storeResult: "POST",save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond compareResultService.list(params), model:[compareResultCount: compareResultService.count()]
    }

    def show(Long id) {
        respond compareResultService.get(id)
    }

  JSONObject resultMarshaller(CompareResult result) {
    JSONObject jsonObject = new JSONObject()
//    jsonObject.cohortA = result.cohortA.name
//    jsonObject.cohortB = result.cohortA.name
//    jsonObject.gmt = result.gmt.name
//    jsonObject.gmtId = result.gmt.id
//    def dataObject = new JsonSlurper().parseText(result.result) as JSONObject
    jsonObject.genesets = dataObject.data as List<Float>
//    jsonObject.samples = dataObject.samples
    return jsonObject
  }

  @Transactional
  def storeResult() {
    def json = request.JSON
    String cohortNameA = json.cohortA
    String cohortNameB = json.cohortB
    String method = json.method
    String gmtname = json.geneset
    String samples = json.samples
//    String result = json.result

    println "analyzing '${cohortNameA} / ${cohortNameB} ' with method '${method}' and gmt name '${gmtname}'"
    Cohort cohortA = Cohort.findByName(cohortNameA)
    Cohort cohortB = Cohort.findByName(cohortNameB)
    Gmt gmt = Gmt.findByName(gmtname)
    println "cohortA ${cohortA}, cohortB, ${cohortB}, GMT: ${gmt}"

    println "storing"
//    println json.result as JSON

    CompareResult compareResult = CompareResult.findByMethodAndCohortAAndCohortBAndGmt(method,cohortA,cohortB,gmt)
//    println "save string 1:"
//    println "[${json.result}]"
//    println "save string 2:"
//    println "${json.result as JSON}"
    if (compareResult == null) {
      println "trying to save"
      compareResult = new CompareResult(
        method: method,
        cohortA: cohortA,
        cohortB: cohortB,
        samples: samples,
        gmt: gmt,
        result: "${json.result as JSON}"
      ).save(failOnError: true, flush: true )
      println "saved ${compareResult}"
    }
    else{
      println "updating ${compareResult}"
      compareResult.result = "${json.result as JSON}"
      compareResult.save(failOnError: true, flush: true )
      println "updated ${compareResult}"
    }
    println "result in database is: "
//    println compareResult.result
    OutputHandler.printMemory()
    json = null
    System.gc()
    OutputHandler.printMemory()
//    respond compareResult, [status: CREATED, view:"show"]
    respond new JSONObject(), [status: CREATED, view:"show"]
  }

  def findResult(String method, String geneSetName,String cohortNameA,String cohortNameB,String samples) {
    println "finding result with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}, ${samples}"
    Gmt gmt = Gmt.findByName(geneSetName)
    println "gmt name ${gmt}"
    Cohort cohortA = Cohort.findByName(cohortNameA)
    Cohort cohortB = Cohort.findByName(cohortNameB)
    println "cohort name ${cohortA} / ${cohortB}"
    CompareResult result = CompareResult.findByMethodAndGmtAndCohortAAndCohortB(method,gmt,cohortA,cohortB)
    if(result){
      println "retrievering result with string"
//      println result.result
//      respond result

      response.outputStream << result.result
      response.outputStream.flush()
    }
    else{
      respond new JSONObject()
    }
  }

  Cohort findCohort(String name,String tpmUrl){
    println "FINDING cohort: ${name}, tpmUrl: ${tpmUrl}"
    Cohort cohort = Cohort.findByName(name)
    // TODO: get cohorts, etc.
    if(!cohort){
      cohort = new Cohort(name: name).save()
    }
    Tpm tpm = Tpm.findByCohort(cohort)
    if(!tpm){
      File tpmFile = analysisService.getTpmFile(cohort,tpmUrl)
      tpm = new Tpm(cohort: cohort,url: tpmUrl,data: tpmFile.absolutePath).save()
    }
    assert tpm.data.length()>0
    return cohort
  }

  @Transactional
  def generateScoredResult(String method, String geneSetName,String cohortNameA,String cohortNameB,String tpmUrlA,String tpmUrlB,String samples) {
    println "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}, ${tpmUrlA},${tpmUrlB}, ${samples}"
    Gmt gmt = Gmt.findByName(geneSetName)
    println "gmt name ${gmt}"
    Cohort cohortA = findCohort(cohortNameA,tpmUrlA)
    Cohort cohortB = findCohort(cohortNameB,tpmUrlB)
    println "cohorts ${Cohort.count} -> ${cohortA}, ${cohortB}"



    if(gmt==null) throw new RuntimeException("Unable to find gmt for ${geneSetName}")
    if(cohortA==null)throw new RuntimeException("Unable to find cohort for ${cohortNameA}")
    if(cohortB==null)throw new RuntimeException("Unable to find cohort for ${cohortNameB}")




    println "cohort name ${cohortA} / ${cohortB}"
    CompareResult compareResult = CompareResult.findByMethodAndGmtAndCohortAAndCohortB(method,gmt,cohortA,cohortB)
    if(!compareResult){
      analysisService.checkAnalysisEnvironment()
      // pull in TPM files



      File gmtFile = File.createTempFile(gmt.name, ".gmt")
      gmtFile.write(gmt.data)
      gmtFile.deleteOnExit()

      // TODO: run these in parallel if needed, or just 1?
      Result resultA = analysisService.doBpaAnalysis(cohortA,gmtFile,gmt,method,tpmUrlA)
      Result resultB = analysisService.doBpaAnalysis(cohortB,gmtFile,gmt,method,tpmUrlB)

      compareResult = analysisService.calculateCustomGeneSetActivity(gmt,resultA,resultB,method,samples)

    }
    response.outputStream << compareResult.result
    response.outputStream.flush()

  }



  @Transactional
  def save(CompareResult compareResult) {
    if (compareResult == null) {
            render status: NOT_FOUND
            return
        }
        if (compareResult.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond compareResult.errors
            return
        }

        try {
            compareResultService.save(compareResult)
        } catch (ValidationException e) {
            respond compareResult.errors
            return
        }

        respond compareResult, [status: CREATED, view:"show"]
    }

    @Transactional
    def update(CompareResult compareResult) {
        if (compareResult == null) {
            render status: NOT_FOUND
            return
        }
        if (compareResult.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond compareResult.errors
            return
        }

        try {
            compareResultService.save(compareResult)
        } catch (ValidationException e) {
            respond compareResult.errors
            return
        }

        respond compareResult, [status: OK, view:"show"]
    }

    @Transactional
    def delete(Long id) {
        if (id == null || compareResultService.delete(id) == null) {
            render status: NOT_FOUND
            return
        }

        render status: NO_CONTENT
    }
}
