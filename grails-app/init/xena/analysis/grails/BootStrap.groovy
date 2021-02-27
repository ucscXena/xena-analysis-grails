package xena.analysis.grails

import org.xena.analysis.AuthenticatedUser
import org.xena.analysis.CohortService
import org.xena.analysis.RoleEnum
import org.xena.analysis.UserService

class BootStrap {

    CohortService cohortService
    UserService userService

    def init = { servletContext ->
      cohortService.validateCohorts()
      userService.createAdmins()
    }
    def destroy = {
    }
}
