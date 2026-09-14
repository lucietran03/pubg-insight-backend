WebSocket connection to 'ws://localhost:5173/?token=5AMtbKc_VRGs' failed:
(anonymous) @ client:868
client:878 WebSocket connection to 'ws://localhost:5173/?token=5AMtbKc_VRGs' failed:
(anonymous) @ client:878
client:888 [vite] failed to connect to websocket.
your current setup:
(browser) localhost:5173/ <--[HTTP]--> localhost:5173/ (server)
(browser) localhost:5173/ <--[WebSocket (failing)]--> localhost:5173/ (server)
Check out your Vite / network configuration and https://vite.dev/config/server-options.html#server-hmr .
connect @ client:888
react-dom_client.js?v=7fa763d1:14334 Download the React DevTools for a better development experience: https://react.dev/link/react-devtools