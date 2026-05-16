# Clukva (Special for Mizuki)

Special utility toolkit for Java

* Type-safe collections 
* HTTP client 
* Advanced logging 
* Concurrency

## Purpose

Ebanina Utils provides industrial-strength tools for everyday development tasks:
- HTTP API clients (Request/Response pattern)
- Type-safe Map wrappers (auto/strict modes)
- Structured logging (7 levels: info/warn/severe/profiler/suppress/print/print_ln_st)
- Collections and concurrency utilities

Tested in production Mizuka/Music API projects with Deezer/Spotify integration.

## Architecture
### rf.ebanina.utils

* network
* logging
* concurrency
* weak

## Quick Start

```java
// HTTP Client (3 lines)
Request req = new Request("https://api.deezer.com/track/123");
Response res = req.send();
String json = res.getBody().toString();

// Type-safe Map
StrictTypicalMapWrapper<String> config = new StrictTypicalMapWrapper<>(
    String.class, Integer.class, Boolean.class
);
config.put("port", 8080, Integer.class);
int port = config.get("port", Integer.class);

// Structured Logging
ILogging logger = Logging.getLogger(YourClass.class);
logger.info("Starting...");
logger.profiler("Request: 245ms");
logger.printf("Time: %d ms", 245);
```

## Core Features

### HTTP Client (network/)
```java
Response res = new Request(url).send();
int code = res.getCode();     // 200/404/429  
String json = res.getBody().toString();  // {"data":[...]}
```
- Fluent builder API
- StringBuilder body (no copying)
- equals/hashCode by body content (cache-friendly)
- Thread-safe per request

### Type-safe Collections (collections/)
```java
// Auto-type inference (runtime)
TypicalMapWrapper<String> auto = new TypicalMapWrapper<>();
auto.putAuto("name", "KUTE");
String name = auto.getAuto("name");

// Strict compile-time safety
StrictTypicalMapWrapper<String> strict = new StrictTypicalMapWrapper<>(
    String.class, Integer.class
);
strict.put("age", 30, Integer.class);
```
- `putAuto()` / `getAuto()` — runtime type inference
- `StrictTypicalMapWrapper` — compile-time type validation
- Serializable with full type preservation

### Advanced Logging (loggining/)
```java
ILogging logger = Logging.getLogger(YourClass.class);

logger.info("Request sent");           // Normal operation
logger.warn("Rate limit hit");          // Recoverable issues  
logger.severe(e);                       // Critical failures
logger.profiler("245ms");               // Performance metrics
logger.suppress("Cache miss");          // Graceful degradation
logger.print_ln_st("Debug state");      // Full stack trace
logger.printf("Time: %d ms", 245);      // Printf-style formatting
```
**7 levels:** info • warn • severe • profiler • suppress • print • print_ln_st

### Concurrency Utilities
- Weak reference management
- Executor service helpers
- Thread-safe collection wrappers

## Usage Example

```java
public class MusicApiClient {
    private final ILogging log = Logging.getLogger(getClass());
    private final StrictTypicalMapWrapper<String> config = 
        new StrictTypicalMapWrapper<>(String.class, Integer.class);
    
    public TrackData fetchTrack(String trackId) {
        log.info("Fetching track: " + trackId);
        
        Response res = new Request("https://api.deezer.com/track/" + trackId).send();
        
        if (res.getCode() == 200) {
            log.profiler("Success: " + res.getBody().length() + " bytes");
            return JsonProcess.parseTrack(res.getBody().toString());
        } else {
            log.warn("HTTP " + res.getCode() + ": " + trackId);
            return null;
        }
    }
}
```

## Project Status

| Component | Status |
|-----------|--------|
| HTTP Client | Production Ready |
| Logging | Production Ready |
| Collections | Production Ready |
| Concurrency | Stable |

## Dependencies

Zero external dependencies. JDK 17+ only:
- `java.base` — core functionality
- `java.logging` — optional JUL bridge

## Integration

Add to module path or classpath:
```xml
<!-- Maven -->
<dependency>
    <groupId>rf.ebanina</groupId>
    <artifactId>utils</artifactId>
    <version>1.0</version>
</dependency>
```

**Minimalist • Type-safe • Production-proven utilities**
