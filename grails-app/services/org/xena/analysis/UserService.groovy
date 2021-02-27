package org.xena.analysis

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
        String jwtString = authHeader.split("jwt=")[1]
        SignedJWT signedJWT = SignedJWT.parse(jwtString);
        def claimObject = new JSONObject(signedJWT.getJWTClaimsSet().toJSONObject())
        Boolean isEmailVerified = claimObject.getBoolean("email_verified")
        if(!isEmailVerified){
            throw new RuntimeException("Not validated")
        }

        def validationText = new URL("https://oauth2.googleapis.com/tokeninfo?id_token=${jwtString}").text
        def validationObject = new JSONObject(validationText)
        if(!validationObject.email_verified || validationObject.email != claimObject["email"] ){
            throw new RuntimeException("Invalid signatuture for key: "+validationText)
        }
        else{
            println "valid signature"
        }

//        // TODO: need to get the access  token to work here
//        https://developers.google.com/identity/sign-in/web/backend-auth
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
