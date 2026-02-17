package com.example

import grails.converters.JSON
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.core.connections.ConnectionSource
import org.grails.datastore.mapping.core.connections.ConnectionSourcesSupport

/**
 * Demonstrates the GormEnhancer.allQualifiers() bug.
 *
 * Visit http://localhost:8080/bugDemo/index to see the issue.
 */
class BugDemoController {

    def index() {
        // 1. Show what datasources Metric DECLARES
        def entity = grailsApplication.mappingContext.getPersistentEntity(Metric.name)
        def declaredDatasources = ConnectionSourcesSupport.getConnectionSourceNames(entity)

        // 2. Show what allQualifiers() actually returns
        // (This is the bug — it expands to ALL datasources instead of preserving 'secondary')
        def enhancer = new GormEnhancer(grailsApplication.mainContext.getBean('hibernateDatastore'))
        def actualQualifiers = enhancer.allQualifiers(
                grailsApplication.mainContext.getBean('hibernateDatastore'),
                entity
        )

        // 3. Save a Metric via direct domain class call
        System.setProperty('gorm.tenantId', 'tenant1')
        def metric = new Metric(name: 'test-metric', value: 42.0)
        metric.save(flush: true, failOnError: true)

        // 4. Try to find it on the secondary datasource
        def foundOnSecondary = null
        try {
            foundOnSecondary = Metric.secondary.count()
        } catch (Exception e) {
            foundOnSecondary = "ERROR: ${e.message}"
        }

        // 5. Check if it ended up on the default datasource instead
        def foundOnDefault = null
        try {
            foundOnDefault = Metric.count()
        } catch (Exception e) {
            foundOnDefault = "ERROR: ${e.message}"
        }

        def result = [
                bug_description: 'GormEnhancer.allQualifiers() overrides explicit datasource for MultiTenant entities',
                domain_class: 'Metric',
                declared_datasource: declaredDatasources,
                actual_qualifiers_from_allQualifiers: actualQualifiers,
                expected_qualifiers: ['secondary'],
                metric_saved: metric.id != null,
                count_on_default_datasource: foundOnDefault,
                count_on_secondary_datasource: foundOnSecondary,
                verdict: 'If count_on_default > 0 and count_on_secondary == 0, the data was routed to the WRONG database'
        ]

        render result as JSON
    }
}
