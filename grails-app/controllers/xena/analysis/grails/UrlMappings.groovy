package xena.analysis.grails

class UrlMappings {

  static mappings = {


    delete "/$controller/$id(.$format)?"(action: "delete")
    get "/$controller(.$format)?"(action: "index")
    get "/$controller/$id(.$format)?"(action: "show")
    post "/$controller(.$format)?"(action: "save")
    put "/$controller/$id(.$format)?"(action: "update")
    patch "/$controller/$id(.$format)?"(action: "patch")

    "/$controller/$action?/$id?"{
      constraints {
        // apply constraints here
      }
    }

    "/user"(resources:'authenticatedUser')


//    "/result/test"(controller: 'result', action: 'test')
//    "/result/analyze"(controller: 'result', action: 'analyze')
    "/"(controller: 'application', action: 'index')
    "500"(view: '/error')
    "404"(view: '/notFound')
  }
}
