# Mouse Tracker - Missionary Network Bridge Test

This is a demonstration of streaming mouse coordinates from a ClojureScript frontend to a Clojure backend using Missionary and WebSockets.

## Architecture

- **Frontend**: ClojureScript running in the browser, using Missionary to create a reactive stream of mouse movements
- **Network**: WebSocket connection for real-time bidirectional communication
- **Backend**: Clojure server using http-kit to receive and print coordinates

## Setup & Running

### Terminal 1: Start the Backend Server

```bash
cd clay-clj
clojure -M:dev -m nrepl.cmdline --middleware "[cider.nrepl/cider-middleware]"
```

Then in your REPL:
```clojure
(require '[clay.server.mouse-server :as server])
(server/start-server!)
;; You should see: "Server started at ws://localhost:9090"
```

Or use the user namespace:
```clojure
(start)  ;; starts the server
```

### Terminal 2: Start the Frontend Dev Server

```bash
cd clay-clj
npx shadow-cljs watch mouse-tracker
```

Wait for the build to complete. You should see:
```
[:mouse-tracker] Build completed.
```

### Terminal 3: Open the Browser

Once shadow-cljs is running, open your browser to:

**http://localhost:8081**

## Testing the Network Bridge

1. Make sure the backend server is running (you should see "Server started at ws://localhost:9090" in the REPL)
2. Make sure shadow-cljs is watching and the build is complete
3. Open http://localhost:8081 in your browser
4. Move your mouse around the page
5. **Watch your REPL/backend console** - you should see mouse coordinates being printed:

```
Client connected. Total clients: 1
Mouse: x=234 y=456
Mouse: x=235 y=460
Mouse: x=238 y=465
...
```

## How It Works

### Frontend (mouse_tracker.cljs)

1. Creates a Missionary flow using `m/observe` that listens to `mousemove` events
2. Throttles the stream to emit at most once every 50ms using Missionary's `m/sample`
3. Connects to the WebSocket server
4. Sends each coordinate pair as JSON over the WebSocket

### Backend (mouse_server.clj)

1. Sets up an http-kit WebSocket server on port 9090
2. Tracks connected clients
3. Receives JSON messages and parses them
4. Prints the x,y coordinates to the console

## Code Locations

- Backend server: [src/clay/server/mouse_server.clj](src/clay/server/mouse_server.clj)
- Frontend tracker: [src/clay/client/mouse_tracker.cljs](src/clay/client/mouse_tracker.cljs)
- HTML page: [mouse-tracker/index.html](mouse-tracker/index.html)
- Shadow-cljs config: [shadow-cljs.edn](shadow-cljs.edn)

## Stopping

### Stop the Backend
In your REPL:
```clojure
(server/stop-server!)
;; or
(stop)
```

### Stop the Frontend
Press `Ctrl+C` in the terminal running shadow-cljs

## Troubleshooting

**"WebSocket connection failed"**
- Make sure the backend server is running on port 9090
- Check that nothing else is using port 9090

**"No mouse coordinates appearing"**
- Check browser console for errors (F12 → Console)
- Make sure the WebSocket shows as connected (green status)
- Verify the backend REPL is actually running the server

**"Build not found"**
- Make sure you ran `npx shadow-cljs watch mouse-tracker` (not `canvas2d-renderer`)
