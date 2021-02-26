package org.xena.analysis

import com.nimbusds.jose.JWSSigner
import com.nimbusds.jose.JWSVerifier
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.crypto.RSASSAVerifier
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
import com.nimbusds.jwt.SignedJWT
import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONObject

import javax.servlet.http.HttpServletRequest
import java.security.interfaces.RSAKey

@Transactional
class UserService {

    public final static String[] ROLES = [ "ADMIN", "USER","BANNED","INACTIVE"]

    private final String[] ADMIN_USERS = [
            "nathandunn@lbl.gov",
            "jing",
            "bcraft",
            "mariangoldman"
    ]

    AuthenticatedUser getUserFromRequest(HttpServletRequest httpServletRequest) {

        String authHeader = httpServletRequest.getHeader('Authorization')
        println "authHeader"
        println authHeader
        String jwtString = authHeader.split("jwt=")[1]

        // TODO:
        // from https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-rsa-signature
        //
        RSAKey rsaJWK = new RSAKeyGenerator(2048)
                .keyID(System.getenv("GOOGLE_KEY"))
                .generate();
        RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();
// Create RSA-signer with the private key
        JWSSigner signer = new RSASSASigner(rsaJWK);
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        JWSVerifier verifier = new RSASSAVerifier(rsaPublicJWK);
        Boolean validated  = signedJWT.verify(verifier)
//        assertTrue(signedJWT.verify(verifier));
        println "is valid token: ${validated}"

        // TODO: do stuff to extract the token and clean stuff up
        def tokens = jwtString.split("\\.")
        println tokens.length
        def headers =  new String(java.util.Base64.decoder.decode(tokens[1]))
        println headers
        def jsonObject = new JSONObject(headers)
        println jsonObject.toString(2)

        String username = jsonObject.email
        // TODO: get user object
        AuthenticatedUser user = AuthenticatedUser.findByEmail(username)
        if(!user){
            user = new AuthenticatedUser(
                    firstName: jsonObject.profileObject.firstName,
                    lastName: jsonObject.profileObject.lastName,
                    email: jsonObject.profileObject.email,
            ).save(flush: true, failOnError:true,insert:true)
        }
        return user

    }
}
