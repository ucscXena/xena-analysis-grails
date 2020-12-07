package org.xena.analysis

import grails.converters.JSON
import grails.validation.ValidationException
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
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond resultService.list(params), model:[resultCount: resultService.count()]
    }

    def show(Long id) {
        respond resultService.get(id)
    }


  Tpm generateTpmFromCohort(Object o) {
    null
  }

  Gmt generateGmtFile(Object o1, Object o2) {
    null
  }

  File generateEmptyAnalysisFile(Gmt gmt, Object o) {
    null
  }

  void checkAnalysisEnvironment() {

  }

  @Transactional
  def analyze() {
    def json = request.JSON
    prinltn "input json ${json as JSON}"
    def cohort = json.cohort
    def genesetName = json.genesetName
    def gmtData = json.gmtData

    Tpm tpmFile = generateTpmFromCohort(cohort)
    Gmt gmtPath = generateGmtFile(genesetName,gmtData) // TODO: write to file
    def outputFile = this.generateEmptyAnalysisFile(gmtPath,cohort) // TODO: write an output file based on hash of geneset and cohort

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
