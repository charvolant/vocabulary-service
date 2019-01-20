package au.org.ala.vocabulary

import grails.boot.*
import grails.boot.config.GrailsAutoConfiguration
import org.apache.commons.jcs.JCS

class Application extends GrailsAutoConfiguration {

    static void main(String[] args) {
        GrailsApp.run(Application, args)
    }

    /**
     * Configure JCS at startup.
     *
     * @param event
     */
    @Override
    void onStartup(Map<String, Object> event) {
        super.onStartup(event)
        URL cacheConfig = config.cacheManager.config ? new URL(config.cacheManager.config) : this.class.getResource('/cache.ccf')
        Properties base = new Properties()
        base.load(cacheConfig.openStream())
        JCS.configProperties = base
    }
}