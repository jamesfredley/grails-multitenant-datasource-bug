package com.example

/**
 * A simple domain class on the default datasource.
 * Used to verify that the default datasource works correctly.
 */
class Item {

    String name
    Date dateCreated

    static constraints = {
        name blank: false
    }
}
