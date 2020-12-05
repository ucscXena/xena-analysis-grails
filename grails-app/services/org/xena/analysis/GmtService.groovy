package org.xena.analysis

import grails.gorm.services.Service

@Service(Gmt)
interface GmtService {

    Gmt get(Serializable id)

    List<Gmt> list(Map args)

    Long count()

    Gmt delete(Serializable id)

    Gmt save(Gmt gmt)

}
