# FIXORA — Database

## Where the schema comes from

Fixora uses **Hibernate DDL auto-update** for local development, configured in
`backend/src/main/resources/application.properties`:

```
spring.jpa.hibernate.ddl-auto=update
```

On backend startup Hibernate creates any missing tables, columns, indexes and
unique constraints from the JPA entities in `com.fixora.entity`. Nothing has to
be created by hand.

## Where the data comes from

`com.fixora.config.SeedDataInitializer` runs on startup and is **idempotent**:
it exits immediately if the `categories` table already contains rows. Restarting
the backend therefore never duplicates the catalogue, and it never deletes
existing data.

## Folder layout

```
database/
├── init/
│   └── 01-charset.sql   # mounted into the MySQL container, pins utf8mb4
└── README.md            # this file
```

## Production note

`ddl-auto=update` is a development convenience. For a real deployment switch to
versioned migrations (Flyway or Liquibase) with:

```
spring.jpa.hibernate.ddl-auto=validate
```

and never let an application auto-modify a production schema.
