#!/bin/bash

COMPONENT=$1

if [[ "$COMPONENT" =~ ^(all|client|server)$ ]]; then
  echo "Build request for component: $COMPONENT"
else
  echo "Unknown component: $COMPONENT"
  exit 1
fi

read -p "Are you sure you want to deploy? y/n: " USER_RESPONSE

if [ "$USER_RESPONSE" != "y" ]
then
  echo "Aborting deploy..."
  exit 0
fi

echo "Running build and deploy script..."

PROJECT_ID=$(gcloud config get-value project)
CONTEXT=$(kubectl config current-context)

# `EXPECTED_PROJECT_ID` and `EXPECTED_CONTEXT` should
# be set as env vars. Will be specific to where app should
# be deployed to per your setup

if [ "$PROJECT_ID" != "$EXPECTED_PROJECT_ID" ]
then
  echo "Gcloud project id is incorrect. Aborting build..."
  exit 1
fi

if [ "$CONTEXT" != "$EXPECTED_CONTEXT" ]
then
  echo "kubectl context is incorrect. Aborting build..."
  exit 1
fi

CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)

COMMIT_ID="$(git rev-parse --short=7 HEAD)"

echo "Checked out to $CURRENT_BRANCH"

echo "Commit ID is $COMMIT_ID"


########
# SERVER
########
if [[ "$COMPONENT" =~ ^(all|server)$ ]]; then
  echo "Building and pushing API..."
  gcloud builds submit --config api/cloudbuild.yaml api --substitutions=SHORT_SHA=$COMMIT_ID

  echo "prev output $?"

  if [ $? != 0 ]
  then
    echo "Failed to build and push server"
    exit 1
  fi

  echo "API built and pushed"
fi

########
# CLIENT
########
if [[ "$COMPONENT" =~ ^(all|client)$ ]]; then
  echo "Building and pushing client..."

  gcloud builds submit --config web/cloudbuild.yaml web --substitutions=SHORT_SHA=$COMMIT_ID

  echo "prev output $?"

  if [ $? != 0 ]
  then
    echo "Failed to build and push client"
    exit 1
  fi

  echo "Client built and pushed"
fi


###########
# K8s files
###########
rm -rf k8s-tmp || true
mkdir k8s-tmp

if [[ "$COMPONENT" =~ ^(all|server)$ ]]; then
  cp "k8s/server.yaml" k8s-tmp
  cp "k8s/ingress.yaml" k8s-tmp
  SERVER_FILE_PATH="k8s-tmp/server.yaml"
  SERVER_COMPONENT_NAME="video-api"
  sed -i.bak "s#gcr.io/${PROJECT_ID}/${SERVER_COMPONENT_NAME}:latest#gcr.io/${PROJECT_ID}/${SERVER_COMPONENT_NAME}:${COMMIT_ID}#" "$SERVER_FILE_PATH"
fi

if [[ "$COMPONENT" =~ ^(all|client)$ ]]; then
  cp "k8s/client.yaml" k8s-tmp
  cp "k8s/ingress.yaml" k8s-tmp
  CLIENT_FILE_PATH="k8s-tmp/client.yaml"
  CLIENT_COMPONENT_NAME="video-client"
  sed -i.bak "s#gcr.io/${PROJECT_ID}/${CLIENT_COMPONENT_NAME}:latest#gcr.io/${PROJECT_ID}/${CLIENT_COMPONENT_NAME}:${COMMIT_ID}#" "$CLIENT_FILE_PATH"
fi

rm k8s-tmp/*.bak

kubectl apply -f k8s-tmp

if [ $? != 0 ]
then
  echo "Failed to apply k8s manifests. Deploy failed..."
  exit 1
fi

echo "Successfully deployed"
