package com.example

import grails.converters.JSON
import groovy.sql.Sql
import org.grails.datastore.gorm.GormEnhancer
import org.grails.datastore.mapping.core.connections.ConnectionSourcesSupport

import javax.sql.DataSource

/**
 * Demonstrates the GormEnhancer.allQualifiers() bug.
 *
 * Visit http://localhost:8080/bugDemo/index to see the issue.
 *
 * The bug: when a domain class implements MultiTenant AND declares a specific
 * datasource (e.g. datasource 'secondary'), allQualifiers() ignores the declared
 * datasource and returns ALL datasources instead.
 */
class BugDemoController {

    DataSource dataSource            // Primary
    DataSource dataSource_secondary  // Secondary

    def index() {
        System.setProperty('gorm.tenantId', 'tenant1')

        def datastore = grailsApplication.mainContext.getBean('hibernateDatastore')
        def enhancer = new GormEnhancer(datastore)

        // --- Metric: MultiTenant + datasource 'secondary' (BUG TARGET) ---
        def metricEntity = grailsApplication.mappingContext.getPersistentEntity(Metric.name)
        def metricDeclared = ConnectionSourcesSupport.getConnectionSourceNames(metricEntity)
        def metricActual = enhancer.allQualifiers(datastore, metricEntity)

        // --- Item: non-MultiTenant, default datasource (CONTROL) ---
        def itemEntity = grailsApplication.mappingContext.getPersistentEntity(Item.name)
        def itemDeclared = ConnectionSourcesSupport.getConnectionSourceNames(itemEntity)
        def itemActual = enhancer.allQualifiers(datastore, itemEntity)

        // --- Table diagnostics: which tables ended up where ---
        def primarySql = new Sql(dataSource)
        def primaryTables = primarySql.rows(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'"
        ).collect { it.TABLE_NAME }
        primarySql.close()

        def secondarySql = new Sql(dataSource_secondary)
        def secondaryTables = secondarySql.rows(
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC'"
        ).collect { it.TABLE_NAME }
        secondarySql.close()

        // --- Verdict ---
        def qualifiersBugged = metricActual != metricDeclared

        def result = [
                metric: [
                        implements_multi_tenant: true,
                        declared_datasource: metricDeclared,
                        allQualifiers_returns: metricActual,
                        expected: metricDeclared,
                        match: !qualifiersBugged
                ],
                item_control: [
                        implements_multi_tenant: false,
                        declared_datasource: itemDeclared,
                        allQualifiers_returns: itemActual,
                        expected: itemDeclared,
                        match: itemActual == itemDeclared
                ],
                tables: [
                        primary_db: primaryTables,
                        secondary_db: secondaryTables
                ],
                bug_present: qualifiersBugged,
                verdict: qualifiersBugged
                        ? "BUG CONFIRMED: Metric declares datasource ${metricDeclared} but allQualifiers() returned ${metricActual}. The MultiTenant trait causes allQualifiers() to ignore the declared datasource and expand to ALL datasources. Compare with Item (non-MultiTenant control): declared ${itemDeclared}, allQualifiers() returned ${itemActual} — correct."
                        : 'NO BUG: allQualifiers() correctly returned only the declared datasource.',
                root_cause: 'GormEnhancer.allQualifiers() checks MultiTenant FIRST and returns ConnectionSource.ALL, overriding the explicit datasource declaration. Non-MultiTenant entities are unaffected.',
                workaround: 'Use GormEnhancer.findStaticApi(DomainClass, "secondary") to explicitly route queries to the correct datasource, bypassing allQualifiers().'
        ]

        render result as JSON
    }
}
