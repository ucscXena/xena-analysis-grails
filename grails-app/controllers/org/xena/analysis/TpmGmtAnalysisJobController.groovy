package org.xena.analysis


import grails.rest.*
import grails.converters.*

class TpmGmtAnalysisJobController extends RestfulController {
    static responseFormats = ['json', 'xml']
    TpmGmtAnalysisJobController() {
        super(TpmGmtAnalysisJob)
    }
}
