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
class TpmController {

    TpmService tpmService

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond tpmService.list(params), model:[tpmCount: tpmService.count()]
    }

    def show(Long id) {
        respond tpmService.get(id)
    }

    @Transactional
    def save(Tpm tpm) {
        if (tpm == null) {
            render status: NOT_FOUND
            return
        }
        if (tpm.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond tpm.errors
            return
        }

        try {
            tpmService.save(tpm)
        } catch (ValidationException e) {
            respond tpm.errors
            return
        }

        respond tpm, [status: CREATED, view:"show"]
    }

    @Transactional
    def update(Tpm tpm) {
        if (tpm == null) {
            render status: NOT_FOUND
            return
        }
        if (tpm.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond tpm.errors
            return
        }

        try {
            tpmService.save(tpm)
        } catch (ValidationException e) {
            respond tpm.errors
            return
        }

        respond tpm, [status: OK, view:"show"]
    }

    @Transactional
    def delete(Long id) {
        if (id == null || tpmService.delete(id) == null) {
            render status: NOT_FOUND
            return
        }

        render status: NO_CONTENT
    }
}
