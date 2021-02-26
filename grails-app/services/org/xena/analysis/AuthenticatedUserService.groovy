package org.xena.analysis

import grails.gorm.services.Service

@Service(AuthenticatedUser)
interface AuthenticatedUserService {

    AuthenticatedUser get(Serializable id)

    List<AuthenticatedUser> list(Map args)

    Long count()

    AuthenticatedUser delete(Serializable id)

    AuthenticatedUser save(AuthenticatedUser authenticatedUser)

}
