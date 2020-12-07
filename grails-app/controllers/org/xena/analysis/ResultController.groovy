package org.xena.analysis

import grails.converters.JSON
import grails.validation.ValidationException
import org.grails.web.json.JSONArray
import org.grails.web.json.JSONObject

import java.security.MessageDigest

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional

@ReadOnly
class ResultController {

    ResultService resultService

    static responseFormats = ['json', 'xml']
//    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE",analyze: "POST", checkAnalysisEnvironment: "GET"]

    def index(Integer max) {
      println "number is ${max}"
        params.max = Math.min(max ?: 10, 100)
        respond resultService.list(params), model:[resultCount: resultService.count()]
    }

    def show(Long id) {
        respond resultService.get(id)
    }

  def test(){
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

  def checkAnalysisEnvironment() {

    render new JSONObject() as JSON

  }


  @Transactional
  def analyze() {
    println "doing analyze"
    def json = request.JSON
    println "input json ${json as JSON}"
    String cohort = json.cohort
    String gmtname = json.gmtname
    String gmtdata = json.gmtdata
    String tpmUrl = json.tpmurl


    // handle and write gmt file
    String gmtDataHash = gmtdata.md5()
    Gmt gmt = Gmt.findByName(gmtname)
    if(gmt==null){
      gmt = new Gmt(name:gmtname,hash:gmtDataHash,data: gmtdata)
      gmt.save(failOnError: true, flush: true)
    }
    File gmtFile = File.createTempFile(gmtname,"gmt")
    gmtFile.write(gmtdata)
    gmtFile.deleteOnExit()


    // handl and write tpm file
    Tpm tpm = Tpm.findByCohort(cohort)
    if(tpm == null){
      String tpmData = new URL(tpmUrl).text
      tpm = new Tpm(
        cohort: cohort,
        url: tpmUrl,
        data: tpmData
      ).save(failOnError: true, flush: true)
    }


    // create output file
    File outputFile = File.createTempFile("output-${cohort.replaceAll("[ |\\(|\\)]",'_')}${hash}","tsv")
    outputFile.write("")
//    def outputFile = this.generateEmptyAnalysisFile(gmtFile,cohort) // TODO: write an output file based on hash of geneset and cohort

    this.checkAnalysisEnvironment()
//    console.log(`analysis environmeent fine "${method}"`)
//    if(method==='BPA'){
//      if(fs.existsSync(outputFile) && fs.statSync(outputFile).size == 0 ){
//        console.log(`exists and is blank`)
//        fs.unlinkSync(outputFile)
//      }
//      if(!fs.existsSync(outputFile)){
//        console.log('running BPA')
//        this.runBpaAnalysis(gmtPath,tpmFile,outputFile)
//        console.log('RAN BPA')
//      }
//    }
//    else{
//      console.log('methid is not BPA ? ',method)
//    }
//    console.log('reading file')
//    const result = await fs.readFileSync(outputFile,"utf8")
//    console.log('read file')

//    const convertedResult = this.convertTsv(result)
//    console.log('adding gene sets to results')
//    console.log('result',result)
//    this.addGeneSetResult(method,genesetName,convertedResult)
//    console.log('added result')
//    this.saveGeneSetState(DEFAULT_PATH)
//    console.log('saved gene state')
//    if (result == null) {
//      render status: NOT_FOUND
//      return
//    }
//    if (result.hasErrors()) {
//      transactionStatus.setRollbackOnly()
//      respond result.errors
//      return
//    }
//
//    try {
//      resultService.save(result)
//    } catch (ValidationException e) {
//      respond result.errors
//      return
//    }
//
//    respond result, [status: CREATED, view:"show"]
    Result result = new Result()
    save(result)

    render new JSONObject() as JSON
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

        respond result, [status: CREATED, view:"show"]
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

        respond result, [status: OK, view:"show"]
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
