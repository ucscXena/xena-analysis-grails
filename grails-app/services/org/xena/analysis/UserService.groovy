package org.xena.analysis

import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONObject

import javax.servlet.http.HttpServletRequest

@Transactional
class UserService {

    def serviceMethod() {

    }

    AuthenticatedUser getUserFromRequest(HttpServletRequest httpServletRequest) {
        String authHeader = httpServletRequest.getHeader('Authorization')
        println "authHeader"
        println authHeader
        // TODO: do stuff to extract the token and clean stuff up
        String token = authHeader
        def tokens = token.split("\\.")
        println tokens.length
        def headers =  new String(java.util.Base64.decoder.decode(tokens[1]))
        println headers
        def jsonObject = new JSONObject(headers)
        println jsonObject.toString(2)
    }
}
