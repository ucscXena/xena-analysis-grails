package org.xena.analysis

//import com.nimbusds.jose.JWSSigner
//import com.nimbusds.jose.JWSVerifier
//import com.nimbusds.jose.crypto.RSASSASigner
//import com.nimbusds.jose.crypto.RSASSAVerifier
//import com.nimbusds.jose.jwk.RSAKey
//import com.nimbusds.jose.jwk.gen.RSAKeyGenerator
//import com.nimbusds.jwt.JWT
//import com.nimbusds.jwt.JWTParser
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory;
import com.nimbusds.jwt.SignedJWT
import grails.gorm.transactions.Transactional
import org.grails.web.json.JSONObject

import javax.servlet.http.HttpServletRequest

@Transactional
class UserService {

    public final static String[] ROLES = [ "ADMIN", "USER","BANNED","INACTIVE"]

    private final String CLIENT_ID = System.getenv("GOOGLE_ID")

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
        String accessToken = httpServletRequest.getHeader('GoogleAccessToken')
        println "accessToken"
        println accessToken

//        // TODO:
//        // from https://connect2id.com/products/nimbus-jose-jwt/examples/jwt-with-rsa-signature
//        //
//        JWT jwt = JWTParser.parse(jwtString)
//        println "is signed? ${jwt instanceof SignedJWT}"
//        RSAKey rsaJWK = new RSAKeyGenerator(2048)
//                .keyID(System.getenv("GOOGLE_ID"))
//                .generate();
//        RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();
//// Create RSA-signer with the private key
//        JWSSigner signer = new RSASSASigner(rsaJWK);
//        JWSVerifier verifier = new RSASSAVerifier(rsaPublicJWK);
//        Boolean validated  = signedJWT.verify(verifier)
//        assertTrue(signedJWT.verify(verifier));
//        println "is valid token: ${validated}"

        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        def claimObject = new JSONObject(signedJWT.getJWTClaimsSet().toJSONObject())
        Boolean isEmailVerified = claimObject.getBoolean("email_verified")
        if(!isEmailVerified){
            throw new RuntimeException("Not validated")
        }

        // TODO: do stuff to extract the token and clean stuff up
//        def tokens = jwtString.split("\\.")
//        println tokens.length
//        def headers =  new String(java.util.Base64.decoder.decode(tokens[1]))
//        println headers
//        def jsonObject = new JSONObject(headers)


        // TODO: need to get the access  token to work here
//        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new JacksonFactory())
//        // Specify the CLIENT_ID of the app that accesses the backend:
//                .setAudience(Collections.singletonList(CLIENT_ID))
//        // Or, if multiple clients access the backend:
//        //.setAudience(Arrays.asList(CLIENT_ID_1, CLIENT_ID_2, CLIENT_ID_3))
//                .build();
//        println "E"
//        GoogleIdToken idToken = verifier.verify(accessToken);
//        println "id token ${idToken}"
//

        String username = claimObject.email
        // TODO: get user object
        AuthenticatedUser user = AuthenticatedUser.findByEmail(username)
        if(!user){
            user = new AuthenticatedUser(
                    firstName: claimObject["given_name"],
                    lastName: claimObject["family_name"],
                    email: claimObject["email"],
                    role: RoleEnum.USER
            ).save(flush: true, failOnError:true,insert:true)
        }
        else{
            // update if different
            user.firstName = claimObject["given_name"]
            user.lastName = claimObject["family_name"]
            user.save(insert:false,flush: true,failOnError: true)
        }
        return user

    }

    def createAdmins(){
        AuthenticatedUser.findOrSaveByEmailAndRole("ndunnme@gmail.com",RoleEnum.ADMIN)
        AuthenticatedUser.findOrSaveByEmailAndRole("jzhu@soe.ucsc.edu",RoleEnum.ADMIN)
        AuthenticatedUser.findOrSaveByEmailAndRole("craft@soe.ucsc.edu",RoleEnum.ADMIN)
        AuthenticatedUser.findOrSaveByEmailAndRole("mary@soe.ucsc.edu",RoleEnum.ADMIN)
    }
}
