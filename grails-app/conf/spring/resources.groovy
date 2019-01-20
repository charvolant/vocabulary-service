import org.eclipse.rdf4j.repository.http.HTTPRepository

beans = {
    repositoryBean(
            HTTPRepository,
            application.config.repository.service as String,
            application.config.repository.id as String
    ) { bean ->
        bean.initMethodName = 'initialize'
    }
}