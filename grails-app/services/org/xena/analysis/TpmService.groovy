package org.xena.analysis

import grails.gorm.services.Service

@Service(Tpm)
interface TpmService {

    Tpm get(Serializable id)

    List<Tpm> list(Map args)

    Long count()

    Tpm delete(Serializable id)

    Tpm save(Tpm tpm)

}
