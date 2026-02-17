package com.example

class BootStrap {

    def init = {
        // Set a tenant ID for discriminator multi-tenancy
        System.setProperty('gorm.tenantId', 'tenant1')

        // Create a test item on the default datasource
        new Item(name: 'Default Item').save(flush: true)
        println "BootStrap: Item count on default datasource = ${Item.count()}"

        // Try to create a Metric (which declares datasource 'secondary')
        // BUG: This will silently go to the default datasource instead
        try {
            def metric = new Metric(name: 'boot-metric', value: 99.0)
            metric.save(flush: true, failOnError: true)
            println "BootStrap: Metric saved with id=${metric.id}"
            println "BootStrap: Metric.count() = ${Metric.count()} (this queries the default datasource due to the bug)"
        } catch (Exception e) {
            println "BootStrap: Metric save failed — ${e.message}"
            println "BootStrap: This may indicate the table doesn't exist on the default datasource"
        }
    }

    def destroy = {
    }

}