package org.xena.analysis

import grails.gorm.services.Service

@Service(CompareResult)
interface CompareResultService {

    CompareResult get(Serializable id)

    List<CompareResult> list(Map args)

    Long count()

    CompareResult delete(Serializable id)

    CompareResult save(CompareResult compareResult)

}
