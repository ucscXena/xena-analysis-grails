package org.xena.analysis

import grails.converters.JSON
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.*

@ReadOnly
class GmtController {

  private globalMean
  private globalVariance

  GmtService gmtService
  AnalysisService analysisService
  TpmAnalysisService tpmAnalysisService

  static responseFormats = ['json', 'xml']
  static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE",analyzeGmt: "POST"]

  def index(Integer max,String method) {

    println "method: ${method}"
    println "max: ${max}"

    respond Gmt.list(params)
  }

  /**
   * Loads all TPM files for cohorts
   */
  def loadAllCohorts(){
    def json = request.JSON
    def requestedCohorts = json.cohorts as JSONObject
    def cohortNames = requestedCohorts.name  as List<String>
    def foundCohortList = Cohort.findAllByNameInList(cohortNames) as List<Cohort>
    def foundCohortNames = foundCohortList.name

    // add any cohorts that are not yet found
    def missingCohortNameList = cohortNames - foundCohortNames
    missingCohortNameList.eachParallel {
      Cohort cohort = new Cohort(
        name: it,
        remoteUrl: requestedCohorts[it],
      )
      if(! new File(cohort.localTpmFile).exists() ){
         // download to local file
        analysisService.getOriginalTpmFile(it)
      }
      cohort.save(failOnError: true,flush:true)
    }
    foundCohortList.eachParallel { Cohort it ->
      if(! new File(it.localTpmFile).exists()){
        analysisService.getOriginalTpmFile(it)
      }
    }

    // assert that all cohorts are there
    // assert that the input matches the output cohort names
    assert cohortNames.sort() == Cohort.all.name.sort()

    def stats = analysisService.calculateMeanAndVariance()
    globalMean = stats[0]
    globalVariance = stats[1]

//    analysisService.createZScores(globalMean,globalVariance)

  }

  /**
   * Analyzes loaded GMT files
   */
  def analyzeGmt(){
    def json = request.JSON
    String method = json.method
    String gmtname = json.gmtname
    println "analyzing with method '${method}' and gmt name '${gmtname}'"
    Gmt gmt = Gmt.findByName(gmtname)
    if (gmt == null) {
      throw new RuntimeException("Gmt file not found for ${gmtname}")
    }

  }

  def names(String method) {
    println "method: ${method}"
    def gmtList = Gmt.executeQuery(" select g.name,g.geneSetCount,g.availableTpmCount,count(r) from Gmt g left outer join g.results r group by g")
    println "gmtlist ${gmtList}"
    JSONArray jsonArray = new JSONArray()
    gmtList.each { def gmtEntry ->
      def obj = new JSONObject()
      obj.name = gmtEntry[0]
      obj.geneCount = gmtEntry[1]
//      obj.hash = it.hash
//      obj.id = it.id
      obj.method = method
      obj.availableCount = gmtEntry[2]
      obj.readyCount = gmtEntry[3]
      obj.ready = obj.availableCount == obj.readyCount
      jsonArray.add(obj)
    }
    render jsonArray as JSON
  }

  def show(Long id) {
    respond gmtService.get(id)
  }


  @Transactional
  def store() {
    def json = request.JSON
    String method = json.method
    String gmtname = json.gmtname
    String gmtDataHash = json.gmtdata.md5()
    def geneCount = json.gmtdata.split("\n").findAll{it.split("\t").size()>2 }.size()


    println "stroring with method '${method}' and gmt name '${gmtname}' '${gmtDataHash}"
    Gmt gmt = Gmt.findByName(gmtname)
    if (gmt == null) {
      def sameDataGmt = Gmt.findByHashAndMethod(gmtDataHash,method)
      if(sameDataGmt){
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: sameDataGmt.data, method: method, geneSetCount: geneCount)
      }
      else{
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: json.gmtdata, method: method, geneSetCount: geneCount)
      }
      gmt.save(failOnError: true)
    }

//    File allTpmFile  = new File(AnalysisService.ALL_TPM_FILE_STRING)
    def cohorts = new JSONObject(new URL(CohortService.COHORT_URL).text)
    gmt.availableTpmCount = cohorts.keySet().size()
    gmt.save(failOnError: true, flush: true)


    respond(gmt)

    tpmAnalysisService.loadTpmForGmtFiles(gmt)
  }


  @Transactional
  def save(Gmt gmt) {
    if (gmt == null) {
      render status: NOT_FOUND
      return
    }
    if (gmt.hasErrors()) {
      transactionStatus.setRollbackOnly()
      respond gmt.errors
      return
    }

    try {
      gmtService.save(gmt)
    } catch (ValidationException e) {
      respond gmt.errors
      return
    }

    respond gmt, [status: CREATED, view: "show"]
  }

  @Transactional
  def update(Gmt gmt) {
    if (gmt == null) {
      render status: NOT_FOUND
      return
    }
    if (gmt.hasErrors()) {
      transactionStatus.setRollbackOnly()
      respond gmt.errors
      return
    }

    try {
      gmtService.save(gmt)
    } catch (ValidationException e) {
      respond gmt.errors
      return
    }

    respond gmt, [status: OK, view: "show"]
  }

  @Transactional
  def delete(Long id) {
    if (id == null || gmtService.delete(id) == null) {
      render status: NOT_FOUND
      return
    }

    render status: NO_CONTENT
  }
}
