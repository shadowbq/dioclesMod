# Diocles Mod (scoreboard + external sync)


## Features

* Tracks deaths using a Minecraft scoreboard (persistent).
* On player death: posts a JSON payload for that player to POST_URL + /api/deathboard.
* On server day change: posts the full scoreboard to POST_URL + /api/sync.
* Commands:
  * /diocles deathboard        (public) - shows your deaths + top 10
  * /diocles deathboard-full   (op)     - prints full scoreboard JSON to console
  * /diocles serverday         (public) - shows current server day
  * /diocles helloworld        (op)     - title to all players
  * /diocles version           (op)     - prints version
  * /diocles debug-ping        (op)     - test API connectivity with health endpoint fallback
  * /diocles debug-scoreboard  (op)     - show current scoreboard state
  * /diocles debug-fullstats   (op)     - show detailed death statistics

## Health Check Behavior

The `debug-ping` command implements a robust health check with fallback endpoints:

1. **Primary**: `GET /health` - Standard health check endpoint
2. **Secondary**: `GET /ping` - Alternative ping endpoint  
3. **Fallback**: `GET /` (base URL) - Root endpoint check

The command tries each endpoint in order until one returns HTTP 200, providing feedback about which endpoint succeeded. This ensures maximum compatibility with different API server configurations.

## Local Mode Operation

Diocles is designed with **graceful degradation** and works perfectly even when no external HTTP API server is available. The mod operates in "local mode" when API configuration is missing or unreachable.

### ✅ What Works in Local Mode

- **Death Tracking**: Minecraft scoreboard continues to track deaths normally
- **All Commands**: Local commands function without any limitations:
  - `/diocles deathboard` - Shows deaths and top 10 from local scoreboard
  - `/diocles serverday` - Displays current server day
  - `/diocles debug-scoreboard` - Shows current scoreboard state
  - `/diocles debug-fullstats` - Displays detailed local death statistics
- **No Server Crashes**: Robust error handling prevents any server instability
- **Zero Performance Impact**: No network timeouts or blocking operations

### 🔧 Local Mode Activation

Local mode is automatically enabled when:
- Environment variables `DIOCLES_XHOST` and `DIOCLES_AUTHKEY` are not set
- Config file `config/diocles.json` is missing or contains no API URL
- External API server is unreachable (network errors are logged gracefully)

### 📝 Local Mode Indicators

- Server log shows: `[Diocles] initialized. POST base=none`
- `/diocles debug-ping` reports: `"No API URL configured"`
- HTTP errors are logged as: `[Diocles] Failed to POST to /api/endpoint: [error message]`

### 🚀 Deployment Safety

This design makes Diocles safe to deploy in any environment:
- **Development servers** can run without setting up external APIs
- **Production servers** continue working if the API service is temporarily down
- **Gradual rollout** allows deploying the mod before the external API infrastructure is ready

All core Minecraft functionality remains intact while external synchronization features are simply skipped when unavailable.

## Config

* Environment variables (preferred):
    DIOCLES_XHOST    e.g. http://xhost:3000
    DIOCLES_AUTHKEY  header value sent as 'authkey'
* Fallback config file in world save: config/diocles.json
   {
     "deathboard_uri": "http://xhost:3000",
     "authkey": "secret"
   }

## Testing

This mod includes comprehensive unit tests:

```bash
./gradlew test
```

### Test Coverage:
* **DeathboardManagerTest** (7 tests) - Tests core functionality, JSON serialization, URL handling, and configuration
* **ConfigurationTest** (7 tests) - Tests environment variables, config file structure, and fallback logic  
* **DioclesModTest** (2 tests) - Tests mod initialization and class structure
* **HttpApiIntegrationTest** (8 tests) - Mock server tests for HTTP API endpoints using WireMock
* **OfflineOperationTest** (5 tests) - Tests graceful degradation and local mode operation

#### API Integration Testing
The HTTP API integration tests use WireMock to simulate the external deathboard API:
* Tests `/api/deathboard` and `/api/sync` endpoints
* Tests health check endpoints (`/health`, `/ping`, base URL fallback)
* Validates JSON payload structure and authentication headers
* Tests error handling and timeout scenarios
* Verifies API contract compliance

#### Offline Mode Testing
The offline operation tests verify graceful degradation when no external API is available:
* Tests initialization with missing configuration
* Validates local scoreboard operations continue normally
* Verifies error handling doesn't crash the server
* Tests JSON payload generation works independently
* Confirms configuration fallback behavior

View detailed test results in `build/reports/tests/test/index.html` after running tests.

**Total: 29 tests, 100% success rate**

## Building from Source

### Build Steps

1. Clone the repository:

   ```bash
   git clone https://github.com/shadowbq/loginlogger-fabricmod.git
   cd loginlogger-fabricmod
   ```

2. Build the mod:

   ```bash
   ./gradlew build
   ```

3. The built JAR file will be located in `build/libs/`
