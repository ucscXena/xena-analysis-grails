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
class AuthenticatedUserController {

    AuthenticatedUserService authenticatedUserService

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond authenticatedUserService.list(params), model:[authenticatedUserCount: authenticatedUserService.count()]
    }

    def show(Long id) {
        respond authenticatedUserService.get(id)
    }

    @Transactional
    def save(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            render status: NOT_FOUND
            return
        }
        if (authenticatedUser.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond authenticatedUser.errors
            return
        }

        try {
            authenticatedUserService.save(authenticatedUser)
        } catch (ValidationException e) {
            respond authenticatedUser.errors
            return
        }

        respond authenticatedUser, [status: CREATED, view:"show"]
    }

    @Transactional
    def update(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null) {
            render status: NOT_FOUND
            return
        }
        if (authenticatedUser.hasErrors()) {
            transactionStatus.setRollbackOnly()
            respond authenticatedUser.errors
            return
        }

        try {
            authenticatedUserService.save(authenticatedUser)
        } catch (ValidationException e) {
            respond authenticatedUser.errors
            return
        }

        respond authenticatedUser, [status: OK, view:"show"]
    }

    @Transactional
    def delete(Long id) {
        if (id == null || authenticatedUserService.delete(id) == null) {
            render status: NOT_FOUND
            return
        }

        render status: NO_CONTENT
    }
}
