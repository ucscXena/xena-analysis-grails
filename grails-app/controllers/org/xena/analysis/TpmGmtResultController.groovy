package org.xena.analysis


import grails.rest.*
import grails.converters.*

class TpmGmtResultController extends RestfulController {
    static responseFormats = ['json', 'xml']
    TpmGmtResultController() {
        super(TpmGmtResult)
    }
}
