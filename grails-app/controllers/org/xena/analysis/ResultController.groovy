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
