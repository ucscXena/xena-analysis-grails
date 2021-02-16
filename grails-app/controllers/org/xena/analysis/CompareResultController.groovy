package org.xena.analysis

import grails.converters.JSON
import grails.validation.ValidationException
import org.grails.web.json.JSONArray
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
    static allowedMethods = [storeResult: "POST",save: "POST", update: "PUT", delete: "DELETE",generateScoredResult:"POST"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond compareResultService.list(params), model:[compareResultCount: compareResultService.count()]
    }

    def show(Long id) {
        respond compareResultService.get(id)
    }

  JSONObject resultMarshaller(CompareResult result) {
    JSONObject jsonObject = new JSONObject()
    jsonObject.genesets = dataObject.localTpmFile as List<Float>
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

//    println "storing"
//    println json.result as JSON

    CompareResult compareResult = CompareResult.findByMethodAndCohortAAndCohortBAndGmt(method,cohortA,cohortB,gmt)
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

  Cohort findCohort(String name){
    println "FINDING cohort: ${name}"
    Cohort cohort = Cohort.findByName(name)
    // TODO: get cohorts, etc.
    if(!cohort){
      cohort = new Cohort(name: name).save()
    }
    Tpm tpm = Tpm.findByCohort(cohort)
    if(!tpm){
      File tpmFile = analysisService.getOriginalTpmFile(cohort)
//      tpm = new Tpm(cohort: cohort,url: tpmUrl, localFile: tpmFile.absolutePath).save()
    }
    assert tpm.localFile.length()>0
    return cohort
  }


    long startTimer = System.currentTimeMillis()
    long endTimer = System.currentTimeMillis()
    void measureTime(String msg){
        endTimer = System.currentTimeMillis()
        println "---------------------"
        println "$msg : ${ (endTimer - startTimer ) / 1000.0}s "
        println "---------------------"
        startTimer = System.currentTimeMillis()
    }


    @Transactional
  def retrieveScoredResult(){

        measureTime("init retrieve")

      def json = request.JSON

    String method = json.method
    String geneSetName = json.geneSetName
    String cohortNameA = json.cohortNameA
    String cohortNameB = json.cohortNameB
    String samples = json.samples
    JSONArray samplesArray = new JSONArray(samples)

    log.debug "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}, ${samples}"
    log.debug "samples array as JSON"
    log.debug samplesArray as JSON
//    println "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}"
    Gmt gmt = Gmt.findByName(geneSetName)
    log.info "gmt name ${gmt}"
    Cohort cohortA = Cohort.findByName(cohortNameA)
    Cohort cohortB = Cohort.findByName(cohortNameB)
    log.info "cohorts ${Cohort.count} -> ${cohortA}, ${cohortB}"


    if(gmt==null) throw new RuntimeException("Unable to find gmt for ${geneSetName}")
    if(cohortA==null)throw new RuntimeException("Unable to find cohort for ${cohortNameA}")
    if(cohortB==null)throw new RuntimeException("Unable to find cohort for ${cohortNameB}")


        measureTime("post A")

    TpmGmtResult resultA = TpmGmtResult.findByMethodAndCohortAndGmt(method,cohortA,gmt)
    TpmGmtResult resultB = TpmGmtResult.findByMethodAndCohortAndGmt(method,cohortB,gmt)

        measureTime("post B")

//    log.info "resultA: $resultA"
//    log.info "resultB: $resultB"

    if(resultA==null)throw new RuntimeException("No results available for $method ${cohortNameA} $gmt.name")
    if(resultB==null)throw new RuntimeException("No results available for $method ${cohortNameB} $gmt.name")

    JSONObject returnObject = new JSONObject()
    // I don't think this is used
    JSONObject gmtObject= new JSONObject()
    gmtObject.name = gmt.name
    gmtObject.hash = gmt.hash
    gmtObject.stats = gmt.stats

    returnObject.put("gmt",gmtObject)

    log.debug "creating mean map "
        measureTime("pre mean-map")

        Map meanMap = analysisService.createMeanMapFromTpmGmt(gmt,resultA,resultB,samplesArray)
    log.debug "created mean map"
        measureTime("post mean-map")

    String gmtData = gmt.data
    println "generating result"
    JSONArray inputArray = analysisService.generateResult(gmtData,meanMap)
    println "generated result"
        measureTime("post input array")

    returnObject.data = inputArray

    println "dumping out"
    response.outputStream << returnObject.toString()
    response.outputStream.flush()
        measureTime("post output stream")

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
