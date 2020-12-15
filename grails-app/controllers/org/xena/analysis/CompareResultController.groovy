package org.xena.analysis

import grails.converters.JSON
import grails.validation.ValidationException
import groovy.json.JsonSlurper
import org.grails.web.json.JSONObject

import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional

@ReadOnly
class CompareResultController {

    CompareResultService compareResultService

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
    String result = json.result

    println "analyzing '${cohortNameA} / ${cohortNameB} ' with method '${method}' and gmt name '${gmtname}'"
    Cohort cohortA = Cohort.findByName(cohortNameA)
    Cohort cohortB = Cohort.findByName(cohortNameB)
    Gmt gmt = Gmt.findByName(gmtname)
    println "cohortA ${cohortA}, cohortB, ${cohortB}, GMT: ${gmt}"

    CompareResult compareResult = CompareResult.findByMethodAndCohortAAndCohortBAndGmt(method,cohortA,cohortB,gmt)
    if (compareResult == null) {
      println "trying to save"
      compareResult = new CompareResult(
        method: method,
        cohortA: cohortA,
        cohortB: cohortB,
        samples: samples,
        gmt: gmt,
        result: result,
      ).save(failOnError: true, flush: true )
      println "saved ${compareResult}"
    }
    else{
      println "updating ${compareResult}"
      compareResult.result = result
      compareResult.save(failOnError: true, flush: true )
      println "updated ${compareResult}"
    }

    respond compareResult, [status: CREATED, view:"show"]
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
      respond result
    }
    else{
      respond new JSONObject()
    }

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
