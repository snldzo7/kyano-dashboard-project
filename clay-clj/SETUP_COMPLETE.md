# Mouse Tracker - Fully Dynamic ClojureScript UI with Missionary

## System Overview

✅ **Fully working network bridge** streaming mouse coordinates from ClojureScript frontend to Clojure backend using Missionary and WebSockets.

✅ **100% ClojureScript-controlled UI** - No hardcoded HTML, everything built dynamically from CLJS.

## Architecture

```
Browser (ClojureScript)
    ↓ Missionary Flow (m/observe)
    ↓ Throttled with m/sample (50ms)
    ↓ WebSocket Client
    ↓
WebSocket (ws://localhost:9090)
    ↓
Backend (Clojure)
    ↓ http-kit WebSocket Server
    ↓ Prints coordinates to console
```

## What Was Built

### 1. Minimal HTML Shell
[mouse-tracker/index.html](mouse-tracker/index.html) - Just a `<div id="app"></div>` container

### 2. ClojureScript Frontend
[src/clay/client/mouse_tracker.cljs](src/clay/client/mouse_tracker.cljs)
- DOM manipulation from scratch (no React/Reagent)
- CSS injection
- UI building with helper functions
- Missionary flow for mouse events
- WebSocket client
- Real-time coordinate display

### 3. Clojure Backend
[src/clay/server/mouse_server.clj](src/clay/server/mouse_server.clj)
- http-kit WebSocket server
- JSON parsing
- Client tracking
- Console logging

## Running the System

### Terminal 1: Backend Server
```bash
cd clay-clj
clojure -M:dev scripts/start_server.clj
```

Output:
```
Starting WebSocket server on port 9090...
Server started at ws://localhost:9090
Backend ready - Press Ctrl+C to stop
```

### Terminal 2: Frontend Dev Server
```bash
cd clay-clj
npx shadow-cljs watch mouse-tracker
```

Output:
```
shadow-cljs - HTTP server available at http://localhost:8084
[:mouse-tracker] Build completed.
```

### Open Browser
Navigate to: **http://localhost:8084/**

## What You'll See

### In the Browser
- Beautiful gradient purple background
- Mouse Tracker title
- Live coordinates display updating in real-time
- Connection status indicator (green when connected)
- All UI elements created dynamically from ClojureScript

### In the Backend Console
```
Client connected. Total clients: 1
Mouse: x=234 y=456
Mouse: x=235 y=460
Mouse: x=238 y=465
...
```

## Key Features

✅ **Missionary Flow** - Reactive stream of mouse events
✅ **Throttling** - Network sends throttled to 50ms using `m/sample`
✅ **UI Updates** - Instant visual feedback (no throttle)
✅ **Dynamic DOM** - All HTML/CSS generated from ClojureScript
✅ **WebSocket Bridge** - Real-time bidirectional communication
✅ **Hot Reload** - shadow-cljs auto-reloads on code changes

## Files Created

```
clay-clj/
├── mouse-tracker/
│   ├── index.html              # Minimal HTML shell
│   └── test-simple.html        # Vanilla JS test version
├── src/
│   ├── clay/
│   │   ├── client/
│   │   │   └── mouse_tracker.cljs    # ClojureScript frontend
│   │   └── server/
│   │       └── mouse_server.clj      # Clojure backend
├── scripts/
│   └── start_server.clj        # Server startup script
├── dev/
│   └── user.clj               # REPL convenience functions
└── shadow-cljs.edn            # Shadow-cljs configuration
```

## Code Highlights

### Dynamic UI Creation
```clojure
(defn build-ui! []
  (let [app (.getElementById js/document "app")
        container (create-element "div" :class "container")
        title (create-element "div" :class "title" :text "🖱️ Mouse Tracker")
        coords-display (create-element "div" :class "coords-display")
        ...]
    (append-child! app container)))
```

### Missionary Flow
```clojure
(defn create-mouse-flow []
  (m/observe
   (fn [emit!]
     (let [handler (fn [e]
                     (emit! {:x (.-clientX e)
                             :y (.-clientY e)}))]
       (.addEventListener js/document "mousemove" handler)
       (fn [] (.removeEventListener js/document "mousemove" handler))))))
```

### Throttling with Missionary
```clojure
(let [mouse-events (create-mouse-flow)
      throttled-flow (m/sample
                      (m/ap (loop [] (m/? (m/sleep 50)) (recur)))
                      mouse-events)]
  (throttled-flow process-mouse-coords! error-handler))
```

## Network Bridge Verified ✅

The system successfully streams mouse coordinates from the browser to the backend server, demonstrating a working network bridge using:

- **Frontend**: ClojureScript + Missionary
- **Transport**: WebSocket
- **Backend**: Clojure + http-kit
- **UI**: 100% dynamically generated from ClojureScript

Move your mouse on the page and watch the coordinates flow from browser → WebSocket → backend console!
