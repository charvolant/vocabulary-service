package vocabulary.service

class UrlMappings {

    static mappings = {
        "/$controller/$action" ()
        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
