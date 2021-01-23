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

  @Transactional
  def oldGenerateScoredResult(){

    def json = request.JSON

    String method = json.method
    String geneSetName = json.geneSetName
    String cohortNameA = json.cohortNameA
    String cohortNameB = json.cohortNameB
//    String tpmUrlA = json.tpmUrlA
//    String tpmUrlB = json.tpmUrlB
    String samples = json.samples

//    println "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}, ${samples}"
    Gmt gmt = Gmt.findByName(geneSetName)
    println "gmt name ${gmt}"
    Cohort cohortA = findCohort(cohortNameA)
    Cohort cohortB = findCohort(cohortNameB)
    println "cohorts ${Cohort.count} -> ${cohortA}, ${cohortB}"



    if(gmt==null) throw new RuntimeException("Unable to find gmt for ${geneSetName}")
    if(cohortA==null)throw new RuntimeException("Unable to find cohort for ${cohortNameA}")
    if(cohortB==null)throw new RuntimeException("Unable to find cohort for ${cohortNameB}")




//    println "method: ${method}"
//    println "gmt: ${gmt.name} / ${gmt.hash}"
//    println "cohort name ${cohortA.name} / ${cohortB.name}"
    CompareResult compareResult = CompareResult.findByMethodAndGmtAndCohortAAndCohortBAndSamples(method,gmt,cohortA,cohortB,samples)
    println "found compare result ${compareResult}"

//    CompareResult.all.each {
//      println "it.method: ${it.method}"
//      println "it.gmt: ${it.gmt.name} / ${it.gmt.hash}"
//      println "it.cohort name ${it.cohortA.name} / ${it.cohortB.name}"
//      println "method == ${it.method == method}"
//      println "gmt == ${it.gmt == gmt}"
//      println "cohort A == ${it.cohortA == cohortA}"
//      println "cohort B == ${it.cohortB == cohortB}"
//    }

    if(!compareResult){
      println "prior result not found running analysis -> check analysis environment"
      analysisService.checkAnalysisEnvironment()
      println "analysis found"
      // pull in TPM files



      File gmtFile = File.createTempFile(gmt.name, ".gmt")
      gmtFile.write(gmt.data)
      gmtFile.deleteOnExit()

      println "gmt file ${gmtFile} . . exists ${gmtFile.exists()}, size: ${gmtFile.size()}"

      // TODO: run these in parallel if needed, or just 1?
      JSONArray samplesA = null
      JSONArray samplesB = null
      try {
        if(samples){
          JSONArray samplesJsonArray = new JSONArray(samples)
//          println "samples json array "
//          println samplesJsonArray as JSON
          samplesA = samplesJsonArray.getJSONArray(0)
//          println samplesA as JSON
          samplesB = samplesJsonArray.getJSONArray(1)
//          println samplesB as JSON
          //      println "result A: ${resultA}"
        }
      } catch (e) {
        log.error(e)
      }

      Result resultA = analysisService.doBpaAnalysis(cohortA,gmtFile,gmt,method,samplesA)
      println "result A: ${resultA}"
      Result resultB = analysisService.doBpaAnalysis(cohortB,gmtFile,gmt,method,samplesB)
      println "result B: ${resultB}"

      compareResult = analysisService.calculateCustomGeneSetActivity(gmt,resultA,resultB,method,samples)
      println "compare result: ${compareResult}"

    }
    response.outputStream << compareResult.result
    response.outputStream.flush()

  }


  @Transactional
  def retrieveScoredResult(){

    def json = request.JSON

    String method = json.method
    String geneSetName = json.geneSetName
    String cohortNameA = json.cohortNameA
    String cohortNameB = json.cohortNameB
    String samples = json.samples
    JSONArray samplesArray = new JSONArray(samples)

    log.info "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}, ${samples}"
    log.info "samples array as JSON"
    println samplesArray as JSON
//    println "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}"
    Gmt gmt = Gmt.findByName(geneSetName)
    log.info "gmt name ${gmt}"
    Cohort cohortA = Cohort.findByName(cohortNameA)
    Cohort cohortB = Cohort.findByName(cohortNameB)
    log.info "cohorts ${Cohort.count} -> ${cohortA}, ${cohortB}"


    if(gmt==null) throw new RuntimeException("Unable to find gmt for ${geneSetName}")
    if(cohortA==null)throw new RuntimeException("Unable to find cohort for ${cohortNameA}")
    if(cohortB==null)throw new RuntimeException("Unable to find cohort for ${cohortNameB}")



    TpmGmtResult resultA = TpmGmtResult.findByMethodAndCohortAndGmt(method,cohortA,gmt)
    TpmGmtResult resultB = TpmGmtResult.findByMethodAndCohortAndGmt(method,cohortB,gmt)

    println "resultA: $resultA"
    println "resultB: $resultB"

//    println "mean $gmt.mean"
//    println "variance $gmt.variance"

    if(resultA==null)throw new RuntimeException("No results available for $method ${cohortNameA} $gmt.name")
    if(resultB==null)throw new RuntimeException("No results available for $method ${cohortNameB} $gmt.name")

//    if(gmt.mean == null || gmt.variance == null){
//      throw new RuntimeException("Analysis not yet calculated for $method  $gmt.name")
//    }

    JSONObject returnObject = new JSONObject()
    JSONObject gmtObject= new JSONObject()
    gmtObject.name = gmt.name
    gmtObject.hash = gmt.hash
    gmtObject.stats = gmt.stats
//    gmtObject.variance = gmt.variance

    returnObject.put("gmt",gmtObject)
//    returnObject.put("gmtName",gmt.name)
//    returnObject.put("gmtName",gmt.name)

    Map meanMap = analysisService.createMeanMapFromTpmGmt(gmt,resultA,resultB,samplesArray)

    String gmtData = gmt.data
    // TODO: implement
    JSONArray inputArray = analysisService.generateResult(gmtData,meanMap)

//    println "input array as JSON"
//    println inputArray as JSON
//    JSONArray returnArray = new JSONObject()
//    returnArray.add(JSON.parse(resultA.result))
//    returnArray.add(JSON.parse(resultB.result))
//
//
    returnObject.data = inputArray

    // TODO: store it in new cached object

    response.outputStream << returnObject.toString()
    response.outputStream.flush()

  }

  @Transactional
  def generateScoredResult(){

    def json = request.JSON

    String method = json.method
    String geneSetName = json.geneSetName
    String cohortNameA = json.cohortNameA
    String cohortNameB = json.cohortNameB
//    String tpmUrlA = json.tpmUrlA
//    String tpmUrlB = json.tpmUrlB
    String samples = json.samples

//    println "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}, ${samples}"
    println "generate scored results with ${method},${geneSetName}, ${cohortNameA}, ${cohortNameB}"
    Gmt gmt = Gmt.findByName(geneSetName)
    println "gmt name ${gmt}"
    Cohort cohortA = Cohort.findByName(cohortNameA)
    Cohort cohortB = Cohort.findByName(cohortNameB)
    println "cohorts ${Cohort.count} -> ${cohortA}, ${cohortB}"


    if(gmt==null) throw new RuntimeException("Unable to find gmt for ${geneSetName}")
    if(cohortA==null)throw new RuntimeException("Unable to find cohort for ${cohortNameA}")
    if(cohortB==null)throw new RuntimeException("Unable to find cohort for ${cohortNameB}")




//    println "method: ${method}"
//    println "gmt: ${gmt.name} / ${gmt.hash}"
//    println "cohort name ${cohortA.name} / ${cohortB.name}"
    CompareResult compareResult = CompareResult.findByMethodAndGmtAndCohortAAndCohortBAndSamples(method,gmt,cohortA,cohortB,samples)
    println "found compare result ${compareResult}"

//    CompareResult.all.each {
//      println "it.method: ${it.method}"
//      println "it.gmt: ${it.gmt.name} / ${it.gmt.hash}"
//      println "it.cohort name ${it.cohortA.name} / ${it.cohortB.name}"
//      println "method == ${it.method == method}"
//      println "gmt == ${it.gmt == gmt}"
//      println "cohort A == ${it.cohortA == cohortA}"
//      println "cohort B == ${it.cohortB == cohortB}"
//    }

    if(!compareResult){
      println "prior result not found running analysis -> check analysis environment"
      analysisService.checkAnalysisEnvironment()
      println "analysis found"
      // pull in TPM files



      File gmtFile = File.createTempFile(gmt.name, ".gmt")
      gmtFile.write(gmt.data)
      gmtFile.deleteOnExit()

      println "gmt file ${gmtFile} . . exists ${gmtFile.exists()}, size: ${gmtFile.size()}"

      // TODO: run these in parallel if needed, or just 1?
      JSONArray samplesA = null
      JSONArray samplesB = null
      try {
        if(samples){
          JSONArray samplesJsonArray = new JSONArray(samples)
//          println "samples json array "
//          println samplesJsonArray as JSON
          samplesA = samplesJsonArray.getJSONArray(0)
//          println samplesA as JSON
          samplesB = samplesJsonArray.getJSONArray(1)
//          println samplesB as JSON
  //      println "result A: ${resultA}"
        }
      } catch (e) {
        log.error(e)
      }

      Result resultA = analysisService.doBpaAnalysis(cohortA,gmtFile,gmt,method,samplesA)
      println "result A: ${resultA}"
      Result resultB = analysisService.doBpaAnalysis(cohortB,gmtFile,gmt,method,samplesB)
      println "result B: ${resultB}"

      compareResult = analysisService.calculateCustomGeneSetActivity(gmt,resultA,resultB,method,samples)
      println "compare result: ${compareResult}"

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
