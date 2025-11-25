# Clay CLJ - Mouse Tracker & Canvas2D Renderer

A full-stack ClojureScript project featuring real-time mouse tracking with WebSocket backend and Canvas2D rendering capabilities.

## Architecture

- **Backend**: Clojure with HTTP-Kit WebSocket server
- **Frontend**: ClojureScript with Missionary reactive flows
- **Build Tool**: Shadow-CLJS with deps.edn integration
- **Dev Tool**: Launchpad for unified REPL management

## Getting Started

### Prerequisites

- [Babashka](https://babashka.org/) - For running Launchpad
- [Java](https://www.java.com/) - JDK 11 or higher
- [Node.js](https://nodejs.org/) - For Shadow-CLJS (v16+ recommended)

### Installation

```bash
# 1. Install npm dependencies
npm install

# 2. (Optional) Copy and customize your personal Launchpad configuration
cp deps.local.edn.template deps.local.edn
# Edit deps.local.edn with your preferences

# 3. (Optional) Setup environment variables
cp .env.template .env
# Edit .env with your configuration
```

## Development Workflow

### Quick Start (Full-Stack)

```bash
# Terminal 1: Start Clojure REPL with Launchpad
bin/launchpad --cider-nrepl

# In REPL:
(start)    ; Start WebSocket backend server on port 9090

# Terminal 2: Start Shadow-CLJS watch
npm run watch

# Open browser:
# http://localhost:8081 - Mouse Tracker app
# http://localhost:8080 - Development index
```

### Launchpad Commands

This project uses [Launchpad](https://github.com/lambdaisland/launchpad) for unified REPL management.

**Basic usage:**
```bash
bin/launchpad                    # Start with default configuration
bin/launchpad --cider-nrepl      # With VS Code/Calva support (recommended)
bin/launchpad --emacs            # With Emacs/CIDER support
bin/launchpad --portal           # With Portal data inspector
bin/launchpad --cider-nrepl --portal  # Combine multiple options
```

**REPL Functions** (from `dev/user` namespace):
```clojure
(start)    ; Start WebSocket server
(stop)     ; Stop WebSocket server
(restart)  ; Restart server
```

### Shadow-CLJS Commands

**Development:**
```bash
npm run watch           # Watch mouse-tracker build (auto-rebuild)
npm run watch:mouse     # Same as above (explicit)
npm run server          # Start Shadow-CLJS server only
```

**Production:**
```bash
npm run compile         # Compile once (no optimization)
npm run release         # Production build (optimized)
```

**Cleanup:**
```bash
npm run clean           # Remove compiled files
```

## Project Structure

```
clay-clj/
├── bin/
│   └── launchpad                    # Launchpad startup script
├── dev/
│   └── user.clj                     # REPL development utilities
├── resources/
│   └── public/                      # Web assets (served by Shadow-CLJS)
│       ├── index.html               # Development index page
│       ├── canvas2d-test.html       # Standalone Canvas2D test
│       └── mouse-tracker/
│           ├── index.html           # Mouse tracker HTML
│           └── js/                  # Compiled ClojureScript (gitignored)
├── scripts/
│   └── start_server.clj             # Standalone server startup
├── src/
│   ├── clay/
│   │   ├── client/
│   │   │   └── mouse_tracker.cljs   # Mouse tracker ClojureScript
│   │   ├── server/
│   │   │   └── mouse_server.clj     # WebSocket server
│   │   ├── schema.cljc              # Shared schemas
│   │   ├── color.cljc               # Color utilities
│   │   ├── theme.cljc               # Theming system
│   │   └── dsl/                     # DSL utilities
│   └── demo/                        # Demo code
├── bb.edn                           # Babashka configuration
├── deps.edn                         # Clojure dependencies
├── deps.local.edn.template          # Personal config template
├── shadow-cljs.edn                  # Shadow-CLJS configuration
└── package.json                     # npm dependencies
```

## Configuration Files

### shadow-cljs.edn

Configured with:
- **deps.edn integration**: Uses `:dev` alias from deps.edn
- **Output directory**: `resources/public/mouse-tracker/js/`
- **Dev server**: Port 8081 for mouse-tracker
- **Hot reload**: Automatic code reload on save

### deps.edn

Main dependency file with aliases:
- `:dev` - Development tools (nREPL, CIDER)
- `:test` - Testing tools
- `:nrepl` - nREPL server configuration
- `:build` - Build tools

### deps.local.edn (personal, gitignored)

Copy from `deps.local.edn.template` to customize:
- Default aliases to load
- Editor preferences (--cider-nrepl, --emacs)
- Shadow-CLJS auto-start configuration
- Portal integration
- Custom nREPL port

## Available Builds

### Mouse Tracker (`:mouse-tracker`)

Real-time mouse coordinate tracking with WebSocket backend.

- **Build ID**: `:mouse-tracker`
- **Output**: `resources/public/mouse-tracker/js/`
- **Dev Server**: http://localhost:8081
- **Entry Point**: `clay.client.mouse-tracker/init!`
- **Backend**: WebSocket server on port 9090

**Features:**
- Missionary reactive flows for state management
- Throttled network updates (50ms)
- Real-time UI updates
- WebSocket with Transit serialization

### Canvas2D Test (Standalone)

Vanilla JavaScript Canvas2D renderer for visual testing.

- **File**: `resources/public/canvas2d-test.html`
- **Dev Server**: http://localhost:8080/canvas2d-test.html
- **Technology**: Pure JavaScript (no ClojureScript)

**Features:**
- Auto-reload render commands from JSON
- Keyboard shortcuts (r, a, c)
- Device pixel ratio scaling
- Supports rectangles, borders, text, images, clipping

## Development Scenarios

### 1. Full-Stack Development (Backend + Frontend)

```bash
# Terminal 1: Start REPL
bin/launchpad --cider-nrepl

# In REPL:
(start)

# Terminal 2: Watch ClojureScript
npm run watch

# Browser: http://localhost:8081
```

### 2. Frontend Only (ClojureScript)

Configure `deps.local.edn`:
```clojure
{:launchpad/aliases [:dev]
 :launchpad/main-opts ["--cider-nrepl"]}
```

```bash
npm run watch
# Browser: http://localhost:8081
```

### 3. Backend Only (Clojure)

```bash
bin/launchpad --cider-nrepl

# In REPL:
(start)    ; WebSocket server ready on port 9090
```

### 4. Production Build

```bash
npm run release

# Optimized output in resources/public/mouse-tracker/js/
```

## Port Configuration

| Service | Port | Description |
|---------|------|-------------|
| Shadow-CLJS (mouse-tracker) | 8081 | ClojureScript dev server |
| Shadow-CLJS (root) | 8080 | Static files & Canvas2D test |
| WebSocket Backend | 9090 | Real-time mouse coordinates |
| nREPL | Dynamic | Assigned by Launchpad (or 7888 if configured) |

## Troubleshooting

### Shadow-CLJS build fails

```bash
# Clean and rebuild
npm run clean
rm -rf node_modules package-lock.json
npm install
npm run watch
```

### WebSocket connection fails

1. Ensure backend server is running:
```clojure
; In REPL:
(start)
```

2. Check WebSocket URL in [mouse_tracker.cljs](src/clay/client/mouse_tracker.cljs:158):
```clojure
(connect-websocket! "ws://localhost:9090")
```

### Port conflicts

Check if ports are in use:
```bash
lsof -i :8080  # Canvas2D
lsof -i :8081  # Mouse Tracker
lsof -i :9090  # WebSocket Backend
```

Kill processes or configure different ports in:
- `shadow-cljs.edn` (`:dev-http`)
- `src/clay/client/mouse_tracker.cljs` (WebSocket URL)

### Launchpad not found

Install Babashka:
```bash
# macOS
brew install babashka

# Linux
bash < <(curl -s https://raw.githubusercontent.com/babashka/babashka/master/install)
```

## Testing

```bash
# Run tests (configured in deps.edn :test alias)
clojure -M:test
```

## Build for Production

```bash
# Create optimized ClojureScript build
npm run release

# Output: resources/public/mouse-tracker/js/main.js (minified)
```

## Integration with Launchpad

Launchpad provides:
- ✅ Unified nREPL startup
- ✅ Editor integration (Calva, CIDER)
- ✅ Personal configuration via `deps.local.edn`
- ⚠️ Shadow-CLJS integration (manual via npm scripts recommended)

For Shadow-CLJS, use npm scripts instead of Launchpad's built-in support:
```bash
# In one terminal:
bin/launchpad --cider-nrepl

# In another:
npm run watch
```

## Resources

- [Launchpad Documentation](https://github.com/lambdaisland/launchpad)
- [Shadow-CLJS User Guide](https://shadow-cljs.github.io/docs/UsersGuide.html)
- [Missionary Documentation](https://github.com/leonoel/missionary)
- [Portal Data Inspector](https://github.com/djblue/portal)
- [Calva (VS Code)](https://calva.io/)

## License

[Your License Here]
