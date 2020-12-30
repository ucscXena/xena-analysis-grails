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

  GmtService gmtService

  static responseFormats = ['json', 'xml']
  static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

  def index(Integer max,String method) {

    println "method: ${method}"
    println "max: ${max}"

      respond gmtService.list(params)
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
    gmtList.each({ jsonArray.add(it.name) })
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
    println "stroring with method '${method}' and gmt name '${gmtname}' '${gmtDataHash}"
    Gmt gmt = Gmt.findByName(gmtname)
    if (gmt == null) {
      def sameDataGmt = Gmt.findByHashAndMethod(gmtDataHash,method)
      if(sameDataGmt){
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: sameDataGmt.data, method: method)
      }
      else{
        gmt = new Gmt(name: gmtname, hash: gmtDataHash, data: json.gmtdata, method: method)
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
