# Kyano Dashboard Project

A React-based supply chain decision intelligence dashboard.

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Run development server:
   ```bash
   npm run dev
   ```

3. Build for production:
   ```bash
   npm run build
   ```

4. Preview production build:
   ```bash
   npm run preview
   ```

## Project Structure

```
kyano-dashboard-project/
├── src/
│   ├── KyanoDashboard.jsx  # Main dashboard component
│   ├── main.jsx            # React entry point
│   └── index.css           # Global styles with Tailwind
├── index.html              # HTML entry point
├── vite.config.js          # Vite configuration
├── tailwind.config.js      # Tailwind CSS configuration
├── postcss.config.js       # PostCSS configuration
└── package.json            # Dependencies and scripts
```

## Features

- OTIF Performance Timeline
- Decision History
- Decision Room (Collaborative)
- What-If Scenarios
- Retailer Collaboration
- Similar Cases Analysis
