package xena.analysis.grails

import org.xena.analysis.AnalysisService
import org.xena.analysis.CohortService

class BootStrap {

    CohortService cohortService

    def init = { servletContext ->
      cohortService.validateCohorts()
    }
    def destroy = {
    }
}
