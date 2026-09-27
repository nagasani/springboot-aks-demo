# springboot-aks-demo

An intentionally small Java 17 / Spring Boot 4.1.1 service, unit/context tests, JaCoCo,
optional SonarQube Cloud CI, multi-stage Dockerfile and Helm chart. It is intended to
pair with the sibling **aks-infra-demo** repository.

## 1. Run locally

Requires Java 17+ and Maven 3.9+.

```bash
mvn -B clean verify
mvn spring-boot:run
curl http://localhost:8080/api/hello
# {"message":"Hello from AKS!"}
curl http://localhost:8080/actuator/health/readiness
```

## 2. Create this repository on GitHub

Install [GitHub CLI](https://cli.github.com/), authenticate with `gh auth login`, then:

```bash
git init -b main
git add .
git commit -m "Initial Spring Boot AKS lab"
gh repo create springboot-aks-demo --public --source=. --remote=origin --push
```

For code quality, import your **public** repository into the SonarQube Cloud Free plan.
In GitHub repository settings, set secret `SONAR_TOKEN` and variables
`SONAR_ORGANIZATION` and `SONAR_PROJECT_KEY` to values shown by SonarQube Cloud.
The CI workflow runs tests/coverage on every PR and push to `main`; it will run
SonarQube analysis and wait for the quality gate **only after** these values are set.
Protect `main` and require the `tests-and-quality` status check after its first run.

## 3. Build and publish to your new ACR

First deploy the **aks-infra-demo** Terraform project and attach AKS to ACR. Then:

```bash
export ACR_NAME=<YOUR_GLOBALLY_UNIQUE_ACR_NAME>
export LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)
az acr login --name "$ACR_NAME"
docker build -t "$LOGIN_SERVER/springboot-aks-demo:v1" .
docker push "$LOGIN_SERVER/springboot-aks-demo:v1"
```

These commands use your local Azure login. The *optional* publish-image.yml workflow
supports passwordless GitHub OIDC for later automation; see the infra repo's OIDC.md.

## 4. Deploy with Helm, without a public load balancer

```bash
# az aks get-credentials ...  (follow sibling infra README first)
helm lint helm/springboot-aks-demo
helm upgrade --install hello helm/springboot-aks-demo \
  --namespace demo --create-namespace \
  --set image.repository="$LOGIN_SERVER/springboot-aks-demo" \
  --set image.tag=v1 --wait --timeout 5m
kubectl -n demo get deployments,pods,services
kubectl -n demo rollout status deployment/springboot-aks-demo
kubectl -n demo port-forward service/springboot-aks-demo 8080:80
# In another terminal: curl http://localhost:8080/api/hello
```

After testing, **destroy the Terraform infrastructure**, not merely the Helm release,
to stop AKS/VM/registry charges. This lab is non-production: one small deployment,
no application ingress or public app endpoint, and no remote Terraform backend.
