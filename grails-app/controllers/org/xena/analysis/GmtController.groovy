package org.xena.analysis

import grails.validation.ValidationException
import static org.springframework.http.HttpStatus.CREATED
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.NO_CONTENT
import static org.springframework.http.HttpStatus.OK
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY

import grails.gorm.transactions.ReadOnly
import grails.gorm.transactions.Transactional

@ReadOnly
class GmtController {

    GmtService gmtService

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond gmtService.list(params), model:[gmtCount: gmtService.count()]
    }

    def show(Long id) {
        respond gmtService.get(id)
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

        respond gmt, [status: CREATED, view:"show"]
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

        respond gmt, [status: OK, view:"show"]
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
