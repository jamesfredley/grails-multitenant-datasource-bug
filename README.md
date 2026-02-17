# GormEnhancer.allQualifiers() Overrides Explicit Datasource for MultiTenant Entities

**Grails Version**: 7.0.7  
**Severity**: Critical — silent data routing to wrong database

## Bug Description

When a domain class implements `MultiTenant` AND declares an explicit non-default datasource via `datasource 'secondary'` in its `static mapping` block, `GormEnhancer.allQualifiers()` **clears** the explicit datasource qualifier and replaces it with `DEFAULT` plus all known connection sources.

This causes `.save()`, `.get()`, `.count()`, and all other GORM operations to silently route to the **default datasource** instead of the declared one. Data is written to the wrong database with no error or warning.

## Steps to Reproduce

1. Clone this repository
2. Run `./gradlew bootRun`
3. Visit `http://localhost:8080/bugDemo/index`
4. Observe the JSON response showing that `allQualifiers()` returns `['DEFAULT', 'secondary']` instead of `['secondary']`

## Expected Behavior

A domain class with `datasource 'secondary'` should have its GORM operations route to the `secondary` datasource, regardless of whether it implements `MultiTenant`.

`allQualifiers()` should return `['secondary']` for this entity.

## Actual Behavior

`allQualifiers()` returns `['DEFAULT', 'secondary']` — expanding the qualifiers to ALL known connection sources. Since `findStaticApi()` defaults to the `DEFAULT` qualifier for DISCRIMINATOR multi-tenancy, all operations silently route to the wrong database.

## Root Cause

In `GormEnhancer.groovy` (lines 180-193), the `allQualifiers()` method:

```groovy
if ((MultiTenant.isAssignableFrom(entity.javaClass) || qualifiers.contains(ConnectionSource.ALL))
    && (datastore instanceof ConnectionSourcesProvider)) {
    qualifiers.clear()                        // ← CLEARS the explicit 'secondary' mapping!
    qualifiers.add(ConnectionSource.DEFAULT)   // ← Replaces with DEFAULT
    // adds all connection sources
}
```

The `MultiTenant` check unconditionally triggers qualifier expansion, even when the entity has an explicit non-default datasource. This logic is intended for DATABASE multi-tenancy mode (where each tenant maps to a different connection), but it incorrectly fires for DISCRIMINATOR mode as well.

## Workaround

Use `@Transactional(connection = 'secondary')` on Data Service abstract classes AND explicitly implement CRUD methods routing through `GormEnhancer.findStaticApi(Metric, 'secondary')`.

## Environment

- **Grails**: 7.0.7
- **Spring Boot**: 3.5.10
- **Groovy**: 4.0.30
- **JDK**: 17+

## Project Structure

| File | Purpose |
|------|---------|
| `grails-app/domain/com/example/Metric.groovy` | MultiTenant domain with `datasource 'secondary'` |
| `grails-app/domain/com/example/Item.groovy` | Simple domain on default datasource |
| `grails-app/controllers/com/example/BugDemoController.groovy` | Demonstrates the bug via JSON endpoint |
| `grails-app/init/com/example/BootStrap.groovy` | Seeds data to show routing behavior |
| `grails-app/conf/application.yml` | Two H2 datasources + DISCRIMINATOR multi-tenancy |

