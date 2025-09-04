# End-to-End Deployment Guide: Kotlin App on Cloud Run with Supabase & GitHub Actions

This guide provides a complete, step-by-step process for deploying your application. The goal is to create a CI/CD 
pipeline that automatically builds your application on pushes to main and deploys it to Google Cloud Run when you 
create a version tag.

> **Important**: Due to complexities with cross-platform builds, the recommended way to create your initial image 
> is to use the CI pipeline itself, not your local machine.

## Core Architecture

* Application Hosting: Google Cloud Run (Serverless)
* Database: Supabase (Serverless PostgreSQL)
* Container Registry: Google Artifact Registry
* CI/CD Pipeline: GitHub Actions
* Secrets Management: Google Secret Manager
* Cloud Authentication: Google Workload Identity Federation

## Phase 1: One-Time Infrastructure Setup

You only need to perform these steps once for your project.

### 1. Set up Supabase

1. Go to Supabase, create a new project. 
2. Navigate to Project Settings > Database. 
3. Under Connection string, copy the JDBC.

### 2. Configure Google Cloud Project

1. Enable APIs:
   ```
   gcloud services enable run.googleapis.com secretmanager.googleapis.com artifactregistry.googleapis.com iam.googleapis.com storage.googleapis.com
   ```
2. Store Secrets:
   1. Database URL (from Supabase):
   ```
   echo -n "jdbc:postgresql://aws-1-eu-west-3.pooler.supabase.com:5432/postgres?user=[YOUR-USER]&password=[YOUR-PASSWORD]" | gcloud secrets create db-connection-string --data-file=-
   ```
   2. Other Application Secrets: (Use -n to avoid adding extra newline characters)
   ```
   echo -n "your-google-client-id" | gcloud secrets create google-client-id --data-file=-
   echo -n "your-google-client-secret" | gcloud secrets create google-client-secret --data-file=-
   echo -n "your-super-secret-crypto-key" | gcloud secrets create crypto-key --data-file=-
   echo -n "your-super-secret-crypto-salt" | gcloud secrets create crypto-salt --data-file=-
   echo -n "your-db-user" | gcloud secrets create db-user --data-file=-
   echo -n "your-db-password" | gcloud secrets create db-password --data-file=-
   ```
3. Create Artifact Registry:
    ```
    gcloud artifacts repositories create my-app-repo --repository-format=docker --location=europe-west1
    ```

### 3. Set up Secure Connection (GitHub <-> GCP)

Follow the official Google Cloud guide to set up Workload Identity Federation. This is a secure, keyless method for 
authentication. During the setup, you will:

1. Create a Service Account for GitHub Actions:
   ```
   gcloud iam service-accounts create github-actions-sa --display-name="GitHub Actions Service Account"
   ```
2. Grant Permissions to the Service Account:
   ```
   PROJECT_ID=$(gcloud config get-value project)

   # Allow pushing images to Artifact Registry
   gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:github-actions-sa@$PROJECT_ID.iam.gserviceaccount.com" --role="roles/artifactregistry.writer"

   # Allow deploying and managing Cloud Run services
   gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:github-actions-sa@$PROJECT_ID.iam.gserviceaccount.com" --role="roles/run.admin"

   # Allow it to act as a user of other service accounts (needed for deployments)
   gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:github-actions-sa@$PROJECT_ID.iam.gserviceaccount.com" --role="roles/iam.serviceAccountUser"
   ```
3. Create a Workload Identity Pool and Provider, linking it to your specific GitHub repository.
   ```
   PROJECT_ID=$(gcloud config get-value project)
   OWNER=[YOUR-GITHUB-ORG-OR-USERNAME]
   REPO_NAME=[YOUR-REPO-NAME]
   
   # Create the Identity Pool
   gcloud iam workload-identity-pools create "github-pool" --project="$PROJECT_ID" --location="global" --display-name="GitHub Actions Pool"

   # Create the Identity Provider within the pool
   gcloud iam workload-identity-pools providers create-oidc "github-provider" \
      --project="$PROJECT_ID" \
      --location="global" \
      --workload-identity-pool="github-pool" \
      --display-name="GitHub Actions Provider" \
      --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
      --issuer-uri="https://token.actions.githubusercontent.com" \
      --attribute-condition="assertion.repository=='$OWNER/$REPO_NAME'"
   ```
