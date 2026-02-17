package com.example

import grails.gorm.MultiTenant

/**
 * A MultiTenant domain class mapped to the 'secondary' datasource.
 *
 * BUG: GormEnhancer.allQualifiers() clears the explicit 'secondary' datasource
 * qualifier for MultiTenant entities, causing .save()/.get() to silently route
 * to the default datasource instead.
 */
class Metric implements MultiTenant<Metric> {

    String tenantId
    String name
    Double value
    Date dateCreated

    static mapping = {
        tenantId name: 'tenantId'
        datasource 'secondary'
    }

    static constraints = {
        name blank: false
        value nullable: false
        tenantId nullable: true
    }
}
