package org.xena.analysis


import grails.rest.*
import grails.converters.*

class TpmGmtAnalysisJobController extends RestfulController {
    static responseFormats = ['json', 'xml']
    TpmGmtAnalysisJobController() {
        super(TpmGmtAnalysisJob)
    }
//  /**
//   * Lists all resources up to the given maximum
//   *
//   * @param max The maximum
//   * @return A list of resources
//   */
//  def index(Integer max) {
//    if (max < 0) { max = null }
//    params.max = Math.min(max ?: 10, 100)
//    respond listAllResources(params), model: [("${resourceName}Count".toString()): countResources()]
//  }
}
