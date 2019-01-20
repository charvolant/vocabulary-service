package au.org.ala.vocabulary

class UrlMappings {

    static mappings = {
        "/$controller" ( action: 'list' )
        "/$controller/$action" ()
        "/"(view:"/index")
        "500"(view:'/error')
        "404"(view:'/notFound')
    }
}
