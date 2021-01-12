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

  static responseFormats = ['json', 'xml']
  static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE",analyzeGmt: "POST"]

  def index(Integer max,String method) {

    println "method: ${method}"
    println "max: ${max}"

      respond gmtService.list(params)
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
      if(! new File(cohort.localFile).exists() ){
         // download to local file
        analysisService.getOriginalTpmFile(it)
      }
      cohort.save(failOnError: true,flush:true)
    }
    foundCohortList.eachParallel { Cohort it ->
      if(! new File(it.localFile).exists()){
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
//    List<Gmt> gmtList = Gmt.findAllByMethod(method)
    List<Gmt> gmtList = gmtService.list().sort{ a,b ->
      if(a.name.startsWith("Default")) return -1
      if(b.name.startsWith("Default")) return 1
      return a.name.toLowerCase() < b.name.toLowerCase() ? -1 : 1
    }
    println "gmtlist ${gmtList.name}"
    JSONArray jsonArray = new JSONArray()
    gmtList.each {
      def obj = new JSONObject()
      obj.name = it.name
      obj.geneCount = it.geneCount
      obj.hash = it.hash
      obj.id = it.id
      obj.method = it.method
      jsonArray.add(obj)
    }
    render jsonArray.unique() as JSON
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
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: sameDataGmt.data, method: method,geneCount: geneCount)
      }
      else{
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: json.gmtdata, method: method,geneCount: geneCount)
      }
      gmt.save(failOnError: true, flush: true)
    }

    respond(gmt)

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
