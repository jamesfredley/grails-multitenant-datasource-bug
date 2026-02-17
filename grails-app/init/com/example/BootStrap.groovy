package com.example

class BootStrap {

    def init = {
        // Set a tenant ID for discriminator multi-tenancy
        System.setProperty('gorm.tenantId', 'tenant1')

        println ''
        println '=' * 60
        println 'GormEnhancer.allQualifiers() Multi-Datasource Bug Demo'
        println '=' * 60
        println ''
        println 'Visit http://localhost:8080/bugDemo/index to see the bug'
        println ''
        println 'This app demonstrates that GormEnhancer.allQualifiers()'
        println 'overrides the explicit datasource declaration for'
        println 'MultiTenant domain classes, routing data to ALL'
        println 'datasources instead of only the declared one.'
        println '=' * 60
    }

    def destroy = {
    }

}