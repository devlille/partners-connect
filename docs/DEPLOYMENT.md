
# End-to-End Deployment Guide: Kotlin App on Clever Cloud with Supabase & GitHub Actions

This guide provides a complete, step-by-step process for deploying your application. The goal is to create a CI/CD 
pipeline that automatically builds your application on pushes to main and deploys it to Clever Cloud when you 
create a version tag.

> **Important**: Due to complexities with cross-platform builds, the recommended way to create your initial image 
> is to use the CI pipeline itself, not your local machine.

## Core Architecture

* Application Hosting: Clever Cloud (Docker)
* Database: Supabase (Serverless PostgreSQL)
* CI/CD Pipeline: GitHub Actions
* Secrets Management: GitHub Secrets (Clever Cloud API Token)

## Phase 1: One-Time Infrastructure Setup

You only need to perform these steps once for your project.

### 1. Set up Supabase

1. Go to Supabase, create a new project. 
2. Navigate to Project Settings > Database. 
3. Under Connection string, copy the JDBC.


### 2. Configure Clever Cloud Application & Secrets

1. Create a Clever Cloud account and application (choose Docker as the deployment type).
2. Note your Clever Cloud App ID (from the dashboard).
3. Generate a Clever Cloud API token (from your account settings) and add it as a GitHub secret named `CLEVERCLOUD_TOKEN`.
4. Add your Clever Cloud App ID as a GitHub secret named `CLEVERCLOUD_APP_ID`.
5. Migrate all secrets previously stored in Google Secret Manager to GitHub secrets. For each secret listed in your previous `service.yaml`, create a corresponding GitHub secret:
	- `PROJECT_ID`
	- `FRONTEND_BASE_URL`
	- `GOOGLE_CLIENT_ID`
	- `GOOGLE_CLIENT_SECRET`
	- `CRYPTO_KEY`
	- `CRYPTO_SALT`
	- `EXPOSED_DB_DRIVER`
	- `EXPOSED_DB_URL`
	- `EXPOSED_DB_USER`
	- `EXPOSED_DB_PASSWORD`
6. These secrets will be injected as environment variables into your Clever Cloud app after deployment by the CD workflow.
7. (Optional) You can also configure additional environment variables in the Clever Cloud dashboard or via the GitHub Action.


### 3. Initial Deployment

1. Push your code to the GitHub main branch to trigger the CI workflow (`ci.yaml`). This workflow:
	- Builds the Docker image for your server application.
	- Pushes the image to Google Artifact Registry using your GCP credentials.

2. Create and push a git tag (e.g., `git tag 1.0.0 && git push origin 1.0.0`) to trigger the CD workflow (`cd.yaml`). This workflow:
	- Authenticates to Google Cloud and pulls the Docker image from Artifact Registry.
	- Tags the image for Clever Cloud.
	- Deploys the image to Clever Cloud using the official GitHub Action.
	- Installs the Clever Cloud CLI (`clever-tools`) and logs in using your API token.
	- Dynamically fetches the Clever Cloud app's public base URL after deployment.
	- Updates the `SERVER_BASE_URL` environment variable in Clever Cloud with the actual deployed URL.

### 4. Ongoing CI/CD workflow

Your pipeline is now fully configured:

1. Code Changes (CI): Pushing to main builds and pushes a new Docker image to Google Artifact Registry.

2. Release (CD): Creating and pushing a git tag deploys the latest image to Clever Cloud and updates all required environment variables (including the base URL) automatically from GitHub secrets.

### 5. Troubleshooting & Notes

* Ensure your Dockerfile exposes the correct port (default for Ktor is 8080).
* Clever Cloud will automatically detect and run your Docker image.
* All secrets and sensitive configuration values are now managed as GitHub secrets and injected as Clever Cloud environment variables after deployment.
* The Clever Cloud app base URL is set dynamically after deployment using the CLI and GitHub Actions.
* You can set additional environment variables via the Clever Cloud dashboard or in the GitHub Action step.
* For more details, see: https://www.clever-cloud.com/doc/deploy/with-github-actions/