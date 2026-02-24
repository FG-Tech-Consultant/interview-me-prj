# Common Module

This module contains shared code used by both backend and frontend (if applicable).

## Contents

- **DTOs (Data Transfer Objects)**: Java records for API requests and responses
- **Constants**: Shared constants and enumerations

## Dependencies

- Jakarta Validation API for validation annotations
- Lombok for boilerplate reduction

## Usage

This module is referenced as a dependency in the backend module:

```kotlin
dependencies {
    implementation(project(":common"))
}
```