4. Allow GitHub to Impersonate the Service Account: This final step links your GitHub repository to the GCP service account.
   ```
   PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format='get(projectNumber)')
   OWNER=[YOUR-GITHUB-ORG-OR-USERNAME]
   REPO_NAME=[YOUR-REPO-NAME]

   gcloud iam service-accounts add-iam-policy-binding "github-actions-sa@$PROJECT_ID.iam.gserviceaccount.com" \
      --project="$PROJECT_ID" \
      --role="roles/iam.workloadIdentityUser" \
      --member="principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/attribute.repository/repo:$OWNER/$REPO_NAME"
   ```

> You will need the Workload Identity Provider string for your GitHub Actions workflow file. You can construct it from 
> the values above. It will look like: `projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github-pool/providers/github-provider`

### 4. Configure GitHub Actions Secrets

**Secret 1**:
* Name: `WIF_PROVIDER`
* Value: `projects/[PROJECT_NUMBER]/locations/global/workloadIdentityPools/github-pool/providers/github-provider` (Replace [PROJECT_NUMBER] with your actual GCP project number).

**Secret 2**:
* Name: `SERVICE_ACCOUNT`
* Value: `github-actions-sa@[PROJECT_ID].iam.gserviceaccount.com` (Replace [PROJECT_ID] with your actual GCP project ID).

### 5. Initial Cloud Run Deployment

#### Continuous integration setup

We will use the CI pipeline to perform the initial build, then deploy manually.

**Create ci.yaml and cd.yaml Workflows**:

* Create a `.github/workflows` directory in your project root.
* Create the `ci.yaml` and `cd.yaml` files inside it using the templates provided in the Appendix at the end of this guide.

> **Important**: Fill in the placeholder values at the top of the workflow files (PROJECT_ID, etc.).

**Run the CI Pipeline**:

* Commit and push the new `.github/workflows/ci.yaml` file to your main branch.
* Go to the "Actions" tab in your GitHub repository.
* You will see the "Build and Push Container" workflow running. Wait for it to complete successfully. 
* Once it's green, a container image tagged with your latest commit SHA has been pushed to Artifact Registry.

#### Continuous deployment setup

**Deploy manually (First Time Only)**:

* Find the Image URI: Go to your Artifact Registry in the GCP console, find your new image, and copy its full URI. It will look like: `europe-west1-docker.pkg.dev/YOUR_PROJECT_ID/my-app-repo/partners-connect-server:latest-commit-sha`
* Update `service.yaml`: Paste this full image URI into the image: field in your `service.yaml` file.
* Deploy the Service:
  ```
  gcloud run services replace service.yaml --region europe-west1
  ```

This first deployment will fail with a Permission denied error on secrets. This is expected. Proceed to the next step.

**Grant Permissions to the Cloud Run Service**:

1. Get Service Account Email: The error from the previous step will tell you the exact service account email used by Cloud Run.
2. Grant Access:
    ```
    SERVICE_ACCOUNT_EMAIL="[EMAIL_FROM_ERROR]"
    PROJECT_ID=$(gcloud config get-value project)
    gcloud secrets add-iam-policy-binding db-connection-string --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/secretmanager.secretAccessor"
    gcloud secrets add-iam-policy-binding google-client-id --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/secretmanager.secretAccessor"
    gcloud secrets add-iam-policy-binding google-client-secret --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/secretmanager.secretAccessor"
    gcloud secrets add-iam-policy-binding crypto-key --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/secretmanager.secretAccessor"
    gcloud secrets add-iam-policy-binding crypto-salt --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/secretmanager.secretAccessor"
    gcloud secrets add-iam-policy-binding db-user --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/secretmanager.secretAccessor"
    gcloud secrets add-iam-policy-binding db-password --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/secretmanager.secretAccessor"
    gcloud projects add-iam-policy-binding $PROJECT_ID --member="serviceAccount:$SERVICE_ACCOUNT_EMAIL" --role="roles/storage.objectAdmin"
    ```

3. Re-run the Deployment: This final run will succeed.
    ```
    gcloud run services replace service.yaml --region europe-west1
    ```

#### Ongoing CI/CD workflow

Your pipeline is now fully configured.

1. Code Changes (CI): Pushing to main builds and pushes a new container image.
2. Release (CD): Creating and pushing a git tag deploys the latest image to Cloud Run.