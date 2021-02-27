package org.xena.analysis

import grails.converters.JSON
import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional
import grails.validation.ValidationException
import grails.web.RequestParameter
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject
import org.springframework.http.HttpStatus

import static org.springframework.http.HttpStatus.*

@ReadOnly
class GmtController {

  private globalMean
  private globalVariance

  GmtService gmtService
  AnalysisService analysisService
  TpmAnalysisService tpmAnalysisService
  UserService userService

  static responseFormats = ['json', 'xml']
  static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE",deleteByMethodAndName: "DELETE", analyzeGmt: "POST"]

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

  @Transactional
  def names(String method) {
    println "method: ${method}"
//    println "req: ${request.getHeader('Authorization')}"
    def publicGmtList = Gmt.executeQuery(" select g.name,g.geneSetCount,g.availableTpmCount,g.isPublic,count(r) from Gmt g left outer join g.results r where g.isPublic = 't' group by g")
    println "gmt list: ${publicGmtList}"

    if(request.getHeader('Authorization')){
      AuthenticatedUser user = userService.getUserFromRequest(request)
      if(user){
        def privateList = []
        if(user.role == RoleEnum.ADMIN){
          privateList = Gmt.executeQuery(" select g.name,g.geneSetCount,g.availableTpmCount,g.isPublic,count(r),u from Gmt g left outer join g.results r join g.authenticatedUser u where g.isPublic != 't' group by g, u")
        }
        else
        if(user.role == RoleEnum.USER){
          privateList = Gmt.executeQuery(" select g.name,g.geneSetCount,g.availableTpmCount,g.isPublic,count(r),u from Gmt g left outer join g.results r  join g.authenticatedUser u where g.authenticatedUser=:user group by g, u",[user:user])
        }
        publicGmtList = publicGmtList + privateList
      }
    }


//    println "gmtlist ${publicGmtList as JSON}"
    JSONArray jsonArray = new JSONArray()
    publicGmtList.sort{ a,b ->   a[0].toString().compareTo(b[0].toString())} .each { def gmtEntry ->
      def obj = new JSONObject()
      obj.name = gmtEntry[0]
      obj.geneCount = gmtEntry[1]
      obj.method = method
      obj.availableCount = gmtEntry[2]
      obj.public = gmtEntry[3]
      obj.readyCount = gmtEntry[4]
      obj.ready = obj.availableCount == obj.readyCount
      if(gmtEntry.size()>5){
        obj.user = gmtEntry[5].firstName + " " + gmtEntry[5].lastName
      }
      jsonArray.add(obj)
    }
    render jsonArray as JSON
  }

  def show(Long id) {
    respond gmtService.get(id)
  }


  @Transactional
  def store() {

    AuthenticatedUser user = userService.getUserFromRequest(request)
    if(!user){
       throw new RuntimeException("Not authorized")
    }


    def json = request.JSON
    String method = json.method
    String gmtname = json.gmtname
    String gmtDataHash = json.gmtdata.md5()
    def geneCount = json.gmtdata.split("\n").findAll{it.split("\t").size()>2 }.size()


//    println "stroring with method '${method}' and gmt name '${gmtname}' '${gmtDataHash}"
    Gmt gmt = Gmt.findByName(gmtname)
    if (gmt == null) {
      def sameDataGmt = Gmt.findByHashAndMethod(gmtDataHash,method)
      if(sameDataGmt){
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: sameDataGmt.data, method: method, geneSetCount: geneCount,authenticatedUser:user,isPublic: false)
      }
      else{
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: json.gmtdata, method: method, geneSetCount: geneCount,authenticatedUser:user,isPublic: false)
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
  def deleteByMethodAndName(@RequestParameter('method')  String method, @RequestParameter('geneSetName') String geneSetName) {
    Gmt gmt = Gmt.findByNameAndMethod(geneSetName,method)
    if(gmt==null){
      render status: NOT_FOUND
      return
    }

    TpmGmtAnalysisJob.deleteAll(TpmGmtAnalysisJob.findAllByGmt(gmt))
    TpmGmtResult.deleteAll(TpmGmtResult.findAllByGmt(gmt))
    gmt.delete()

    render status: OK
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
