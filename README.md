# AgriShare

AgriShare is a management application for farming and livestock operations.

It allows users to track inventory, production, investments, and profits in one place. The application also helps organize farming projects and keep a clear view of expenses and available resources.

AgriShare also makes it easier to find veterinarians and connect with a professional who fits the needs of the operation.

An artificial intelligence feature analyzes project information and provides recommendations for better budget management. These recommendations are intended to support decisions and do not replace professional advice.

## Project status

AgriShare is currently under development. Some features are still being built, and the displayed data may be used for application testing.

## Technologies

The interface is built with Next.js, React, TypeScript, and Tailwind CSS. The backend uses Spring Boot and PostgreSQL. Firebase is also used for some application services, while Genkit supports the artificial intelligence features.

## Running the project

From the `services/frontend` directory, install the dependencies with:

```bash
npm install
```

To start the interface in development mode:

```bash
npm run dev
```

The interface is then available at `http://localhost:9002`.

The backend and database can be started with Docker Compose from the `deployment/dev` directory:

```bash
docker compose up --build
```
