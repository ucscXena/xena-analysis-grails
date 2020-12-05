package org.xena.analysis

import grails.gorm.services.Service

@Service(Result)
interface ResultService {

    Result get(Serializable id)

    List<Result> list(Map args)

    Long count()

    Result delete(Serializable id)

    Result save(Result result)

}
