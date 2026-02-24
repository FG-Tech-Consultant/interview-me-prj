# Frontend Module

React + TypeScript frontend for Live Resume & Career Copilot platform.

## Technology Stack

- **React 18**
- **TypeScript**
- **Vite** (build tool)
- **TanStack Query** (data fetching)
- **Material-UI (MUI)** (component library)
- **React Router** (routing)
- **Axios** (HTTP client)

## Module Structure

```
frontend/
├── src/
│   ├── api/           # API client and auth functions
│   ├── components/    # Reusable React components
│   ├── pages/         # Page components
│   ├── hooks/         # Custom React hooks
│   ├── types/         # TypeScript type definitions
│   ├── App.tsx        # Main app component
│   └── main.tsx       # Entry point
├── public/            # Static assets
├── dist/              # Build output (auto-generated)
└── package.json
```

## Key Features

### Authentication Flow
- Register page with tenant creation
- Login page with JWT storage
- Protected routes with automatic redirect
- Token stored in localStorage
- Automatic 401 handling (logout)

### Data Fetching
- TanStack Query for server state management
- Automatic caching and refetching
- Optimistic updates
- Error handling

### UI Components
- Material-UI component library
- Responsive design
- Theme customization
- Form validation

## Development

```bash
# Install dependencies
npm install

# Run dev server (proxies API to localhost:8080)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint
npm run lint
```

## Building

The frontend is built as part of the main Gradle build:

```bash
# From project root
./gradlew frontend:npmBuild
```

The output (`dist/`) is automatically copied to `backend/src/main/resources/static/` and served by Spring Boot.

## Configuration

### Vite Config (`vite.config.ts`)

```typescript
export default defineConfig({
  plugins: [react()],
  base: '/',
  build: {
    outDir: 'dist'
  },
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
```

### API Client

Base URL is `/api` for same-origin deployment. In development, Vite proxies to `localhost:8080`.

## Pages

- **Login** (`/login`) - User authentication
- **Register** (`/register`) - New user and tenant registration
- **Dashboard** (`/dashboard`) - Protected user dashboard

## Dependencies

Main dependencies:
- `react` - UI library
- `react-router-dom` - Routing
- `@tanstack/react-query` - Data fetching
- `@mui/material` - Component library
- `axios` - HTTP client

Dev dependencies:
- `vite` - Build tool
- `typescript` - Type checking
- `@vitejs/plugin-react` - React support

See `package.json` for full dependency list.
